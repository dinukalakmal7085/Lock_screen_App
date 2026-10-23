#!/usr/bin/env bash
# Source this in the cloud workspace. Local Android Studio uses its own SDK/JDK.
export LUMA_TOOLCHAINS="${LUMA_TOOLCHAINS:-/workspace/toolchains}"
export JAVA_HOME="$LUMA_TOOLCHAINS/jdk-21.0.8"
export ANDROID_HOME="$LUMA_TOOLCHAINS/android"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$LUMA_TOOLCHAINS/android-user-home"
export GRADLE_USER_HOME="$LUMA_TOOLCHAINS/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$LUMA_TOOLCHAINS/gradle-8.11.1/bin:$PATH"
# Retain the cloud proxy's CA rather than disabling TLS verification.
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    luma_proxy_settings=$(python3 - <<'PY'
import os,urllib.parse
u=urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
if u.hostname:
 print(f'-Dhttps.proxyHost={u.hostname} -Dhttps.proxyPort={u.port or 80} -Dhttp.proxyHost={u.hostname} -Dhttp.proxyPort={u.port or 80} -Dhttp.nonProxyHosts=localhost|127.*')
PY
)
    export GRADLE_OPTS="${GRADLE_OPTS:-} $luma_proxy_settings"
    unset luma_proxy_settings
fi
