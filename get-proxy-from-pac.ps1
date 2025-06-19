# PowerShell Script: get-proxy-from-pac.ps1

$pacUrl = (Get-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings').AutoConfigURL
if (-not $pacUrl) {
    Write-Error "No PAC URL found"
    exit 1
}

try {
    # Verwende WebClient, damit der Windows-Proxy verwendet wird
    $wc = New-Object System.Net.WebClient
    $pac = $wc.DownloadString($pacUrl)

    # Finde erste PROXY-Zeile (nur bei statischen PACs!)
    if ($pac -match 'PROXY\s+([^\s;]+)') {
        Write-Output $Matches[1]  # z. B. proxy.firma.local:8080
        exit 0
    } else {
        Write-Error "No PROXY entry found"
        exit 2
    }
}
catch {
    Write-Error "PAC download failed: $_"
    exit 3
}
