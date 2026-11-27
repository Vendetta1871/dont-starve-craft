# Don't Starve Together - World Structures and Resource Nodes

Purpose: ground-truth reference (from the shipped Lua source) for porting DST world structures and resource nodes to a Minecraft mod.

Citation convention: `file.lua:line`, paths relative to `scripts/` (unpacked game scripts). TUNING values are resolved numbers (from the evaluated TUNING table). Time: seg = 30 s, day = 480 s (16 segs). "x d" = days (seconds / 480). Anything not located in the code is written "not found". Display names come from `strings.lua` NAMES (line cited where used).

---

## Plants (pickable / diggable)

Shared pickable rules (components/pickable.lua): after a pick, regrowth takes `regentime` (`pickable.lua:439`, `:560`, multiplied by `SpringGrowthMod`); each pick does `cycles_left = max(0, cycles_left - 1)` (`pickable.lua:538`); at 0 cycles the plant becomes barren (`pickable.lua:369`, `MakeBarren` `:398`). `cycles_left == nil` means unlimited picks (comment `berrybush.lua:89`). `MakeNoGrowInWinter` pauses regrowth in winter (`standardcomponents.lua:830`).
Withering (components/witherable.lua): each plant rolls `wither_temp` in 70..94 and `rejuvenate_temp` in 45..55 (`witherable.lua:21-22`, TUNING `MIN/MAX_PLANT_WITHER_TEMP` 70/94, `MIN/MAX_PLANT_REJUVENATE_TEMP` 45/55); withers when local temp > wither_temp and not rained on (`:79`).

### Grass tuft - `grass` (prefabs/grass.lua)
- Display name: "Grass Tuft" is the dug item (`strings.lua:1572`); product "Cut Grass" (`strings.lua:1604`).
- Pick: `pickable:SetUp("cutgrass", TUNING.GRASS_REGROW_TIME)` (`grass.lua:248`), 1 cutgrass per pick (default `numtoharvest = 1`, `pickable.lua:222-228`).
- Regrow: GRASS_REGROW_TIME = 1440 s = 3 d. Cycles: GRASS_CYCLES = 20 (`grass.lua:253-254`), then barren (stage 1 spawns barren, `:259`). No regrowth in winter (`grass.lua:277`).
- Dig (DIG, work 1; `grass.lua:268-270`): `dig_up` (`:68-86`): spawns product if currently pickable (`:79-81`), then `dug_grass` (or `cutgrass` if withered) (`:83`).
- Withering: `witherable` component (`grass.lua:257`); withered dig gives cutgrass instead of dug_grass.
- Flammable: `MakeMediumBurnable` + `MakeSmallPropagator` (`grass.lua:275-276`).
- Grass gekko morph: picking or digging by a player, when not winter, rolls `GRASSGEKKO_MORPH_CHANCE` = 0.01 (`grass.lua:73-76`, `:122-126`) to trigger nearby grass to morph. Each tuft has timers `morphdelay` = GRASSGEKKO_MORPH_DELAY 12000 s (25 d) +/- variance 2400 s (`grass.lua:158`, `:166`). On "morphing" timer a tuft becomes `grassgekko` only if fewer than GRASSGEKKO_MAX_DENSITY 6 gekkos within radius GRASSGEKKO_DENSITY_RANGE 20 (`grass.lua:143`). Gekko health 150 (`grassgekko.lua:123`); its loot includes cutgrass 0.75 x2 (`grassgekko.lua:17-21`, rest of table not read).

### Sapling - `sapling` / `sapling_moon` (prefabs/sapling.lua)
- Product "twigs": `pickable:SetUp("twigs", TUNING.SAPLING_REGROW_TIME)` (`sapling.lua:141`); SAPLING_REGROW_TIME = 1920 s = 4 d. No `max_cycles` set in the file -> unlimited picks (not found; see `berrybush.lua:89` comment on nil cycles).
- Dig (DIG work 1, `sapling.lua:155-158`): `dig_up` (`:32-42`): product if pickable, then `dug_sapling` (`dug_sapling_moon` for the moon variant), `twigs` if withered.
- `sapling_moon` = same code with `_is_moon` flag (`sapling.lua:80-90`, prefabs list `:21-24`, Prefab `:208`); lunar-themed variant (name/loot identical except the dug item).
- Flammable: `MakeMediumBurnable` (`sapling.lua:161`); no growth in winter (`:163`); witherable (`:148`).

