# 매출 등록(락) k6 부하 테스트

`POST /api/sales`(매출 전표 등록)가 같은 규격(item_spec)에 동시에 여러 요청이
몰려도 오버셀 없이 처리되는지를 실제 HTTP 계층에서 검증한다.

## 전제

- 로컬 백엔드(`./gradlew bootRun`)와 MySQL(`erp_db`)이 떠 있어야 한다.
- 로그인 계정은 기본값으로 `owner@myerp.com` / `password123`을 쓴다. 다르면
  `-e TEST_EMAIL=... -e TEST_PASSWORD=...`로 오버라이드.
- 서버 주소가 `http://localhost:8080`이 아니면 `-e BASE_URL=...`로 오버라이드.
- [k6](https://k6.io) 설치 필요.
- 실행할 때 인터넷 연결 필요 — `handleSummary()`가 `jslib.k6.io`/`raw.githubusercontent.com`에서
  요약·HTML 리포트 렌더링 스크립트를 원격으로 불러온다(k6 표준 방식, 별도 설치 불필요).

## 중요한 한계

이 스크립트는 HTTP로만 때리기 때문에 **낙관적 락(`OptimisticLockSaleService`)만
검증한다.** `SaleController`가 `@Qualifier("optimisticLockSaleService")`로
고정되어 있어 비관적 락(`PessimisticLockSaleService`)은 REST API로 접근할 방법이
없다 — 그건 `SaleConcurrencyIntegrationTest`(JUnit, `ExecutorService`)의 몫으로
남아있다.

서버 설정은 명시적 오버라이드가 없어 Spring Boot 기본값을 그대로 쓴다:
HikariCP 커넥션 풀 10개, Tomcat 최대 스레드 200개. 아래 세 스크립트의 VU 수는
이 숫자를 기준으로 잡았다.

## 3단계 구성

| 파일 | 이름 | 목적 | VU | 재고 |
|---|---|---|---|---|
| `01-smoke.js` | 무리가 가지 않는 상황 | 락 로직 자체의 정합성(오버셀 안 남) | 3, 1회씩 | 3(일부러 부족) |
| `02-load.js` | 견딜 수 있는 상황 | 지속 부하에서 커넥션/스레드 안 새는지 | 8, 45초 유지 | 5000(넉넉) |
| `03-stress.js` | 한계 상황 | 순수 동시접속 한계점 탐색(0→100 램프업) | 0→100 | 100000(사실상 무제한) |

각 스크립트는 실행 시작 시(`setup()`) 전용 거래처/회사정보/카테고리/품목/규격을
새로 만들어 실데이터와 절대 섞이지 않는다. 끝나면(`teardown()`) 실제 재고를
다시 조회해 **음수가 아닌지**를 자동으로 확인한다.

## 원장(5단계) 도입 이후 추가된 변형 — `02b`/`03b`

`OptimisticLockSaleService.register()`는 재고 차감 뒤 같은 트랜잭션 안에서
`LedgerService.recordReceivableChange`를 호출해 `partner.receivable_balance`를
갱신한다. item_spec 쪽은 낙관적 락(대기 없이 재시도)이지만, partner 쪽은
`UPDATE ... WHERE id=?`로 진짜 DB 행 잠금을 건다.

`02-load.js`/`03-stress.js`는 전 VU가 거래처 1개를 공유하기 때문에, 이 partner
행 잠금 경합이 item_spec 낙관적 락 성능과 뒤섞여서 측정된다. `02b-load-multi-partner.js`/
`03b-stress-multi-partner.js`는 조건은 완전히 동일하되 VU마다 서로 다른 거래처를
써서 partner 잠금 경합을 제거한다 — item_spec 재고/조건은 그대로 공유되므로
그 변수는 고정된 채로, "원장 잠금이 얼마나 발목을 잡는지"만 분리해서 본다.

`01-smoke.js`는 원래 목적(락 정합성의 경계 케이스 확인)이 파트너 분산과 무관해서
변형을 만들지 않았다.

## 실행

```bash
k6 run k6/01-smoke.js
k6 run k6/02-load.js
k6 run k6/02b-load-multi-partner.js
k6 run k6/03-stress.js
k6 run k6/03b-stress-multi-partner.js
```

순서대로 실행 권장 — smoke가 실패하면(락 정합성 자체가 깨지면) load/stress
결과는 의미가 없다. `02`/`02b`, `03`/`03b`는 같은 시간대에 동시 실행하지 말 것
(같은 서버·DB 상태를 두고 비교해야 결과가 의미 있다).

**비교하는 법**: `02`와 `02b`(또는 `03`과 `03b`)의 처리량/`http_req_duration`
p95를 나란히 보면 된다. `02b`/`03b`가 뚜렷하게 더 빠르거나 더 높은 VU까지
버틴다면, partner 원장 잠금이 실제 병목이라는 근거다 — 그 경우 "같은 거래처에
매출이 동시다발로 몰리는 게 실무에서 흔한 시나리오인지"부터 다시 따져볼
필요가 있다. 차이가 거의 없다면 병목은 원장이 아니라 HikariCP 풀/Tomcat
스레드 같은 다른 공용 자원이라는 뜻이다.

## 결과 읽는 법

지표는 전부 `helpers.classifyResponse()` 한 곳에서 채운다(스크립트마다 이름이
어긋나지 않게). 응답 하나마다 `sale_status_codes`(상태코드별 카운트)는 항상
채워지고, `sale_response_time`(Trend, `http_req_duration`과 별개로 이 도메인
전용)은 **연결 실패(status 0)를 제외한** 응답에만 기록한다 — 연결 자체가 안
된 요청의 duration은 "서버가 얼마나 빨리 응답했는가"와 무관해서 섞으면 왜곡됨.
나머지는 분류에 따라 갈린다:

- `sale_success_rate` / `sale_insufficient_stock_rate` / `sale_lock_conflict_rate`:
  정상적인 비즈니스 분기 비율(201 / 409-재고부족 / 409-락충돌). 몇 %든 그 자체로
  문제는 아님 — smoke는 재고를 일부러 부족하게 잡아서 insufficient_stock이
  나오는 게 오히려 의도된 결과다.
- `sale_client_error_rate`: 400(검증 실패)/404(존재하지 않는 리소스) 비율.
  **0이어야 정상** — 여기 잡히면 서버가 아니라 **스크립트가 보낸 요청 자체가
  잘못됐다**는 뜻이다(서버 결함인 `sale_server_error_rate`와 반드시 구분해서 볼 것).
- `sale_server_error_rate`: 5xx 비율. **smoke/load/02b/03b 전부 0이어야 정상.**
  0이 아니면 실제 서버 결함(대개 데드락)이므로 최우선으로 봐야 한다.
- `sale_connection_error_rate`: 응답 자체를 못 받은 비율(연결 실패/타임아웃).
  stress에서 VU가 10(HikariCP 풀 크기)을 크게 넘는 구간부터 올라가기 시작하면
  풀 크기가 병목이라는 뜻. `sale_connection_error_codes`(`{error_code, error}`
  태그)로 원인까지 갈라볼 수 있다 — 타임아웃(느려지다가 못 버팀, 큐잉 문제)과
  연결거부(리스너/OS 레벨에서 아예 안 받아줌)는 서로 다른 결론으로 이어진다.
- `sale_error_messages`: `sale_client_error_rate`/`sale_server_error_rate`가
  0이 아닐 때 원인을 메시지별로 쪼개서 보여준다(`{message: "...", status: "500"}`
  태그). 500 하나로 뭉뚱그리지 않고 "어떤 에러가 몇 건인지"를 바로 구분할 수
  있다 — 데드락처럼 여러 원인이 섞일 수 있는 5xx를 진단할 때 특히 필요.
- `sale_auth_error_count`: 401/403이 하나라도 찍히면 테스트 자체가 잘못된 것
  (토큰 만료, 계정 오류 등) — 서버 결함이 아니라 테스트 셋업 문제라는 신호.
- 마지막 `[stress] 초기재고=... 최종재고=...` 로그와 재고 음수 체크
  (`최종 재고는 절대 음수가 될 수 없음`)가 실패하면, 그건 곧바로 락에 실제
  버그가 있다는 뜻이므로 다른 무엇보다 먼저 봐야 한다.

## HTML 리포트

각 스크립트는 실행이 끝나면 터미널 요약(기존과 동일)에 더해 `k6/reports/`
아래에 그 스크립트 이름으로 HTML 리포트를 남긴다(`k6-reporter` 사용). 예:
`k6 run k6/01-smoke.js` → `k6/reports/01-smoke.html`.

`reports/` 디렉터리는 실행할 때마다 새로 생기는 산출물이라 `.gitignore`에
등록되어 있다 — 커밋되지 않는다. 브라우저로 열어서 스테이지별 그래프를
바로 볼 수 있고, `02`/`02b`(또는 `03`/`03b`) 결과를 나란히 열어두면
비교하기 쉽다.
