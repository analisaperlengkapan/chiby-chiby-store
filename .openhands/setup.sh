#!/usr/bin/env bash
#
# .openhands/setup.sh — prepared environment for the Chiby Chiby Store repo.
#
# OpenHands runs this once every time it starts working with this repository.
# See https://docs.openhands.dev/openhands/usage/customization/repository
#
# It installs what the Android build needs (a JDK and the Android SDK) and
# warms the Gradle caches, so the first `./gradlew` call in a session does not
# pay for downloads. The build itself needs JDK 17+, Android platform 36 and
# build-tools 36.0.0 (see app/build.gradle and AGENTS.md). platform-tools (adb)
# is installed too when a sdkmanager is available, because the repo ships an
# instrumented androidTest suite; it is best-effort since builds do not need it.
#
# Design rules:
#   * Idempotent  — safe to run every session; anything already present is reused.
#   * Non-fatal   — a failing step logs a warning and the script continues, so a
#                   flaky network never blocks the session. There is no `set -e`.
#   * Configurable— every version and path below can be overridden from the env.
#
# Usage:
#   bash .openhands/setup.sh                  # normal setup
#   SETUP_DOCTOR=1 bash .openhands/setup.sh   # only report what is installed
#   SETUP_BUILD=1  bash .openhands/setup.sh   # also run :app:assembleDebug
#   SETUP_WARM_TESTS=1 bash .openhands/setup.sh  # also warm the Robolectric jars
#
# Environment overrides (all optional):
#   SETUP_JDK_MAJOR          preferred JDK major version            (default 17)
#   SETUP_JDK_FALLBACK       JDK major used when preferred missing  (default 21)
#   SETUP_ANDROID_SDK        Android SDK root to use/create         (default $HOME/android-sdk)
#   SETUP_ANDROID_PLATFORM   platforms;android-N to install         (default 36)
#   SETUP_BUILD_TOOLS        build-tools version to install         (default 36.0.0)
#   SETUP_CMDLINE_TOOLS_BUILD  commandlinetools-linux build number  (default 13114758)
#   SETUP_WARM               resolve app dependencies at the end    (default 1)
#   SETUP_WARM_TIMEOUT       seconds allowed for the warm step      (default 900)

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

JDK_MAJOR="${SETUP_JDK_MAJOR:-17}"
JDK_FALLBACK="${SETUP_JDK_FALLBACK:-21}"
ANDROID_PLATFORM="${SETUP_ANDROID_PLATFORM:-36}"
BUILD_TOOLS="${SETUP_BUILD_TOOLS:-36.0.0}"
CMDLINE_TOOLS_BUILD="${SETUP_CMDLINE_TOOLS_BUILD:-13114758}"
WARM="${SETUP_WARM:-1}"
WARM_TIMEOUT="${SETUP_WARM_TIMEOUT:-900}"

log()  { printf '\033[1;35m[chiby-setup]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[chiby-setup] ok\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[chiby-setup] warn\033[0m %s\n' "$*" >&2; }
have() { command -v "$1" >/dev/null 2>&1; }

# sudo only when needed and available; empty when already root.
SUDO=""
if [ "$(id -u)" -ne 0 ] && have sudo; then
    SUDO="sudo"
fi

download() { # download <url> <dest-file>
    if have curl; then
        curl -fsSL --retry 3 --retry-delay 2 -o "$2" "$1"
    elif have wget; then
        wget -q -O "$2" "$1"
    else
        warn "neither curl nor wget is available; cannot download $1"
        return 1
    fi
}

extract_zip() { # extract_zip <zip> <dest-dir>
    if have unzip; then
        unzip -q -o "$1" -d "$2"
    elif have python3; then
        python3 -c 'import sys, zipfile; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])' "$1" "$2"
    else
        warn "no unzip and no python3; cannot extract $1"
        return 1
    fi
}

java_major_of() { # java_major_of <path-to-java> -> prints major version or nothing
    [ -x "$1" ] || return 1
    "$1" -version 2>&1 | head -n 1 | sed -n 's/.*version "\([0-9]*\).*/\1/p'
}

