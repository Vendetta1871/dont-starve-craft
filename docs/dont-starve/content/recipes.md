# Don't Starve Together - recipes reference (ground truth from recipes.lua)

Purpose: exact ingredients and tech requirement of the early-game recipes, as a reference for a Minecraft port.

Citation convention: paths are relative to `scripts/` of the unpacked DST game; `recipes.lua:N` is the line where `Recipe2("<name>", ...)` is defined. Display names come from `strings.lua` `STRINGS.NAMES`. Amounts are exactly as written in the `Ingredient(...)` calls (items given up, one craft). `numtogive` = items produced per craft (default 1).

## How recipes.lua is structured

- `Recipe2(name, {Ingredient(prefab, amount), ...}, TECH.X, {config})` (class defined recipe.lua:228; base `Recipe` recipe.lua:67). `name` is also the product prefab unless `product=` is given (recipe.lua:98).
- `TECH.X` is the tech level needed (see section below). `TECH.NONE` = hand-craftable anywhere, no prototyper.
- `config` keys seen here: `placer` (ghost-placement prefab, i.e. a buildable structure), `min_spacing`, `numtogive` (recipe.lua:133), `builder_tag` / `no_builder_tag` (character gating, recipe.lua:136-138; checked in components/builder.lua:954-961), `no_builder_skill` / `forward_ingredients` (skill-tree variants of rope/boards), `station_tag`. Recipes with no `builder_tag` can be crafted by every character.
- `CHARACTER_INGREDIENT.HEALTH` / `SANITY` (constants.lua:2127-2133) are not items: the amount is deducted from the crafter's health/sanity; builder refuses if it would kill (components/builder.lua:594-599, `current > amount`).
- A recipe whose tech is not NONE must be crafted once next to a matching prototyper (see below) which also unlocks it permanently for that character (`Builder:UnlockRecipe`, components/builder.lua:463-472, which also gives +15 sanity SANITY_MED at :467).

## Recipes

