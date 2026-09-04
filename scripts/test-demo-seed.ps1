param(
    [Parameter(Mandatory = $true)][string]$Database,
    [string]$Server = '127.0.0.1',
    [int]$Port = 5449,
    [string]$Username = 'postgres',
    [string]$Psql = 'psql'
)
$ErrorActionPreference = 'Stop'
if ($Database -notmatch '^cares_seed_validation[A-Za-z0-9_]*$') {
    throw 'This test resets data. Use a dedicated cares_seed_validation* database only.'
}
$seedPath = Join-Path (Split-Path $PSScriptRoot -Parent) 'src/main/resources/data.sql'
$connection = @('-X', '-h', $Server, '-p', "$Port", '-U', $Username, '-d', $Database, '-v', 'ON_ERROR_STOP=1', '-q')
$others = & $Psql @connection -At -c 'SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() AND backend_type=''client backend'';'
if ($LASTEXITCODE -ne 0 -or [int]$others -ne 0) { throw 'Stop the backend and other connections to the validation database first.' }

# Both runs share one session: catches temporary-object/idempotency issues as well as duplicate keys.
& $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo_reset='yes';" -f $seedPath -f $seedPath
if ($LASTEXITCODE -ne 0) { throw 'Repeated seed failed.' }
foreach ($clock in @('2026-09-04 00:00:00', '2026-09-04 08:00:00', '2026-09-04 16:00:00',
    '2026-09-30 23:59:59.500000', '2027-01-01 00:00:00', '2028-02-29 08:00:00')) {
    & $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo_reset='yes'; SET cares.demo_now='$clock';" -f $seedPath
    if ($LASTEXITCODE -ne 0) { throw "Boundary seed failed: $clock" }
    Write-Host "PASS $clock (UTC session)"
}

# A missing acknowledgement must not truncate the previously committed data.
$before = & $Psql @connection -At -c 'SELECT md5(string_agg(record_id::text || completed_at::text, '','' ORDER BY record_id)) FROM medical_record;'
& $Psql @connection -f $seedPath 2>&1 | Out-Host
if ($LASTEXITCODE -eq 0) { throw 'Reset without explicit acknowledgement unexpectedly succeeded.' }
$after = & $Psql @connection -At -c 'SELECT md5(string_agg(record_id::text || completed_at::text, '','' ORDER BY record_id)) FROM medical_record;'
if ($before -ne $after) { throw 'Guard failure changed the committed dataset.' }

# This date is earlier than demo patients' births. The final domain assertion must fail
# AFTER TRUNCATE/INSERT and roll the entire transaction back.
& $Psql @connection -c "SET cares.demo_reset='yes'; SET cares.demo_now='2000-01-01 08:00:00';" -f $seedPath 2>&1 | Out-Host
if ($LASTEXITCODE -eq 0) { throw 'Invalid clinical chronology unexpectedly succeeded.' }
$afterRollback = & $Psql @connection -At -c 'SELECT md5(string_agg(record_id::text || completed_at::text, '','' ORDER BY record_id)) FROM medical_record;'
if ($before -ne $afterRollback) { throw 'Failed assertion did not preserve the previous committed dataset.' }

# Return the validation database to the actual current date.
& $Psql @connection -c "SET TIME ZONE 'UTC'; SET cares.demo_reset='yes';" -f $seedPath
if ($LASTEXITCODE -ne 0) { throw 'Restoring current-date demo failed.' }
Write-Host 'PASS: repeated runs, day/shift/month/year/leap-day boundaries, reset guard, current-date restore.'
