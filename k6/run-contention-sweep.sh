#!/usr/bin/env bash
#
# A3: 경합도 스윕 (k6/LOAD_TEST_PLAN.md)
#
# 규격 개수(1/10/100) × 락 전략(비관/낙관) 6회차. 나머지 조건은 전부 고정한다.
# A1과 A2의 결론이 모두 "규격 하나에 몰릴 때"라는 단서를 달고 있어서, 경합을
# 흩뜨렸을 때 어디서 결론이 뒤집히는지를 본다.
#
# 락 전략은 서버 설정이라 회차마다 재기동이 필요하다. 풀 크기는 기본값(10)으로 둔다 —
# A2에서 풀이 처리량에 영향이 없음을 확인했으므로 변수를 늘리지 않는다.
#
# 실행: bash k6/run-contention-sweep.sh   (리포지토리 루트에서)
set -u

SPEC_COUNTS="1 10 100"
STRATEGIES="pessimistic optimistic"
DURATION=135
REPORTS="k6/reports"
K6_BIN="/c/xk6/k6.exe"
: "${DB_PASSWORD:?DB_PASSWORD 환경변수가 필요합니다}"

mkdir -p "$REPORTS"

kill_app() {
  powershell -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force -ErrorAction SilentlyContinue }" >/dev/null 2>&1
  sleep 3
}

for STRATEGY in $STRATEGIES; do
  for SPECS in $SPEC_COUNTS; do
    TAG="${STRATEGY}-spec${SPECS}"
    echo ""
    echo "=================== $TAG ==================="

    kill_app

    ./gradlew.bat bootRun --console=plain \
        --args="--myerp.sale.lock-strategy=$STRATEGY" > "/tmp/boot-$TAG.log" 2>&1 &

    echo "앱 기동 대기..."
    until curl -s -o /dev/null http://localhost:8080/actuator/health; do sleep 2; done

    sleep 5

    powershell -File k6/sample-process-cpu.ps1 -DurationSeconds "$DURATION" \
        -OutputPath "$REPORTS/cpu-$TAG.csv" > /dev/null 2>&1 &
    CPU_PID=$!

    SPEC_COUNT="$SPECS" "$K6_BIN" run k6/04-contention.js 2>&1 \
        | tee "$REPORTS/k6-$TAG.log" \
        | grep -E "sale_success_rate|sale_lock_conflict_rate|http_reqs"

    wait $CPU_PID 2>/dev/null

    echo "완료: $TAG"
  done
done

kill_app
echo ""
echo "스윕 완료. 결과: $REPORTS/k6-*-spec*.log, $REPORTS/cpu-*-spec*.csv"