### Berry bush - `berrybush`, `berrybush2`, `berrybush_juicy` (prefabs/berrybush.lua)
- Display names: "Berry Bush" (`strings.lua:1641`), "Juicy Berry Bush" (`:1643`). `berrybush2` is the same code with another look; inspect name overridden to "berrybush" (`berrybush.lua:311-312`, `:370-372`).
- Normal bush: product `berries` (1 per pick), `max_cycles = BERRYBUSH_CYCLES (3) + math.random(2)` = 4-5 picks (`berrybush.lua:342-346`).
  Regrow time (`:110-121`): `BERRY_REGROW_TIME 1440 s (3 d) + BERRY_REGROW_INCREASE 240 s * cycles_passed + BERRY_REGROW_VARIANCE 960 s * random()`.
- Juicy bush: product `berries_juicy`, 3 per pick (`SetUp(..., 3)` `:354`), `max_cycles = BERRYBUSH_JUICY_CYCLES (3) + math.random(2)` = 4-5 (`:356`). Regrow (`:123-134`): `4320 s (9 d) + 240 * cycles_passed + 960 * random()`.
- Barren (cycles exhausted) needs fertilizing; no growth in winter (`:318`).
- Dig (DIG work 1, `:305-307`; `dig_up_common` `:171-190`): withered or barren -> 2 twigs; else if pickable -> berries (1 normal / 3 juicy) + `dug_<prefab>` (`dug_berrybush`, `dug_berrybush2`, `dug_berrybush_juicy`).
- Perd: picking spawns a `perd` 3-6 s later with chance PERD_SPAWNCHANCE = 0.1 (`berrybush.lua:103`, `:104-106`, spawn task at `:104-110` region), not when the picker has tag `berrythief`. Perd health 50.
- Flammable: `MakeLargeBurnable` (`:296`); witherable (`:294`).

---

## Trees

### Evergreen - `evergreen`, `evergreen_sparse` (Lumpy Evergreen), `twiggytree` (prefabs/evergreens.lua)
- Display names: "Evergreen" (`strings.lua:1650`), "Lumpy Evergreen" (`:1651`), "Twiggy Tree" (`:2450`).
- Work: ACTIONS.CHOP (`evergreens.lua:885`). Stages (growable, looping, `springgrowth`, `:889-900`): short -> normal -> tall -> old (`:348-380`).
  | stage | chops | grow time before next stage (base +/- random, s) |
  |---|---|---|
  | short (1) | EVERGREEN_CHOPS_SMALL = 5 (`:250`) | 450 +/- 150 (~0.9 d) |
  | normal (2) | EVERGREEN_CHOPS_NORMAL = 10 (`:274`) | 1500 +/- 600 (~3.1 d) |
  | tall (3) | EVERGREEN_CHOPS_TALL = 15 (`:296`) | 1500 +/- 600 (~3.1 d) |
  | old (4) | 1 (`:319`) | 300 +/- 150 (then loops back to stage 1: `loopstages`, `:898`) |
  Twiggy tree times: 450, 900 +/- 300, 900 +/- 300, 1500 +/- 150 (`TWIGGY_TREE_GROW_TIME`, `:93`).
- Loot (`builds` table `evergreens.lua:56-100`; `SetLoot` per stage `:254`, `:278`, `:300`, `:323-328`):
  | build | short | normal | tall | old |
  |---|---|---|---|---|
  | evergreen | log | log, log, pinecone | log x3, pinecone x2 | pinecone |
  | evergreen_sparse | log | log x2 | log x3 | none (empty loot) |
  | twiggytree | twigs | log, twigs, twiggy_nut | log, twigs x2, twiggy_nut x2 | none (`drop_pinecones_twiggy` never set) |
  All listed drops are guaranteed (no chances), except burnt trees below.