| id | name | ingredients | tech | extra config | recipes.lua |
|---|---|---|---|---|---|
| `researchlab` | Science Machine | 1x `goldnugget`; 4x `log`; 4x `rocks` | TECH.NONE | {placer="researchlab_placer", min_spacing=2} | :382 |
| `researchlab2` | Alchemy Engine | 4x `boards`; 2x `cutstone`; 2x `transistor` | TECH.SCIENCE_ONE | {placer="researchlab2_placer", min_spacing=2} | :383 |
| `researchlab4` | Prestihatitator | 4x `rabbit`; 4x `boards`; 1x `tophat` | TECH.SCIENCE_ONE | {placer="researchlab4_placer", min_spacing=2} | :387 |
| `researchlab3` | Shadow Manipulator | 3x `livinglog`; 1x `purplegem`; 7x `nightmarefuel` | TECH.MAGIC_TWO | {placer="researchlab3_placer", min_spacing=2} | :388 |
| `cookpot` | Crock Pot | 3x `cutstone`; 6x `charcoal`; 6x `twigs` | TECH.SCIENCE_ONE | {placer="cookpot_placer", min_spacing=2} | :475 |
| `treasurechest` | Chest | 3x `boards` | TECH.SCIENCE_ONE | {placer="treasurechest_placer", min_spacing=1} | :560 |
| `icebox` | Ice Box | 2x `goldnugget`; 1x `gears`; 1x `cutstone` | TECH.SCIENCE_TWO | {placer="icebox_placer", min_spacing=1.5} | :563 |
| `meatrack` | Drying Rack | 3x `twigs`; 2x `charcoal`; 3x `rope` | TECH.SCIENCE_ONE | {placer="meatrack_placer"} | :476 |
| `tent` | Tent | 6x `silk`; 4x `twigs`; 3x `rope` | TECH.SCIENCE_TWO | {placer="tent_placer"} | :578 |
| `siestahut` | Siesta Lean-to | 2x `silk`; 4x `boards`; 3x `rope` | TECH.SCIENCE_TWO | {placer="siestahut_placer"} | :579 |
| `birdtrap` | Bird Trap | 3x `twigs`; 4x `silk` | TECH.SCIENCE_ONE | - | :452 |
| `trap` | Trap | 2x `twigs`; 6x `cutgrass` | TECH.NONE | - | :451 |
| `resurrectionstatue` | Meat Effigy | 4x `boards`; 4x `beardhair`; 40 (TUNING.EFFIGY_HEALTH_PENALTY) HEALTH (character stat, `CHARACTER_INGREDIENT.HEALTH`) | TECH.MAGIC_TWO | {placer="resurrectionstatue_placer", min_spacing=2} | :580 |
| `pighouse` | Pig House | 4x `boards`; 3x `cutstone`; 4x `pigskin` | TECH.SCIENCE_TWO | {placer="pighouse_placer"} | :582 |
| `campfire` | Campfire | 3x `cutgrass`; 2x `log` | TECH.NONE | {placer="campfire_placer", min_spacing=2} | :426 |
| `firepit` | Fire Pit | 2x `log`; 12x `rocks` | TECH.NONE | {placer="firepit_placer", min_spacing=2.5} | :427 |
| `torch` | Torch | 2x `cutgrass`; 2x `twigs` | TECH.NONE | - | :425 |
| `lantern` | Lantern | 3x `twigs`; 2x `rope`; 2x `lightbulb` | TECH.SCIENCE_TWO | - | :433 |
| `rope` | Rope | 3x `cutgrass` | TECH.SCIENCE_ONE | {no_builder_skill="walter_camp_rope", forward_ingredients={"walter_rope"}} | :457 |
| `boards` | Boards | 4x `log` | TECH.SCIENCE_ONE | {no_builder_tag="cancarveboards", forward_ingredients={"woodie_boards"}} | :459 |
| `cutstone` | Cut Stone | 3x `rocks` | TECH.SCIENCE_ONE | - | :460 |
| `papyrus` | Papyrus | 4x `cutreeds` | TECH.SCIENCE_ONE | - | :461 |
| `spear` | Spear | 2x `twigs`; 1x `rope`; 1x `flint` | TECH.SCIENCE_ONE | - | :478 |
| `axe` | Axe | 1x `twigs`; 1x `flint` | TECH.NONE | - | :393 |
| `pickaxe` | Pickaxe | 2x `twigs`; 2x `flint` | TECH.NONE | - | :395 |
| `shovel` | Shovel | 2x `twigs`; 2x `flint` | TECH.SCIENCE_ONE | - | :397 |
| `hammer` | Hammer | 3x `twigs`; 3x `rocks`; 6x `cutgrass` | TECH.NONE | - | :401 |
| `bugnet` | Bug Net | 4x `twigs`; 2x `silk`; 1x `rope` | TECH.SCIENCE_ONE | - | :399 |
| `fishingrod` | Freshwater Fishing Rod | 2x `twigs`; 2x `silk` | TECH.SCIENCE_ONE | - | :641 |
| `armorwood` | Log Suit | 8x `log`; 2x `rope` | TECH.SCIENCE_ONE | - | :497 |
| `armorgrass` | Grass Suit | 10x `cutgrass`; 2x `twigs` | TECH.NONE | - | :496 |
| `footballhat` | Football Helmet | 1x `pigskin`; 1x `rope` | TECH.SCIENCE_TWO | - | :504 |
| `healingsalve` | Healing Salve | 2x `ash`; 1x `rocks`; 1x `spidergland` | TECH.SCIENCE_ONE | - | :414 |
| `amulet` | Life Giving Amulet | 3x `goldnugget`; 2x `nightmarefuel`; 1x `redgem` | TECH.MAGIC_TWO | - | :674 |
| `reviver` | Telltale Heart | 3x `cutgrass`; 1x `spidergland`; 40 HEALTH (character stat, `CHARACTER_INGREDIENT.HEALTH`) | TECH.NONE | - | :420 |
| `bedroll_straw` | Straw Roll | 6x `cutgrass`; 1x `rope` | TECH.SCIENCE_ONE | - | :422 |
| `bedroll_furry` | Fur Roll | 1x `bedroll_straw`; 2x `manrabbit_tail` | TECH.SCIENCE_TWO | - | :423 |
| `strawhat` | Straw Hat | 12x `cutgrass` | TECH.NONE | - | :530 |
| `flowerhat` | Garland | 12x `petals` | TECH.NONE | - | :529 |
| `wall_hay_item` | Hay Wall | 4x `cutgrass`; 2x `twigs` | TECH.SCIENCE_ONE | {numtogive=4} | :596 |
| `wall_wood_item` | Wood Wall | 2x `boards`; 1x `rope` | TECH.SCIENCE_ONE | {numtogive=8} | :598 |
| `wall_stone_item` | Stone Wall | 2x `cutstone` | TECH.SCIENCE_TWO | {numtogive=6} | :599 |
| `fence_item` | Wood Fence | 3x `twigs`; 1x `rope` | TECH.SCIENCE_ONE | {numtogive=6} | :597 |
| `trap_teeth` | Tooth Trap | 1x `log`; 1x `rope`; 1x `houndstooth` | TECH.SCIENCE_TWO | - | :510 |
| `birdcage` | Birdcage | 2x `papyrus`; 6x `goldnugget`; 2x `seeds` | TECH.SCIENCE_TWO | {placer="birdcage_placer"} | :453 |
| `firesuppressor` | Ice Flingomatic | 2x `gears`; 15x `ice`; 2x `transistor` | TECH.SCIENCE_TWO | {placer="firesuppressor_placer", min_spacing=2.5} | :569 |

