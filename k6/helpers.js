import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.4/index.js';
// main 브랜치 추종 대신 태그 고정(재현성) — 2026-08-16 존재 확인
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/2.4.0/dist/bundle.js';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ================== 매출 등록(POST /api/sales) 결과 지표 ==================
// classifyResponse()가 매 응답마다 채운다. 스크립트마다 각자 Counter를 새로
// 만들던 방식을 걷어내고 여기 한 곳으로 모아서, 02 vs 02b/03 vs 03b 비교 시
// 지표 이름·의미가 스크립트마다 어긋나지 않게 한다.
// status===0(연결 실패)은 실제 HTTP 왕복이 없었던 요청이라 duration이 의미가
// 없다 — classifyResponse()에서 그 경우만 제외하고 기록한다.
export const saleResponseTime = new Trend('sale_response_time'); // ms, http_req_duration과 별개로 이 도메인 전용 이름을 갖게
export const saleStatusCodes = new Counter('sale_status_codes'); // {status: "201"} 태그로 상태코드 분포
export const saleSuccessRate = new Rate('sale_success_rate'); // 201
export const saleInsufficientStockRate = new Rate('sale_insufficient_stock_rate'); // 409, 재고부족
export const saleLockConflictRate = new Rate('sale_lock_conflict_rate'); // 409, 낙관적 락 충돌(메시지에 "충돌" 포함)
// 400(검증 실패)/404(존재하지 않는 리소스) — 서버 결함이 아니라 스크립트가 보낸
// 요청 자체가 잘못됐다는 신호. 500과 같은 통에 담으면 "서버가 죽었다"와
// "테스트가 잘못됐다"가 구분이 안 돼서 별도 지표로 뺐다.
export const saleClientErrorRate = new Rate('sale_client_error_rate');
export const saleServerErrorRate = new Rate('sale_server_error_rate'); // 5xx — smoke/load/stress 전부 0이어야 정상
export const saleConnectionErrorRate = new Rate('sale_connection_error_rate'); // status 0(연결 실패/타임아웃)
// 연결 실패의 세부 원인(타임아웃/연결거부/DNS 등)을 k6의 res.error_code로 구분.
// stress에서 "느려지다가 타임아웃"인지 "아예 연결을 거부당함"인지는 서로 다른
// 결론(전자는 큐잉, 후자는 리스너/OS 레벨 한계)으로 이어지므로 뭉치면 안 된다.
export const saleConnectionErrorCodes = new Counter('sale_connection_error_codes');
export const saleAuthErrorCount = new Counter('sale_auth_error_count'); // 401/403 — 테스트 자체가 잘못됐다는 신호
// 정상 비즈니스 분기(201/409/0)가 아닌 모든 응답의 원인 메시지별 카운트.
// 데드락처럼 원인이 뒤섞일 수 있는 500을 하나로 뭉뚱그리지 않고
// {message, status} 태그로 쪼개서 보기 위함.
export const saleErrorMessages = new Counter('sale_error_messages');

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

// ================== 서버 내부 지표(actuator) ==================
// k6가 보는 건 클라이언트 측 결과(응답시간/상태코드)뿐이라 "왜 느려졌는가"가
// 추측이 된다. 부하와 같은 실행 안에서 actuator를 주기적으로 폴링해 같은 시간축
// 위에 서버 내부 상태를 함께 남긴다 — 별도 수집 파이프라인이나 CSV 정렬 없이
// 리포트 하나에서 "응답시간이 튄 구간에 풀이 말라 있었는지"를 바로 볼 수 있다.
//
// 주의 두 가지:
// 1) 폴링 요청도 http_req_duration에 잡힌다. 부하 쪽 임계값은 반드시
//    http_req_duration{scenario:...}로 범위를 좁혀야 이 요청들이 섞이지 않는다.
// 2) tomcat.threads.busy에는 폴링 요청 자신도 1건 포함된다(상시 +1 정도).
export const serverHikariActive = new Trend('server_hikari_active'); // 사용 중 커넥션
export const serverHikariPending = new Trend('server_hikari_pending'); // 커넥션 대기 중인 스레드 — 0보다 크면 풀이 병목
export const serverTomcatBusy = new Trend('server_tomcat_busy'); // 처리 중인 요청 스레드
export const serverHeapUsedMb = new Trend('server_heap_used_mb'); // 힙 사용량(MB)

function readMetricValue(token, name) {
  const res = http.get(`${BASE_URL}/actuator/metrics/${name}`, {
    headers: authHeaders(token).headers,
    tags: { probe: 'actuator' },
  });
  if (res.status !== 200) return null;
  try {
    const measurements = res.json('measurements');
    return measurements && measurements.length ? measurements[0].value : null;
  } catch (e) {
    return null; // 부하 때문에 응답이 깨져도 폴링이 테스트를 죽이면 안 된다
  }
}

/**
 * actuator에서 서버 내부 지표를 한 번 읽어 위 Trend들에 기록한다.
 * 부하 시나리오와 별개인 1 VU 시나리오(server_probe)에서 1초 간격으로 호출한다.
 */
export function sampleServerMetrics(token) {
  const active = readMetricValue(token, 'hikaricp.connections.active');
  const pending = readMetricValue(token, 'hikaricp.connections.pending');
  const busy = readMetricValue(token, 'tomcat.threads.busy');
  const heapBytes = readMetricValue(token, 'jvm.memory.used');

  if (active !== null) serverHikariActive.add(active);
  if (pending !== null) serverHikariPending.add(pending);
  if (busy !== null) serverTomcatBusy.add(busy);
  if (heapBytes !== null) serverHeapUsedMb.add(heapBytes / 1024 / 1024);
}

