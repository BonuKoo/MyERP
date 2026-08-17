/**
 * A3 — 경합도 스윕
 *
 * 목적: A1(낙관 vs 비관)과 A2(풀 크기)의 결론이 전부 "규격 하나에 전부 몰릴 때"라는
 * 단서를 달고 있다. 경합을 흩뜨리면 어디서 결론이 뒤집히는지를 본다.
 *
 * 규격 개수만 SPEC_COUNT로 바꾸고 나머지는 03-stress.js와 동일하게 유지한다
 * (0→100 VU 램프 120초, 재고는 사실상 무제한). VU는 자기 번호로 규격을 골라
 * 균등하게 흩어진다 — SPEC_COUNT=1이면 100 VU가 한 행에 몰리고, 100이면 VU당 한 행씩
 * 차지해 행 경합이 사라진다.
 *
 * 락 전략은 서버 설정(myerp.sale.lock-strategy)이라 스크립트가 아니라 기동 시 정한다.
 * 두 전략 × 세 경합도를 도는 것은 run-contention-sweep.sh가 담당한다.
 *
 * 예상: 경합이 사라질수록 낙관적 락의 충돌(409)이 줄어 실질 처리량이 올라가고,
 * 어느 지점에서 비관적 락을 역전한다. 재고 행이 상한을 정한다는 A2 후속 결론이
 * 맞다면 규격이 늘수록 전체 처리량 자체도 올라가야 한다.
 *
 * 실행: SPEC_COUNT=10 k6 run k6/04-contention.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, login, authHeaders, createFixture, fetchCurrentStock, classifyResponse, buildHandleSummary, sampleServerMetrics } from './helpers.js';

const EMAIL = __ENV.TEST_EMAIL || 'owner@myerp.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

const SPEC_COUNT = parseInt(__ENV.SPEC_COUNT || '1', 10);
const QUANTITY = 1;
// 규격마다 이만큼씩 채운다. 경합도가 높을 때(규격 1개) 한 행에서 다 빠져나가므로
// 재고 부족으로 409가 나지 않도록 넉넉히 잡는다 — 이 테스트가 보려는 건 재고 소진이
// 아니라 행 경합이다.
const INITIAL_STOCK = 100000;

export const options = {
  scenarios: {
    ramp_to_breakpoint: {
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
    server_probe: {
      executor: 'constant-vus',
      vus: 1,
      duration: '120s',
      exec: 'probeServer',
    },
  },
};

export function setup() {
  const token = login(EMAIL, PASSWORD);
  const fixture = createFixture(token, INITIAL_STOCK, 1, SPEC_COUNT);
  console.log(`[contention] 규격 ${SPEC_COUNT}개 생성, 각 재고 ${INITIAL_STOCK}`);
  return { token, ...fixture };
}

export default function (data) {
  // VU 번호로 규격을 고정 배정한다(난수보다 분포가 고르고 회차 간 재현성이 있다).
  const itemSpecId = data.itemSpecIds[(__VU - 1) % data.itemSpecIds.length];

  const body = JSON.stringify({
    partnerId: data.partnerId,
    companyInfoId: data.companyInfoId,
    saleDate: new Date().toISOString().slice(0, 10),
    items: [{ itemSpecId, quantity: QUANTITY, unitPrice: 2000 }],
  });

  const res = http.post(`${BASE_URL}/api/sales`, body, authHeaders(data.token));
  classifyResponse(res);

  check(res, {
    '요청이 완전히 끊기지 않음(응답 자체는 옴)': (r) => r.status !== 0,
  });
}

export function probeServer(data) {
  sampleServerMetrics(data.token);
  sleep(1);
}

export function teardown(data) {
  // 규격이 여러 개면 전부 확인한다 — 하나라도 음수면 락에 실제 버그가 있다는 뜻.
  let negative = 0;
  for (const specId of data.itemSpecIds) {
    const stock = fetchCurrentStock(data.token, data.itemId, specId);
    if (stock === null || stock < 0) negative++;
  }

  check(null, {
    '모든 규격의 최종 재고는 음수가 아님': () => negative === 0,
  });
  console.log(`[contention] 규격 ${data.itemSpecIds.length}개 중 음수 재고 ${negative}개`);
}

export const handleSummary = buildHandleSummary(`04-contention-spec${SPEC_COUNT}`);
