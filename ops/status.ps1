#Requires -Version 5.1

[Console]::OutputEncoding = [Text.Encoding]::UTF8
[Console]::InputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.Encoding]::UTF8

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

function Get-PidOnPort([int]$Port) {
    $lines = netstat -ano 2>$null | Select-String ":${Port}\s" | Select-String "LISTENING"
    $pids = $lines | ForEach-Object { ($_ -split '\s+')[-1] } | Where-Object { $_ -match '^\d+$' } | Select-Object -Unique
    return ($pids -join ", ")
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  BlogHub Service Status" -ForegroundColor Cyan
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
    $procId = if ($ok) { Get-PidOnPort $s.P } else { "-" }
    $url  = if ($s.U -and $ok) { "  $($s.U)" } else { "" }
    Write-Host ("  {0} {1,-15} PID: {2,-8}{3}" -f $icon, $s.N, $procId, $url) -ForegroundColor $clr
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
