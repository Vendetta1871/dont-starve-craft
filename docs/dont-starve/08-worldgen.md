# 08 — World Generation

Sources: `scripts/map/` (pipeline), `map/levels/`, `map/rooms/`, `map/tasksets/`,
`map/customize.lua`, `map/boons.lua`, `map/lockandkey.lua`.

## Levels

`map/levels/` ships four level types: **`forest`** (surface world), **`caves`**
(underground, connected via `cave_entrance`/`cave_exit`), `lavaarena` (Dragonfly
arena), `quagmire` (event arena). A DST world = forest + caves stacked; falling in a
cave entrance teleports between levels.

## Forest generation pipeline

1. **Layout** (`map/layout.lua`, `map/layouts.lua`, `map/rooms.lua`): modern DST forest
   is **room-based** — rectangular rooms stitched into landmasses, not noise blobs.
   Room definitions: `map/rooms/forest/*.lua`:
   - terrain rooms: `terrain_forest`, `terrain_grass`, `terrain_rocky`,
     `terrain_savanna`, `terrain_marsh`, `terrain_dirt`, `terrain_ocean`,
     `terrain_moonisland`, `terrain_noise`, `terrain_impassable`
   - content rooms: `pigs`, `spider`, `bee`, `beefalo`, `merms`, `walrus`, `tallbird`,
     `giants` (boss lairs), `graveyard`, `mandrake`, `chess` (adventure), `starts`,
     `wormhole`, `blockers`
2. **Tasks** (`map/tasksets/forest.lua` via `AddTaskSet`): named task sets
   (`"default"`, `"classic"`, …) list `tasks`, `optionaltasks`, `valid_start_tasks`,
   `set_pieces`, `ocean_population` — e.g. "Make a pick", "Place beefalo herd",
   "Fill in the ocean". Tasks defined in `map/tasks.lua` + `map/tasks/`.
3. **Boons** (`map/boons.lua`): guaranteed-resource rules (e.g. flint near spawn,
   pig houses need forest adjacency) — the day-1 survivability guarantee.
4. **Lock & key** (`map/lockandkey.lua`): structure gating (ruins entrance needs a
   `deerclops_eyeball` pedestal in the ruins, etc.).
5. **Bunches** (`map/bunches.lua`, `bunch_spawner.lua`): grouped resource scatter
   (berry bushes, flint patches) — the "generous spawn" mechanism.

World customization options (season length, bosses, world size, start location) are
enumerated in `map/customize.lua`; presets in `map/settings.lua`.

## Caves

Rooms (`map/rooms/cave/`): `greenmush`/`bluemush`/`redmush` (fungus biomes),
`fungusnoise`, `bats`, `rabbits`, `spillagmites`, `swamp` (tentacles), `mud`,
`rocky`, `sinkhole`, `ruins` (ancient city with `statuemaxwell`), `vents`,
`toadstoolarena`, `terrain_mazes`. Caves are dark (light required), temperature pinned
near 0 (`CAVES_TEMP_*`).

## Moon content (current build)

There is **no separate lunar-island dimension** in this build. Moon content lives in
the forest level:

- `moonbase` — "Moon Stone" structure holding the King's Staff (`prefabs/moonbase.lua`).
- `moonrockidol` — **Moon Rock Idol**: recipe 1 `moonrocknugget` + 1 `purplegem`,
  TECH.CELESTIAL_ONE (`recipes.lua:832`); functions as `moonportalkey` — lights up near
  a `moonportal` (`ms_moonportalproximity` event, `moonrockidol.lua:55-103`).
- `multiplayer_portal_moonrock` — the Moon Portal (tag `moonportal`).
- Moon variants: `moonpig`, `moonhound`, moon spider variants (`spider.lua:1087-1094`).

**Our Sky Portal should mirror this pattern**: idol as *key item* + portal structure,
one-way. Canonical ids: `moonrocknugget` (Moon Rock), `moonrockidol`, `moonportal`.

## Maxwell's Door

`prefabs/statuemaxwell.lua` — "Maxwell Statue" in the ruins (tags `maxwell`, `statue`);
mining it (`TUNING.MARBLEPILLAR_MINE` hits) unlocks the adventure chess pieces
(`ms_unlockchesspiece`). **[DS1]** single-player's adventure-mode **Maxwell's Door**
(portal framed in marble, opened with `key_to_city` / deerclops drop) is the flavor
source for our marble П-portal; DST has no walkable door prefab.

## Sacrifice

**No sacrifice altar / `sacrificer` component exists in current DST code**
(grep-verified). Our "kill a mob near the frame to light it" is original mechanics —
flavor-adjacent to DS blood-magic themes (Woodie's were-form lore, `statuemaxwell`
marble), not a port. Keep it that way in docs/UI.