/**
 * 테스트 전용 거래처/회사정보/카테고리/품목/규격을 새로 만들고 initialStock만큼 채운다.
 * 이름에 타임스탬프+난수를 붙여서 실데이터/다른 실행과 절대 안 겹치게 한다.
 * 반환값을 setup()에서 그대로 리턴하면 모든 VU와 teardown()에 공유된다.
 *
 * specCount(기본 1)를 1보다 크게 주면 같은 품목 아래 규격을 그만큼 만들어 itemSpecIds로
 * 반환한다(각각 initialStock만큼 채운다). 경합도를 바꾸는 A3 스윕에 쓴다 — VU 100이
 * 규격 1개에 몰릴 때와 100개에 흩어질 때 사이 어디서 결론이 뒤집히는지를 본다.
 * itemSpecId는 첫 번째 규격으로 계속 채워주므로 기존 스크립트는 그대로 동작한다.
 *
 * partnerCount(기본 1)를 1보다 크게 주면 거래처를 그만큼 만들어 partnerIds로 반환한다.
 * 5단계(원장) 도입 이후 매출 등록은 partner 행에도 UPDATE를 걸기 때문에, 같은
 * 거래처로 몰리는 시나리오(A, partnerCount=1)와 거래처를 분산하는 시나리오(B,
 * partnerCount=VU 수)를 비교하면 "원장 잠금이 item_spec 낙관적 락과 별개로
 * 처리량에 얼마나 영향을 주는지"를 분리해서 볼 수 있다. item_spec/재고는 두
 * 시나리오에서 동일하게 공유되므로 그 변수는 고정된다.
 */
export function createFixture(token, initialStock, partnerCount = 1, specCount = 1) {
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

  const itemSpecIds = [];
  for (let i = 0; i < specCount; i++) {
    const specRes = http.post(
      `${BASE_URL}/api/items/${itemId}/specs`,
      JSON.stringify({ specName: `표준-${i}`, unit: 'EA', costPrice: 1000, salePrice: 2000, safetyStock: 0 }),
      headers,
    );
    check(specRes, { '규격 생성 성공(201)': (r) => r.status === 201 });
    const specId = specRes.json('id');
    itemSpecIds.push(specId);

    // 규격 등록 직후 재고는 항상 0으로 시작 → 수동 조정으로 원하는 초기 재고만큼 채운다.
    const adjustRes = http.post(
      `${BASE_URL}/api/item-specs/${specId}/stock/adjust`,
      JSON.stringify({ quantityDelta: initialStock }),
      headers,
    );
    check(adjustRes, { '초기 재고 세팅 성공(200)': (r) => r.status === 200 });
  }

  return { partnerId: partnerIds[0], partnerIds, companyInfoId, itemId, itemSpecId: itemSpecIds[0], itemSpecIds };
}

export function fetchCurrentStock(token, itemId, itemSpecId) {
  const res = http.get(`${BASE_URL}/api/items/${itemId}/specs`, authHeaders(token));
  if (res.status !== 200) return null;
  const specs = res.json();
  const spec = specs.find((s) => s.id === itemSpecId);
  return spec ? spec.currentStock : null;
}

function extractErrorMessage(res) {
  try {
    const body = res.json();
    if (body && typeof body.message === 'string') return body.message;
  } catch (e) {
    // JSON이 아닌 바디(빈 응답, 연결 실패 등) — 상태코드로 대체
  }
  return `status_${res.status}`;
}

/**
 * 매출 등록 응답 하나를 분류하면서 위 지표들을 전부 채운다.
 * Rate 지표는 매 호출마다 true/false를 넘겨야 "전체 요청 대비 비율"이 맞게
 * 계산된다(해당되는 경우에만 add(1)을 부르면 그 지표의 모수가 좁아져서
 * 비율이 왜곡된다) — 그래서 Rate.add()는 매번 전부 호출한다.
 */
export function classifyResponse(res) {
  saleStatusCodes.add(1, { status: String(res.status) });

  const isConnectionError = res.status === 0;
  if (!isConnectionError) {
    saleResponseTime.add(res.timings.duration);
  }

  const isSuccess = res.status === 201;
  const isConflict = res.status === 409;
  const isLockConflict = isConflict && typeof res.body === 'string' && res.body.includes('충돌');
  const isInsufficientStock = isConflict && !isLockConflict;
  const isClientError = res.status === 400 || res.status === 404;
  const isAuthError = res.status === 401 || res.status === 403;
  const isServerError = res.status >= 500;

  saleSuccessRate.add(isSuccess);
  saleInsufficientStockRate.add(isInsufficientStock);
  saleLockConflictRate.add(isLockConflict);
  saleClientErrorRate.add(isClientError);
  saleServerErrorRate.add(isServerError);
  saleConnectionErrorRate.add(isConnectionError);

  if (isAuthError) {
    saleAuthErrorCount.add(1, { status: String(res.status) });
  }

  if (isConnectionError) {
    saleConnectionErrorCodes.add(1, { error_code: String(res.error_code), error: res.error || 'unknown' });
    return 'connectionError';
  }
  if (isSuccess) return 'success';
  if (isLockConflict) return 'lockConflict';
  if (isInsufficientStock) return 'insufficientStock';

  // 여기부터는 정상 비즈니스 분기(201/409/0)가 아닌 전부 — 원인 메시지별로 쪼개서 기록.
  const message = extractErrorMessage(res);
  saleErrorMessages.add(1, { message, status: String(res.status) });
  console.error(`예상 못한 응답: status=${res.status} body=${res.body}`);

  if (isClientError) return 'clientError';
  if (isAuthError) return 'authError';
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
      [`k6/reports/${reportName}.html`]: htmlReport(data),
    };
  };
}