- Burnt tree (`OnBurnt` `:171-215`): work 1; chopping gives `charcoal` + (evergreen only) `pinecone` at 0.1 (`:184-186`, `:159`); a burning tree also drops its normal loot once 10 s after burning (`tree_burnt` `:561-570`, `onpineconetask` `:551-558`).
- Stump: after the fall `make_stump` (`:459-487`): DIG work 1 (`:480-482`), digging gives `log` (`dig_up_stump` `:137-140`). Stump and burnt tree decay (`decay` timer) after `DEAD_DECAY_TIME` 14400 s (30 d) with variance 50% (`:213`, `:485`, removal `:700-722`).
- Leif (Treeguard): when a tree is chopped down (`chop_down_tree` `:498-549`), if the chopper's age (days) >= LEIF_MIN_DAY = 3 it rolls `LEIF_PERCENT_CHANCE` = 0.01333 (1.33%) (`:532-539`), x WOODCUTTER_LEIF_CHANCE_MOD 1.5 for woodcutter tag (Woodie), x BEAVER_LEIF_CHANCE_MOD 0 for beaver. On success, number of leifs = 1 if age <= 30 d, `random(2)` if <= 80 d, else `random(3)` (`:540`). Each transforms another non-leif/non-stump/non-burnt evergreen within LEIF_MAXSPAWNDIST = 15 (`:541-544`, `TransformIntoLeif` `:489-495`; spawn `:442-457`, 1-4 s delay; leif scale 0.7/1/1.25 by stage `:357-371`). Evergreen -> `leif`, sparse -> `leif_sparse`, twiggy none (`:70`, `:84`). Nearby leifs wake when chopping within LEIF_REAWAKEN_RADIUS 20 (`:412`). Leif health LEIF_HEALTH 3000, damage 150.
- Regrowth: `plantregrowth` component (`evergreens.lua:907-910`, components/plantregrowth.lua): every OFFSPRING_TIME (`:152`, variance +/-20%) a tree tries to plant a sapling product nearby: evergreen 2400 s (5 d) -> `pinecone_sapling`; sparse 3840 s (8 d) -> `lumpy_sapling`; twiggy 3840 s -> `twiggy_nut_sapling`. Spawn needs a free point within the tree's "five radius" and fewer than 5 same-tag trees there (`plantregrowth.lua:184-243`). Season multiplier: evergreen x2 in summer, x0 in winter, x1 otherwise (`plantregrowth.lua:24-26`); sparse and twiggy always x1 (`:27-32`). Pinecone growth time PINECONE_GROWTIME 225 +/- 75 s.
- Flammable: `MakeLargeBurnable(inst, TREE_BURN_TIME = 45)` (`evergreens.lua:653`).

### Birchnut tree - `deciduoustree` (prefabs/deciduoustrees.lua)
- Display name "Birchnut Tree" (`strings.lua:2301`). CHOP (`:1370`). Stages short/normal/tall only (old stage commented out `:425`): chops 5 / 10 / 15 (`DECIDUOUS_CHOPS_*`, `:366`, `:387`, `:405`); grow times 450+/-150, 1500+/-600, 1500+/-600 (`:422-424`). Growth springgrowth.
- Loot by leaf build (`:44-106`):
  | build | short | normal | tall | acorn on burnt (0.1) |
  |---|---|---|---|---|
  | normal / red / orange / yellow (leafy) | log | log x2 | log x3, acorn | yes |
  | barren (winter) | log | log x2 | log x3 | no |
  | poison (monster) | livinglog, acorn | livinglog, acorn x2 | livinglog, acorn x3 | yes |
  Extra: a leafy tall tree also drops one more `acorn` when felled (`:607-617`, stage 3 and `leaf_state == "colorful"`).
- Burnt: work 1, `charcoal` + acorn at 0.1 (`:634`, `:667-669`). Stump DIG 1 gives `log` (`livinglog` if monster, `:462`); decays 14400 s (`:524`).
- Regrowth (`:1358-1361`): `acorn_sapling` every DECIDUOUS_REGROWTH OFFSPRING_TIME 1440 s (3 d), only in spring (multiplier 0 otherwise, `plantregrowth.lua:33-35`).
- Monster tree (birchnutter wave): when a tall (stage 3), non-barren tree is chopped down by a char aged >= DECID_MONSTER_MIN_DAY 3, roll `season_chance * mod` (`:532-556`): autumn 0.15, spring 0.08, summer 0.033, winter 0. `mod` = 0.2 (<20 d), 0.5 (<35 d), 1 (<70 d), 1.12 (>=70 d) (`:542-548`), x1.5 woodcutter, x0 beaver. On success 3-4 other birchnut trees within 30 start the monster state after 1-4 s (`:560-575`, `delayed_start_monster` `:477`). Monster state: chops DECIDUOUS_CHOPS_MONSTER = 18 (`:792`), lasts DECID_MONSTER_DURATION 240 s +/-33% (components/deciduoustreeupdater.lua:27), spawns `birchnutdrake` ("Birchnutter", `strings.lua:2300`; health 50, loot acorn 0.4 / twigs 0.6 `birchnutdrake.lua:133`, `:115-117`) - MIN_TREE_DRAKES 3 to MAX_TREE_DRAKES 5 per wake (`deciduoustreeupdater.lua:277`). Killing a monster tree adds 20% livinglog + 20% nightmarefuel (`deciduoustrees.lua:579-580`). Also a seed planted in autumn grows red/orange/yellow, winter barren (`:749-760`).
- Flammable: `MakeLargeBurnable(TREE_BURN_TIME 45)` (`:942`, `:1349`).

