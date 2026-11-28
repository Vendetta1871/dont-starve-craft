# Roadmap

Goal: all of **Don't Starve Together** content in Minecraft, ported faithfully
(DS prefab ids, stats, loot, behaviour from the DST Lua; no vanilla stand-ins, no
temporary deviations). Content arrives in nested subsets:

1. **DS** — the base single-player game's content (forest, caves, ruins, DS roster).
2. **+ RoG** — Reign of Giants (spring/summer, wetness, overheating, new biomes, giants).
3. **+ DST** — Together-only content (multiplayer systems, characters, ocean, lunar,
   late bosses, rifts).

Subsets are about *content*, not balance: numbers always come from the current DST
`tuning.lua`; a DS-only item is ported only if it also exists in DST (see open
decisions). Ours-by-design, already in place: the islands dimension, Maxwell's
Codex Umbra entry, the sky-portal escape, ghosts and the all-ghost wipe.

## How each stage is verified

| Mark | Who | What it covers |
|---|---|---|
| **A** | me, autotest | Headless server scenarios (`tests/run-all.sh`, RCON) and headless client scenarios: stats and rates, timers (with time skips), loot tables, recipes, worldgen placement, AI state changes visible in NBT, save/load. |
| **S** | me, screenshots | Headless client in gamescope captures fixed scenes: models, textures, HUD, UI screens, lighting/darkness, particles. I check them and show them to you only when a look is worth your opinion. |
| **M** | you, by hand | Feel and judgement: combat timing, AI that "looks right", controls and UI ergonomics, pacing of a real run, sound, two real players together, final art. |

Every stage ends with `tests/run-all.sh` green and a commit.

## Stage 0 — infrastructure (before content)

Done: screenshots (`#shot`), DS time skips (`#ds-wait`), the DST data extractor
(`tools/dst_extract.sh` → `docs/dont-starve/data/`), `DSScale`. Stat-setting test
commands come with the stats themselves in DS-1. Mob speeds are calibrated: a
navigating mob covers 43.17 × attribute² blocks/s (`DSScale.mobSpeed`, `speed.txt`).

- Screenshot capture for the headless client: a dev-only client command/key that saves
  a frame after camera placement, plus scene scripts in `demo/` that build a scene
  and photograph it. **A** (the pipeline itself)
- Test commands: skip DS time (`/ds time skip <segments>`), set stats, spawn with
  state; a scenario helper for "wait N DS seconds". **A**
- Data extraction from the DST Lua into reviewable tables (tuning, recipes, loot,
  perish times) so ports cite numbers instead of retyping them. **A**
- Scaling rules written down once and enforced in code: stats ×2/15, time ×2.5,
  distances 1:1 (1 DS unit = 1 block), mob speeds in blocks/s (attribute mapping to
  be measured in game). **A** + **M**
  (speed feel).

## DS — base game

### DS-1. Survival core

Done: the DST clock and seasons per run (`DSClock`: season split slides between seasons,
moon cycle, ambient light; islands lighting and HUD clock), hunger / sanity / health with
the DST rates and HUD badges, sanity by light and monster auras, ghost drain, the grue,
world and body temperature with freezing and the frost overlay, food spoilage with rot.
Known gaps, each landing with its content: heaters and insulation (DS-3 fire, clothing),
the moon icon in the clock, rate arrows on the badges, insanity effects (DS-8),
overheating sources (RoG).
- Hunger, Sanity, Health as DS stats with DS rates, own HUD meters (badges) replacing
  the vanilla hunger bar. **A** rates, **S** HUD, **M** readability.
- DS clock per island: day/dusk/night segments, the clock HUD. **A**, **S**.
- Darkness and Charlie (the Grue): damage in full darkness, light sources' radii. **A**
  damage/timing, **S** darkness look, **M** how it feels at night.
- Temperature with autumn/winter, freezing, insulation. **A**, **S** (frost overlay).
- Spoilage (freshness, stale/spoiled, rot). **A**.

### DS-2. Crafting and tech

