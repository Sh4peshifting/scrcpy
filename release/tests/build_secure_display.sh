#!/bin/bash
set -euxo pipefail

fixture="$PWD/release/tests/secure-display"
work="$PWD/release/work/secure-display"
tools="$ANDROID_HOME/build-tools/36.0.0"
android="$ANDROID_HOME/platforms/android-36/android.jar"
mkdir -p "$work/classes" "$work/dex"
javac -source 8 -target 8 -bootclasspath "$android" -d "$work/classes" "$fixture/SecureActivity.java"
"$tools/d8" --lib "$android" --output "$work/dex" "$work/classes/com/genymobile/scrcpy/test/SecureActivity.class"
"$tools/aapt2" link -I "$android" --manifest "$fixture/AndroidManifest.xml" -o "$work/test.apk"
zip -j "$work/test.apk" "$work/dex/classes.dex"
keytool -genkeypair -keystore "$work/test.keystore" -storepass android -keypass android \
    -alias test -dname CN=scrcpy-test -keyalg RSA -validity 30
"$tools/apksigner" sign --ks "$work/test.keystore" --ks-pass pass:android "$work/test.apk"