### Spiky tree - `marsh_tree`, Spiky Bush - `marsh_bush` (prefabs/marsh_tree.lua, marsh_bush.lua)
- `marsh_tree` ("Spiky Tree", `strings.lua:1809`): CHOP work 10 (`marsh_tree.lua:177-178`); loot `twigs` 1.0, `log` 0.2 (`:16`); stump DIG 1 -> `log` (`:50-51`, `:66-68`); burnt gives charcoal (`:89`).
- `marsh_bush` ("Spiky Bush", `strings.lua:1805`): pick `twigs`, regrow MARSHBUSH_REGROW_TIME 1920 s (4 d) (`marsh_bush.lua:105`); picking hurts the picker for MARSHBUSH_DAMAGE 3 (`:40`) unless `bramble_resistant`; DIG 1 -> `dug_marsh_bush` (+ twigs if withered) (`:30-32`, `:113-115`); `MakeLargeBurnable` -> `burnt_marsh_bush` gives ash (`:58`, `:119`).

---

## Rocks and minerals

### Boulders - `rock1`, `rock2`, `rock_flintless`, `rock_flintless_med`, `rock_flintless_low` (prefabs/rocks.lua)
- All display "Boulder" (`strings.lua:1687-1693`). ACTIONS.MINE (`rocks.lua:294`), 3 visual stages by remaining work: full / med / low when `workleft < N*2/3` / `< N/3` (`:184-185`). Loot dropped at workleft <= 0 with `rock_break_fx` (`:168-172`). Rocks have no regrowth entry (no `rock` in components/regrowthmanager.lua; not found).
- Work: `rock1`, `rock2`, `rock_flintless` = ROCKS_MINE 6 (`:295`); `rock_flintless_med` = ROCKS_MINE_MED 4 (`:358`); `rock_flintless_low` = ROCKS_MINE_LOW 2 (`:373`).
- Loot tables (each `{item, chance}` = one independent roll; `rocks.lua:65-120`):
  | prefab | guaranteed | chance rolls |
  |---|---|---|
  | rock1 (`:65`) | rocks x3, nitre, flint | nitre 0.25, flint 0.60 |
  | rock2 (`:76`) | rocks x3, goldnugget, flint | goldnugget 0.25, flint 0.60 |
  | rock_flintless (`:87`) | rocks x4 | rocks 0.6 |
  | rock_flintless_med (`:96`) | rocks x3 | rocks 0.4 |
  | rock_flintless_low (`:104`) | rocks x2 | rocks 0.2 |
- Not flammable (no burnable in `rocks.lua`).

### Moon rocks - `rock_moon`, `rock_moon_shell`, `moonglass_rock`, `moonrock_pieces`
- `rock_moon` (Boulder): MINE 6; loot (`rocks.lua:111-118`): rocks 1.0, flint 1.0, moonrocknugget 1.0, moonrocknugget 1.0, moonrocknugget 0.6, moonrocknugget 0.3.
- `rock_moon_shell` ("Suspicious Boulder", `strings.lua:1690`): loot table (`rocks.lua:121-129`): rocks 1.0, flint 1.0, moonrocknugget x3 (1.0), moonrocknugget 0.3; on work-finished also spawns `moonrockseed` (`:392-399`, listener `:420`). Work count: not found separately (uses rock base, 6).
- `moonglass_rock` ("Moon Glass", `strings.lua:3289`): loot moonglass 1.0, moonglass 1.0, moonglass 0.25 (`rocks.lua:131-135`); worldgen only on the METEOR tile (`map/terrain.lua:115`).
- `moonrock_pieces` ("Moon Rubble", `strings.lua:2232`): MINE work 1 (`moonrock_pieces.lua:56-57`), no loot, only removed (`:28-31`).
- Moonrocknugget sources: rock_moon, rock_moon_shell, and meteor smashes (below).
- Meteors (`prefabs/shadowmeteor.lua:232-290`, spawner `meteorspawner.lua` -> `meteorshower` component): medium meteor drops rocks (0.6, 0.2), flint (1.0, 0.12), moonrocknugget 0.4; large meteor leaves boulder: `rock_moon` if rand <= METEOR_CHANCE_BOULDERMOON 0.15, else flintless boulder (random of 3 sizes) if <= 0.3, else `rock1` + moonrocknugget 0.4; `"rockmoonshell"` size leaves `rock_moon_shell`. Shower levels (tuning): LVL1 base 2880 s, LVL2 4320 s, LVL3 5760 s between showers (`METEOR_SHOWER_LVLn_BASETIME`); meteor damage 50, radius 3.5, large chance 0.2, medium 0.4.

