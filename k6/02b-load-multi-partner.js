/**
 * load-multi-partner — "견딜 수 있는 상황, 원장 잠금 분리 버전"
 *
 * 02-load.js와 완전히 동일한 조건(VU 8, 45초, 재고 5000)이되, 딱 하나만 다르다 —
 * 전 VU가 거래처 1개를 공유하는 대신, VU마다 서로 다른 거래처를 쓴다.
 *
 * 왜 필요한가: 5단계(원장) 도입 이후 매출 등록은 item_spec 낙관적 락 재시도뿐
 * 아니라, 같은 트랜잭션 안에서 partner 행에도 UPDATE(배타적 행 잠금)를 건다.
 * 02-load.js처럼 거래처를 공유하면 이 두 종류의 경합이 뒤섞여서 측정된다.
 * 거래처를 분산해 원장 잠금 경합을 제거하면 item_spec 낙관적 락 자체의 성능만
 * 남는다 — 02-load.js와 이 스크립트의 처리량/p95 지연시간 차이가 곧 "원장 잠금이
 * 실제로 얼마나 발목을 잡는가"의 정량적 증거다.
 *
 * 실행: k6 run k6/02b-load-multi-partner.js
 *       02-load.js와 같은 시간대에 실행하지 말 것(같은 서버/DB를 두고 비교해야 함).
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse, buildHandleSummary, sampleServerMetrics } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const VUS = 8;
const QUANTITY = 1;
const INITIAL_STOCK = 5000;

export const options = {
  scenarios: {
    sustained_load_multi_partner: {
      executor: 'constant-vus',
      vus: VUS,
      duration: '45s',
    },
    // 02-load.js와 동일 조건으로 비교해야 하므로 서버 지표 수집도 똑같이 붙인다.
    server_probe: {
      executor: 'constant-vus',
      vus: 1,
      duration: '45s',
      exec: 'probeServer',
    },
  },
  thresholds: {
    sale_client_error_rate: ['rate==0'],
    sale_server_error_rate: ['rate==0'],
    sale_connection_error_rate: ['rate==0'],
    // actuator 폴링 요청이 섞이지 않도록 부하 시나리오로 범위를 좁힌다.
    'http_req_duration{scenario:sustained_load_multi_partner}': ['p(95)<2000'],
  },
};

export function setup() {
  const token = login(EMAIL, PASSWORD);
  const fixture = createFixture(token, INITIAL_STOCK, VUS);
  return { token, ...fixture };
}

export default function (data) {
  const partnerId = data.partnerIds[(__VU - 1) % data.partnerIds.length];
  const body = JSON.stringify({
    partnerId,
    companyInfoId: data.companyInfoId,
    saleDate: new Date().toISOString().slice(0, 10),
    items: [{ itemSpecId: data.itemSpecId, quantity: QUANTITY, unitPrice: 2000 }],
  });

  const res = http.post(`${BASE_URL}/api/sales`, body, authHeaders(data.token));
  classifyResponse(res);

  check(res, {
    '201 또는 409만 정상': (r) => r.status === 201 || r.status === 409,
  });

  sleep(Math.random() * 0.3);
}

export function probeServer(data) {
  sampleServerMetrics(data.token);
  sleep(1);
}

export function teardown(data) {
  const finalStock = fetchCurrentStock(data.token, data.itemId, data.itemSpecId);
  check(null, {
    '최종 재고는 절대 음수가 될 수 없음': () => finalStock !== null && finalStock >= 0,
  });
  console.log(`[load-multi-partner] 초기재고=${INITIAL_STOCK}, 최종재고=${finalStock}`);
}

export const handleSummary = buildHandleSummary('02b-load-multi-partner');
