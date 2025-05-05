$DebugEnabled = $false  # oder $true zum Anschalten

# 1. WPAD-Skript ausführen
$wpadScript = Join-Path $PSScriptRoot "get-wpad-url.ps1"

if (-not (Test-Path $wpadScript)) {
    Write-Host "[ERROR] get-wpad-url.ps1 nicht gefunden."
    exit 1
}

$pacUrl = & $wpadScript

if (-not $pacUrl) {
    Write-Host "[ERROR] Keine PAC-URL gefunden."
    exit 1
}

# 1a. Debug-Ausgabe: Lade und zeige PAC-Datei korrekt als Text
try {
    $response = Invoke-WebRequest -Uri $pacUrl.Trim() -UseBasicParsing
    $bytes = $response.Content
    $pacContent = [System.Text.Encoding]::UTF8.GetString($bytes)

    if ($DebugEnabled) {
        Write-Host "`n========== PAC-Datei (Debug-Ausgabe) ==========" -ForegroundColor Cyan
        Write-Output $pacContent
        Write-Host "===============================================`n"
    }
} catch {
    Write-Host "[ERROR] PAC-Datei konnte nicht geladen werden für Debug-Ausgabe: $pacUrl"
    exit 1
}

# 2. Parser-Skript aufrufen
$parserScript = Join-Path $PSScriptRoot "parse-pac.ps1"

if (-not (Test-Path $parserScript)) {
    Write-Host "[ERROR] parse-pac.ps1 nicht gefunden."
    exit 1
}

$proxyHostPort = & $parserScript -PacUrl $pacUrl -DebugEnabled:$DebugEnabled

if (-not $proxyHostPort -or $proxyHostPort -notmatch ':') {
    Write-Host "[ERROR] Kein Proxy aus PAC extrahiert."
    exit 1
}

# 3. Host und Port trennen
$parts = $proxyHostPort -split ':'
$proxyHost = $parts[0]
$proxyPort = $parts[1]

# 4. Git konfigurieren
Write-Host "[INFO] Setze Git-Proxy auf http://${proxyHost}:${proxyPort}"
git config --global http.proxy "http://${proxyHost}:${proxyPort}"
git config --global https.proxy "http://${proxyHost}:${proxyPort}"

Write-Host "[SUCCESS] Git-Proxy erfolgreich gesetzt."
