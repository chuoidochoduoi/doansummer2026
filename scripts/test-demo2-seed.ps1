param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$Server = '127.0.0.1',
    [int]$Port = 5432,
    [string]$Username = 'postgres',
    [string]$Psql = 'psql'
)
$ErrorActionPreference = 'Stop'
if ($Database -notmatch '^cares_demo2_validation[A-Za-z0-9_]*$') {
    throw 'This test resets data. Use a dedicated cares_demo2_validation* database only.'
}
$root = Split-Path $PSScriptRoot -Parent
$seedPath = Join-Path $root 'src/main/resources/data2.sql'
$connection = @('-X', '-h', $Server, '-p', "$Port", '-U', $Username, '-d', $Database,
    '-v', 'ON_ERROR_STOP=1', '-q')
$others = & $Psql @connection -At -c "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() AND backend_type='client backend';"
if ($LASTEXITCODE -ne 0 -or [int]$others -ne 0) {
    throw 'Stop the backend and other connections to the validation database first.'
}

# Same-session repetition catches temporary-object and deterministic-ID issues.
& $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo2_reset='yes';" -f $seedPath -f $seedPath
if ($LASTEXITCODE -ne 0) { throw 'Repeated data2 seed failed.' }

$clocks = @(
    '2026-09-07 07:30:00', # Monday, morning boundary
    '2026-09-07 10:00:00',
    '2026-09-07 12:00:00', # lunch, closed
    '2026-09-07 13:30:00',
    '2026-09-07 17:29:59',
    '2026-09-07 17:30:00', # after hours
    '2026-09-12 09:00:00', # Saturday
    '2026-09-13 09:00:00', # Sunday
    '2026-12-31 10:00:00',
    '2028-02-29 14:00:00'
)
foreach ($clock in $clocks) {
    & $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo2_reset='yes'; SET cares.demo2_now='$clock';" -f $seedPath
    if ($LASTEXITCODE -ne 0) { throw "Boundary seed failed: $clock" }
    Write-Host "PASS $clock (UTC session)"
}

# Missing acknowledgement must fail before data.sql can reset anything.
$before = & $Psql @connection -At -c "SELECT md5(string_agg(record_id::text || coalesce(completed_at::text,''), ',' ORDER BY record_id)) FROM medical_record;"
& $Psql @connection -f $seedPath 2>&1 | Out-Host
if ($LASTEXITCODE -eq 0) { throw 'Reset without explicit acknowledgement unexpectedly succeeded.' }
$after = & $Psql @connection -At -c "SELECT md5(string_agg(record_id::text || coalesce(completed_at::text,''), ',' ORDER BY record_id)) FROM medical_record;"
if ($before -ne $after) { throw 'Guard failure changed the committed dataset.' }

& $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo2_reset='yes';" -f $seedPath
if ($LASTEXITCODE -ne 0) { throw 'Restoring current-date board demo failed.' }
Write-Host 'PASS: repeated runs, shifts, lunch, Saturday, Sunday, month/year/leap-day and guard.'
