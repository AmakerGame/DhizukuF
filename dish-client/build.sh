#!/bin/sh
# Rebuilds app/src/main/assets/dish_dhizukuf.dex from dish-client/src.
# Needs a JDK (javac) and one dex tool:
#   - d8 from the Android SDK build-tools (set ANDROID_HOME, or D8=/path/to/d8), or
#   - dx (Debian/Ubuntu: apt install dalvik-exchange), or DX=/path/to/dx.
# android.* classes come from the stubs/ folder (compile-time only).
# The result is checked with verify_dex.py (if python3 exists) before it replaces the old file, so a
# failed build never leaves a broken dex behind.
set -e
cd "$(dirname "$0")"
OUT=../app/src/main/assets/dish_dhizukuf.dex

find_d8() {
  if [ -n "$D8" ]; then echo "$D8"; return; fi
  if command -v d8 >/dev/null 2>&1; then command -v d8; return; fi
  for root in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" /usr/local/lib/android/sdk "$HOME/Android/Sdk"; do
    [ -n "$root" ] || continue
    best=$(ls -d "$root"/build-tools/*/d8 2>/dev/null | sort -V | tail -n 1)
    if [ -n "$best" ]; then echo "$best"; return; fi
  done
}

rm -rf build
mkdir -p build/stubs build/classes build/dex

# --release 8 keeps the bytecode simple; fall back for JDKs that no longer ship it.
JAVAC="javac"
REL="--release 8"
$JAVAC $REL -Xlint:-options -d build/stubs $(find stubs -name '*.java') 2>/dev/null || REL=""
$JAVAC $REL -Xlint:-options -d build/stubs $(find stubs -name '*.java')
$JAVAC $REL -Xlint:-options -cp build/stubs -d build/classes $(find src -name '*.java')

D8BIN=$(find_d8)
if [ -n "$D8BIN" ]; then
  LIB=""
  for root in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" /usr/local/lib/android/sdk "$HOME/Android/Sdk"; do
    [ -n "$root" ] || continue
    jar=$(ls "$root"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -n 1)
    if [ -n "$jar" ]; then LIB="--lib $jar"; break; fi
  done
  "$D8BIN" --release --min-api 26 $LIB --output build/dex $(find build/classes -name '*.class')
  NEW=build/dex/classes.dex
else
  DX=${DX:-$(command -v dx || echo /usr/lib/android-sdk/build-tools/debian/dx)}
  "$DX" --dex --output=build/dex/classes.dex build/classes
  NEW=build/dex/classes.dex
fi

[ -s "$NEW" ] || { echo "no dex was produced" >&2; exit 1; }
if command -v python3 >/dev/null 2>&1; then
  python3 verify_dex.py "$NEW"
fi
mkdir -p "$(dirname "$OUT")"
cp "$NEW" "$OUT"
echo "OK: $OUT"