Done: the DS crafting menu replaces the vanilla 2x2 grid in the islands inventory (grey
Minecraft panels: DST filter tabs, recipe grid coloured by build state, details with
ingredient counts and Build / Prototype / Place or the prototyper hint); vanilla crafting
yields nothing on the islands (crafting table included). Recipes come straight from the
DST data (`recipes.json`, `crafting.json` in the jar): a recipe is in the book once its
product and ingredients exist in the mod. Builder rules from `builder.lua`: NONE recipes
known, one tech level up hinted, prototyping at the closest prototyper within 4 blocks
teaches the recipe (+15 sanity), nounlock recipes need the station every time, missing
intermediates are crafted first, health/sanity ingredients, structures are paid first
(buffered) and placed with a green/red preview (left click place, right click cancel),
min_spacing kept. Science Machine, Alchemy Engine, Prestihatitator, Shadow Manipulator
with their PROTOTYPER_TREES; Cut Stone, Rope, Electrical Doodad. Vanilla equivalents:
DST `goldnugget`, `flint`, `log` are the vanilla gold nugget, flint and logs.
Known gaps: blueprints (with their loot sources), the
Prestihatitator and Shadow Manipulator recipes appear with rabbits, top hats, living
logs, gems and nightmare fuel; build animations and DS sounds (placeholders).
The sky-portal escape follows DST: the Moon Rock Idol is the DST recipe (moon rock +
purple gem at a Celestial Orb, never learned) and the portal is not in the menu; it
corresponds to DST's Celestial Portal, a construction site (boards + rope plans, then a
purple moon eye and 20 moon rocks), and becomes buildable with the moon content.
- The DS crafting menu (tabs, tech levels, prototyping, blueprints) as our own screen;
  vanilla crafting off on the islands. **A** recipes/tech gating, **S** screen,
  **M** usability (the biggest UI risk of the whole port).
- Science Machine, Alchemy Engine, Prestihatitator, Shadow Manipulator. **A**, **S**.

### DS-3. Tools, fire, basic resources

Done: DST work (`workable.lua`): one filled mining bar is one hit with the right tool,
targets take their DST number of hits (boulders MINE 6, prototypers HAMMER 4, plants
DIG 1, evergreens CHOP by size); tools wear per use and per attack (`finiteuses.lua`,
golden tools 1/4, pitchfork 1/8) and break when used up. Axe, Pickaxe, Shovel, Hammer,
Pitchfork (golden variants) and Razor. Hammering returns half the recipe, a quarter of
it for burnt structures. Evergreens (normal and lumpy) grow through short/normal/tall,
drop logs and pinecones, leave stumps, burn into burnt trees (charcoal); pinecones plant
saplings. Berry bushes, carrots, flowers (the rose pricks), reeds with DST pick, regrow,
transplant barrenness and cycles; dug plants replant. The regrowth manager respawns
carrots, flowers and reeds away from players; loose flint lies on the meadows.
Fire after `burnable.lua`/`propagator.lua`: small/medium/large burnables and
propagators, heat, smouldering, spread, fire damage, burnt wrecks, ash; campfire (burns
out, ash or charcoal) and fire pit with DST fuel values and four fuel sections, light
and warmth; the torch lights burnables. The meteorite is the DST `rock_moon` boulder.
Known gaps: rain and wetness (flowers regrow only on wet ground, torch rain burn rate),
fertilizer items, reeds in worldgen (with the marsh), Treeguards (leif), cooking on
fire (DS-4), birchnut trees (RoG).
- Axe, Pickaxe, Shovel, Hammer, Razor, Pitchfork with DS durability. **A**.
- Evergreen lifecycle (sizes, stumps, pinecones, burnt trees, charcoal), lumpy
  evergreens, birchnut/deciduous later in RoG; berry bushes, carrots, reeds, flowers,
  ground flint spawns, regrowth. **A** loot/timers, **S** stages.
- Fire: campfire, fire pit, fire spread, smouldering, burnables, fuel values. **A**,
  **S**, **M** (fire spread must not feel like vanilla fire).
- Torch: rain burn rate (needs rain). **A**.

### DS-4. Food and cooking
- Cooking on fire, Crock Pot with all DS recipes and priorities, Drying Rack, Ice Box,
  Bee Box, basic farms. **A** (recipe resolver is fully testable), **S** UI.

### DS-5. Forest fauna
- Passive/neutral: rabbits (+holes), beefalo (herds, mating, domestication is DST),
  pigs/pig houses/Pig King, koalefants, birds, butterflies, frogs, bees, mosquitos.
  **A** loot/stats/spawners, **S** placeholder models, **M** AI feel.
- Hostile: spiders full (den tiers 1–3, warriors, Spider Queen), tentacles, merms,
  tallbirds, MacTusk party, Krampus, hound variants (fire/ice), penguins. **A**, **M**.
- Beardlings → beard hair → Meat Effigy becomes craftable. **A**.

### DS-6. World generation
- DS biomes (grassland, forest, deciduous later, rocky, savanna, marsh, graveyard,
  checkerboard, mosaic) and set pieces, wormholes, touch stones, statues,
  boulders, Pig King set piece, roads. Island size and layout follow DS room/task
  rules at 4×4 blocks per tile. **A** placement statistics, **S** aerial shots, **M** walk-around feel.

