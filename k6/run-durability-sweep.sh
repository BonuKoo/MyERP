#!/usr/bin/env bash
#
# fsync 튜닝 스윕 (A3 후속 — k6/EXPERIMENT_SUMMARY.md "다음 단계" 1·2번)
#
# A3에서 규격 100개 + 비관적 락 조건의 처리량 상한이 커밋 fsync(건당 약 9ms)임을
# 확인했다. 이 스크립트는 그 상한이 실제로 fsync 설정에 좌우되는지 검증한다.
#
# 세 조건을 같은 앱 프로세스에서 순서대로 측정한다(durability 설정은 MySQL 전역
# 변수라 앱 재시작 없이 즉시 반영됨 — 풀 크기 스윕과 달리 재기동이 필요 없다):
#
#   1) baseline       — 현재 설정 그대로 (flush_at_commit=1, sync_binlog=1)
#   2) group_commit   — 그룹 커밋 지연만 켬(2ms). 내구성은 그대로, 커밋을 묶어
#                        fsync 횟수만 줄인다. 대가 없는 최적화인지 확인.
#   3) relaxed        — 완전 완화(flush_at_commit=2, sync_binlog=0). 이 PC의 개발
#                        DB이므로 정전 시 유실 위험을 감수하고 "디스크가 낼 수 있는
#                        진짜 최대치"를 본다.
#
# 회차마다 MySQL 상태 카운터(Com_commit, Innodb_os_log_fsyncs, Innodb_data_fsyncs)를
# 전후로 재서 "커밋당 fsync 횟수"가 설정대로 실제로 줄었는지 직접 확인한다 —
# 처리량만 보고 "설정이 먹었다"고 추측하지 않기 위함.
#
# 안전장치: 스크립트가 어떻게 끝나든(성공/실패/중단) trap으로 반드시 기본값으로
# 원복한다. root 비밀번호가 필요하다(로컬 개발 DB 전용, MYSQL_ROOT_PASSWORD로 주입).
#
# 실행: MYSQL_ROOT_PASSWORD='1234' DB_PASSWORD='erp_pass1234!' bash k6/run-durability-sweep.sh
set -u

SPEC_COUNT=100
REPORTS="k6/reports"
K6_BIN="/c/xk6/k6.exe"
MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
: "${DB_PASSWORD:?DB_PASSWORD 환경변수가 필요합니다}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 환경변수가 필요합니다}"

mkdir -p "$REPORTS"

mysql_root() {
  "$MYSQL" -u root -p"$MYSQL_ROOT_PASSWORD" -N -B -e "$1" 2>/dev/null
}

mysql_app() {
  "$MYSQL" -u erp_user -p'erp_pass1234!' -N -B -e "$1" 2>/dev/null
}

# 스크립트가 어떤 이유로 끝나든 반드시 기본값(가장 안전한 상태)으로 되돌린다.
revert_durability() {
  echo ""
  echo "원복: flush_at_commit=1, sync_binlog=1, group_commit_sync_delay=0"
  mysql_root "SET GLOBAL innodb_flush_log_at_trx_commit = 1;"
  mysql_root "SET GLOBAL sync_binlog = 1;"
  mysql_root "SET GLOBAL binlog_group_commit_sync_delay = 0;"
  mysql_root "SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit'; SHOW VARIABLES LIKE 'sync_binlog'; SHOW VARIABLES LIKE 'binlog_group_commit_sync_delay';"
}
trap revert_durability EXIT

kill_app() {
  powershell -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id \$_.OwningProcess -Force -ErrorAction SilentlyContinue }" >/dev/null 2>&1
  sleep 3
}

get_counters() {
  # Com_commit, 리두 로그 fsync, 데이터 fsync를 한 번에 뽑아 "이름\t값" 형태로 반환
  mysql_app "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_commit','Innodb_os_log_fsyncs','Innodb_data_fsyncs')"
}

run_one() {
  local TAG="$1"
  echo ""
  echo "=================== $TAG ==================="
  mysql_root "SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit'; SHOW VARIABLES LIKE 'sync_binlog'; SHOW VARIABLES LIKE 'binlog_group_commit_sync_delay';"

  local BEFORE
  BEFORE=$(get_counters)

  powershell -File k6/sample-process-cpu.ps1 -DurationSeconds 135 \
      -OutputPath "$REPORTS/cpu-durability-$TAG.csv" > /dev/null 2>&1 &
  CPU_PID=$!

  SPEC_COUNT="$SPEC_COUNT" "$K6_BIN" run k6/04-contention.js 2>&1 \
      | tee "$REPORTS/k6-durability-$TAG.log" \
      | grep -E "sale_success_rate|sale_response_time|server_hikari_pending|http_reqs"

  wait $CPU_PID 2>/dev/null

  local AFTER
  AFTER=$(get_counters)

  echo "--- $TAG: Com_commit / fsync 카운터 (전/후) ---"
  echo "$BEFORE" | sed 's/^/  전  /'
  echo "$AFTER"  | sed 's/^/  후  /'
  {
    echo "tag,$TAG"
    echo "before"; echo "$BEFORE"
    echo "after"; echo "$AFTER"
  } > "$REPORTS/mysql-counters-$TAG.txt"

  echo "완료: $TAG"
}

kill_app
DB_PASSWORD="$DB_PASSWORD" ./gradlew.bat bootRun --console=plain \
    --args='--myerp.sale.lock-strategy=pessimistic' > /tmp/boot-durability.log 2>&1 &

echo "앱 기동 대기..."
until curl -s -o /dev/null http://localhost:8080/actuator/health; do sleep 2; done
sleep 5

# 1) 기준선 — 현재 설정 그대로
run_one "baseline"

# 2) 그룹 커밋 지연만 (내구성 그대로, 커밋을 묶어 fsync 횟수만 절감)
mysql_root "SET GLOBAL binlog_group_commit_sync_delay = 2000;"  # 마이크로초 → 2ms
run_one "group-commit-2ms"
mysql_root "SET GLOBAL binlog_group_commit_sync_delay = 0;"

# 3) 완전 완화 (이 PC의 개발 DB 한정, 정전 시 유실 위험 감수)
mysql_root "SET GLOBAL innodb_flush_log_at_trx_commit = 2;"
mysql_root "SET GLOBAL sync_binlog = 0;"
run_one "relaxed"

kill_app
echo ""
echo "스윕 완료. 결과: $REPORTS/k6-durability-*.log, $REPORTS/mysql-counters-*.txt"
echo "(원복은 트랩으로 스크립트 종료 시 자동 실행됨)"