### Marble - `marblepillar`, `marbletree`, `marbleshrub` (prefabs/marble*.lua)
- `marblepillar` ("Marble Pillar", `strings.lua:2095`): MINE work MARBLEPILLAR_MINE = 10 (`marblepillar.lua:66`); loot marble 1.0, marble 1.0, marble 0.33 (`:12-16`).
- `marbletree` ("Marble Tree", `strings.lua:2096`): MINE work MARBLETREE_MINE = 8 (`marbletree.lua:98`); loot marble 1.0, marble 0.5 (`:14-17`). Prefab generated as `marbletree` plus numbered variants (`:111-118`, exact suffixes not verified).
- `marbleshrub` (+ `_short`/`_normal`/`_tall`, "Marble Shrub", `strings.lua:1789`): grows 3 stages, each 2700 +/- 300 s (`marbleshrub.lua:75-85`, MARBLESHRUB_GROW_TIME); MINE work small 6 / normal 8 / tall 10 (`:29`, `:39`, `:51`). Loot: small = marble; normal = marble (75%) or marble + marblebean (25%) (`:41`); tall = marble + (marble 50% / marblebean 50%) (`:53`). Regrows via `marblebean` planted by hand.

---

## Spawner structures

### Spider den - `spiderden` (stage 1), `spiderden_2`, `spiderden_3` (prefabs/spiderden.lua)
- Display "Spider Den" (`strings.lua:1707`). No player work action: `workable:SetWorkAction(nil)` (`spiderden.lua:805`); the work callback `OnWork` converts hits into health damage of 250 (`SPIDERDEN_HEALTH[1]`) (`:672-680`). Destroyed when health reaches 0 -> `OnKilled` (`:342-349`) releases all children and drops loot.
- Per tier (TUNING; `SetStage` `:142-160`, `:146-149`):
  | tier | health | max spiders | warriors released on attack | emergency warriors / radius | creep radius | loot (`LOOT_DATA` `:61`) |
  |---|---|---|---|---|---|---|
  | 1 | 250 | 3 | 0 | 0 / 10 | 5 | silk x2 |
  | 2 | 500 | 6 | 1 | 4 / 15 | 9 | silk x4 |
  | 3 | 1000 | 9 | 3 | 8 / 20 | 9 | silk x6 + spidereggsack |
  (max spiders pass through `SpringCombatMod`, floored; spider type for tier 1 `spider`, tiers 2-3 `spider_warrior` per SPIDERDEN_SPIDER_TYPE.)
- Spawning: childspawner (`:736-754`): child `spider`, regen 1 spider per SPIDERDEN_REGEN_TIME 90 s, release period 5 s; on attack releases up to 2/4/6 (`:351`), defenders (warriors) per tier above; investigators 1/2/3 (`:404`).
- Growth: growable stages small -> med -> large -> queen (`:568-575`): small -> med after `SPIDERDEN_GROW_TIME[1] = 2400 s * (1 + random())` (5-10 d, `:515-517`); med -> large `2400 * (1 + random())` (`:519-521`); large -> queen `SPIDERDEN_GROW_TIME_QUEEN 6000 s * (1 + random())` (12.5-25 d, `:523-525`). It is NOT driven by feeding or health: it also advances by Webber's silk (`UPGRADETYPES.SPIDER`, `upgradeable` 3 stages x `upgradesperstage = 5`, `components/upgradeable.lua:19-21`, `spiderden.lua:785-789`, `onstageadvancefn` = DoGrowth `:548-551`); shaving (`shaveable`) downgrades a tier (`:638-654`).
- Queen: `AttemptMakeQueen` (`:261-310`): needs no bedazzle, nobody sleeping inside, a player within 30; counts dens+queens within SPIDERDEN_QUEEN_RANGE_CHECK 60; if fewer than SPIDERDEN_QUEEN_CAP 4 the den resets to stage 1 and also spawns a `spiderqueen` (duplicate), else the den is removed and replaced by the queen (`:246-254`). Spider queen health 2500, damage 80.

