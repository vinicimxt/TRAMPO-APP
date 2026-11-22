param(
    [Parameter(Position = 0)]
    [string]$Serial
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$adbPath = (Get-Command adb.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
$defaultSdkPath = if ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not $adbPath) {
    foreach ($sdkPath in @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME, $defaultSdkPath)) {
        if (-not $sdkPath) { continue }
        $candidate = Join-Path $sdkPath 'platform-tools\adb.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $adbPath = (Resolve-Path -LiteralPath $candidate).Path
            break
        }
    }
}
if (-not $adbPath) {
    throw 'ADB não encontrado. Instale Android SDK Platform-Tools pelo Android Studio.'
}
Write-Host "[OK] ADB encontrado em $adbPath"

$adbSdkPath = Split-Path -Parent (Split-Path -Parent $adbPath)
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT -and
    (Test-Path -LiteralPath (Join-Path $adbSdkPath 'platform-tools\adb.exe') -PathType Leaf)) {
    $env:ANDROID_HOME = $adbSdkPath
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $studioJdk = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $studioJdk 'bin\java.exe')) {
        $env:JAVA_HOME = $studioJdk
    }
}

& $adbPath start-server
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível iniciar o servidor ADB.' }
$usbSerial = & $adbPath -d get-serialno
if ($LASTEXITCODE -ne 0 -or -not $usbSerial) {
    throw 'Conecte um único dispositivo Android físico via USB e autorize a depuração USB.'
}
if ($Serial -and $Serial -cne $usbSerial) {
    throw "O dispositivo USB conectado é $usbSerial. Dispositivo solicitado: $Serial."
}
$state = & $adbPath -s $usbSerial get-state
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') {
    throw 'Autorize a depuração USB no dispositivo Android e tente novamente.'
}
Write-Host "[OK] Dispositivo USB encontrado: $usbSerial"

& $adbPath -s $usbSerial reverse tcp:5165 tcp:5165
if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar adb reverse.' }
$reverseOutput = @(& $adbPath -s $usbSerial reverse --list)
if ($LASTEXITCODE -ne 0 -or -not ($reverseOutput | Where-Object { $_ -match '(?:^|\s)tcp:5165\s+tcp:5165(?:\s|$)' })) {
    throw "ADB reverse não confirmado. Saída: $($reverseOutput -join '; ')"
}
Write-Host "[OK] ADB reverse confirmado: $($reverseOutput -join ', ')"

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') assembleDebug '-PtrampoDebugApiUrl=http://127.0.0.1:5165/'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o APK Debug.' }
    & $adbPath -s $usbSerial install -r -t (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar o APK no dispositivo selecionado.' }
} finally {
    Pop-Location
}
Write-Host '[OK] APK instalado'
Write-Host '[OK] TRAMPO pronto para usar http://127.0.0.1:5165/'
