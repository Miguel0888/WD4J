param (
    [string]$PacUrl,
    [switch]$DebugEnabled  # optionaler Schalter
)

# --- 1. PAC-Datei laden ---
try {
    $response = Invoke-WebRequest -Uri $PacUrl.Trim() -UseBasicParsing
    $bytes = $response.Content
    $pacContent = [System.Text.Encoding]::UTF8.GetString($bytes)
} catch {
    Write-Host "[ERROR] PAC-Datei konnte nicht geladen werden: $PacUrl"
    exit 1
}

if ($DebugEnabled) {
    Write-Host "`n[PARSER] Eingehender PAC-Inhalt:"
    Write-Output $pacContent
    Write-Host "----------"
}

# --- 2. Variablen extrahieren ---
$proxyVars = @{}
$pacContent -split "`n" | ForEach-Object {
    $line = $_.Trim()
    if ($line -match 'var\s+(\w+)\s*=\s*"PROXY\s+([^"\s:]+):(\d+)"') {
        $proxyVars[$Matches[1]] = "$($Matches[2]):$($Matches[3])"
    }
}

if ($proxyVars.Count -eq 0) {
    Write-Host "[ERROR] Keine Proxy-Zuweisungen gefunden."
    exit 1
}

# --- 3. FindProxyForURL() interpretieren ---
$pacContent -join "`n" -match 'function\s+FindProxyForURL\s*\([^\)]*\)\s*\{([^}]+)\}' | Out-Null
$functionBody = $Matches[1] -split "`n"

foreach ($line in $functionBody) {
    $clean = $line.Trim()

    # Direktes return: "PROXY host:port"
    if ($clean -match 'return\s+"PROXY\s+([^"\s:]+):(\d+)"') {
        $host = $Matches[1]
        $port = $Matches[2]
        Write-Output "${host}:${port}"
        return
    }

    # Indirekt: return proxy_var;
    if ($clean -match 'return\s+(\w+);') {
        $var = $Matches[1]
        if ($proxyVars.ContainsKey($var)) {
            Write-Output $proxyVars[$var]
            return
        }
    }
}

Write-Host "[ERROR] Kein gültiges return in FindProxyForURL() gefunden."
exit 1
