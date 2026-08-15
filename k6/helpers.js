import http from 'k6/http';
import { check } from 'k6';

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
 */
export function createFixture(token, initialStock) {
  const headers = authHeaders(token);
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;

  const partnerRes = http.post(
    `${BASE_URL}/api/partners`,
    JSON.stringify({ name: `k6거래처-${suffix}`, partnerType: 'CUSTOMER' }),
    headers,
  );
  check(partnerRes, { '거래처 생성 성공(201)': (r) => r.status === 201 });
  const partnerId = partnerRes.json('id');

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

  return { partnerId, companyInfoId, itemId, itemSpecId };
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