### Pig house - `pighouse` (prefabs/pighouse.lua)
- "Pig House" (`strings.lua:1677`), player-built (recipe `recipes.lua:582`: boards 4, cutstone 3, pigskin 4). HAMMER work 4 (`pighouse.lua:467-468`). Hammering releases the pig (`:187-189`) and drops 50% of recipe ingredients rounded up (`lootdropper.lua:134-153`, `HAMMER_LOOT_PERCENT` 0.5): boards 2, cutstone 2, pigskin 2; burnt: `BURNT_HAMMER_LOOT_PERCENT` 0.25 -> 1 of each (+ charcoal 40%, `lootdropper.lua:253-255`).
- Spawner: `spawner:Configure("pigman", PIGHOUSE_SPAWN_TIME = 1920 s)` = 4 d respawn (`:472-474`). Flammable: `MakeMediumBurnable`, `MakeLargePropagator` (`:492-493`).

### Rabbit hole - `rabbithole` and rabbit hutch - `rabbithouse`
- `rabbithole` ("Rabbit Hole", `strings.lua:1685`): DIG work 1 (`rabbithole.lua:272-274`); digging releases the rabbit if occupied (`:23-29`). Spawner `rabbit`, respawn RABBIT_RESPAWN_TIME 1200 s (2.5 d) (`:264-265`). Rabbit health 25. World regrowth (components/regrowthmanager.lua:160-162): RABBITHOLE_REGROWTH_TIME 2400 s (5 d), multiplier 1 in summer and 0 otherwise (i.e. only regrows in summer). A lucky roll RABBITKING_LUCKY_ODDS 0.01 (0.1 in quaker) with a player within 30 spawns the Rabbit King event (`rabbithole.lua:224-225`).
- `rabbithouse` ("Rabbit Hutch", `strings.lua:1517`; recipe boards 4, carrot 10, manrabbit_tail 4 `recipes.lua:583`): HAMMER work 4 (`rabbithouse.lua:232-233`), spawns `bunnyman` every RABBITHOUSE_SPAWN_TIME 480 s (1 d) (`:238-239`). Hammer loot = 50% of ingredients: boards 2, carrot 5, manrabbit_tail 2.

### Beehive - `beehive` and Killer bee hive - `wasphive`
- `beehive` ("Beehive", `strings.lua:1832`): no work action; killed by health 200 (`beehive.lua:183`, death -> `OnKilled` `:77-86`); loot honey x3, honeycomb x1 (`:205`, guaranteed). Bees: BEEHIVE_BEES 5 (spring: ceil(5 * SPRING_COMBAT_MOD 1.33) = 7, `:104`), regen 30 s per bee, release period 50 s (`:106-108`, tuning). Emergency (when attacked): BEEHIVE_EMERGENCY_BEES 8 within 20 (`:193-194`). Bees only exit during the day outside winter (`:27`, `:39-49`). Honey is only the loot above; no honey production in this prefab. `MakeLargeBurnable` (`:209`).
- `wasphive` ("Killer Bee Hive", `strings.lua:1837`): health 250 (`wasphive.lua:129`), WASPHIVE_WASPS 5, regen 20 s, release 20 s (`:134-136`), emergency 8 `killerbee` within 25; same loot honey x3, honeycomb (`:150`). Releases killer bees whenever hit or a target is near (`:16`, `:41`, `:47`).

### Hound mound - `houndmound` (prefabs/houndmound.lua)
- "Hound Mound" (`strings.lua:2090`). Health 300 (`:172`), killed by damage (`OnKilled` `:75-84`). Max hounds = `math.random(2, 3)` (HOUNDMOUND_HOUNDS_MIN/MAX, `:180`); regen 180 s, release 30 s (`:178-179`). Seasonal rare child: 20% `icehound` in winter, `firehound` in summer (`:86-98`); special-hound chance by world day (`HOUND_SPECIAL_CHANCE`: 0 / 10% from d15 / 20% from d30 / 33% from d50 / 50% from d75, `:33`). Loot (`:19-27`): houndstooth x3, boneshard x2, redgem 0.01, bluegem 0.01. Hits spawn all guards (`:196`).

