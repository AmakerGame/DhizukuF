#!/bin/sh
# Rebuilds app/src/main/assets/dish_dhizukuf.dex from dish-client/src.
# Needs: a JDK (javac) and dx (Debian/Ubuntu: apt install openjdk-21-jdk-headless dalvik-exchange)
#        or set DX=/path/to/dx. android.* classes come from the stubs/ folder (compile-time only).
set -e
cd "$(dirname "$0")"
DX=${DX:-$(command -v dx || echo /usr/lib/android-sdk/build-tools/debian/dx)}
rm -rf build
mkdir -p build/stubs build/classes
javac --release 8 -Xlint:-options -d build/stubs $(find stubs -name '*.java')
javac --release 8 -Xlint:-options -cp build/stubs -d build/classes $(find src -name '*.java')
mkdir -p ../app/src/main/assets
"$DX" --dex --output=../app/src/main/assets/dish_dhizukuf.dex build/classes
echo "OK: app/src/main/assets/dish_dhizukuf.dex"
