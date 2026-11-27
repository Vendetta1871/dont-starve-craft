#!/usr/bin/env bash
# Runs every scenario. Exit code is non-zero if any scenario fails. PLAYER
# scenarios get a headless, silent game client (servertest.py --client ->
# tools/autotest.sh in gamescope); SKIP_PLAYER=1 runs only the server-only ones.
set -u
cd "$(dirname "$0")/.."
export JAVA_HOME="${DST_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
port=${1:-25699}
status=0
player_scenarios="ghost run codex harness wipe"
[ -n "${SKIP_PLAYER:-}" ] && player_scenarios=""
for scenario in smoke grave meteorite island speed $player_scenarios; do
    echo "=== $scenario ==="
    extra=""
    case " $player_scenarios " in *" $scenario "*) extra="--client --player Tester" ;; esac
    python3 tests/servertest.py . "tests/scenarios/$scenario.txt" --port "$port" $extra 2>&1 |
        grep -E '^(PASS|FAIL|===|SERVER|CLIENT|SHOT|port|!!)'
    rc=${PIPESTATUS[0]}
    [ "$rc" -ne 0 ] && status=1
    port=$((port + 10))
done
echo "=== all: $([ "$status" -eq 0 ] && echo OK || echo FAILED) ==="
exit "$status"
