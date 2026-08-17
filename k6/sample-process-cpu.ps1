<#
.SYNOPSIS
부하 테스트 중 전체 CPU와 앱(java)/MySQL(mysqld)/부하생성기(k6)의 CPU·메모리를 1초 간격으로 기록한다.

.DESCRIPTION
왜 필요한가: k6·Spring Boot·MySQL이 한 PC에 같이 있으면 "풀을 키웠더니 느려졌다"의
원인이 두 가지로 갈리는데, 클라이언트 지표만으로는 구분할 수 없다.

  (a) 대기가 커넥션 풀에서 InnoDB 행 잠금으로 옮겨간 것    ← 알고 싶은 것
  (b) 세 프로세스가 같은 코어를 두고 다툰 것                ← 한 PC에 몰아넣어 생긴 가짜 신호

잠금을 기다리는 스레드는 CPU를 쓰지 않는다. 부하 중 전체 CPU가 포화와 거리가 멀면
(b)를 배제할 수 있고 (a)라는 결론이 선다. 반대로 포화에 가까우면 PC를 분리해 다시 재야 한다.

구현 노트(두 번 갈아엎은 이유):
1. 처음엔 Get-Process의 .CPU 차분을 스크립트 안에서 계산했는데 전 구간 0%가 나왔다.
   유휴 상태에서만 스모크 테스트를 해서 "정상인데 한가함"과 "고장"을 구분하지 못했다.
2. 누적 CPU 초를 원본으로 남기게 고치니 app/k6는 측정됐지만, mysqld는 서비스 계정
   소유라 TotalProcessorTime 접근이 막혔다.
3. 그래서 CIM 성능 카운터로 옮겼다 — mysqld까지 권한 문제 없이 읽히고, 전체 CPU도
   같이 얻을 수 있다.

앱은 이름이 아니라 8080 포트를 점유한 PID로 특정한다(Gradle 데몬 등 다른 java
프로세스가 섞이는 것을 막기 위함).

주의: 프로세스별 PercentProcessorTime은 코어 1개를 100%로 보는 값이라 멀티코어에서는
100을 넘을 수 있다(8코어면 최대 800). 전체 CPU(_Total)는 0~100이다. 후처리에서
프로세스 값을 코어 수로 나누면 "머신 전체 대비 몇 %"가 된다.

.EXAMPLE
  powershell -File k6/sample-process-cpu.ps1 -DurationSeconds 135 -OutputPath k6/reports/cpu-pool10.csv
#>
param(
    [int]$DurationSeconds = 135,
    [string]$OutputPath = "k6/reports/process-cpu.csv",
    [int]$AppPort = 8080
)

$cores = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors
Write-Output "logical cores: $cores"

"# cores=$cores" | Out-File -FilePath $OutputPath -Encoding ascii
"elapsed_ms,scope,pid,cpu_percent,working_set_mb" | Out-File -FilePath $OutputPath -Append -Encoding ascii

$start = Get-Date
$procFilter = "Name LIKE 'java%' OR Name LIKE 'mysqld%' OR Name LIKE 'k6%'"

for ($i = 0; $i -lt $DurationSeconds; $i++) {

    $elapsedMs = [math]::Round(((Get-Date) - $start).TotalMilliseconds)
    $out = @()

    # 8080 리스너의 PID — 이 PID와 일치하는 java 인스턴스만 'app'으로 본다
    $appPid = -1
    $conn = Get-NetTCPConnection -LocalPort $AppPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) { $appPid = [int]$conn.OwningProcess }

    try {
        $total = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter "Name='_Total'" -ErrorAction Stop
        $out += ("{0},total,,{1}," -f $elapsedMs, $total.PercentProcessorTime)
    } catch {
        $out += ("{0},total,,," -f $elapsedMs)
    }

    try {
        $procs = Get-CimInstance Win32_PerfFormattedData_PerfProc_Process -Filter $procFilter -ErrorAction Stop
        foreach ($p in $procs) {
            $label = $null
            if ($p.IDProcess -eq $appPid) { $label = 'app' }
            elseif ($p.Name -like 'mysqld*') { $label = 'mysqld' }
            elseif ($p.Name -like 'k6*') { $label = 'k6' }
            if (-not $label) { continue }   # Gradle 데몬 등 나머지 java는 버린다

            $wsMb = [math]::Round($p.WorkingSetPrivate / 1MB, 1)
            $out += ("{0},{1},{2},{3},{4}" -f $elapsedMs, $label, $p.IDProcess, $p.PercentProcessorTime, $wsMb)
        }
    } catch {
        $out += ("{0},procs_error,,," -f $elapsedMs)
    }

    $out | Out-File -FilePath $OutputPath -Append -Encoding ascii

    Start-Sleep -Seconds 1
}

Write-Output "wrote: $OutputPath"
