#!/bin/sh
# Headless game client for the PLAYER test scenarios: runs a Gradle client task inside a monitorless
# gamescope on the eGPU, so no window opens on the desktop. Normally started by
# `tests/servertest.py --client`, which passes the server address and player name:
#   tools/autotest.sh runAutotestClient -PautotestServer=localhost:25599 -PautotestUser=Bob
#   AUTOTEST_SIZE="1920 1080"   - window size (default 854 480)
#   AUTOTEST_GPU=10de:1db1      - Vulkan device for gamescope (default: the V100)
#   AUTOTEST_WINDOW=1           - on the desktop instead, without gamescope (only on request)
# Silent: OpenAL Soft runs on its null backend and the client's options.txt zeroes every sound level.
# The log is build/autotest.log; the script exits with the Gradle exit code.
cd "$(dirname "$0")/.." || exit 1
task=${1:-runAutotestClient}
[ $# -gt 0 ] && shift
uid=$(id -u)
export XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/run/user/$uid}
export DBUS_SESSION_BUS_ADDRESS=${DBUS_SESSION_BUS_ADDRESS:-unix:path=$XDG_RUNTIME_DIR/bus}
export JAVA_HOME="${DST_JAVA_HOME:-${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}}"
size=${AUTOTEST_SIZE:-854 480}
w=${size% *} h=${size#* }
mkdir -p build run-autotest
# Muted, no first-launch screens; written once, later runs keep the file.
if [ ! -f run-autotest/options.txt ]; then
    {
        for c in master music record weather block hostile neutral player ambient voice; do
            echo "soundCategory_$c:0.0"
        done
        echo "onboardAccessibility:false"
        echo "skipMultiplayerWarning:true"
        echo "joinedFirstServer:true"
        echo "tutorialStep:none"
        echo "pauseOnLostFocus:false"
    } > run-autotest/options.txt
fi
if [ -n "$AUTOTEST_WINDOW" ]; then
    ALSOFT_DRIVERS=null ./gradlew --no-daemon --console=plain "$task" "$@" > build/autotest.log 2>&1 < /dev/null
    status=$?
else
    # gamescope may crash on shutdown after the client exited, so the Gradle exit code goes through a file.
    rm -f build/autotest.status
    sh -c '"$@"; true' sh env -u DISPLAY -u WAYLAND_DISPLAY XDG_SESSION_TYPE=wayland ALSOFT_DRIVERS=null \
        gamescope --backend headless --prefer-vk-device "${AUTOTEST_GPU:-10de:1db1}" -W "$w" -H "$h" -w "$w" -h "$h" -r 1000 -- \
        sh -c './gradlew --no-daemon --console=plain "$@"; echo $? > build/autotest.status' sh "$task" "$@" \
        > build/autotest.log 2>&1 < /dev/null
    status=$?
    [ -f build/autotest.status ] && status=$(cat build/autotest.status)
fi
grep -E 'BUILD (SUCCESSFUL|FAILED)|Exception' build/autotest.log | head -20
exit $status
