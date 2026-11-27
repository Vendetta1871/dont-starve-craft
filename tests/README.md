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
- `#ds-wait N` — run N DS seconds of world time (N × 2.5 × 20 ticks) at full speed
  with `tick sprint`; timers, burning and regrowth advance as in real play.
- `#shot NAME [hud]` — PLAYER: the client saves a screenshot (HUD hidden unless
  `hud`), collected to `build/screenshots/<scenario>/NAME.png`. Place the camera
  first with `tp {P} x y z yaw pitch`.
- `{X}` `{Y}` `{Z}` — coordinates captured from the last response containing
  `[x, y, z]` or from `centered at X, Y, Z` (`ds island current`); `{X+16}` arithmetic works.
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
| `speed.txt` | Hound and spider move at their DST speeds in blocks/s (`/ds speed`), zombie as the control of the 43.17 × attribute² law |
| `codex.txt` | PLAYER: the Codex Umbra drops belongings and joins the run; DS torch burn + light; the way home leads to the reading spot |
| `ghost.txt` | PLAYER: ghost perks (invisibility, mayfly); all-ghost countdown starts and is averted |
| `run.txt` | PLAYER: void death puts the grave at the last ground and raises the ghost to it; a member of a wiped run is sent home |
| `harness.txt` | PLAYER: the test tools — `#ds-wait` burns a held torch by DS time, `#shot` saves screenshots with and without HUD |
| `clock.txt` | DST clock and seasons: day/dusk/night split per season (incl. the slide between seasons), season lengths, moon cycle, ambient light, world temperature |
| `clockview.txt` | PLAYER: the run clock runs with a member present; screenshots of day, dusk, night, torch at night, full moon |
| `stats.txt` | PLAYER: hunger burn, starvation, sanity by light (torch vs dark), the grue, freezing, eating, ghost stats; HUD and frost screenshots |
| `perish.txt` | PLAYER: food spoilage carried and on the ground, stale/spoiled food values, rot, merging stacks |
| `wipe.txt` | PLAYER, SLOW: full all-ghost wipe — countdown, island advance, ejection home |

## Test-only commands

`/ds island goto <n>` jumps the active island; `/ds codex <player>` reads a
Codex Umbra for the player (the right-click path); `/ds ghost <player>
on|off|check` drives the ghost flow; `/ds run <player> join|check` sets/reads
run membership (what walking through the portal does); `/ds hound now <wave>` releases a hound
wave at once; `/ds shot <player> <name> [hud]` makes that client save a screenshot;
`/ds speed <mob> <x> <z> <ticks>` walks a mob towards (x, z) and logs its real speed;
`/ds clock [set <day> day|dusk|night [ticks]]` reads or jumps the run clock; `/ds stats <player>
[set hunger|sanity|temperature <v>]`, `/ds eat <player>` and `/ds perish <player> [percent]` drive
the DS stats and spoilage; `/ds hound pause on|off` keeps hound waves away from clock jumps.
They exist for these scenarios and manual
probing, not for gameplay.
