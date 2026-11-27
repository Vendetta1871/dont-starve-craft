# 09 — Entity Reference

Extracted from `scripts/prefabs/*` + `tuning.lua`; key values spot-checked against
source. **Prefab id** is the save-safe name. DST doubles many boss/elite HP vs DS1
(`×2` multiplayer modifier baked into TUNING).

> Detailed, per-entry pages with full loot/behaviour: see [content/](content/mobs.md) (mobs, items, recipes, structures, cheat-sheet). Where they differ from the tables below, the content pages are authoritative.

## Mobs

| Prefab id | Display | HP | Dmg | Atk period | Speed walk/run | Drops | Source |
|---|---|---|---|---|---|---|---|
| `pig` (pigman.lua) | Pig | 250 | 33 | 3 s | 3 / 5 | `meat` 75 %, `pigskin` 25 % | pigman.lua:344-359 |
| `werepig` | Werepig | 525 | 40 | 2 s | 3 / 7 | meat×2, pigskin | pigman.lua:552-574 |
| `beefalo` | Beefalo | 1000 | 34 (ornery 50) | 4 s | 1 / 7 | `meat`×4, `beefalowool`×3, `beefalohorn` 33 % | beefalo.lua:1158-1223 |
| `spider` | Spider | 100 | 20 | 3 s | 3 / 5 | `monstermeat` 50 %, `silk` 25 %, `spidergland` 25 % | spider.lua:765-772 |
| `spider_warrior` | Spider Warrior | 400 | 20 | 4-6 s | 4 / 5 | as spider | spider.lua:1088 |
| `hound` | Hound | 150 | 20 | 2 s | — / 10 | `monstermeat`, `houndstooth` 12.5 % | hound.lua:522-564 |
| `firehound` | Fire Hound | 100 | 30 | 2 s | — / 10 | monstermeat, houndstooth, `redgem` 20 % | hound.lua:924 |
| `deerclops` | Deerclops | 4000 | 150 | 4 s | 3 | `meat`×8, `deerclops_eyeball` | deerclops.lua:420,505 |
| `bearger` | Bearger | 6000 | 175 | 3 s | 3 / 10 | meat×8, `bearger_fur` | bearger.lua:545-581 |
| `dragonfly` | Dragonfly | 27500 | 150 (300 fire) | 4 s | 5 / 7 | `dragon_scales`, `lavae_egg` 33 %, meat×6, `goldnugget`×4 | dragonfly.lua:524-567 |
| `beequeen` | Bee Queen | 22500 | 120 | 2 s | 4 | `royal_jelly`×6, `honeycomb`, `honey`×3, `stinger`, `hivehat` | beequeen.lua:424-437 |
| `klaus` | Klaus | 10000 (enraged ≈27440) | 75 (≈206) | 3 s | 2.75 | monstermeat, `charcoal` | klaus.lua:385,551 |
| `krampus` | Krampus | 300 | 50 | 1.2 s | — / 7 | monstermeat, charcoal×2, `krampus_sack` 1 % | krampus.lua:89-112 |
| `bunnyman` | Bunnyman | 200 | 40 | 2 s | 3 / 6 | `carrot`/`meat` 37.5 % ea, `manrabbit_tail` 25 % | bunnyman.lua:660-668 |
| `merm` | Merm | 500 | 30 | 3 s | 3 / 8 | `pondfish`, `froglegs` | merm.lua:497-514 |
| `penguin` | Pengull | 150 | 33 | 3 s | 0.75 | `feather_crow` 20 %, `smallmeat`/`drumstick` 10 % ea | penguin.lua:250-275 |
| `rocky` | Rock Lobster | 3000 | 75 | 3 s | 2.7 | `rocks`×2, meat, `flint`×2 | rocky.lua:491-532 |
| `tallbird` | Tallbird | 800 | 50 | 2 s | 7 | meat×2 | tallbird.lua:196-212 |
| `bat` | Batilisk | 50 | 20 | 1 s | 8 | `batwing` 25 %, `guano` 15 %, monstermeat 10 % | bat.lua:259-288 |
| `crawlingnightmare` | Crawling Nightmare | 300 | 20 | 2.5 s | 3 | `nightmarefuel` 100 %+50 % | nightmarecreature.lua:279 |
| `ghost` | Ghost | 200 | — | — | 2 | — | ghost.lua:86-100 |

Hostility is brain-driven (see [05](05-combat.md)); "conditional" mobs (pig, beefalo,
merm, tallbird, bunnyman, rocky) retaliate or defend homes rather than hunt.

