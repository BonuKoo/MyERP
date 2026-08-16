/**
 * stress-multi-partner — "한계 상황, 원장 잠금 분리 버전"
 *
 * 03-stress.js와 완전히 동일한 조건(0→100 VU 램프업, 재고 100000)이되, VU마다
 * 서로 다른 거래처를 쓴다는 점만 다르다(최대 VU 수인 100개를 미리 만들어둠).
 *
 * 목적은 03-stress.js와 동일하게 "동시접속 자체의 한계"를 보는 것이지만, 이번엔
 * partner 행 잠금 경합을 제거한 상태에서 본다. 03-stress.js와 비교했을 때:
 * - 이 스크립트가 더 높은 VU까지 버틴다면 → partner 원장 잠금이 03-stress.js의
 *   한계를 실제로 앞당기고 있었다는 뜻.
 * - 두 스크립트의 한계점이 비슷하다면 → 병목은 원장이 아니라 HikariCP 풀/
 *   Tomcat 스레드 등 다른 공용 자원이라는 뜻(원래 03-stress.js가 노리던 지점).
 *
 * 실행: k6 run k6/03b-stress-multi-partner.js
 *       03-stress.js와 같은 시간대에 실행하지 말 것(같은 서버/DB를 두고 비교해야 함).
 * 주의: 로컬 개발 서버·DB에 실제로 부하가 걸린다.
 */
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse, buildHandleSummary } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const MAX_VUS = 100;
const QUANTITY = 1;
const INITIAL_STOCK = 100000;

export const options = {
  scenarios: {
    ramp_to_breakpoint_multi_partner: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '20s', target: 10 },
        { duration: '20s', target: 30 },
        { duration: '20s', target: 60 },
        { duration: '20s', target: 100 },
        { duration: '20s', target: 0 },
      ],
    },
  },
};

export function setup() {
  const token = login(EMAIL, PASSWORD);
  const fixture = createFixture(token, INITIAL_STOCK, MAX_VUS);
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
    '요청이 완전히 끊기지 않음(응답 자체는 옴)': (r) => r.status !== 0,
  });
}

export function teardown(data) {
  const finalStock = fetchCurrentStock(data.token, data.itemId, data.itemSpecId);
  check(null, {
    '최종 재고는 절대 음수가 될 수 없음': () => finalStock !== null && finalStock >= 0,
  });
  console.log(`[stress-multi-partner] 초기재고=${INITIAL_STOCK}, 최종재고=${finalStock}`);
}

export const handleSummary = buildHandleSummary('03b-stress-multi-partner');
