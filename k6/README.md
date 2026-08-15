# 매출 등록(락) k6 부하 테스트

`POST /api/sales`(매출 전표 등록)가 같은 규격(item_spec)에 동시에 여러 요청이
몰려도 오버셀 없이 처리되는지를 실제 HTTP 계층에서 검증한다.

## 전제

- 로컬 백엔드(`./gradlew bootRun`)와 MySQL(`erp_db`)이 떠 있어야 한다.
- 로그인 계정은 기본값으로 `owner@myerp.com` / `password123`을 쓴다. 다르면
  `-e TEST_EMAIL=... -e TEST_PASSWORD=...`로 오버라이드.
- 서버 주소가 `http://localhost:8080`이 아니면 `-e BASE_URL=...`로 오버라이드.
- [k6](https://k6.io) 설치 필요.

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

## 실행

```bash
k6 run k6/01-smoke.js
k6 run k6/02-load.js
k6 run k6/03-stress.js
```

순서대로 실행 권장 — smoke가 실패하면(락 정합성 자체가 깨지면) load/stress
결과는 의미가 없다.

## 결과 읽는 법

- `sale_success` / `sale_insufficient_stock` / `sale_lock_conflict`: 정상적인
  비즈니스 분기. 몇 건이든 문제 아님.
- `sale_unexpected_error`: 409/201이 아닌 응답(주로 500). **smoke/load에서는
  0이어야 정상.** stress에서는 특정 VU 수를 넘는 순간부터 늘어날 걸로 예상되고,
  그 지점이 바로 "한계"다.
- `sale_connection_error`: 응답 자체를 못 받은 경우(연결 실패/타임아웃).
  stress에서 VU가 10(HikariCP 풀 크기)을 크게 넘는 구간부터 나타나기 시작하면
  풀 크기가 병목이라는 뜻.
- 마지막 `[stress] 초기재고=... 최종재고=...` 로그와 재고 음수 체크
  (`최종 재고는 절대 음수가 될 수 없음`)가 실패하면, 그건 곧바로 락에 실제
  버그가 있다는 뜻이므로 다른 무엇보다 먼저 봐야 한다.
