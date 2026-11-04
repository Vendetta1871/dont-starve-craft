#!/usr/bin/env bash
# Runs every headless scenario that needs no connected player. Exit code is
# non-zero if any scenario fails. PLAYER scenarios (ghost, wipe) must be run
# by hand with a connected client — see README.md.
set -u
cd "$(dirname "$0")/.."
export JAVA_HOME="${DST_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
port=${1:-25699}
status=0
for scenario in smoke grave meteorite portal; do
    echo "=== $scenario ==="
    python3 tests/servertest.py . "tests/scenarios/$scenario.txt" --port "$port" 2>&1 |
        grep -E '^(PASS|FAIL|===|SERVER|port|!!)'
    rc=${PIPESTATUS[0]}
    [ "$rc" -ne 0 ] && status=1
    port=$((port + 10))
done
echo "=== all: $([ "$status" -eq 0 ] && echo OK || echo FAILED) ==="
exit "$status"