### Merm house - `mermhouse` (Leaky Shack) and `mermhouse_crafted` (Craftsmerm House)
- Names: "Leaky Shack" (`strings.lua:2093`), "Craftsmerm House" (`:3448`). Both HAMMER work 2 (`mermhouse.lua:267-269`); hitting releases the merms (`:110-118`).
- Wild `mermhouse`: childspawner `merm`, max MERMHOUSE_MERMS 3, emergency 3 within 15, regen MERMHOUSE_REGEN_TIME 1920 s, release 10 s (`:333-337`); spawns in daytime and not in winter (`:123`, `:210-215`). Loot (`:21-26`, `:330`): boards, rocks, pondfish.
- `mermhouse_crafted` (recipe `recipes.lua:107`: boards 4, cutreeds 3, pondfish 2; Wurt only, marsh tiles only): max 1 merm, regen = 1920 / 2 = 960 s (scaled by offering-pot count, down to x0.5) (`:412-416`, `:402-405`); hammer loot = 50% ingredients: boards 2, cutreeds 2, pondfish 1.

### Tallbird nest - `tallbirdnest` (prefabs/tallbirdnest.lua)
- "Tallbird Nest" (`strings.lua:1857`). Pickable `tallbirdegg` (`:166`); egg re-lay after TALLBIRD_LAY_EGG_TIME_MIN 1440 + random * TALLBIRD_LAY_EGG_TIME_VAR 960 s (3-5 d) (`:46`). childspawner `tallbird`, max 1, regen 5 * 16 * 30 = 2400 s (5 d) (`:175-182`). In spring spawns a smallbird 960-3840 s later (`:132`).

### Pond - `pond` (frogs), `pond_mos`, `pond_cave` (prefabs/pond.lua)
- "Pond" (`strings.lua:1680`). No work. childspawner: children max `random(3, 4)`, spawn 75 s, regen 150 s (`:347-352` frogs, `:313-318` mosquitoes); spawning stops while frozen in winter (`:151`, `:291-292`). `fishable` respawn FISH_RESPAWN_TIME 100 s, adds `pondfish` (`:265`, `:329`).

### Tentacles (marsh) - `tentacle`
- Placed by worldgen (`map/forest_map.lua:171` "tentacles", `map/terrain.lua:90`); no spawner prefab found. Tentacle health 500, damage 34, range 4 (`tentacle.lua:109-113`); loot monstermeat x2 (1.0 each), tentaclespike 0.5, tentaclespots 0.2 (`:15-20`).

### Skeleton - `skeleton` (prefabs/skeleton.lua)
- "Skeleton" (`strings.lua:1698`). HAMMER work SKELETON_WORK 3 (`skeleton.lua:162-163`). Loot (`:13-18`): boneshard x2 (1.0), scrapbook_page 0.10; `skeleton_player`: boneshard x2 only (`:20-23`).

---

## Respawn structures

### Meat Effigy - `resurrectionstatue` (prefabs/resurrectionstatue.lua)
- "Meat Effigy" (`strings.lua:1755`). Recipe (`recipes.lua:580`): boards 4, beardhair 4, health ingredient `EFFIGY_HEALTH_PENALTY` = 40, tech MAGIC_TWO. Health ingredient is a character ingredient: it is paid by the Builder (`components/builder.lua:597-599`, never at 40 or below) and is not part of hammer loot (`recipe.lua:85-91`).
- Attunement: `attunable` with tag `remoteresurrector` (`:169`). Cost on manual attune: 40 health (`onattunecost` `:61-72`), fails (`NOHEALTH`) if current health <= 40 (`:64-66`). On build, the builder is auto-linked without paying again (`onbuilt` `:96-113`).
- One effigy per player: linking a new `remoteresurrector` unlinks the old (`components/attunable.lua:17-25`). A burnt effigy loses attunement (`onburnt` `:49-52`). Re-links on rejoin (`attunable.lua:34-40`).
- Used on resurrection: a ghost respawn via the effigy runs `rebirth` stategraph state (`prefabs/player_common_extensions.lua:375-376`) after pushing `activateresurrection` on the source (`:366`). The effigy listens `inst:ListenForEvent("activateresurrection", inst.Remove)` (`resurrectionstatue.lua:176`): **it is destroyed (removed, no loot) after one resurrection.** Not reusable.
- After revive: health `RESURRECT_HEALTH` 50 (`player_common_extensions.lua:850`, hunger raised to at least 20% `:846-848`).
- Structure: HAMMER work 4 (`:153`); hammer loot = 50% of recipe item ingredients: boards 2, beardhair 2 (`lootdropper.lua:134-153`, `:242-250`); burnt -> only charcoal (`:20-26`). Burn time 10 s, `MakeLargePropagator` (`:158-161`).

