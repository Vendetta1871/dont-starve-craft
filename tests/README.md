# Server tests

Headless scenario tests: `servertest.py` starts a dedicated server for this
checkout (fresh world, seed `dst-test`, RCON on a private port), sends the
commands of a scenario over RCON, prints the responses and evaluates the
assertions. Exit code 0 means every check passed.

```sh
python3 tests/servertest.py . tests/scenarios/smoke.txt
# parallel runs: --port 25711 (game port is 100 lower); DST_JAVA_HOME picks the JDK 21
python3 tests/servertest.py . tests/scenarios/ghost.txt --client --player Bob
bash tests/run-all.sh            # every scenario, one command (SKIP_PLAYER=1: server-only)
```

Scenarios marked SLOW take minutes. Scenarios marked PLAYER need an online
player: `--client` starts one as a headless, silent game client —
`tools/autotest.sh` runs the `runAutotestClient` Gradle run inside a monitorless
gamescope on the eGPU (OpenAL null backend, all sound levels zeroed in
`run-autotest/options.txt`), and the client joins the test server on its own
via `--quickPlayMultiplayer`. Its log is `build/autotest.log`. Never start a
client on the desktop for tests.

## Scenario syntax

One entry per line, commands without the slash:

- `// text` — a comment.
- `#sleep N` — wait N seconds.
- `#wait-player NAME [timeout]` — block until that player is online (default 180 s).
- `#expect REGEX` — the previous command's response must match.
- `#expect-log REGEX [timeout]` — wait for a server log line (default 120 s).
- `{X}` `{Y}` `{Z}` — coordinates captured from the last response containing
  `[x, y, z]` (e.g. `ds island current`); `{X+16}` arithmetic works.
- `{P}` — the `--player` name.

The world is deleted before each run (unless `--keep-world`), so island indices
start from 0; scenarios that must not collide with real runs use high island
indices (`ds island goto 900`).

## Scenarios

| Scenario | Checks |
|---|---|
| `smoke.txt` | Server boots with the mod; island state, items, blocks and recipes register |
| `grave.txt` | Grave stores items + XP; contents survive breaking via `copy_components` |
| `meteorite.txt` | Deterministic plaza floor; one moon meteorite per island (island 900) |
| `island.txt` | Grass tufts, no gravel, evergreens on both island halves, dig/mine loot of tufts and boulders, den releases spiders only at night, hound waves, hound and spider loot (island 910) |
| `codex.txt` | PLAYER: the Codex Umbra drops belongings and joins the run; DS torch burn + light; the way home leads to the reading spot |
| `ghost.txt` | PLAYER: ghost perks (invisibility, mayfly); all-ghost countdown starts and is averted |
| `run.txt` | PLAYER: void death puts the grave at the last ground and raises the ghost to it; a member of a wiped run is sent home |
| `wipe.txt` | PLAYER, SLOW: full all-ghost wipe — countdown, island advance, ejection home |

## Test-only commands

`/ds island goto <n>` jumps the active island; `/ds codex <player>` reads a
Codex Umbra for the player (the right-click path); `/ds ghost <player>
on|off|check` drives the ghost flow; `/ds run <player> join|check` sets/reads
run membership (what walking through the portal does); `/ds hound now <wave>` releases a hound
wave at once. They exist for these scenarios and manual
probing, not for gameplay.
