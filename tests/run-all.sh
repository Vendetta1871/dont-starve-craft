#!/usr/bin/env bash
# Runs scenarios in one server session (one server start, one headless client for the
# scenarios that need a player); `ds test reset` gives each scenario a clean slate.
#   bash tests/run-all.sh                 # every scenario
#   bash tests/run-all.sh crafting smoke  # just these, in the canonical order below
#   SKIP_PLAYER=1 bash tests/run-all.sh   # only the scenarios without a player
# Exit code is non-zero if any scenario fails.
set -u
cd "$(dirname "$0")/.."
export JAVA_HOME="${DST_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"
port=${PORT:-25699}
# Server-only scenarios first (no player around), wipe last (it ends the run); smoke
# must stay first, it checks the fresh world's island 0.
all="smoke grave meteorite island speed clock ghost run codex harness clockview stats perish crafting tools trees flora fire wipe"
for s in "$@"; do
    [[ " $all " == *" $s "* ]] || { echo "unknown scenario: $s" >&2; exit 2; }
done
files=()
for s in $all; do
    [ $# -gt 0 ] && [[ " $* " != *" $s "* ]] && continue
    f="tests/scenarios/$s.txt"
    [ -n "${SKIP_PLAYER:-}" ] && grep -q '{P}' "$f" && continue
    files+=("$f")
done
python3 tests/servertest.py . "${files[@]}" --port "$port" 2>&1 |
    grep -E '^(PASS|FAIL|===|SERVER|CLIENT|SHOT|port|!!|---)'
exit "${PIPESTATUS[0]}"
