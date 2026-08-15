/**
 * stress — "한계 상황"
 *
 * 목적: 순수하게 동시접속 자체가 이 서버 구성을 어디까지 버티게 하는지 탐색.
 * HikariCP 기본 풀(10)을 지나는 지점에서 큐잉이, 그 이상에서 커넥션 타임아웃/5xx가
 * 나타날 것으로 예상한다. 재고는 사실상 무제한(100000)으로 잡아서 "재고 소진"이
 * 아니라 "동시접속 자체의 한계"만 보이게 한다.
 *
 * 임계값(thresholds)을 강하게 걸지 않는다 — 이 테스트의 목적은 통과/실패 판정이
 * 아니라 어디서 꺾이는지를 관찰하는 것. k6 요약 출력에서 각 스테이지 구간별
 * http_req_duration/http_req_failed 추이와 sale_connection_error 카운트를 보면 된다.
 *
 * 실행: k6 run k6/03-stress.js
 * 주의: 로컬 개발 서버·DB에 실제로 부하가 걸린다. 운영 중인 다른 작업이 있다면
 * 실행 타이밍을 조절할 것.
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const QUANTITY = 1;
const INITIAL_STOCK = 100000;

const counters = {
  success: new Counter('sale_success'),
  insufficientStock: new Counter('sale_insufficient_stock'),
  lockConflict: new Counter('sale_lock_conflict'),
  connectionError: new Counter('sale_connection_error'),
  unexpectedError: new Counter('sale_unexpected_error'),
};

export const options = {
  scenarios: {
    ramp_to_breakpoint: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 10 },  // HikariCP 기본 풀 크기(10) 지점
        { duration: '20s', target: 10 },  // 그 경계에서 잠깐 유지하며 관찰
        { duration: '20s', target: 30 },
        { duration: '20s', target: 60 },
        { duration: '20s', target: 100 },
        { duration: '20s', target: 0 },   // 정리
      ],
    },
  },
};

export function setup() {
  const token = login(EMAIL, PASSWORD);
  const fixture = createFixture(token, INITIAL_STOCK);
  return { token, ...fixture };
}

export default function (data) {
  const body = JSON.stringify({
    partnerId: data.partnerId,
    companyInfoId: data.companyInfoId,
    saleDate: new Date().toISOString().slice(0, 10),
    items: [{ itemSpecId: data.itemSpecId, quantity: QUANTITY, unitPrice: 2000 }],
  });

  const res = http.post(`${BASE_URL}/api/sales`, body, authHeaders(data.token));
  classifyResponse(res, counters);

  check(res, {
    '요청이 완전히 끊기지 않음(응답 자체는 옴)': (r) => r.status !== 0,
  });
}

export function teardown(data) {
  const finalStock = fetchCurrentStock(data.token, data.itemId, data.itemSpecId);
  check(null, {
    '최종 재고는 절대 음수가 될 수 없음': () => finalStock !== null && finalStock >= 0,
  });
  console.log(`[stress] 초기재고=${INITIAL_STOCK}, 최종재고=${finalStock}`);
}
