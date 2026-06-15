#Requires -Version 5.1
param(
    [switch]$SkipServices,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8
$Root = Split-Path -Parent $PSScriptRoot

function Write-Step($msg)  { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)    { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "    [SKIP] $msg" -ForegroundColor Yellow }
function Write-Err($msg)   { Write-Host "    [FAIL] $msg" -ForegroundColor Red }

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

function Wait-Port([int]$Port, [string]$Name, [int]$TimeoutSec = 60) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 1
    }
    Write-Err "$Name did not start in ${TimeoutSec}s (port $Port)"
    return $false
}

# 1. MySQL
Write-Step "MySQL"
if (Test-Port 3306) {
    Write-Warn "MySQL already running (port 3306)"
} elseif (-not $SkipServices) {
    net start MySQL57 2>$null
    if (Wait-Port 3306 "MySQL") { Write-Ok "MySQL started" }
} else {
    Write-Warn "Skipped MySQL service"
}

# 2. Redis
Write-Step "Redis"
if (Test-Port 6379) {
    Write-Warn "Redis already running (port 6379)"
} elseif (-not $SkipServices) {
    net start redis 2>$null
    if (Wait-Port 6379 "Redis") { Write-Ok "Redis started" }
} else {
    Write-Warn "Skipped Redis service"
}

# 3. XXL-Job Admin
Write-Step "XXL-Job Admin"
if (Test-Port 8082) {
    Write-Warn "XXL-Job Admin already running (port 8082)"
} else {
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-File",
        (Join-Path $PSScriptRoot "start-xxl-job.ps1")
    ) -WindowStyle Normal
    if (Wait-Port 8082 "XXL-Job Admin" 90) { Write-Ok "XXL-Job Admin started (http://localhost:8082/xxl-job-admin)" }
}

# 4. Backend (Spring Boot)
Write-Step "Backend"
if (Test-Port 8081) {
    Write-Warn "Backend already running (port 8081)"
} else {
    $backendDir = Join-Path $Root "blog-backend"

    if (-not $SkipBuild) {
        Write-Host "    Building backend JAR..." -ForegroundColor Gray
        Push-Location $backendDir
        mvn package -DskipTests -q 2>&1 | Out-Null
        Pop-Location
    }

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-File",
        (Join-Path $PSScriptRoot "start-backend.ps1")
    ) -WindowStyle Normal

    if (Wait-Port 8081 "Backend" 120) { Write-Ok "Backend started (http://localhost:8081)" }
}

# 5. Frontend (Vite)
Write-Step "Frontend"
if (Test-Port 3000) {
    Write-Warn "Frontend already running (port 3000)"
} else {
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-File",
        (Join-Path $PSScriptRoot "start-frontend.ps1")
    ) -WindowStyle Normal
    if (Wait-Port 3000 "Frontend" 30) { Write-Ok "Frontend started (http://localhost:3000)" }
}

# Summary
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  BlogHub Startup Complete" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$svcs = @(
    @{ N="MySQL";         P=3306; U="" },
    @{ N="Redis";         P=6379; U="" },
    @{ N="XXL-Job Admin"; P=8082; U="http://localhost:8082/xxl-job-admin" },
    @{ N="Backend";       P=8081; U="http://localhost:8081" },
    @{ N="Frontend";      P=3000; U="http://localhost:3000" }
)
foreach ($s in $svcs) {
    $ok = Test-Port $s.P
    $icon = if ($ok) { "[ON] " } else { "[OFF]" }
    $clr  = if ($ok) { "Green" } else { "Red" }
    $url  = if ($s.U -and $ok) { "  $($s.U)" } else { "" }
    Write-Host ("  {0} {1,-15}{2}" -f $icon, $s.N, $url) -ForegroundColor $clr
}
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
