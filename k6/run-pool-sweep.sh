#!/usr/bin/env bash
#
# A2: HikariCP 커넥션 풀 크기 스윕 (k6/LOAD_TEST_PLAN.md)
#
# 풀 크기만 10/20/50/100으로 바꿔가며 같은 부하(03-stress.js)를 걸어 처리량 곡선을 본다.
# 비관적 락으로 고정한다 — 대기가 실제로 쌓이는 쪽이라 풀 크기 효과가 가장 선명하고,
# 낙관적 락은 풀을 키우면 동시 시도가 늘어 충돌률이 오르는 별개 현상이 섞여 해석이 흐려진다.
#
# 각 회차마다:
#   1) 8080을 쓰던 프로세스를 정리하고 해당 풀 크기로 재기동
#   2) actuator로 풀 크기가 실제 반영됐는지 검증 — 다르면 즉시 중단(잘못된 조건으로 재는 사고 방지)
#   3) 프로세스별 CPU 샘플러를 띄운 뒤 k6 실행
#      → mysqld CPU가 낮게 유지되면 "잠금 대기"이고, 포화면 "한 PC에 몰아넣은 탓"이다
#
# 실행: bash k6/run-pool-sweep.sh   (리포지토리 루트에서)
set -u

POOL_SIZES="10 20 50 100"
DURATION=135          # 03-stress.js는 20s × 6 = 120s. 셋업/teardown 여유를 둔다.
REPORTS="k6/reports"
K6_BIN="/c/xk6/k6.exe"
: "${DB_PASSWORD:?DB_PASSWORD 환경변수가 필요합니다}"

mkdir -p "$REPORTS"

kill_app() {
  powershell -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force -ErrorAction SilentlyContinue }" >/dev/null 2>&1
  sleep 3
}

for POOL in $POOL_SIZES; do
  echo ""
  echo "=================== POOL=$POOL ==================="

  kill_app

  DB_POOL_SIZE="$POOL" ./gradlew.bat bootRun --console=plain \
      --args='--myerp.sale.lock-strategy=pessimistic' > "/tmp/boot-pool-$POOL.log" 2>&1 &

  echo "앱 기동 대기..."
  until curl -s -o /dev/null http://localhost:8080/actuator/health; do sleep 2; done

  TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"email":"owner@myerp.com","password":"password123"}' \
      | python -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

  ACTUAL=$(curl -s "http://localhost:8080/actuator/metrics/hikaricp.connections.max" \
      -H "Authorization: Bearer $TOKEN" \
      | python -c "import sys,json; print(int(json.load(sys.stdin)['measurements'][0]['value']))")

  if [ "$ACTUAL" != "$POOL" ]; then
    echo "중단: 풀 크기가 반영되지 않았습니다 (기대=$POOL, 실제=$ACTUAL)"
    kill_app
    exit 1
  fi
  echo "풀 크기 확인: $ACTUAL"

  sleep 5   # 기동 직후 안정화

  powershell -File k6/sample-process-cpu.ps1 -DurationSeconds "$DURATION" \
      -OutputPath "$REPORTS/cpu-pool-$POOL.csv" > /dev/null 2>&1 &
  CPU_PID=$!

  "$K6_BIN" run k6/03-stress.js 2>&1 | tee "$REPORTS/k6-pool-$POOL.log" | grep -E "sale_success_rate|sale_response_time|server_hikari_pending|http_reqs"

  wait $CPU_PID 2>/dev/null

  # handleSummary가 항상 같은 이름으로 덮어쓰므로 회차별로 남긴다
  [ -f "$REPORTS/03-stress.html" ] && cp "$REPORTS/03-stress.html" "$REPORTS/03-stress-pool-$POOL.html"

  echo "완료: POOL=$POOL"
done

kill_app
echo ""
echo "스윕 완료. 결과: $REPORTS/k6-pool-*.log, $REPORTS/cpu-pool-*.csv"
