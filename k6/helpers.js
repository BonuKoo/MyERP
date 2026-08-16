import http from 'k6/http';
import { check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { '로그인 성공(200)': (r) => r.status === 200 });
  return res.json('accessToken');
}

export function authHeaders(token) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } };
}

/**
 * 테스트 전용 거래처/회사정보/카테고리/품목/규격을 새로 만들고 initialStock만큼 채운다.
 * 이름에 타임스탬프+난수를 붙여서 실데이터/다른 실행과 절대 안 겹치게 한다.
 * 반환값을 setup()에서 그대로 리턴하면 모든 VU와 teardown()에 공유된다.
 *
 * partnerCount(기본 1)를 1보다 크게 주면 거래처를 그만큼 만들어 partnerIds로 반환한다.
 * 5단계(원장) 도입 이후 매출 등록은 partner 행에도 UPDATE를 걸기 때문에, 같은
 * 거래처로 몰리는 시나리오(A, partnerCount=1)와 거래처를 분산하는 시나리오(B,
 * partnerCount=VU 수)를 비교하면 "원장 잠금이 item_spec 낙관적 락과 별개로
 * 처리량에 얼마나 영향을 주는지"를 분리해서 볼 수 있다. item_spec/재고는 두
 * 시나리오에서 동일하게 공유되므로 그 변수는 고정된다.
 */
export function createFixture(token, initialStock, partnerCount = 1) {
  const headers = authHeaders(token);
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;

  const partnerIds = [];
  for (let i = 0; i < partnerCount; i++) {
    const partnerRes = http.post(
      `${BASE_URL}/api/partners`,
      JSON.stringify({ name: `k6거래처-${suffix}-${i}`, partnerType: 'CUSTOMER' }),
      headers,
    );
    check(partnerRes, { '거래처 생성 성공(201)': (r) => r.status === 201 });
    partnerIds.push(partnerRes.json('id'));
  }

  const companyRes = http.post(
    `${BASE_URL}/api/company-info`,
    JSON.stringify({ companyName: `k6회사-${suffix}` }),
    headers,
  );
  const companyInfoId = companyRes.json('id');

  const mainRes = http.post(
    `${BASE_URL}/api/categories/main`,
    JSON.stringify({ name: `k6대분류-${suffix}`, displayOrder: 1 }),
    headers,
  );
  const categoryMainId = mainRes.json('id');

  const subRes = http.post(
    `${BASE_URL}/api/categories/sub`,
    JSON.stringify({ categoryMainId, name: `k6중분류-${suffix}`, displayOrder: 1 }),
    headers,
  );
  const categorySubId = subRes.json('id');

  const itemRes = http.post(
    `${BASE_URL}/api/items`,
    JSON.stringify({ categorySubId, name: `k6품목-${suffix}` }),
    headers,
  );
  const itemId = itemRes.json('id');

  const specRes = http.post(
    `${BASE_URL}/api/items/${itemId}/specs`,
    JSON.stringify({ specName: '표준', unit: 'EA', costPrice: 1000, salePrice: 2000, safetyStock: 0 }),
    headers,
  );
  check(specRes, { '규격 생성 성공(201)': (r) => r.status === 201 });
  const itemSpecId = specRes.json('id');

  // 규격 등록 직후 재고는 항상 0으로 시작 → 수동 조정으로 원하는 초기 재고만큼 채운다.
  const adjustRes = http.post(
    `${BASE_URL}/api/item-specs/${itemSpecId}/stock/adjust`,
    JSON.stringify({ quantityDelta: initialStock }),
    headers,
  );
  check(adjustRes, { '초기 재고 세팅 성공(200)': (r) => r.status === 200 });

  return { partnerId: partnerIds[0], partnerIds, companyInfoId, itemId, itemSpecId };
}

export function fetchCurrentStock(token, itemId, itemSpecId) {
  const res = http.get(`${BASE_URL}/api/items/${itemId}/specs`, authHeaders(token));
  if (res.status !== 200) return null;
  const specs = res.json();
  const spec = specs.find((s) => s.id === itemSpecId);
  return spec ? spec.currentStock : null;
}

export function classifyResponse(res, counters) {
  if (res.status === 201) {
    counters.success.add(1);
    return 'success';
  }
  if (res.status === 409 && typeof res.body === 'string' && res.body.includes('충돌')) {
    counters.lockConflict.add(1);
    return 'lockConflict';
  }
  if (res.status === 409) {
    counters.insufficientStock.add(1);
    return 'insufficientStock';
  }
  if (res.status === 0) {
    counters.connectionError.add(1);
    return 'connectionError';
  }
  counters.unexpectedError.add(1);
  console.error(`예상 못한 응답: status=${res.status} body=${res.body}`);
  return 'unexpectedError';
}

/**
 * handleSummary()를 각 스크립트마다 똑같이 반복하지 않도록 묶어둔 팩토리.
 * 터미널 요약(stdout)은 그대로 유지하면서 k6/reports/{reportName}.html도 남긴다.
 * reports/ 디렉터리는 실행할 때마다 생성되는 산출물이라 git엔 안 올라간다(.gitignore).
 */
export function buildHandleSummary(reportName) {
  return function (data) {
    return {
      stdout: textSummary(data, { indent: ' ', enableColors: true }),
      [`reports/${reportName}.html`]: htmlReport(data),
    };
  };
}
