#!/bin/bash
set -euxo pipefail

# Run only against the disposable emulator created by the workflow.
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb root
adb wait-for-device
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb install -r release/work/secure-display/test.apk
adb shell am start -W -n com.genymobile.scrcpy.test/.SecureActivity
sleep 2
mkdir -p release/work/smoke
client="$PWD/release/work/build-linux-x86_64/dist/scrcpy"
export SCRCPY_SERVER_PATH="$PWD/release/work/build-server/server/scrcpy-server"
export ADB="$(command -v adb)"
export SDL_VIDEODRIVER=dummy
for mode in regular secure
do
    args=()
    if [[ "$mode" == secure ]]; then args+=(--capture-secure); fi
    timeout 45 "$client" "${args[@]}" --no-audio --no-control --no-playback \
        --record="release/work/smoke/$mode.mkv" --time-limit=3 --max-size=640 \
        --verbosity=debug 2>&1 | tee "release/work/smoke/$mode.log"
    ffmpeg -y -i "release/work/smoke/$mode.mkv" -frames:v 1 \
        -vf 'crop=64:64:(iw-64)/2:(ih-64)/2' -f rawvideo -pix_fmt rgb24 \
        "release/work/smoke/$mode.rgb"
done
python3 - <<'PY'
from pathlib import Path
for name in ("regular", "secure"):
    data = Path(f"release/work/smoke/{name}.rgb").read_bytes()
    assert len(data) == 64 * 64 * 3, (name, len(data))
    rgb = [sum(data[c::3]) / (len(data) // 3) for c in range(3)]
    print(name, rgb)
    if name == "regular":
        assert max(rgb) < 40, "FLAG_SECURE must be black in ordinary capture"
    else:
        assert rgb[0] > 200 and rgb[1] < 50 and rgb[2] > 200, "Secure capture must show magenta"
PY
# Verify that ordinary shell audio calls still work after privileged creation.
if (( $(adb shell getprop ro.build.version.sdk | tr -d '\r') >= 33 )); then
    timeout 45 "$client" --capture-secure --require-audio --no-control --no-playback \
        --record=release/work/smoke/audio.mkv --time-limit=3 --max-size=640 \
        --verbosity=debug 2>&1 | tee release/work/smoke/audio.log
    test "$(ffprobe -v error -select_streams a -show_entries stream=codec_type \
        -of csv=p=0 release/work/smoke/audio.mkv)" = audio
fi
# Rotate during capture to exercise display destruction and recreation.
adb shell settings put system accelerometer_rotation 0
timeout 45 "$client" --capture-secure --no-audio --no-control --no-playback \
    --record=release/work/smoke/rotated.mkv --time-limit=8 --max-size=640 \
    --verbosity=debug > release/work/smoke/rotated.log 2>&1 &
capture_pid=$!
sleep 3
adb shell settings put system user_rotation 1
wait "$capture_pid"
cat release/work/smoke/rotated.log
ffprobe -v error -show_streams release/work/smoke/rotated.mkv
adb logcat -d > release/work/smoke/logcat.txt