## Items (exact prefab ids — use these in our registry)

| Display | Prefab id | Notes |
|---|---|---|
| Morsel | `smallmeat` | NOT `morsel` |
| Meat | `meat` | file `meats.lua` holds all meat variants (`*_cooked`, `*_jerky`) |
| Monster Meat | `monstermeat` | |
| Rot | `spoiled_food` | replacement target for perishables |
| Moon Rock | `moonrocknugget` | NOT `moonrockshard` |
| Nightmare Fuel | `nightmarefuel` | |
| Beefalo Wool | `beefalowool` | |
| Pig Skin | `pigskin` | |
| Gold Nugget | `goldnugget` | |
| Cut Grass / Twigs | `cutgrass` / `twigs` | |
| Rocks / Flint | `rocks` / `flint` | file `inv_rocks.lua` for rocks |
| Boards / Rope / Silk / Nitre | `boards` / `rope` / `silk` / `nitre` | |
| Bug Net | `bugnet` | catches butterflies/bees/frogs/fish |
| Trap | `trap` | catches small mobs |
| Sleep Dart | `blowdart_sleep` | Blow Dart = `blowdart_pipe` |
| Walking Cane | `cane` | NOT `walkingcane` |
| Night Armor | `armor_sanity` | NOT `nightarmor` |
| Football Helmet | `footballhat` | via `hats.lua` `MakeHat("football")` |
| Miner Hat | `minerhat` | via `hats.lua` `MakeHat("miner")` |
| Spear | `spear` | 34 dmg / 150 uses |
| Torch / Campfire / Fire Pit | `torch` / `campfire` / `firepit` | |
| Backpack | `backpack` | |
| Ice Box | `icebox` | tag `fridge` |
| Tent / Straw Roll | `tent` / `bedroll_straw` | sleepingbag: +1 san +1 hp per tick, −1 hunger/tick |
| Caged Bird | `birdcage` is the block; live item = `caged_bird` | mob import via item — our chosen pattern |
| Spider Eggs | `spider_eggs` | hatches spiders — mob import via item |

Stack sizes: default 20 (`STACK_SIZE_MEDITEM`), small items 40, boards 10
(`stackable.lua:28`, `tuning.lua:81-83`).

## Structures / crafting stations

| Display | Prefab id | Recipe (recipes.lua) | Tier |
|---|---|---|---|
| Science Machine | `researchlab` | 1 goldnugget + 4 log + 4 rocks | unlocks SCIENCE_ONE |
| Alchemy Engine | `researchlab2` | 4 boards + 2 cutstone + 2 transistor | SCIENCE_ONE → unlocks SCIENCE_TWO |
| Prestihatitator | `researchlab4` | 4 rabbit + 4 boards + 1 tophat | SCIENCE_ONE → unlocks MAGIC_ONE |
| Shadow Manipulator | `researchlab3` | 3 livinglog + 1 purplegem + 7 nightmarefuel | unlocks MAGIC_TWO |
| Campfire / Fire Pit | `campfire` / `firepit` | 3 cutgrass+2 log / 2 log+12 rocks | NONE |
| Ice Box | `icebox` | 2 goldnugget + 1 gears + 1 cutstone | SCIENCE_TWO |
| Tent | `tent` | 6 silk + 4 twigs + 3 rope | SCIENCE_TWO |
| Meat Effigy | `resurrectionstatue` | 4 boards + 4 beardhair + 40 HP | MAGIC_TWO |
| Telltale Heart | `reviver` | 3 cutgrass + 1 spidergland + 40 HP | NONE |
| Moon Rock Idol | `moonrockidol` | 1 moonrocknugget + 1 purplegem | CELESTIAL_ONE |
| Pig House | `pighouse` | 4 boards + 3 cutstone + 4 pigskin | SCIENCE_TWO |

Note the **old-prefab-name quirk**: research stations live in
`scienceprototyper.lua` / `magicprototyper.lua` but register as `researchlab*`
("Using old prefab names", `scienceprototyper.lua:252`).

## Not in this build (don't cite them)

`gel`, `coal`, `mosquitofish`, `butterflytrap`, `preserver` item, `sacrificer`
component, `lunarisland`, `obsidianfirepit` (→ `dragonflyfurnace` "Scaled Furnace"),
`telltaleheart.lua` (→ `reviver.lua`), `meateffigy.lua` (→ `resurrectionstatue.lua`).
