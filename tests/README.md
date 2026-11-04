# Server tests

Headless scenario tests: `servertest.py` starts a dedicated server for this
checkout (fresh world, seed `dst-test`, RCON on a private port), sends the
commands of a scenario over RCON, prints the responses and evaluates the
assertions. Exit code 0 means every check passed.

```sh
python3 tests/servertest.py . tests/scenarios/smoke.txt
# parallel runs: --port 25711 (game port is 100 lower); DST_JAVA_HOME picks the JDK 21
bash tests/run-all.sh            # every headless scenario, one command
```

Scenarios marked SLOW take minutes. Scenarios marked PLAYER need a real client
connected: run the harness, and while it waits (it prints `SERVER STARTED`)
join `localhost:<port - 100>` in a `runClient` session as the `--player` nick,
e.g. `python3 tests/servertest.py . tests/scenarios/ghost.txt --player Bob`
then join localhost:25599 as Bob.

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
| `smoke.txt` | Server boots with the mod; island state, recipes and the marble tag register |
| `grave.txt` | Grave stores items + XP; contents survive breaking via `copy_components` |
| `meteorite.txt` | Deterministic plaza floor; one moon meteorite per island (island 900) |
| `portal.txt` | П-frame lights on mob sacrifice; vault put/take round-trip |
| `ghost.txt` | PLAYER: ghost perks (invisibility, mayfly); all-ghost countdown starts and is averted |
| `wipe.txt` | PLAYER, SLOW: full all-ghost wipe — countdown, island advance, ejection home |

## Test-only commands

`/ds island goto <n>` jumps the active island; `/ds vault put|take <pos> [item]`
stands in for a player being confiscated / ejecting; `/ds ghost <player>
on|off|check` drives the ghost flow. They exist for these scenarios and manual
probing, not for gameplay.