### Touch Stone - `resurrectionstone` (prefabs/resurrectionstone.lua)
- "Touch Stone" (`strings.lua:1756`); worldgen layouts "ResurrectionStone", "ResurrectionStoneLit", "ResurrectionStoneWinter" (`map/layouts.lua:264-266`). Not craftable.
- Activation: a ghost player haunts it (`OnHaunt` `:31-43`), allowed only if `haunter:CanUseTouchStone(inst)` (this player has not used this stone ID: `player_common.lua:194-200`, `components/touchstonetracker.lua:38-40`) and the stone is in the active animation. Haunt value HAUNT_INSTANT_REZ (`:91`, `:387`).
- Per player and per stone, one use only: after use the player's tracker records the stone ID (`guy:PushEvent("usedtouchstone", inst)` `:107`); stone IDs max 64 (`:318`). Other players can still use it.
- On resurrection (`OnActivateResurrection` `:96-108`): lightning strike on the stone (`:101`), **drops loot every use**: rocks, rocks, marble, nightmarefuel, marble (`:381`, `:103`), then starts a cooldown of COOLDOWN = 20 s ("delay between uses by different players", `:15`, `:390-394`), becoming inactive (`idle_off`, hauntable removed `:45-60`) until recharged. The stone is not removed -> reusable for other players. Revival health 50 (`RESURRECT_HEALTH`).
- Timeout 10 s resets the animation if the revive never completes (`:16`, `:18-29`).

### Gravestone and grave mound - `gravestone`, `mound` (prefabs/gravestone.lua, mound.lua)
- "Headstone" (`strings.lua:1668`), "Grave" (`:1676`). The gravestone carries a child `mound` (`gravestone.lua:251-252`) and `gravediggable` (`:255-256`); digging the stone up makes it slide away (`OnDugUp` `:66-80`, then removes the stone and erodes its mound; item given by gravediggable component, not read: not found).
- `mound`: DIG work 1 (`mound.lua:197-199`). On dig (`onfinishcallback` `:70-130`): sanity -SANITY_SMALL (10) to the digger (`:76`); first `spawnghost` with chance `ghost_of_a_chance` = 0.1 (`:58`, `:206`) -> spawns `ghost` next to the mound (`:59-62`); otherwise: 50% a random trinket, else weighted `LOOTS` nightmarefuel 1, amulet 1, gears 1, redgem 5, bluegem 5 (`:35-41`, `:79-82`); plus 10% `cookingrecipecard` (`:84`), 10% `scrapbook_page` (`:90`). Mounds also have a `childspawner` of `ghost` (max 1, 10 s) active only during full moons (`:130-137`, `:212-216`).
- Gravestone ghosts: each day-change a `smallghost` (Wendy's friend) is spawned near a stone with chance GHOST_GRAVESTONE_CHANCE = 0.05 if players are around and none within UNIQUE_SMALLGHOST_DISTANCE 50 (`gravestone.lua:22-45`).

---

## Notes for the port (surprising facts)
- Meat Effigy is consumed by its single resurrection (`resurrectionstatue.lua:176`); it costs 40 health at build and again 40 to re-attune an existing one.
- Touch Stone never breaks: per-player single use per stone, drops 5 items every use, 20 s global cooldown.
- Spider dens grow by timer or Webber silk (not by feeding or health), and spawn a duplicate queen while fewer than 4 dens/queens are within 60 tiles; no player work action, killed by damage.
- Rocks (boulders) have no regrowth code; trees regrow through `plantregrowth` (saplings), evergreen x2 in summer / x0 in winter, birchnut spring only.
- Rabbit holes regrow only in summer (multiplier 0 otherwise).