### DS-7. Bosses (DS)
- Deerclops, Treeguards, Spider Queen. **A** stats/phases/loot, **S**, **M** fights.

### DS-8. Magic and sanity world
- Shadow creatures (insanity), nightmare fuel, magic items (Night Light, Night Armor,
  Dark Sword, amulets, staffs), Pan Flute, One-man Band. **A**, **S** insanity
  visuals, **M**.

### DS-9. Caves and Ruins
- Caves dimension (sinkholes, light flowers, bunnymen, rock lobsters, slurpers,
  depth worms, bats, spilagmites, cave bananas, fern/lichen). **A**, **S**, **M**.
- Ruins: nightmare cycle, clockwork monsters, thulecite, Ancient Pseudoscience
  Station, Ancient Guardian, ruins loot. **A**, **S**, **M**.

### DS-10. DS characters
- Character select; Wilson, Willow, Wolfgang, Wendy (+Abigail), WX-78, Wickerbottom,
  Woodie (+Lucy, werebeaver), Wes, Maxwell (Waxwell) with their DST perks. **A** perk
  numbers, **S** select screen, **M** each character's play feel.

## RoG — Reign of Giants

- Spring and summer, wetness, overheating, lightning and rods, rain/frog rain,
  wildfires, smouldering. **A**, **S** rain/heat overlays, **M**.
- Biomes: desert, deciduous forest (birchnuts, poison birchnut tree), new set pieces.
  **A**, **S**.
- Mobs: catcoons, volt goats, moleworms, buzzards, glommer, mosquito changes, hound
  seasonal variants. **A**, **M**.
- Giants: Moose/Goose (+moslings), Dragonfly (DST version), Bearger. **A**, **S**, **M**.
- Items/structures: Ice Flingomatic, Endothermic fire, Luxury Fan, umbrellas, rain
  gear, Thermal Stone summer use, Hibearnation Vest, Weather Pain, etc. **A**, **S**.
- Wigfrid and Webber. **A**, **M**.

## DST — Together

- Multiplayer systems: resurrection (Touch Stone, Life Giving Amulet, Telltale Heart,
  Meat Effigy — partially done), skeletons/graves, shared world events. **A** (two
  bot players), **M** with two real players.
- DST character roster: Webber/Wigfrid reworks, Winona, Warly, Wortox, Wormwood,
  Wurt, Walter, Wanda, Wolfgang/Wilson/etc. skill trees. **A** perks, **M**.
- Beefalo domestication and riding, Pig King minigames, Hermit Crab island. **A**, **M**.
- Farming (Reap What You Sow): giant crops, nutrients, plant talk. **A**, **S**.
- Bosses: Klaus, Bee Queen, Toadstool, Antlion, Ancient Fuelweaver, Malbatross,
  Crab King, Eye of Terror/Twins, Celestial Champion, Daywalker, rift bosses. **A**,
  **S**, **M** (each fight).
- Return of Them: ocean, boats, sea fishing, lunar island, moon storms, Waterlogged,
  Lunar Grotto, Archive. The sea is plain water around the island. **A**, **S**, **M**.
- Rifts (lunar and shadow) and their gear. **A**, **S**, **M**.
- Seasonal events (Winter's Feast, Hallowed Nights, Year of the X) last. **A**, **M**.

## Art

Every entity/block/item first ships with placeholder visuals. Real models and
textures are our own work, not extracted Klei assets; replacing them is a separate
pass per stage, checked by **S** then **M**. Player-facing README with screenshots
comes once a stage is playable end to end.

## Decisions

- **No DS Adventure Mode.** It is DS-only (absent from DST); the islands run stays.
- **World scale: one DST tile = 4×4 blocks** (a tile is 4 DS units, `TILE_SCALE`),
  so one DS distance unit = one block and distances carry over 1:1. Speeds then
  match too: WILSON_RUN_SPEED 6 units/s = 6 blocks/s ≈ a vanilla sprint (5.6), so the
  player keeps vanilla movement and mobs move at their DST speed in blocks/s.
- **Season clock belongs to the run.** All players share one world and one current
  island; every new island starts at autumn day 1, like a new DST world.
- **Crafting UI: our own DS-style menu** (tabs, tech levels, prototyping) drawn in
  the Minecraft GUI style — square slots, grey panels.
- **Ocean: plain water**, as in DST. The island becomes the DST world surrounded by
  sea instead of void (with the ocean content in Return of Them).
