# 03 — Survival Stats

Wilson baseline: **150 Health / 150 Hunger / 200 Sanity** (`tuning.lua:37-40`).
All rates below are per second unless stated.

## Health (`components/health.lua`)

- No passive regen for players; mobs use `components/healthregen.lua`.
- Death at 0 → `"death"` event with `{cause, afflicter}`.
- Damage sources tag themselves (`"hunger"`, `"cold"`, `"hot"`, `"darkness"`, …) —
  useful for death announcements (`GetNewDeathAnnouncementString`,
  `prefabs/player_common_extensions.lua:236`).

## Hunger (`components/hunger.lua`)

- Max 150; decay `WILSON_HUNGER_RATE = calories_per_day / total_day_time = 75/480 ≈ 0.156/s`
  (`tuning.lua:41,121`) → **2 full days** from full to empty.
- Eating adds `edible.hungervalue` (see [06](06-food-and-spoilage.md)).
- Activity modifies burn via `burnratemodifiers` (`hunger.lua:36,154`).
- At 0: starvation damage `WILSON_HEALTH / STARVE_KILL_TIME = 150/120 = 1.25 HP/s`
  (`player_common.lua:2766`, `tuning.lua:754`).
- `HUNGRY_THRESH = 1/3` drives the "hungry" HUD/voice state (`tuning.lua:755`).

## Sanity (`components/sanity.lua`)

- Max 200 (Wilson). WX-78 uses **lunacy** mode instead (`WX78_SANITY=100`, max 300,
  `tuning.lua:145-153`).
- **No passive daytime gain**: `SANITY_DAY_GAIN = 0` (`tuning.lua:2191`). "Day" means
  `TheWorld.state.isday`, which is false at **dusk** — dusk already uses the night table.
- Night/dark drain by light value (`sanity.lua:470-540`, `tuning.lua:2180-2195`):

| Condition | Rate |
|---|---|
| dusk or night, light ≥ 0.1 (`SANITY_LOW_LIGHT`) | −100/(30·40) = **−0.083/s** |
| dusk or night, light < 0.1 (dark) | −100/(30·4) = **−0.833/s** |
| caves (always treated as night) | same table |
| wetness penalty at max moisture | −100/(300·6) ≈ **−0.056/s** (`tuning.lua:2221`) |

- **Insane** at ≤ 30/200 (15 %), sane again at ≥ 35/200 (`tuning.lua:2185-2186`):
  shadow creatures spawn (`prefabs/nightmarecreature.lua` — `crawlingnightmare`
  300 HP / 20 dmg, drops `nightmarefuel`), screen effects, voices.
- **Auras**: any entity can carry `components/sanityaura.lua` (tag `"sanityaura"`);
  dapperness from equipped hats multiplies `SANITY_DAPPERNESS = 1` (`sanity.lua:513-523`).
- Positive sanity sources are items/events only (flowers picked, dreams, cooking,
  tending plants, safe daytime activities).

## Temperature (`components/temperature.lua`)

- Player comfort band **0 … 70 °C**: freezing below 0 (`temperature.lua:170-171`),
  overheating above `OVERHEAT_TEMP = 70` (`tuning.lua:2301`).
- Damage both ways: `WILSON_HEALTH / FREEZING_KILL_TIME = 150/120 = 1.25 HP/s`
  (`temperature.lua:31,518-521`, `tuning.lua:753`).
- Sources: ambient (season + phase + biome), campfires/fires (`radiantheat`),
  clothing insulation (`insulation` / `overinsulation`), food (`FOOD_TEMP_BRIEF/AVERAGE/LONG
  = 5/10/15 °C`, `tuning.lua:2330-2332`), wetness cooling (`MOISTURE_TEMP_PENALTY = 30`,
  `tuning.lua:2929`).
- Caves: ambient pinned near 0 with 0.6 multiplier (`CAVES_TEMP_LOCUS/MULT`,
  `tuning.lua:2297-2298`) — caves are freezing in winter, safe in summer.

## Wetness (DST only, `components/moisture.lua`)

Rain adds moisture; penalties: sanity (above), temperature (above), lightning strike
risk, harder to light fires. **[DS1]** single-player has no wetness.
