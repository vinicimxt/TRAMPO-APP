param(
    [Parameter(Position = 0)]
    [string]$Serial
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$androidSdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($adbCommand) { $adbCommand.Source } else { Join-Path $androidSdkPath 'platform-tools\adb.exe' }
if (-not (Test-Path -LiteralPath $adbPath)) {
    throw 'ADB não encontrado. Instale o Android SDK Platform Tools ou configure ANDROID_HOME.'
}
if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath $androidSdkPath)) {
    $env:ANDROID_HOME = $androidSdkPath
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $studioJdk = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $studioJdk 'bin\java.exe')) {
        $env:JAVA_HOME = $studioJdk
    }
}

& $adbPath start-server
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o servidor ADB.' }
$deviceLines = @(& $adbPath devices)
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar os dispositivos ADB.' }
$devices = @($deviceLines | Select-Object -Skip 1 | ForEach-Object {
    if ($_ -match '^(\S+)\s+(\S+)(?:\s|$)') {
        [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
    }
})

if ($Serial) {
    $selected = $devices | Where-Object Serial -eq $Serial | Select-Object -First 1
    if (-not $selected) {
        & $adbPath devices
        throw "Dispositivo $Serial não encontrado."
    }
} elseif ($devices.Count -eq 0) {
    throw 'Nenhum dispositivo Android conectado.'
} elseif ($devices.Count -gt 1) {
    & $adbPath devices
    throw 'Mais de um dispositivo conectado. Selecione explicitamente: .\scripts\run-device-debug.ps1 SERIAL'
} else {
    $selected = $devices[0]
}

if ($selected.State -eq 'unauthorized') {
    throw 'Autorize a depuração USB no dispositivo Android.'
}
if ($selected.State -ne 'device') {
    throw "Dispositivo $($selected.Serial) indisponível (estado: $($selected.State))."
}
Write-Host "[OK] Dispositivo encontrado: $($selected.Serial)"

& $adbPath -s $selected.Serial reverse tcp:5165 tcp:5165
if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar adb reverse.' }
$reverseOutput = @(& $adbPath -s $selected.Serial reverse --list)
if ($LASTEXITCODE -ne 0 -or -not ($reverseOutput | Where-Object { $_ -match '(?:^|\s)tcp:5165\s+tcp:5165(?:\s|$)' })) {
    throw "ADB reverse não confirmado. Saída: $($reverseOutput -join '; ')"
}
Write-Host '[OK] ADB reverse: tcp:5165 -> tcp:5165'

Push-Location $projectRoot
try {
    if ($devices.Count -gt 1) {
        & (Join-Path $projectRoot 'gradlew.bat') assembleDebug '-PtrampoDebugApiUrl=http://127.0.0.1:5165/'
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o APK Debug.' }
        & $adbPath -s $selected.Serial install -r (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk')
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar o APK no dispositivo selecionado.' }
    } else {
        & (Join-Path $projectRoot 'gradlew.bat') installDebug '-PtrampoDebugApiUrl=http://127.0.0.1:5165/'
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar o APK Debug.' }
    }
} finally {
    Pop-Location
}
Write-Host '[OK] APK instalado'
Write-Host '[OK] TRAMPO pronto para usar http://127.0.0.1:5165/'
