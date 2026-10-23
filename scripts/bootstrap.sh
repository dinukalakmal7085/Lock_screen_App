#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh
mkdir -p "$LUMA_TOOLCHAINS/downloads" "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
fetch() { if [[ ! -f "$2" ]]; then curl -fLsS --retry 2 --max-time 240 "$1" -o "$2.tmp"; mv "$2.tmp" "$2"; fi; }
verify() { python3 - "$1" "$2" "$3" <<'PY'
import hashlib,sys
with open(sys.argv[1],'rb') as f: actual=hashlib.file_digest(f,sys.argv[2]).hexdigest()
if actual!=sys.argv[3]:raise SystemExit('Artifact checksum mismatch: '+sys.argv[1])
PY
}
if [[ ! -x "$JAVA_HOME/bin/javac" ]]; then
    fetch https://download.oracle.com/java/21/archive/jdk-21.0.8_linux-x64_bin.tar.gz "$LUMA_TOOLCHAINS/downloads/jdk.tar.gz"
    verify "$LUMA_TOOLCHAINS/downloads/jdk.tar.gz" sha256 d87272944278713fc7a120cf024d2818d136b5debc750aa17045e3c6f045b867
    tar -xzf "$LUMA_TOOLCHAINS/downloads/jdk.tar.gz" -C "$LUMA_TOOLCHAINS"
fi
if [[ ! -x "$LUMA_TOOLCHAINS/gradle-8.11.1/bin/gradle" ]]; then
    fetch https://downloads.gradle.org/distributions/gradle-8.11.1-bin.zip "$LUMA_TOOLCHAINS/downloads/gradle.zip"
    verify "$LUMA_TOOLCHAINS/downloads/gradle.zip" sha256 f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
    unzip -qo "$LUMA_TOOLCHAINS/downloads/gradle.zip" -d "$LUMA_TOOLCHAINS"
    chmod +x "$LUMA_TOOLCHAINS/gradle-8.11.1/bin/gradle"
fi
sdkmanager="$ANDROID_HOME/cmdline-tools/v19/cmdline-tools/bin/sdkmanager"
if [[ ! -x "$sdkmanager" ]]; then
    fetch https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip "$LUMA_TOOLCHAINS/downloads/android-tools-19.zip"
    # Published checksum from Google's HTTPS repository2-1.xml.
    verify "$LUMA_TOOLCHAINS/downloads/android-tools-19.zip" sha1 5fdcc763663eefb86a5b8879697aa6088b041e70
    mkdir -p "$ANDROID_HOME/cmdline-tools/v19"
    unzip -qo "$LUMA_TOOLCHAINS/downloads/android-tools-19.zip" -d "$ANDROID_HOME/cmdline-tools/v19"
    chmod +x "$ANDROID_HOME/cmdline-tools/v19/cmdline-tools/bin/"*
fi
proxy_args=()
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    read -r luma_proxy_host luma_proxy_port < <(python3 - <<'PY'
import os,urllib.parse
u=urllib.parse.urlsplit(os.environ['HTTPS_PROXY']);print(u.hostname,u.port or 80)
PY
)
    proxy_args=(--proxy=http "--proxy_host=$luma_proxy_host" "--proxy_port=$luma_proxy_port")
fi
# Avoid SIGPIPE from yes changing sdkmanager's exit status.
set +o pipefail
yes 2>/dev/null | "$sdkmanager" "--sdk_root=$ANDROID_HOME" "${proxy_args[@]}" --licenses > "$LUMA_TOOLCHAINS/downloads/licenses.log" 2>&1
sdk_license_status=${PIPESTATUS[1]}
set -o pipefail
if [[ "$sdk_license_status" != 0 ]]; then cat "$LUMA_TOOLCHAINS/downloads/licenses.log"; exit "$sdk_license_status"; fi
"$sdkmanager" "--sdk_root=$ANDROID_HOME" "${proxy_args[@]}" 'platforms;android-35' 'build-tools;35.0.0' platform-tools
gradle --no-daemon --console=plain assembleDebug testDebugUnitTest lintDebug
