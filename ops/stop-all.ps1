#Requires -Version 5.1
param(
    [switch]$All
)

$ErrorActionPreference = "SilentlyContinue"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8

function Write-Step($msg)  { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "    [SKIP] $msg" -ForegroundColor Yellow }

function Test-Port([int]$Port) {
    try {
        $conn = New-Object System.Net.Sockets.TcpClient
        $conn.Connect("127.0.0.1", $Port)
        $conn.Close()
        return $true
    } catch {
        return $false
    }
}

function Stop-PortProcess([int]$Port, [string]$Name) {
    if (-not (Test-Port $Port)) {
        Write-Warn "$Name not running (port $Port)"
        return
    }
    $lines = netstat -ano | Select-String ":${Port}\s" | Select-String "LISTENING"
    $pids = $lines | ForEach-Object { ($_ -split '\s+')[-1] } | Where-Object { $_ -match '^\d+$' } | Select-Object -Unique
    foreach ($p in $pids) {
        $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "    Killing: $($proc.ProcessName) (PID $p)" -ForegroundColor Gray
            taskkill /F /PID $p 2>$null | Out-Null
        }
    }
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt 10 -and (Test-Port $Port)) {
        Start-Sleep -Milliseconds 500
    }
    if (-not (Test-Port $Port)) {
        Write-Ok "$Name stopped"
    } else {
        Write-Host "    [WARN] Port $Port still in use" -ForegroundColor Yellow
    }
}

# 1. Frontend
Write-Step "Frontend"
Stop-PortProcess 3000 "Frontend"

# 2. Backend
Write-Step "Backend"
Stop-PortProcess 8081 "Backend"

# 3. XXL-Job Admin
Write-Step "XXL-Job Admin"
Stop-PortProcess 8082 "XXL-Job Admin"

# 4. MySQL & Redis (optional)
if ($All) {
    Write-Step "MySQL"
    net stop MySQL57 2>$null
    if (-not (Test-Port 3306)) { Write-Ok "MySQL stopped" } else { Write-Warn "MySQL stop failed" }

    Write-Step "Redis"
    net stop redis 2>$null
    if (-not (Test-Port 6379)) { Write-Ok "Redis stopped" } else { Write-Warn "Redis stop failed" }
} else {
    Write-Host "`n    MySQL/Redis kept running (use -All to stop all)" -ForegroundColor Gray
}

# Summary
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  BlogHub Stopped" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$svcs = @(
    @{ N="MySQL";         P=3306 },
    @{ N="Redis";         P=6379 },
    @{ N="XXL-Job Admin"; P=8082 },
    @{ N="Backend";       P=8081 },
    @{ N="Frontend";      P=3000 }
)
foreach ($s in $svcs) {
    $ok = Test-Port $s.P
    $icon = if ($ok) { "[ON] " } else { "[OFF]" }
    $clr  = if ($ok) { "Green" } else { "Gray" }
    Write-Host ("  {0} {1}" -f $icon, $s.N) -ForegroundColor $clr
}
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
