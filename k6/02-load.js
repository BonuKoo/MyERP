/**
 * load — "견딜 수 있는 상황"
 *
 * 목적: 지속적인 동시 요청 아래서 커넥션/스레드가 새거나 degrade 되지 않는지 확인.
 * VU 수는 HikariCP 기본 풀 크기(10, 명시적 설정 없어 Spring Boot 기본값)보다
 * 약간 낮은 8로 잡는다 — 풀 큐잉 없이 버틸 거라고 기대되는 지점.
 * 재고는 넉넉히(5000) 잡아서 "재고 부족"이 아니라 "오래 두들겨도 서버가 안 죽는지"에
 * 집중한다 (락 경합 자체의 정합성은 01-smoke.js가 담당).
 *
 * 실행: k6 run k6/02-load.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse, buildHandleSummary } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const QUANTITY = 1;
const INITIAL_STOCK = 5000;

export const options = {
  scenarios: {
    sustained_load: {
      executor: 'constant-vus',
      vus: 8,
      duration: '45s',
    },
  },
  thresholds: {
    // 재고 소진에 의한 409는 이 테스트에선 거의 안 나올 것으로 예상(재고 넉넉).
    // 진짜 서버 결함(5xx, 커넥션 실패)과 스크립트 결함(400/404)만 실패로 간주한다.
    sale_client_error_rate: ['rate==0'],
    sale_server_error_rate: ['rate==0'],
    sale_connection_error_rate: ['rate==0'],
    http_req_duration: ['p(95)<2000'],
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
  classifyResponse(res);

  check(res, {
    '201 또는 409만 정상': (r) => r.status === 201 || r.status === 409,
  });

  sleep(Math.random() * 0.3); // 완전 락스텝 방지 — 사람이 연속 클릭하는 정도의 텀
}

export function teardown(data) {
  const finalStock = fetchCurrentStock(data.token, data.itemId, data.itemSpecId);
  check(null, {
    '최종 재고는 절대 음수가 될 수 없음': () => finalStock !== null && finalStock >= 0,
  });
  console.log(`[load] 초기재고=${INITIAL_STOCK}, 최종재고=${finalStock}`);
}

export const handleSummary = buildHandleSummary('02-load');
