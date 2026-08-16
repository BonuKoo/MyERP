/**
 * smoke — "무리가 가지 않는 상황"
 *
 * 목적: 락 로직 자체의 정합성 확인. 소규모 사업장 실사용 최상단 수준의 인원(3명)이
 * 재고가 극도로 빠듯한 상황에서 동시에 매출을 등록했을 때, 오버셀 없이 정확히
 * 처리되는지를 본다. 재고를 일부러 부족하게 잡아(3), 3 VU가 각 2개씩 사려고 하면
 * 산술적으로 최대 1명만 성공할 수 있는 경계 상황을 재현한다.
 *
 * 실행: k6 run k6/01-smoke.js
 *       (BASE_URL, TEST_EMAIL, TEST_PASSWORD 환경변수로 오버라이드 가능)
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse, buildHandleSummary } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const QUANTITY = 2;
const INITIAL_STOCK = 3; // 3 VU * 2개 = 수요 6, 공급 3 → 최대 1명만 성공 가능

const counters = {
  success: new Counter('sale_success'),
  insufficientStock: new Counter('sale_insufficient_stock'),
  lockConflict: new Counter('sale_lock_conflict'),
  connectionError: new Counter('sale_connection_error'),
  unexpectedError: new Counter('sale_unexpected_error'),
};

export const options = {
  scenarios: {
    smoke: {
      executor: 'per-vu-iterations',
      vus: 3,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    sale_unexpected_error: ['count==0'],
    sale_connection_error: ['count==0'],
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
    '201(성공) 또는 409(재고부족/락충돌)만 정상': (r) => r.status === 201 || r.status === 409,
  });
}

export function teardown(data) {
  const finalStock = fetchCurrentStock(data.token, data.itemId, data.itemSpecId);
  check(null, {
    '최종 재고는 절대 음수가 될 수 없음': () => finalStock !== null && finalStock >= 0,
  });
  console.log(`[smoke] 초기재고=${INITIAL_STOCK}, 최종재고=${finalStock}`);
}

export const handleSummary = buildHandleSummary('01-smoke');
