#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd -- "$(dirname -- "$0")/.." && pwd)"
if ! command -v adb >/dev/null 2>&1; then
    echo "ADB não encontrado no PATH. Instale o Android SDK Platform Tools." >&2
    exit 1
fi

adb start-server
devices_output="$(adb devices)"
entries="$(printf '%s\n' "$devices_output" | awk 'NR > 1 && NF >= 2 { print $1, $2 }')"
count="$(printf '%s\n' "$entries" | awk 'NF { n++ } END { print n+0 }')"

if (( $# > 1 )); then
    echo "Uso: ./scripts/run-device-debug.sh [SERIAL]" >&2
    exit 1
fi

if (( $# == 1 )); then
    serial="$1"
    state="$(printf '%s\n' "$entries" | awk -v wanted="$serial" '$1 == wanted { print $2 }')"
    if [[ -z "$state" ]]; then
        adb devices
        echo "Dispositivo $serial não encontrado." >&2
        exit 1
    fi
elif (( count == 0 )); then
    echo "Nenhum dispositivo Android conectado." >&2
    exit 1
elif (( count > 1 )); then
    adb devices
    echo "Mais de um dispositivo conectado. Selecione explicitamente: ./scripts/run-device-debug.sh SERIAL" >&2
    exit 1
else
    serial="$(printf '%s\n' "$entries" | awk 'NF { print $1 }')"
    state="$(printf '%s\n' "$entries" | awk 'NF { print $2 }')"
fi

if [[ "$state" == "unauthorized" ]]; then
    echo "Autorize a depuração USB no dispositivo Android." >&2
    exit 1
fi
if [[ "$state" != "device" ]]; then
    echo "Dispositivo $serial indisponível (estado: $state)." >&2
    exit 1
fi
echo "[OK] Dispositivo encontrado: $serial"

adb -s "$serial" reverse tcp:5165 tcp:5165
reverse_output="$(adb -s "$serial" reverse --list)"
if ! printf '%s\n' "$reverse_output" | grep -Eq '(^|[[:space:]])tcp:5165[[:space:]]+tcp:5165([[:space:]]|$)'; then
    echo "ADB reverse não confirmado. Saída de adb reverse --list:" >&2
    printf '%s\n' "$reverse_output" >&2
    exit 1
fi
echo "[OK] ADB reverse: tcp:5165 -> tcp:5165"

cd "$project_root"
if (( count > 1 )); then
    ./gradlew assembleDebug -PtrampoDebugApiUrl=http://127.0.0.1:5165/
    adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
else
    ./gradlew installDebug -PtrampoDebugApiUrl=http://127.0.0.1:5165/
fi
echo "[OK] APK instalado"
echo "[OK] TRAMPO pronto para usar http://127.0.0.1:5165/"