Notes from the code:
- `reviver` (Telltale Heart) IS craftable by every character, at TECH.NONE: 3 cutgrass + 1 spidergland + 40 health (recipes.lua:420). No builder tag.
- `pighouse` IS craftable (recipes.lua:582): 4 boards + 3 cutstone + 4 pigskin, TECH.SCIENCE_TWO, placer pighouse_placer. (`rabbithouse` is next to it, :583.)
- `resurrectionstatue` (Meat Effigy) costs 40 health (TUNING.EFFIGY_HEALTH_PENALTY = 40, tuning.lua:2173) on top of 4 boards + 4 beardhair; TECH.MAGIC_TWO.
- `researchlab` (Science Machine) needs no tech (TECH.NONE); the Alchemy Engine (`researchlab2`) is TECH.SCIENCE_ONE and `researchlab4` (Prestihatitator) is also only TECH.SCIENCE_ONE; `researchlab3` (Shadow Manipulator) is TECH.MAGIC_TWO.
- `rope` and `boards` carry skill/tag gating for specific characters: rope `no_builder_skill="walter_camp_rope"`, boards `no_builder_tag="cancarveboards"` (Woodie) - normal characters craft them normally.
- `healingsalve` needs `ash`, not honey (recipes.lua:414). `bandage` is a separate recipe (:418).
- `axe` (1 twigs, 1 flint), `pickaxe` (2 twigs, 2 flint) and `torch` (2 cutgrass, 2 twigs) are TECH.NONE; `shovel` and `spear` need Science (SCIENCE_ONE).
- `armorwood` (Log Suit): 8 log + 2 rope, SCIENCE_ONE; `footballhat`: 1 pigskin + 1 rope, SCIENCE_TWO.

## Tech levels (what TECH.* means)

Definitions:
- `TECH` table: constants.lua:1241-1261+. `TECH.NONE = TechTree.Create()` (all zero); `SCIENCE_ONE/TWO/THREE = {SCIENCE=1/2/3}` (:1245-1247); `MAGIC_TWO/THREE = {MAGIC=2/3}` (:1249-1250) ("Magic starts at level 2 so it's not teased from the start", :1248); `ANCIENT_TWO/THREE/FOUR` (:1252-1254); `CELESTIAL_ONE/THREE` (:1256-1257); `SHADOW_TWO = {SHADOW=3}` (:1261). Tech categories are listed in techtree.lua:3-46 (SCIENCE, MAGIC, ANCIENT, CELESTIAL, SHADOW, ...).
- Prototyper trees (`TUNING.PROTOTYPER_TREES`, tuning.lua:1260-1280...):
  - Science Machine (`researchlab`, scienceprototyper.lua:299): SCIENCE 1, MAGIC 1 (tuning.lua:1262-1265)
  - Alchemy Engine (`researchlab2`, scienceprototyper.lua:300): SCIENCE 2, MAGIC 1 (:1267-1270)
  - Prestihatitator (`researchlab4`, magicprototyper.lua:220): MAGIC 2 (:1272-1274)
  - Shadow Manipulator (`researchlab3`, magicprototyper.lua:219): MAGIC 3 (:1276-1278)
  - Ancient altar low/high: ANCIENT 2 / 4; moon orb / altar: CELESTIAL 1 / 3 (tuning.lua after :1280).
- Rule: a recipe can be prototyped when, for every key in the recipe's tech table, `recipe_level <= prototyper_level` (`CanPrototypeRecipe`, widgets/widgetutil.lua:16-23). Keys absent from the recipe are ignored; a key absent from the prototyper's table counts as 0 (TechTree.Create fills all zero, techtree.lua:73-79).
- Only the closest prototyper within `TUNING.RESEARCH_MACHINE_DIST = 4` applies at a time (components/builder.lua:262-273: "activate the first machine in the list. This will be the one you're closest to"). Levels of different machines do not add up.
- Consequences (all derived from the table above): SCIENCE_ONE recipes need Science Machine or Alchemy Engine; SCIENCE_TWO needs the Alchemy Engine; MAGIC_TWO (amulet, Meat Effigy, Shadow Manipulator) needs Prestihatitator or Shadow Manipulator (the Science and Alchemy machines have MAGIC 1 only); the Prestihatitator has SCIENCE 0, so it cannot prototype Science recipes.
- Once a recipe has been built at a prototyper it is learned permanently by that character (`AddRecipe`, builder.lua:463-472) and later crafted anywhere (TECH.NONE recipes need no prototyper).
