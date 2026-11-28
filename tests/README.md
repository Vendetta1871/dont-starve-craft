# Server tests

Headless scenario tests: `servertest.py` starts one dedicated server for this checkout
(fresh world, seed `dst-test`, RCON on a private port) and runs any number of scenarios
in that single session, sending their commands over RCON, printing the responses and
evaluating the assertions. Exit code 0 means every check of every scenario passed.

```sh
bash tests/run-all.sh                  # every scenario, one server session (~10 min)
bash tests/run-all.sh crafting smoke   # only these, in this order
SKIP_PLAYER=1 bash tests/run-all.sh    # only the scenarios without a player
python3 tests/servertest.py . tests/scenarios/smoke.txt tests/scenarios/ghost.txt --port 25711
```

While iterating, run only the scenarios a change touches; the full suite runs once
before a release.

One session, clean slates: before every scenario but the first, `ds test reset @a`
(`TestReset.java`) moves the run to a fresh island (new run clock, no hound waves, no
wipe countdown), restores the test switches and game rules (hound pause, mob spawning,
daylight cycle, tick rate) and sends the player home to the overworld spawn as a fresh
survivor (alive, survival, empty inventory, full stats, no learned recipes, no run).
Scenarios therefore never assume island 0 (except `smoke`, which runs first) and pin
their islands with `ds island goto N` or read them with `ds island current`.

Scenarios that use `{P}` need an online player: before the first of them the runner
starts it once as a headless, silent game client — `tools/autotest.sh` runs the
`runAutotestClient` Gradle run inside a monitorless gamescope on the eGPU (OpenAL null
backend, all sound levels zeroed in `run-autotest/options.txt`), and the client joins
the test server on its own via `--quickPlayMultiplayer` and stays for the rest of the
session. Its log is `build/autotest.log`. Never start a client on the desktop for tests.

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
  Any reply with an `x, y, z` triple recaptures them (vanilla `setblock` says
  "Changed the block at x, y, z"): run `ds island current` again after such commands,
  and keep commas out of positions in our own command replies.
- `{P}` — the `--player` name.

The world is deleted before each session (unless `--keep-world`); scenarios that must
not collide with real runs use high island indices (`ds island goto 900`).

## Scenarios

| Scenario | Checks |
|---|---|
| `smoke.txt` | Server boots with the mod; island state, items, blocks and recipes register |
| `grave.txt` | Grave stores items + XP; contents survive breaking via `copy_components` |
| `meteorite.txt` | Deterministic plaza floor; one moon meteorite (`rock_moon` boulders) per island, its DST loot (island 900) |
| `island.txt` | Grass tufts, no gravel, evergreens on both island halves, dig/mine loot of tufts and boulders, den releases spiders only at night, hound waves, hound and spider loot (island 910) |
| `speed.txt` | Hound and spider move at their DST speeds in blocks/s (`/ds speed`), zombie as the control of the 43.17 × attribute² law |
| `codex.txt` | PLAYER: the Codex Umbra drops belongings and joins the run; DS torch burn + light; the way home leads to the reading spot |
| `ghost.txt` | PLAYER: ghost perks (invisibility, mayfly); all-ghost countdown starts and is averted |
| `run.txt` | PLAYER: void death puts the grave at the last ground and raises the ghost to it; a member of a wiped run is sent home |
| `harness.txt` | PLAYER: the test tools — `#ds-wait` burns a held torch by DS time, `#shot` saves screenshots with and without HUD |
| `clock.txt` | DST clock and seasons: day/dusk/night split per season (incl. the slide between seasons), season lengths, moon cycle, ambient light, world temperature |
| `clockview.txt` | PLAYER: the run clock runs with a member present; screenshots of day, dusk, night, torch at night, full moon |
| `crafting.txt` | PLAYER: vanilla grid off on the islands; DS recipe states (known / hint / prototype), Science Machine built buffered and placed, prototyping (+15 sanity), auto-crafted intermediates, health as an ingredient, MAGIC tech at a Prestihatitator, min_spacing; screenshots of the menu and the placement preview |
| `stats.txt` | PLAYER: hunger burn, starvation, sanity by light (torch vs dark), the grue, freezing, eating, ghost stats; HUD and frost screenshots |
| `trees.txt` | PLAYER: DST evergreens — chops and loot per stage (short 5 / normal 10 / tall 15 / old 1), a crown block works the same tree, stump digging, lumpy build without pinecones, growth steps and loop, burning (burnt tree, stump, sapling), pinecone planting and natural growth via `#ds-wait` |
| `perish.txt` | PLAYER: food spoilage carried and on the ground, stale/spoiled food values, rot, merging stacks |
| `flora.txt` | PLAYER: forest flora — pick by hand (berry bush, carrot, flower, rose, reeds), food values and spoilage, transplant barrenness, fertilize and cycles, dig loot, ground flint, regrowth manager (night/winter/player range), berry regrow time, worldgen scatter |
| `fire.txt` | PLAYER: DST fire — campfire/fire pit build, fuel sections over `#ds-wait`, fuel items, light and warmth, campfire burns out to ash (charcoal when queued), fire pit stays and relights, hammering; prototyper burning, heat spread with smouldering, fire damage, burnt wreck, torch LIGHT |
| `cooking.txt` | PLAYER: DST cooking and food keeping — COOK on a burning campfire/fire pit (products, freshness carry-over, edible values, perish times, not cookable / unlit), Drying Rack placement through the crafting menu, 3 slots, monster meat dries in 480 DS s without perishing, early take and resume, harvest, hammer, burning; Ice Box item filter, spoilage at half rate, hammer |
| `cookpot.txt` | PLAYER: DST Crock Pot — the cooking.lua resolver (tags, priorities, ties, wet goop, cooked names and aliases, items that do not exist yet), a placed pot: fill, cook over `#ds-wait`, finished dish, harvest freshness, spoiling into rot, eating, belly temperature, hammer loot with loaded ingredients and dish, burning to a wreck |
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
`/ds work <player> <pos> [hits|swing]` works a block with the held tool; `/ds tree place|stump|sapling|grow|burn|info|sweep|plant ...`
builds and drives evergreens (`TreeCommands`); `/ds pick <player> <pos>` picks a plant by hand, `/ds fertilize <pos>`
fertilizes a barren plant, `/ds regrow status|advance <ds seconds>` reads or advances the flora regrowth manager;
`/ds fire ignite|extinguish|smoulder|check <pos>`, `heat <pos> <amount>`, `light <player> <pos>` (torch LIGHT action),
`feed <player> <pos>` (main-hand item as fuel) and `fuel <pos> set|add <ds seconds>` drive and read the fire engine;
`/ds cookfire <player> <pos>` cooks the main-hand item on a fire; `/ds dryer <pos> put <player>|check|slot <n>|harvest <player>`
and `/ds icebox <pos> put <player>|check|slot <n>|take <player> <n>` drive the Drying Rack and the Ice Box.
`/ds cook calc <ingredients...>` runs the Crock Pot resolver on prefab names or item ids; `/ds cookpot <pos> put <player>|cook|check|advance <ds seconds>|harvest <player>` drives a pot (`CookCommands`), `/ds cook belly <player>` reads the warm-food effect.
They exist for these scenarios and manual
probing, not for gameplay.