# ---------------------------------------------------------------- JDK --------

jdk_home=""

consider_jdk() { # consider_jdk <home> <wanted-major>
    local home="$1" want="$2"
    [ -n "$home" ] || return 1
    [ -x "$home/bin/java" ] || return 1
    local major
    major="$(java_major_of "$home/bin/java")"
    [ "$major" = "$want" ] || return 1
    jdk_home="$home"
}

find_jdk() { # find_jdk <major>
    local want="$1" candidate resolved

    consider_jdk "${JAVA_HOME:-}" "$want" && return 0

    for candidate in \
        "/usr/lib/jvm/java-${want}-openjdk-amd64" \
        "/usr/lib/jvm/temurin-${want}-jdk-amd64" \
        "/usr/lib/jvm/temurin-${want}-jdk" \
        "/usr/lib/jvm/java-${want}-openjdk" \
        "/opt/java/openjdk" \
        "$HOME/.local/share/chiby/jdk-${want}" ; do
        consider_jdk "$candidate" "$want" && return 0
    done

    # Any install of the wanted major under /usr/lib/jvm (names vary by distro).
    for candidate in /usr/lib/jvm/*; do
        consider_jdk "$candidate" "$want" && return 0
    done

    # Follow whatever `java` resolves to right now.
    if have java; then
        resolved="$(readlink -f "$(command -v java)" 2>/dev/null)"
        consider_jdk "$(dirname "$(dirname "$resolved")")" "$want" && return 0
    fi

    return 1
}

install_jdk_apt() { # install_jdk_apt <major>
    local major="$1"
    have apt-get || return 1
    log "installing OpenJDK $major via apt-get (this can take a minute)"
    $SUDO apt-get update -qq >/dev/null 2>&1
    $SUDO apt-get install -y "openjdk-${major}-jdk-headless" >/dev/null 2>&1
}

install_jdk_tarball() { # install_jdk_tarball <major>
    local major="$1"
    local dest="$HOME/.local/share/chiby/jdk-${major}"
    local url="https://api.adoptium.net/v3/binary/latest/${major}/ga/linux/x64/jdk/hotspot/normal/eclipse"
    local tmp="/tmp/chiby-jdk-${major}.tar.gz"

    [ -x "$dest/bin/java" ] && return 0
    log "downloading Temurin JDK $major from Adoptium"
    download "$url" "$tmp" || return 1
    mkdir -p "$dest" || return 1
    tar -xzf "$tmp" -C "$dest" --strip-components=1 || return 1
    rm -f "$tmp"
}

ensure_java() {
    log "locating JDK $JDK_MAJOR"
    find_jdk "$JDK_MAJOR" && { ok "JDK $JDK_MAJOR at $jdk_home"; return 0; }

    if find_jdk "$JDK_FALLBACK"; then
        warn "JDK $JDK_MAJOR not found; using JDK $JDK_FALLBACK at $jdk_home (build was verified to work on 21)"
        return 0
    fi

    install_jdk_apt "$JDK_MAJOR" && find_jdk "$JDK_MAJOR" && { ok "JDK $JDK_MAJOR at $jdk_home"; return 0; }
    install_jdk_tarball "$JDK_MAJOR" && find_jdk "$JDK_MAJOR" && { ok "JDK $JDK_MAJOR at $jdk_home"; return 0; }

    warn "could not provision a JDK; Gradle will need JAVA_HOME to be set manually"
    return 1
}

export_java() {
    [ -n "$jdk_home" ] || return 0
    export JAVA_HOME="$jdk_home"
    export PATH="$jdk_home/bin:$PATH"

    # Persist for future shells and for any process that ignores JAVA_HOME.
    # This is machine-local state outside the repo, so it never leaks into CI.
    local gprops="$HOME/.gradle/gradle.properties"
    mkdir -p "$HOME/.gradle" 2>/dev/null
    if [ -f "$gprops" ]; then
        grep -v '^org\.gradle\.java\.home=' "$gprops" > "$gprops.tmp" 2>/dev/null && mv "$gprops.tmp" "$gprops"
    fi
    printf 'org.gradle.java.home=%s\n' "$jdk_home" >> "$gprops"

    # gradlew itself needs a java on PATH (or JAVA_HOME) just to bootstrap.
    # Replace the managed block on every run: if a later run selects a different
    # JDK, a stale export would point new shells at a removed installation.
    local bashrc="$HOME/.bashrc"
    touch "$bashrc" 2>/dev/null
    local marker="# chiby-setup: JAVA_HOME (added by .openhands/setup.sh)"
    local desired
    desired="$(printf '%s\nexport JAVA_HOME="%s"\ncase ":$PATH:" in *":$JAVA_HOME/bin:"*) ;; *) export PATH="$JAVA_HOME/bin:$PATH" ;; esac\n' "$marker" "$jdk_home")"

    if grep -qF "$marker" "$bashrc" 2>/dev/null; then
        # Strip the old managed block (the marker line plus the two lines it
        # owns, and any blank line left directly above it) before re-adding it.
        local tmp="$bashrc.chiby.tmp"
        awk -v marker="$marker" '
            BEGIN { skip = 0; blank = 0 }
            skip > 0 { skip--; next }
            $0 == marker { blank = 0; skip = 2; next }
            /^[[:space:]]*$/ { blank++; buf[blank] = $0; next }
            { for (i = 1; i <= blank; i++) print buf[i]; blank = 0; print }
            END { for (i = 1; i <= blank; i++) print buf[i] }
        ' "$bashrc" > "$tmp" 2>/dev/null && mv "$tmp" "$bashrc"
    fi
    printf '\n%s\n' "$desired" >> "$bashrc"
    ok "JAVA_HOME=$jdk_home (recorded in ~/.gradle/gradle.properties and ~/.bashrc)"
}

# --------------------------------------------------------- Android SDK -------

sdk_root=""
sdkmanager=""

find_android_sdk() {
    local candidate
    for candidate in "${SETUP_ANDROID_SDK:-}" "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" \
                     "$HOME/android-sdk" "/usr/local/lib/android/sdk" "/opt/android-sdk" "/opt/android/sdk"; do
        [ -n "$candidate" ] || continue
        [ -d "$candidate" ] || continue
        sdk_root="$candidate"
        return 0
    done
    sdk_root="${SETUP_ANDROID_SDK:-$HOME/android-sdk}"
    return 0
}

sdkmanager_path() { # sdkmanager_path <sdk-root>
    local root="$1" p
    for p in "$root/cmdline-tools/latest/bin/sdkmanager" \
             "$root/cmdline-tools/bin/sdkmanager" \
             "$root/tools/bin/sdkmanager"; do
        if [ -x "$p" ]; then printf '%s\n' "$p"; return 0; fi
    done
    return 1
}

ensure_cmdline_tools() {
    if sdkmanager="$(sdkmanager_path "$sdk_root")"; then
        ok "sdkmanager found at $sdkmanager"
        return 0
    fi

    local url="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"
    local zip="/tmp/chiby-cmdline-tools.zip"
    local unpack="/tmp/chiby-cmdline-tools"

    log "installing Android command-line tools (build $CMDLINE_TOOLS_BUILD)"
    rm -rf "$unpack"
    download "$url" "$zip" || { warn "download failed: $url"; return 1; }
    mkdir -p "$unpack" || return 1
    extract_zip "$zip" "$unpack" || return 1

    mkdir -p "$sdk_root/cmdline-tools" || return 1
    rm -rf "$sdk_root/cmdline-tools/latest"
    mv "$unpack/cmdline-tools" "$sdk_root/cmdline-tools/latest" || return 1
    chmod +x "$sdk_root/cmdline-tools/latest/bin/"* 2>/dev/null
    rm -f "$zip"

    sdkmanager="$(sdkmanager_path "$sdk_root")" || { warn "sdkmanager still not found"; return 1; }
    ok "sdkmanager installed at $sdkmanager"
}

install_android_packages() {
    local want_platform="$sdk_root/platforms/android-${ANDROID_PLATFORM}"
    local want_build_tools="$sdk_root/build-tools/${BUILD_TOOLS}"

    if [ -d "$want_platform" ] && [ -d "$want_build_tools" ]; then
        ok "platform ${ANDROID_PLATFORM} and build-tools ${BUILD_TOOLS} already installed"
        return 0
    fi

    log "installing platforms;android-${ANDROID_PLATFORM} build-tools;${BUILD_TOOLS} platform-tools"
    # sdkmanager reads JAVA_HOME; export_java runs before this.
    yes 2>/dev/null | "$sdkmanager" --sdk_root="$sdk_root" --licenses >/dev/null 2>&1
    "$sdkmanager" --sdk_root="$sdk_root" \
        "platforms;android-${ANDROID_PLATFORM}" \
        "build-tools;${BUILD_TOOLS}" \
        "platform-tools" >/dev/null 2>&1

    if [ -d "$want_platform" ]; then
        ok "Android SDK packages installed under $sdk_root"
        return 0
    fi
    warn "Android SDK package install did not complete; check $sdk_root"
    return 1
}

sdk_has_required_packages() {
    [ -d "$sdk_root/platforms/android-${ANDROID_PLATFORM}" ] &&
        [ -d "$sdk_root/build-tools/${BUILD_TOOLS}" ]
}

ensure_android_sdk() {
    find_android_sdk
    log "using Android SDK root $sdk_root"

    # An SDK can already carry the platform and build-tools Gradle needs while
    # having no command-line tools (sdkmanager). Gradle only reads sdk.dir, so
    # accept and export such an SDK without trying to provision anything —
    # otherwise an offline machine with a perfectly usable SDK gets left
    # unconfigured when the sdkmanager download fails.
    if sdk_has_required_packages; then
        ok "Android SDK at $sdk_root already has platform ${ANDROID_PLATFORM} and build-tools ${BUILD_TOOLS}"
        # Gradle only needs the platform and build-tools, but adb lives in
        # platform-tools. The app ships an instrumented androidTest suite
        # (app/src/androidTest), so try to add platform-tools when a sdkmanager
        # is available and they are missing. This is best-effort: a download
        # failure must not fail the SDK setup, since Gradle builds do not need
        # adb, and the script is non-fatal by design.
        if [ ! -d "$sdk_root/platform-tools" ] && sdkmanager="$(sdkmanager_path "$sdk_root")"; then
            log "installing platform-tools so adb is available for instrumented tests"
            yes 2>/dev/null | "$sdkmanager" --sdk_root="$sdk_root" --licenses >/dev/null 2>&1
            "$sdkmanager" --sdk_root="$sdk_root" platform-tools >/dev/null 2>&1
            if [ -d "$sdk_root/platform-tools" ]; then
                ok "platform-tools installed under $sdk_root"
            else
                warn "platform-tools missing; adb unavailable for instrumented tests (builds are unaffected)"
            fi
        fi
    else
        ensure_cmdline_tools || return 1
        install_android_packages || return 1

        if ! sdk_has_required_packages; then
            warn "Android SDK at $sdk_root is missing platform ${ANDROID_PLATFORM} or build-tools ${BUILD_TOOLS}"
            return 1
        fi
    fi

    export ANDROID_HOME="$sdk_root"
    export ANDROID_SDK_ROOT="$sdk_root"
    ok "ANDROID_HOME=$sdk_root"
}

write_local_properties() {
    # Never point Gradle at an SDK that lacks the required platform and
    # build-tools: that turns an optional provisioning failure into a persistent
    # build failure. On failure leave any existing local.properties untouched.
    if ! sdk_has_required_packages; then
        warn "not writing local.properties: $sdk_root lacks platform ${ANDROID_PLATFORM} / build-tools ${BUILD_TOOLS}"
        return 1
    fi
    # Gradle reads local.properties natively for sdk.dir; the file is gitignored.
    printf 'sdk.dir=%s\n' "$sdk_root" > "$REPO_ROOT/local.properties"
    ok "wrote local.properties (sdk.dir=$sdk_root)"
}

# ------------------------------------------------------------- Gradle --------

prepare_gradle() {
    chmod +x "$REPO_ROOT/gradlew" 2>/dev/null
    ok "gradlew is executable"
}

warm_gradle() {
    [ "$WARM" = "1" ] || { log "SETUP_WARM=$WARM, skipping dependency warm-up"; return 0; }
    log "warming Gradle (wrapper + app dependencies); bounded to ${WARM_TIMEOUT}s"

    ( cd "$REPO_ROOT" && timeout "$WARM_TIMEOUT" ./gradlew --no-daemon --console=plain \
        :app:dependencies --configuration debugRuntimeClasspath >/tmp/chiby-setup-gradle.log 2>&1 )
    if [ $? -eq 0 ]; then
        ok "Gradle dependencies resolved and cached"
    else
        warn "Gradle warm-up did not finish (see /tmp/chiby-setup-gradle.log); the build will still work, it will just download on first use"
    fi
}

optional_build() {
    [ "${SETUP_BUILD:-0}" = "1" ] || return 0
    log "running :app:assembleDebug (SETUP_BUILD=1)"
    ( cd "$REPO_ROOT" && timeout "$WARM_TIMEOUT" ./gradlew --no-daemon --console=plain assembleDebug >/tmp/chiby-setup-build.log 2>&1 )
    if [ $? -eq 0 ]; then
        ok "assembleDebug succeeded"
    else
        warn "assembleDebug failed (see /tmp/chiby-setup-build.log)"
    fi
}

optional_warm_tests() {
    [ "${SETUP_WARM_TESTS:-0}" = "1" ] || return 0
    log "warming Robolectric/unit-test artifacts (SETUP_WARM_TESTS=1)"
    ( cd "$REPO_ROOT" && timeout "$WARM_TIMEOUT" ./gradlew --no-daemon --console=plain testDebugUnitTest >/tmp/chiby-setup-tests.log 2>&1 )
    if [ $? -eq 0 ]; then
        ok "unit tests ran and their artifacts are cached"
    else
        warn "unit-test warm-up did not finish (see /tmp/chiby-setup-tests.log)"
    fi
}

doctor() {
    log "doctor report"
    if find_jdk "$JDK_MAJOR"; then
        printf '  JDK %s        : %s\n' "$JDK_MAJOR" "$jdk_home"
    elif find_jdk "$JDK_FALLBACK"; then
        printf '  JDK %s        : %s (fallback)\n' "$JDK_FALLBACK" "$jdk_home"
    else
        printf '  JDK %s        : NOT FOUND\n' "$JDK_MAJOR"
    fi
    find_android_sdk
    printf '  Android SDK   : %s%s\n' "$sdk_root" "$([ -d "$sdk_root" ] || printf ' (missing)')"
    printf '  platform %s   : %s\n' "$ANDROID_PLATFORM" \
        "$([ -d "$sdk_root/platforms/android-${ANDROID_PLATFORM}" ] && printf present || printf 'missing')"
    printf '  build-tools %s: %s\n' "$BUILD_TOOLS" \
        "$([ -d "$sdk_root/build-tools/${BUILD_TOOLS}" ] && printf present || printf 'missing')"
    printf '  platform-tools: %s\n' \
        "$([ -d "$sdk_root/platform-tools" ] && printf present || printf 'missing (no adb)')"
    printf '  gradlew       : %s\n' "$([ -x "$REPO_ROOT/gradlew" ] && printf executable || printf 'not executable')"
}

# ---------------------------------------------------------------- main -------

log "preparing Chiby Chiby Store environment (repo: $REPO_ROOT)"

if [ "${SETUP_DOCTOR:-0}" = "1" ]; then
    doctor
    exit 0
fi

ensure_java
export_java
if ensure_android_sdk; then
    write_local_properties
else
    warn "Android SDK setup did not complete; leaving local.properties unchanged so builds keep using any previously configured sdk.dir"
fi
prepare_gradle
warm_gradle
optional_build
optional_warm_tests

log "setup complete — try: ./gradlew assembleDebug   or   ./gradlew testDebugUnitTest"
