# Don't Starve — Reference Documentation

Ground-truth reference for this mod, extracted from the **shipped DST Lua source**
(`data/databundles/scripts.zip` of the installed Don't Starve Together build, unpacked
for reference to `/tmp/opencode/dst_scripts/scripts`) and cross-checked against the
official wiki. Where wiki and code disagree, **the code wins** — the wiki lags patches.

Every number and name below cites its source file under `scripts/` so it can be re-verified:

```bash
# refresh the source snapshot:
unzip -o "<Steam>/steamapps/common/Don't Starve Together/data/databundles/scripts.zip" -d /tmp/opencode/dst_scripts
```

> Scope note: this documents **DST** (the installed game). Single-player DS differs
> (no wetness pre-RoG, RoG adds spring/summer, no moisture/sanity-penalty interplay).
> Differences that matter to this mod are flagged inline as **[DS1]**.

## Index

| Doc | Contents |
|---|---|
| [01-architecture.md](01-architecture.md) | How the game code is organized: prefabs, components, entities, events, TheWorld, TUNING, STRINGS, naming conventions |
| [02-time-and-seasons.md](02-time-and-seasons.md) | Day/night clock, segment model, season lengths and day composition |
| [03-survival-stats.md](03-survival-stats.md) | Health, Hunger, Sanity, Temperature: exact rates and thresholds |
| [04-darkness-and-light.md](04-darkness-and-light.md) | Light levels, darkness sanity drain, the Grue ("Charlie") damage loop |
| [05-combat.md](05-combat.md) | Combat component, player/mob damage model, damage types, stunlock |
| [06-food-and-spoilage.md](06-food-and-spoilage.md) | Edible/perishable components, food values, spoilage, preservation, cooking |
| [07-death-ghost-resurrection.md](07-death-ghost-resurrection.md) | Death drops, ghost state, Meat Effigy, Telltale Heart — and how our mod maps them |
| [08-worldgen.md](08-worldgen.md) | Levels, room-based forest generation, tasksets, biomes, caves, moon content |
| [09-reference-entities.md](09-reference-entities.md) | Tables: mobs, items, structures with exact prefab ids and stats |
| [10-glossary-and-mod-mapping.md](10-glossary-and-mod-mapping.md) | DS term → our mod registry name mapping and naming rules |
| [content/mobs.md](content/mobs.md) | Per-mob blocks: stats, aggression, loot, spawners (forest/early mobs) |
| [content/items.md](content/items.md) | Basic resources and early items: stack, perish, edible, fuel, tool stats, sources |
| [content/recipes.md](content/recipes.md) | Recipes with ingredients, tech level, recipes.lua line |
| [content/structures.md](content/structures.md) | Resource nodes and structures: work, loot, regrowth, spawned children |
| [content/mechanics-cheatsheet.md](content/mechanics-cheatsheet.md) | Q&A of commonly misremembered facts, each with a citation |
| [data/](data/) | Generated JSON straight from the DST Lua: `tuning.json` (every TUNING value), `recipes.json` (ingredients, tech, config), `loot.json` (shared loot tables). Regenerate with `tools/dst_extract.sh` after a DST update |

All conversions to Minecraft units go through `DSScale` (stats ×2/15, world time ×2.5,
1 DS unit = 1 block, one tile = 4×4 blocks, mob speeds via `DSScale.mobSpeed`; combat
timings stay real time).

## The three rules for not getting names wrong

1. **Prefab id ≠ display name.** The prefab id is the Lua file's registered name
   (`pigman.lua` registers prefab `pig`; `resurrectionstatue.lua` registers
   `resurrectionstatue` displayed as "Meat Effigy"). Always key off the **prefab id**.
2. **Display names live in `scripts/strings.lua`** under `STRINGS.NAMES.<PREFAB_ID_UPPER>`
   and `STRINGS.CHARACTERS.GENERIC.DESCRIBE.<PREFAB_ID_UPPER>`.
3. **All balance numbers live in `scripts/tuning.lua`** built from the constants at the
   top of `Tune()` (`seg_time=30`, day=480s, wilson 150/150/200, attack 34, …). Resolve
   `TUNING.X` through that header before quoting a number.
