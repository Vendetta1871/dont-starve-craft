# Don't Starve Together - items reference (ground truth from shipped Lua)

Purpose: verified stats, edible values, fuel values and drop sources of the early-game items, as a reference for a Minecraft port.

Citation convention: all paths are relative to `scripts/` of the unpacked DST game (`prefabs/x.lua:LINE`). Numbers in parentheses after TUNING names are resolved values from tuning.lua. 1 DST day = 480 s (PERISH_ONE_DAY=480); perish times are in seconds of game time. `Fuel` = `fuel.fuelvalue` in seconds of burn time for a fire. `Edible` shows hunger/health/sanity deltas given by `edible` fields.

General defaults: edible component defaults are health 10, hunger 10, sanity 0, foodtype GENERIC (components/edible.lua:21-24), so an item that only sets `hungervalue` still carries health 10 (it matters only for eaters whose diet allows that foodtype, e.g. ELEMENTAL is only eaten by WX-78). Stackable default maxsize is STACK_SIZE_MEDITEM = 20 (components/stackable.lua:28). Stack constants: LARGEITEM=10, MEDITEM=20, SMALLITEM=40, TINYITEM=60, PELLET=120.

Items with a `Sources (auto-scan ...)` list were scanned mechanically; read each entry as "file: context@line(chances)". Wherever 'tree_rock_data.lua' appears it is a worldgen/area table for mineable tree-rocks, not a mob drop.

### Cut Grass - `cutgrass` (`prefabs/cutgrass.lua:63`)
- Stack: 40 (cutgrass.lua:36, STACK_SIZE_SMALLITEM)
- Perishable: none (no perishable component in file)
- Edible: ROUGHAGE (cutgrass.lua:39); hunger 4.6875 (CALORIES_TINY/2, :41); health 1 (HEALING_TINY, :40); sanity 0 (default)
- Fuel: 15 (SMALL_FUEL, cutgrass.lua:47)
- Sources (auto-scan of every quoted `"cutgrass"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/crop.lua`: Crop:MakeWithered@266
  - `components/forestresourcespawner.lua`: ?@37
  - `prefabs/balatro_machine.lua`: REWARDS@43(1)
  - `prefabs/boat.lua`: GRASS_BOAT_DATA@857
  - `prefabs/brokenwalls.lua`: ?@99
  - `prefabs/cave_fern_withered.lua`: fn@50(10)
  - `prefabs/firepen.lua`: OnAttack@35
  - `prefabs/flower_withered.lua`: fn@43(10)
  - `prefabs/grass.lua`: dig_up@83; fn@248
  - `prefabs/grassgator.lua`: ?@20
  - `prefabs/grassgekko.lua`: grassgekko@20-21(0.75,0.75)
  - `prefabs/king_cane.lua`: fn@301
  - `prefabs/leafpile.lua`: drops@102(0.1)
  - `prefabs/staff.lua`: red@128
  - `prefabs/staff_common.lua`: onattack_red@89; fns.orange@579
  - `prefabs/tumbleweed.lua`: MakeLoot@150(40)
  - `prefabs/walls.lua`: ?@391
  - `prefabs/wanderingtrader.lua`: SEASONAL@43-57

### Twigs - `twigs` (`prefabs/twigs.lua`)
- Stack: 40 (twigs.lua:44)
- Perishable: none (no perishable component in file)
- Edible: ROUGHAGE + secondary WOOD (twigs.lua:52-53); hunger 9.375 (CALORIES_TINY, :55); health 0.5 (HEALING_TINY/2, :54); sanity 0 (default)
- Fuel: 15 (SMALL_FUEL, twigs.lua:49)
- Sources (auto-scan of every quoted `"twigs"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/forestresourcespawner.lua`: ?@33
  - `cooking.lua`: ?@134
  - `prefabs/ancienttree_defs.lua`: full@289-290(1.0,0.5)
  - `prefabs/balatro_machine.lua`: REWARDS@43(1)
  - `prefabs/bananabush.lua`: OnDig@83-84
  - `prefabs/berrybush.lua`: dig_up_common@176-177
  - `prefabs/birchnutdrake.lua`: fn@117(.6)
  - `prefabs/catcoonden.lua`: catcoonden@22-23(1.00,1.00)
  - `prefabs/cave_banana_tree.lua`: tree_chopped@64-65
  - `prefabs/driftwood_trees.lua`: driftwood_tree@28-29(1.0,1.0); driftwood_small@38(1.0)
  - `prefabs/evergreens.lua`: twiggy@94-98; OnEntityWake@693
  - `prefabs/fence.lua`: ?@9; fence_junk@14(1.0)
  - `prefabs/grassgator.lua`: ?@20
  - `prefabs/leafpile.lua`: drops@101(0.1)
  - `prefabs/marsh_bush.lua`: dig_up@30; fn@105
  - `prefabs/marsh_tree.lua`: marsh_tree@18(1.0)
  - `prefabs/oceantree_pillar.lua`: fn@577
  - `prefabs/oceantreenut.lua`: oceantreenut@15-16(1.00,0.50)
  - `prefabs/oceanvine_cocoon.lua`: oceanvine_cocoon@28(1.0)
  - `prefabs/planted_tree.lua`: fn@157
  - `prefabs/rock_avocado_bush.lua`: on_dug_up@147-148
  - `prefabs/sapling.lua`: dig_up@40; sapling_common@141
  - `prefabs/staff_common.lua`: fns.orange@563
  - `prefabs/tree_rocks.lua`: tree_rock1_chop@38(1.00)
  - `prefabs/tumbleweed.lua`: MakeLoot@151(40)
  - `prefabs/wanderingtrader.lua`: SEASONAL@44-58
  - `prefabs/watertree_root.lua`: watertree_root@16(0.30)
  - `prefabswaps.lua`: ?@51-100
  - `preparedfoods.lua`: ?@92-1049(1,1,2,1,1,3)

### Log - `log` (`prefabs/log.lua`)
- Stack: 20 (log.lua:51; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: none (no perishable component in file)
- Edible: WOOD (log.lua:34); health 0 (:35); hunger 0 (:36); sanity 0 (default)
- Fuel: 45 (MED_FUEL, log.lua:39)
- Sources (auto-scan of every quoted `"log"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/dockmanager.lua`: ?@313-315
  - `prefabs/ancienttree_defs.lua`: full@283-288(1.0,1.0,1.0,1.0,1.0,0.5); stump@294-295(1.0,1.0)
  - `prefabs/brokenwalls.lua`: ?@98
  - `prefabs/catcoonden.lua`: catcoonden@20-21(1.00,1.00)
  - `prefabs/cave_banana_tree.lua`: tree_chopped@63; stump_dug@190
  - `prefabs/deciduoustrees.lua`: ?@48-1000; barren@59-61; red@70-72; orange@81-83; yellow@92-94; dig_up_stump@462
  - `prefabs/evergreens.lua`: normal@66-68; sparse@80-82; twiggy@94-96; dig_up_stump@138; OnEntityWake@691
  - `prefabs/junk_pile_common.lua`: ITEMS@18
  - `prefabs/marsh_tree.lua`: marsh_tree@19(0.2); dig_up_stump@51
  - `prefabs/moontree.lua`: moontree_small@45-46(1.0,1.0); moontree_normal@52-54(1.0,1.0,1.0); moontree_tall@62-65(1.0,1.0,1.0,1.0); dig_up_stump@180-183
  - `prefabs/mushtree.lua`: dig_up_stump@27; onspawnfn@203; ?@319-345
  - `prefabs/mushtree_moon.lua`: dig_up_stump@16; onspawnfn@44
  - `prefabs/mushtree_webbed.lua`: mushtree_tall_webbed@20(1.0)
  - `prefabs/oceantree.lua`: ?@48-50; dig_up_stump@101; chop_down_tree@368; OnCollide@387
  - `prefabs/oceantree_pillar.lua`: DropLog@181; fn@577
  - `prefabs/palmconetree.lua`: palmconetree_small@52-53(1.0,1.0); palmconetree_normal@58-60(1.0,1.0,1.0); palmconetree_tall@65-67(1.0,1.0,1.0); dig_up_stump@167-170
  - `prefabs/staff_common.lua`: fns.orange@571
  - `prefabs/stalagmite_tall.lua`: stalagmite_tall_full_rock@28(0.05)
  - `prefabs/walls.lua`: ?@390
  - `prefabs/winter_tree.lua`: ?@48-97
  - `prefabs/worm.lua`: yots_worm@49-50(1,1)

### Rocks - `rocks` (`prefabs/inv_rocks.lua:82`)
- Stack: 40 (inv_rocks.lua:55)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (inv_rocks.lua:48); hunger 1 (:49); health 10 and sanity 0 are edible.lua defaults (not set in prefab)
- Fuel: none
- Sources (curated, verified in code):
  - Boulder `rock1` (prefabs/rocks.lua:67-71 loot table, prefab :316-322): 3x rocks (1.0 each), 1 nitre, 1 flint guaranteed + extra nitre 0.25, flint 0.6
  - Boulder `rock2` (gold; rocks.lua:78-80, prefab :328-334): 3x rocks 1.0
  - `rock_flintless` (rocks.lua:89-93, prefab :340-346): 4x rocks 1.0 + 1 rocks 0.6; `rock_flintless_med` (:98-101): 3x 1.0 + 0.4; `rock_flintless_low` (:106-108): 2x 1.0 + 0.2. These contain only rocks (no flint/nitre/gold)
- Sources (auto-scan of every quoted `"rocks"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/quaker.lua`: QUAKESTATE@64
  - `prefabs/altar_prototyper.lua`: ancient_altar@59(0.50)
  - `prefabs/ancienttree_defs.lua`: full@216-219(1.0,1.0,1.0,0.5); stump@227-228(1.0,1.0)
  - `prefabs/antlion.lua`: antlion@57-60(1.00,1.00,0.50,0.50)
  - `prefabs/boat_bumpers.lua`: ?@379
  - `prefabs/brokenwalls.lua`: ?@96-97
  - `prefabs/cave_entrance.lua`: closed_fn@173
  - `prefabs/cave_vent_mite.lua`: SetUpChanceLoot@62
  - `prefabs/cave_vents.lua`: cave_vent_rock@55-57(1.00,1.00,1.00); cave_vent_rock_med@64-65(1.00,1.00); cave_vent_rock_low@70(1.00)
  - `prefabs/cavein_boulder.lua`: cavein_boulder@29-31(1,1,.4)
  - `prefabs/crabking_cannontower.lua`: ck_cannontower@31-34(1.00,1.00,0.50,0.50)
  - `prefabs/crabking_mob.lua`: crabking_mob@31-32(1.00,0.25); crabking_mob_knight@44-46(1.00,0.50,0.50)
  - `prefabs/junk_pile_common.lua`: ITEMS@17
  - `prefabs/lavae.lua`: lavae_frozen@27-29(1.0,1.0,1.0)
  - `prefabs/molehill.lua`: fn@111(4)
  - `prefabs/moonspiderden.lua`: moon_spider_hole@27(1.00)
  - `prefabs/resurrectionstone.lua`: fn@381
  - `prefabs/rock_avocado_fruit.lua`: spawned_prefabs@58-70
  - `prefabs/rocks.lua`: rock1@67-69(1.00,1.00,1.00); rock2@78-80(1.00,1.00,1.00); rock_flintless@89-93(1.0,1.0,1.0,1.0,0.6); rock_flintless_med@98-101(1.0,1.0,1.0,0.4); rock_flintless_low@106-108(1.0,1.0,0.2); rock_moon@113(1.00); rock_moon_shell@123(1.00); rock_petrified_tree@140-141(1.00,0.75); rock_petrified_tree_tall@147-149(1.00,1.00,0.35); rock_petrified_tree_short@155-156(1.00,0.35); rock_petrified_tree_old@162-163(0.50,0.50)
  - `prefabs/rocky.lua`: ?@32-33
  - `prefabs/rocky_boss.lua`: rocky_boss_shadow@58-59
  - `prefabs/rubble.lua`: common_fn@63-65(0.99)
  - `prefabs/ruins_cavein_obstacle.lua`: loot1@18-19(1.00,1.00)
  - `prefabs/saltstack.lua`: saltstack_low@15(1.00); saltstack_med@20(1.00); saltstack_full@25(1.00)
  - `prefabs/seastack.lua`: seastack@23-26(1.00,1.00,1.00,1.00)
  - `prefabs/shadowmeteor.lua`: SetSize@239-240
  - `prefabs/shark.lua`: shark@43-47(1.00,1.00,1.00,0.50,0.50)
  - `prefabs/smashables.lua`: smashables@26(0.80)
  - `prefabs/spiderhole.lua`: spider_hole@29-30(1.00,1.00)
  - `prefabs/staff_common.lua`: fns.orange@575
  - `prefabs/stalagmite.lua`: full_rock@23-25(1.00,1.00,1.00); med_rock@37-38(1.00,1.00); low_rock@47(1.00)
  - `prefabs/stalagmite_tall.lua`: stalagmite_tall_full_rock@20-21(1.00,1.00); stalagmite_tall_med_rock@33-34(1.00,1.00); stalagmite_tall_low_rock@43(1.00)
  - `prefabs/tentacle_pillar.lua`: tentacle_pillar@25(1.00)
  - `prefabs/tree_rock_data.lua`: DEFAULT@3; FOREST_AREA@16; SAVANNA_AREA@24; DECIDUOUS_AREA@29; MARSH_AREA@36; GRASS_AREA@43; ROCKY_AREA@49; DESERT_AREA@56; MOON_AREA@61; HERMIT_AREA@70; GRAVE_AREA@80; MOONQUAY_AREA@88; MUD_AREA@97; CAVERN_AREA@106; GUANO_AREA@116; ROCKYLAND_AREA@123-131; RUINS_AREA@148; RUINS_EXTRA_AREA@161; MOON_GROTTO_AREA@173-179; VENT_AREA_SHADOW_RIFT@191; MUSHROOM_AREA@204; SINKHOLE_AREA@213; MUD_MARSH_AREA@222; VINE_LOOT_DATA@235
  - `prefabs/tree_rocks.lua`: tree_rock1_mine@43-45(1.00,1.00,1.00); tree_rock1_mine_break@56-58(1.00,0.50,0.50)
  - `prefabs/vault_pillar_guard.lua`: vault_pillar_guard@68-71(1,1,0.75,0.5)
  - `prefabs/walls.lua`: ?@388-389
  - `prefabs/waterplant_rock.lua`: waterplant_rock@17-18(1.00,1.00)
  - `prefabs/wobster_den.lua`: wobster_den@41-44(1.00,1.00,1.00,0.50); moonglass_wobster_den@57-58(1.00,1.00)
  - `prefabswaps.lua`: ?@119

### Flint - `flint` (`prefabs/flint.lua:66`)
- Stack: 40 (flint.lua:48)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (flint.lua:43); hunger 1 (:44); health 10 / sanity 0 edible.lua defaults
- Fuel: none
- Sources (curated, verified in code):
  - Boulder `rock1`: flint 1.0 + flint 0.6 (prefabs/rocks.lua:71,73; table at :66-74); boulder `rock2` (gold): flint 1.0 + 0.6 (rocks.lua:82,84)
  - `rock_flintless`, `rock_flintless_med`, `rock_flintless_low`: NO flint (rocks.lua:88-109)
  - `rock_moon` (rocks.lua:114) and `rock_moon_shell` (:124): flint 1.0
  - Stalagmites (cave): full_rock flint 1.0 + 0.6 (stalagmite.lua:27-30), med 1.0 + 0.6 (:39-42), low 1.0 + 0.3 (:48-51); tall variants in stalagmite_tall.lua:23-26/35-38/44-47
  - Petrified trees: rocks.lua:143 (0.25), :151 (0.75); mineable `tree_rock1_mine` flint 1.0 + 0.6 (tree_rocks.lua:47-49)
  - Other: cave_entrance.lua:173 (closed entrance SetLoot 3 flint), molehill.lua:114 (random-loot weight 1.5), shark.lua:39-41, crabking_mob.lua:34-35,48-49; ground spawns via components/forestresourcespawner.lua:28-29 and tree_rock_data area tables (see auto list)
- Sources (auto-scan of every quoted `"flint"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/forestresourcespawner.lua`: ?@28-29
  - `components/pointofinterest.lua`: Stand@60
  - `prefabs/ancienttree_defs.lua`: full@220-221(1.0,0.5)
  - `prefabs/carnivalgame_golf_shape.lua`: OnEntityWake_SetupSorting@292
  - `prefabs/cave_entrance.lua`: closed_fn@173
  - `prefabs/cavein_boulder.lua`: cavein_boulder@32(.1)
  - `prefabs/crabking_mob.lua`: crabking_mob@34-35(0.50,0.15); crabking_mob_knight@48-49(1.00,0.25)
  - `prefabs/molehill.lua`: fn@114(1.5)
  - `prefabs/rocks.lua`: rock1@71-73(1.00,0.60); rock2@82-84(1.00,0.60); rock_moon@114(1.00); rock_moon_shell@124(1.00); rock_petrified_tree@143(0.25); rock_petrified_tree_tall@151(0.75); rock_petrified_tree_short@158(0.25); rock_petrified_tree_old@165(0.75)
  - `prefabs/rocky.lua`: ?@32-33
  - `prefabs/rocky_boss.lua`: rocky_boss_shadow@58-59
  - `prefabs/ruins_cavein_obstacle.lua`: loot1@20(0.60)
  - `prefabs/shadowmeteor.lua`: SetSize@241-242
  - `prefabs/shark.lua`: shark@39-41(1.00,1.00,0.50)
  - `prefabs/staff_common.lua`: fns.orange@575
  - `prefabs/stalagmite.lua`: full_rock@27-30(1.00,0.60); med_rock@39-42(1.00,0.60); low_rock@48-51(1.00,0.30)
  - `prefabs/stalagmite_tall.lua`: stalagmite_tall_full_rock@23-26(1.00,0.60); stalagmite_tall_med_rock@35-38(1.00,0.60); stalagmite_tall_low_rock@44-47(1.00,0.30)
  - `prefabs/tree_rock_data.lua`: FOREST_AREA@18; DECIDUOUS_AREA@30; MARSH_AREA@37; GRASS_AREA@44; ROCKY_AREA@51; MOON_AREA@62; HERMIT_AREA@71; GRAVE_AREA@81; GUANO_AREA@117; ROCKYLAND_AREA@124-134; RUINS_AREA@149; RUINS_EXTRA_AREA@162; MOON_GROTTO_AREA@174-180; VENT_AREA_SHADOW_RIFT@192; MUSHROOM_AREA@205; SINKHOLE_AREA@214; MUD_MARSH_AREA@223; VINE_LOOT_DATA@243
  - `prefabs/tree_rocks.lua`: tree_rock1_mine@47-49(1.00,0.60); tree_rock1_mine_break@60-62(1.00,0.20)
  - `prefabs/wanderingtrader.lua`: WARES@19

### Gold Nugget - `goldnugget` (`prefabs/goldnugget.lua:167`)
- Stack: 20 (goldnugget.lua:65; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (goldnugget.lua:56); hunger 2 (:57); health 10 / sanity 0 edible.lua defaults
- Fuel: none
- Also defines `lucky_goldnugget` (goldnugget.lua:168, edible hunger 2 at :146)
- Sources (curated, verified in code):
  - Boulder `rock2` (gold boulder): 1 goldnugget guaranteed + 0.25 chance for a 2nd (prefabs/rocks.lua:78-84, prefab :328-334). Boulder `rock1` has NO gold
  - Stalagmites: full_rock 1.0 + 0.25 (stalagmite.lua:26-29), med 0.5 (:40), low 0.5 (:49); tall: 1.0 + 0.25 (stalagmite_tall.lua:22-25), med 0.15 (:37), low 0.15 (:46)
  - Dragonfly drop: 4x 1.0 + 4x 0.5 (dragonfly.lua:52-60); Pig King trade reward (pigking.lua:75, ontradeforgold); molehill.lua:113 (random-loot weight .5)
- Sources (auto-scan of every quoted `"goldnugget"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/klaussackloot.lua`: items@95-111
  - `components/piratespawner.lua`: additem@580
  - `prefabs/balatro_machine.lua`: REWARDS@47-49(2,8,12)
  - `prefabs/cozy_bunnyman.lua`: OnSetupPrizes@333
  - `prefabs/dragonfly.lua`: dragonfly@52-60(1.00,1.00,1.00,1.00,0.50,0.50,0.50,0.50)
  - `prefabs/mermking.lua`: TradeItem@132
  - `prefabs/molehill.lua`: fn@113(.5)
  - `prefabs/pigking.lua`: ?@32; ontradeforgold@75; StartMinigame@540
  - `prefabs/rocks.lua`: rock2@81-83(1.00,0.25)
  - `prefabs/stalagmite.lua`: full_rock@26-29(1.00,0.25); med_rock@40(0.50); low_rock@49(0.50)
  - `prefabs/stalagmite_tall.lua`: stalagmite_tall_full_rock@22-25(1.00,0.25); stalagmite_tall_med_rock@37(0.15); stalagmite_tall_low_rock@46(0.15)
  - `prefabs/tree_rock_data.lua`: FOREST_AREA@17; DECIDUOUS_AREA@31; ROCKY_AREA@52; CAVERN_AREA@107; GUANO_AREA@118; ROCKYLAND_AREA@125; MOON_GROTTO_AREA@181; VENT_AREA_SHADOW_RIFT@193; MUSHROOM_AREA@206; SINKHOLE_AREA@215; VINE_LOOT_DATA@244
  - `prefabs/yotr_fightring.lua`: OnRingActivated@413

### Nitre - `nitre` (`prefabs/nitre.lua:59`)
- Stack: 40 (nitre.lua:39)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL + secondary NITRE (nitre.lua:32-33); hunger 2 (:34); health 10 / sanity 0 edible.lua defaults
- Fuel: 90 (MED_LARGE_FUEL, nitre.lua:42), fueltype CHEMICAL (:43) - not burnable in normal fires
- Sources (curated, verified in code):
  - Boulder `rock1` (the only normal surface boulder with nitre): nitre 1.0 + 0.25 (prefabs/rocks.lua:70-72; table `rock1`, prefab rocks.lua:316-322). `rock2` (gold) and `rock_flintless*` have none
  - Petrified trees (rocks.lua:142 0.4, :150 0.65, :157 0.25, :164 0.25) and mineable tree rocks (tree_rocks.lua:46-48, 59-61)
  - Mobs/misc: rocky (rocky.lua:330-331 OnWorkFinished), rocky_boss.lua:197-200, bat_acidinfused (bat.lua:33, 0.2), molehill.lua:112 (random-loot weight 1.5), pond.lua:459 (cave pond mined), acid bat wave (components/acidbatwavemanager.lua:20)
- Sources (auto-scan of every quoted `"nitre"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/acidbatwavemanager.lua`: ?@20
  - `prefabs/bat.lua`: bat_acidinfused@33(0.2)
  - `prefabs/molehill.lua`: fn@112(1.5)
  - `prefabs/pond.lua`: OnPondCaveMinedFinished@459
  - `prefabs/rocks.lua`: rock1@70-72(1.00,0.25); rock_petrified_tree@142(0.4); rock_petrified_tree_tall@150(0.65); rock_petrified_tree_short@157(0.25); rock_petrified_tree_old@164(0.25)
  - `prefabs/rocky.lua`: ?@33; OnWorkFinished@330-331
  - `prefabs/rocky_boss.lua`: rocky_boss_shadow@59; OnWorkFinished@197-200
  - `prefabs/staff_common.lua`: fns.orange@575
  - `prefabs/tree_rock_data.lua`: FOREST_AREA@19; ROCKY_AREA@50; SINKHOLE_AREA@216; VINE_LOOT_DATA@245
  - `prefabs/tree_rocks.lua`: tree_rock1_mine@46-48(1.00,0.25); tree_rock1_mine_break@59-61(0.25,0.10)

### Boards - `boards` (`prefabs/boards.lua:57`)
- Stack: 10 (boards.lua:35, STACK_SIZE_LARGEITEM)
- Perishable: none (no perishable component in file)
- Edible: WOOD (boards.lua:30); health 0, hunger 0 (:31-32)
- Fuel: 180 (LARGE_FUEL, boards.lua:50)
- Crafted from 4 logs (recipes.lua:459); no natural source found other than shops/rewards in the list below
- Sources (auto-scan of every quoted `"boards"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/dockmanager.lua`: ?@311
  - `prefabs/boat.lua`: WOOD_BOAT_DATA@792; PIRATE_BOAT_DATA@931; ANCIENT_BOAT_DATA@1010
  - `prefabs/boatfragments.lua`: boatfragment@15(1.00)
  - `prefabs/fence.lua`: ?@8
  - `prefabs/junk_pile_common.lua`: ITEMS@19
  - `prefabs/staff_common.lua`: fns.orange@571
  - `prefabs/support_pillar.lua`: LootSetupFn@444
  - `prefabs/willow_ember.lua`: ThrowFire_SpawnFire@230
  - `prefabs/winter_tree.lua`: ?@11-105

### Rope - `rope` (`prefabs/rope.lua:81`)
- Stack: 20 (rope.lua:64; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: 45 (MED_FUEL, rope.lua:71)
- Sources (auto-scan of every quoted `"rope"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/balatro_machine.lua`: REWARDS@44(1)
  - `prefabs/fence.lua`: ?@8
  - `prefabs/staff_common.lua`: fns.orange@579
  - `prefabs/tumbleweed.lua`: MakeLoot@155(1)
  - `prefabs/vault_lobby_exit.lua`: ?@377

### Cut Stone - `cutstone` (`prefabs/cutstone.lua:50`)
- Stack: 10 (cutstone.lua:30, STACK_SIZE_LARGEITEM)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: none
- Sources (auto-scan of every quoted `"cutstone"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/balatro_machine.lua`: REWARDS@44(1)
  - `prefabs/chesspieces.lua`: ?@79
  - `prefabs/rubble.lua`: common_fn@66(0.10)
  - `prefabs/sacred_chest.lua`: ?@17-22
  - `prefabs/sculptingtable.lua`: ?@18
  - `prefabs/smashables.lua`: smashables@27(0.10)
  - `prefabs/staff_common.lua`: fns.orange@575
  - `prefabs/wagstaff_machinery.lua`: wagstaff_machinery@23(0.75)

### Papyrus - `papyrus` (`prefabs/papyrus.lua:51`)
- Stack: 40 (papyrus.lua:33)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: 15 (SMALL_FUEL, papyrus.lua:38)
- Sources (auto-scan of every quoted `"papyrus"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/erasablepaper.lua`: GiveOrDropItem@15

### Silk - `silk` (`prefabs/silk.lua:52`)
- Stack: 40 (silk.lua:36)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: none
- Sources (auto-scan of every quoted `"silk"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/bernie_inactive.lua`: dodecay@34
  - `prefabs/moonspiderden.lua`: moon_spider_hole@29-33(1.00,0.50)
  - `prefabs/mushtree_webbed.lua`: mushtree_tall_webbed@21-23(1.0,0.3,0.3)
  - `prefabs/oceanvine_cocoon.lua`: oceanvine_cocoon@25-27(1.0,0.3,0.3)
  - `prefabs/smashables.lua`: ?@275(0.1)
  - `prefabs/spider.lua`: create_common@602; ?@647(.5); create_spitter@845
  - `prefabs/spiderden.lua`: ?@63-813(1)
  - `prefabs/spiderhole.lua`: spider_hole@31-35(1.00,0.50)
  - `prefabs/staff_common.lua`: fns.orange@559
  - `prefabs/tree_rock_data.lua`: MARSH_AREA@38; CAVERN_AREA@111; ROCKYLAND_AREA@135; MUD_MARSH_AREA@224; VINE_LOOT_DATA@254; WEB_CREEP@275
  - `prefabs/tumbleweed.lua`: MakeLoot@154(1)
  - `prefabs/webber.lua`: master_postinit@135

### Spider Gland - `spidergland` (`prefabs/spidergland.lua:47`)
- Stack: 20 (spidergland.lua:38; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: none (no perishable component in file)
- Edible: none
- Healer: restores 8 health when used on a character (healer:SetHealthAmount(HEALING_MEDSMALL=8), spidergland.lua:41-42)
- Fuel: none
- Sources (auto-scan of every quoted `"spidergland"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/moonspiderden.lua`: moon_spider_hole@32(0.25)
  - `prefabs/spider.lua`: create_common@602; ?@648-649(.5,1); create_spitter@845
  - `prefabs/spiderhole.lua`: spider_hole@34(0.25)

### Hound's Tooth - `houndstooth` (`prefabs/houndstooth.lua:59`)
- Stack: 40 (houndstooth.lua:41)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: none
- Sources (auto-scan of every quoted `"houndstooth"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/chest_mimic.lua`: chest_mimic@216-217(0.50,0.50)
  - `prefabs/hound.lua`: hound@113(0.125); hound_fire@119(1.0); hound_cold@126-127(1.0,1.0); clayhound@134(0.1); mutatedhound@140-141(1.0,1.0)
  - `prefabs/houndbone.lua`: SetBoneType@27(.5)
  - `prefabs/houndmound.lua`: hound_mound@21-23(1.00,1.00,1.00)
  - `prefabs/tumbleweed.lua`: MakeLoot@178(1)
  - `prefabs/warg.lua`: warg@105-107(1.00,0.66,0.33); claywarg@119-121(1.00,0.66,0.33); gingerbreadwarg@138-140(1.00,0.66,0.33)
  - `prefabs/warglet.lua`: warglet@40-41(1.00,0.33)

### Beard Hair - `beardhair` (`prefabs/beardhair.lua:47`)
- Stack: 40 (beardhair.lua:32)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: 45 (MED_FUEL, beardhair.lua:35)
- Sources (curated, verified in code):
  - Wilson's beard: `components.beard.prize = "beardhair"` (prefabs/wilson.lua:154); beard grows at 4/8/16 days (BEARD_DAYS = {4,8,16}, wilson.lua:95; callbacks :156-158); shaving yields bits 1 / 3 / 9 by beard level (TUNING.WILSON_BEARD_BITS {LEVEL1=1,LEVEL2=3,LEVEL3=9}, used wilson.lua:98-100); each bit spawned in components/beard.lua:146-148. Only Wilson has beardhair as prize (other prizes: webber.lua:135 silk, beefalo.lua:1141 beefalowool)
  - Splumonkey (`monkey`): loot table `monkey` beardhair 1.0 (guaranteed) (prefabs/monkey.lua:25-29, applied :311)
  - Rabbit in its Beardling form: random loot beardhair 0.5 + monstermeat 1 (rabbit.lua:96-111, applied :288; forced-nightmare variant drops only nightmarefuel, rabbit.lua:46)
  - Bunnyman as Beard Lord (killer insane or forced nightmare): guaranteed 2x beardhair + monstermeat (bunnyman.lua:41-42, selected :302-310); forced variant adds nightmarefuel; cozy_bunnyman.lua:25 same table
  - Aggressive Rabbit King: loot_aggressive = monstermeat, beardhair x2, rabbitkingspear (rabbitking.lua:156-166); summoned minion random loot beardhair weight 3 (rabbitking.lua:463)
  - Tumbleweed: 2% chance entry (tumbleweed.lua:173)
  - Bernie (inactive, on decay): SpawnLootPrefab("beardhair") (bernie_inactive.lua:32)

### Pig Skin - `pigskin` (`prefabs/pigskin.lua:47`)
- Stack: 40 (pigskin.lua:28)
- Perishable: none (no perishable component in file)
- Edible: HORRIBLE (pigskin.lua:42); hunger/health/sanity not set -> edible.lua defaults (health 10, hunger 10, sanity 0)
- Fuel: none
- Sources (auto-scan of every quoted `"pigskin"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/gargoyles.lua`: brokenwerepig_loot@16(.5)
  - `prefabs/pigman.lua`: SetNormalPig@359(1); SetGuardPig@486(1); WerepigWakeTest@555; MakeFrankenPig@609; moon@1030
  - `prefabs/wanderingtrader.lua`: RANDOM_UNCOMMONS@27

### Manure - `poop` (`prefabs/poop.lua:126`)
- Stack: 20 (poop.lua:90; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: 45 (MED_FUEL, poop.lua:109)
- Sources (curated, verified in code):
  - Periodic spawners (`periodicspawner:SetPrefab("poop")`): beefalo.lua:1188, babybeefalo.lua:166, koalefant.lua:125, grassgator.lua:201, spat.lua:258, monkey.lua:467
  - Pigman: SpawnPrefab("poop") pigman.lua:139; monkey.lua:61 and powdermonkey.lua:43 (poop given into their inventory); monkeyprojectile.lua:23; ipecacsyrup.lua:93

### Monster Meat - `monstermeat` (`prefabs/meats.lua:902`)
- Stack: 20 (meats.lua:191 common(); no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: 2880 s = 6 days (PERISH_FAST, meats.lua:294)
- Edible: MEAT (meats.lua:184) + secondary MONSTER (:290); hunger 18.75 (CALORIES_MEDSMALL, :292); health -20 (HEALING_MED, :291); sanity -15 (SANITY_MED, :293)
- Cooks to cookedmonstermeat (meats.lua:48/284): hunger 18.75, health -3, sanity -10, perish 7200 s (lines 316-320)
- Fuel: none
- Sources (auto-scan of every quoted `"monstermeat"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@107
  - `prefabs/bat.lua`: bat@26(0.10); bat_acidinfused@32(0.2)
  - `prefabs/bunnyman.lua`: ?@41-42; MakeVampireBunny@414
  - `prefabs/cave_vent_mite.lua`: ?@185(1)
  - `prefabs/cookiecutter.lua`: cookiecutter@23(1.00)
  - `prefabs/cozy_bunnyman.lua`: ?@25
  - `prefabs/eyeofterror.lua`: eyeofterror@470-473(1.00,1.00,0.50,0.50)
  - `prefabs/gargoyles.lua`: brokenhound_loot@9(1)
  - `prefabs/glommer.lua`: glommer@21-23(1.00,1.00,1.00)
  - `prefabs/hound.lua`: hound@112(1.000); hound_fire@118(1.0); hound_cold@125(1.0); mutatedhound@139(1.0)
  - `prefabs/krampus.lua`: krampus@20(1.0)
  - `prefabs/leif.lua`: ?@184
  - `prefabs/meats.lua`: monster@284; cookedmonster@307; driedmonster@328; raw@398
  - `prefabs/merm.lua`: MakeMummyMerm@1352
  - `prefabs/molebat.lua`: fn@306(2)
  - `prefabs/penguin.lua`: mutated_penguin@45(0.25)
  - `prefabs/pigman.lua`: MakeFrankenPig@609
  - `prefabs/rabbit.lua`: SetBeardlingLoot@100(1); SetForcedBeardlingLoot@112(1)
  - `prefabs/rabbitking.lua`: ?@464(3)
  - `prefabs/spider.lua`: create_common@602; ?@646(1); create_spitter@845
  - `prefabs/squid.lua`: squid@44(1.00)
  - `prefabs/tentacle.lua`: tentacle@17-18(1.0,1.0)
  - `prefabs/tentacle_arm.lua`: fn@122
  - `prefabs/warg.lua`: warg@98-103(1.00,1.00,1.00,1.00,0.50,0.50)
  - `prefabs/warglet.lua`: warglet@35-38(1.00,1.00,1.00,0.50)
  - `prefabs/worm.lua`: yots_worm@47-48(1,1); worm@57-60(1,1,1,1)
  - `prefabs/worm_boss.lua`: worm_boss@58-62(1.00,1.00,1.00,0.66,0.66)
  - `preparedfoods.lua`: ?@319
  - `preparedfoods_warly.lua`: ?@175

### Meat - `meat` (`prefabs/meats.lua:899`)
- Stack: 20 (meats.lua:191; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: 2880 s = 6 days (PERISH_FAST, meats.lua:394)
- Edible: MEAT (meats.lua:184); hunger 25 (CALORIES_MED, :391); health 1 (HEALING_TINY, :390); sanity -10 (SANITY_SMALL, :392)
- Cooked meat (`cookedmeat`, meats.lua:343-353): hunger 25, health 3, sanity 0, perish 4800 s = 10 days
- Fuel: none
- Sources (auto-scan of every quoted `"meat"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@106; TestRecipes@322
  - `emoji_items.lua`: ?@459
  - `prefabs/antlion.lua`: antlion@52-55(1.00,1.00,1.00,1.00)
  - `prefabs/babybeefalo.lua`: ?@20
  - `prefabs/bearger.lua`: bearger@78-85(1.00,1.00,1.00,1.00,1.00,1.00,1.00,1.00)
  - `prefabs/beefalo.lua`: beefalo@51-54(1.00,1.00,1.00,1.00)
  - `prefabs/bunnyman.lua`: LootSetupFunction@314(3)
  - `prefabs/catcoon.lua`: catcoon@74(1.00); fn@371
  - `prefabs/cozy_bunnyman.lua`: LootSetupFunction@291(3)
  - `prefabs/crabking.lua`: crabking@89-94(1.00,1.00,1.00,1.00,1.00,0.50)
  - `prefabs/crabking_claw.lua`: crabking_claw@103(1.00)
  - `prefabs/crabking_mob.lua`: crabking_mob@28-29(1.00,0.25); crabking_mob_knight@40-42(1.00,0.50,0.25)
  - `prefabs/deer.lua`: deer@51-52(1.00,0.50); SCRAPBOOK_OVERRIDEDATA_NORMAL@425-429
  - `prefabs/deerclops.lua`: deerclops@331-338(1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0)
  - `prefabs/dragonfly.lua`: dragonfly@45-50(1.00,1.00,1.00,1.00,1.00,1.00)
  - `prefabs/gargoyles.lua`: brokenwerepig_loot@15(1)
  - `prefabs/koalefant.lua`: ?@22-24; SimulateKoalefantDrops@32
  - `prefabs/lightninggoat.lua`: lightninggoat@23-24(1.00,1.00); chargedlightninggoat@30-31(1.00,1.00)
  - `prefabs/malbatross.lua`: malbatross@398-404(1.00,1.00,1.00,1.00,1.00,1.00,1.00)
  - `prefabs/meats.lua`: cooked@344; raw@384
  - `prefabs/minotaur.lua`: minotaur@51-58(1.00,1.00,1.00,1.00,1.00,1.00,1.00,1.00)
  - `prefabs/moose.lua`: moose@28-33(1.00,1.00,1.00,1.00,1.00,1.00)
  - `prefabs/mossling.lua`: mossling@24(1.00)
  - `prefabs/oceanfishdef.lua`: oceanfish_small_1@224; oceanfish_small_2@270; oceanfish_small_3@316; oceanfish_small_4@362; ?@543-964; oceanfish_medium_1@640; oceanfish_medium_2@686; oceanfish_medium_3@732; oceanfish_medium_4@779; oceanfish_medium_6@872; oceanfish_medium_7@918; oceanfish_medium_9@1012
  - `prefabs/otter.lua`: otter@18(1.00)
  - `prefabs/pigman.lua`: SetNormalPig@358(3); SetGuardPig@485(3); WerepigWakeTest@555; moon@1030
  - `prefabs/primemate.lua`: primemate@42(1.0)
  - `prefabs/rabbitking.lua`: ?@466(3)
  - `prefabs/rocky.lua`: ?@32-33
  - `prefabs/rocky_boss.lua`: rocky_boss_shadow@58-59
  - `prefabs/smallbird.lua`: create_teen_smallbird@488
  - `prefabs/spat.lua`: spat@37-40(1.00,1.00,1.00,1.00)
  - `prefabs/tallbird.lua`: ?@15
  - `prefabs/ticoon.lua`: ticoon@20(0.50)
  - `prefabs/toadstool.lua`: toadstool@91-95(1.00,1.00,1.00,0.50,0.25); toadstool_dark@116-120(1.00,1.00,1.00,0.50,0.25)
  - `prefabs/walrus.lua`: walrus@25(1.00); walrus_wee_loot@33(1.0); create_little@215
  - `preparedfoods.lua`: ?@125-439(2,1,1,2)

### Morsel - `smallmeat` (`prefabs/meats.lua:905`)
- Stack: 40 (meats.lua:420)
- Perishable: 2880 s = 6 days (PERISH_FAST, meats.lua:418)
- Edible: MEAT; hunger 12.5 (CALORIES_SMALL, :415); health 0 (:414); sanity -10 (SANITY_SMALL, :416)
- Cooked (`cookedsmallmeat`, meats.lua:427-438): hunger 12.5, health 1, sanity 0, perish 4800 s
- Fuel: none
- Sources (auto-scan of every quoted `"smallmeat"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@109
  - `prefabs/babybeefalo.lua`: ?@18-19
  - `prefabs/birdcage.lua`: OnBirdStarve@438
  - `prefabs/birds.lua`: ?@326(1); puffin_loot_setup@432(1)
  - `prefabs/buzzard.lua`: buzzard@22(0.33)
  - `prefabs/dustmoth.lua`: dustmoth@15(1.0); fn@208
  - `prefabs/hats.lua`: ?@5287-6955
  - `prefabs/mole.lua`: ?@232
  - `prefabs/monkey.lua`: ?@23; monkey@27(1.0)
  - `prefabs/otter.lua`: otter@19(1.00)
  - `prefabs/penguin.lua`: penguin@39(0.1)
  - `prefabs/powdermonkey.lua`: powdermonkey@30(1.0)
  - `prefabs/rabbit.lua`: ?@45
  - `prefabs/smallbird.lua`: create_smallbird@420
  - `preparedfoods.lua`: ?@108-987(2,2,2,1)

### Drumstick - `drumstick` (`prefabs/meats.lua:908`)
- Stack: 20 (meats.lua:191; no override in drumstick() :465-482; no maxsize set, so default STACK_SIZE_MEDITEM=20 at components/stackable.lua:28)
- Perishable: 2880 s = 6 days (meats.lua:476)
- Edible: MEAT; hunger 12.5 (CALORIES_SMALL, :473); health 0 (:472); sanity -10 (:474)
- Cooked (`drumstick_cooked`, :483-492): hunger 12.5, health 1, perish 4800 s
- Fuel: none
- Sources (auto-scan of every quoted `"drumstick"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@108
  - `prefabs/buzzard.lua`: buzzard@21(1.00)
  - `prefabs/meatrack2.lua`: ?@43
  - `prefabs/meats.lua`: drumstick@466; drumstick_cooked@484
  - `prefabs/moose.lua`: moose@34-35(1.00,1.00)
  - `prefabs/mossling.lua`: mossling@25(1.00)
  - `prefabs/penguin.lua`: penguin@40(0.1)
  - `preparedfoods.lua`: ?@238(2)

### Berries - `berries` (`prefabs/veggies.lua (generated, stats table veggies.lua:85)`)
- Stack: 40 (veggies.lua:618, STACK_SIZE_SMALLITEM)
- Perishable: 2880 s = 6 days (PERISH_FAST, veggies.lua:85 arg 4; applied :603)
- Edible: VEGGIE + secondary BERRY (veggies.lua:599-600,89); hunger 9.375 (CALORIES_TINY, :85); health 0; sanity 0 (applied :596-598)
- Cooked (`berries_cooked`, veggies.lua:86 args): hunger 12.5, health 1, perish 1440 s
- Fuel: none
- Sources (auto-scan of every quoted `"berries"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@92
  - `prefabs/berrybush.lua`: ?@1; normal_postinit@343; juicy_postinit@368-369
  - `prefabs/sacred_chest.lua`: ?@17-18
  - `prefabs/tumbleweed.lua`: MakeLoot@174(1)
  - `prefabs/waxed_plants.lua`: Tree_Minimap_CommonPostInit@42-43
  - `prefabswaps.lua`: ?@67-75
  - `preparedfoods.lua`: ?@15-422(1,1,1,1,2)
  - `worldgen_main.lua`: GenerateNew@398

### Carrot - `carrot` (`prefabs/veggies.lua (generated, stats table veggies.lua:45)`)
- Stack: 40 (veggies.lua:618)
- Perishable: 4800 s = 10 days (PERISH_MED, veggies.lua:45 arg 4)
- Edible: VEGGIE (veggies.lua:599); hunger 12.5 (CALORIES_SMALL); health 1 (HEALING_TINY); sanity 0 (veggies.lua:45,596-598)
- Cooked (`carrot_cooked`, veggies.lua:46): hunger 12.5, health 3, perish 2880 s
- Fuel: none
- Note: prefab `carrot` is the picked item; `carrot_planted` (carrot.lua:57) is the world plant
- Sources (auto-scan of every quoted `"carrot"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@100; TestRecipes@322
  - `emoji_items.lua`: ?@129
  - `prefabs/bunnyman.lua`: LootSetupFunction@313(3)
  - `prefabs/carrot.lua`: fn@37(10)
  - `prefabs/cozy_bunnyman.lua`: LootSetupFunction@290(3)
  - `prefabs/rabbitking.lua`: ?@465(3)
  - `prefabs/sacred_chest.lua`: ?@17-19
  - `prefabs/seeds.lua`: pickproduct@57
  - `prefabs/veggies.lua`: MakeVeggie@424; fn@546; ?@607-689
  - `prefabs/wormwood_carrat.lua`: finish_transformed_life@41
  - `preparedfoods.lua`: ?@15-847(2,1,1)

### Seeds - `seeds` (`prefabs/seeds.lua:216`)
- Stack: 40 (seeds.lua:102)
- Perishable: 19200 s = 40 days (PERISH_SUPERSLOW, seeds.lua:120)
- Edible: SEEDS (seeds.lua:105); hunger 4.6875 (CALORIES_TINY/2, :155); health 0 (:154); sanity 0
- Cooked (`seeds_cooked`, seeds.lua:187-189): hunger 4.6875, health 1, perish 4800 s
- Fuel: none
- Sources (auto-scan of every quoted `"seeds"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/birds.lua`: ChooseSeeds@64
  - `prefabs/bundle.lua`: ?@532-533
  - `prefabs/farm_plant_defs.lua`: ?@261
  - `prefabs/fruitfly.lua`: LootSetupFunction@294(0.1)
  - `prefabs/mermking.lua`: ?@43-67
  - `prefabs/tumbleweed.lua`: MakeLoot@156(2)

### Petals - `petals` (`prefabs/petals.lua:138`)
- Stack: 40 (petals.lua:109)
- Perishable: 2880 s = 6 days (PERISH_FAST, petals.lua:103)
- Edible: VEGGIE (petals.lua:89); health 1 (HEALING_TINY, :87); hunger 0 (:88); sanity 0
- Fuel: 7.5 (TINY_FUEL, petals.lua:93)
- Sources (auto-scan of every quoted `"petals"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/driedplants_defs.lua`: ?@4
  - `prefabs/flower.lua`: commonfn@172(10)
  - `prefabs/graveurn.lua`: OnGraveDiggerUsed@130
  - `prefabs/hedgehound_bush.lua`: OnActivate@96
  - `prefabs/hermitcrabtea_defs.lua`: ?@4; MoonBlossom_OnAttacked@133
  - `prefabs/leafpile.lua`: drops@107(0.1)
  - `prefabs/sacred_chest.lua`: ?@17-21
  - `prefabs/staff_common.lua`: fns.orange@579
  - `prefabs/tumbleweed.lua`: MakeLoot@152(1)
  - `prefabs/yotr_rabbitshrine.lua`: getlockedpillow@51; gettotallyrandomset@67; set_pillows@82

### Nightmare Fuel - `nightmarefuel` (`prefabs/nightmarefuel.lua:98`)
- Stack: 40 (nightmarefuel.lua:79)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: 180 (LARGE_FUEL, nightmarefuel.lua:83), fueltype NIGHTMARE (:82) - fuels nightmare-fed items (amulets, Night Light), not normal fires
- Sources (auto-scan of every quoted `"nightmarefuel"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `componentutil.lua`: OnResidueActivated_Fuel_Internal@619
  - `cooking.lua`: ?@150
  - `prefabs/abigail_flower.lua`: fn@289
  - `prefabs/altar_prototyper.lua`: ancient_altar@57(0.50)
  - `prefabs/bat_boss.lua`: bat_boss_shadow@60-61(1.00,0.33); shadow_LootSetupFn@677(1)
  - `prefabs/bishop.lua`: bishop_nightmare@51(0.6)
  - `prefabs/bunnyman.lua`: ?@42
  - `prefabs/charlie_boss.lua`: charlie_boss@35-37(1.00,1.00,0.75)
  - `prefabs/charlie_boss_runner.lua`: charlie_boss_runner@14(0.5)
  - `prefabs/daywalker.lua`: daywalker@31(0.5)
  - `prefabs/deciduoustrees.lua`: chop_down_tree@580
  - `prefabs/eyeofterror.lua`: twinofterror1@558-561(1.00,1.00,0.50,0.50); twinofterror2@628-631(1.00,1.00,0.50,0.50)
  - `prefabs/flower_evil.lua`: fn@58
  - `prefabs/gelblob.lua`: gelblob@16-21(1.0,1.0,1.0,1.0,0.8,0.6)
  - `prefabs/itemmimic_revealed.lua`: ?@16
  - `prefabs/knight.lua`: knight_nightmare@68(0.6)
  - `prefabs/monkey.lua`: ?@24; monkey@30(0.5)
  - `prefabs/nightmarecreature.lua`: nightmare_creature@48-49(1.0,0.5); OnDeath@66; ruinsnightmare@125-128(1.00,1.00,0.50,0.25); ruinsnightmare_rifts@136-137(1.00,0.67)
  - `prefabs/oceanshadowcreature.lua`: ocean_shadow_creature@36-37(1.0,0.5); OnDeath@264
  - `prefabs/rabbit.lua`: ?@46; SetBeardlingLoot@101(1)
  - `prefabs/resurrectionstone.lua`: fn@381
  - `prefabs/rocky_boss.lua`: rocky_boss_shadow@53-54(1.00,0.67)
  - `prefabs/rook.lua`: rook_nightmare@46(0.6)
  - `prefabs/rubble.lua`: common_fn@72(0.01)
  - `prefabs/sacred_chest.lua`: ?@17-22
  - `prefabs/shadow_leech.lua`: ?@13
  - `prefabs/shadowchesspieces.lua`: lootsetfn@52-57; shadow_chesspiece@72-73(1.0,0.5)
  - `prefabs/shadowcreature.lua`: shadow_creature@149-150(1.0,0.5); OnDeath@179
  - `prefabs/shadowthrall_centipede.lua`: shadowthrall_centipede_head@32-36(1.00,1.00,0.50,0.25,0.25)
  - `prefabs/shadowthrall_hands.lua`: shadowthrall_hands@25-27(1.00,1.00,0.67)
  - `prefabs/shadowthrall_horns.lua`: shadowthrall_horns@25-27(1.00,1.00,0.67)
  - `prefabs/shadowthrall_mouth.lua`: shadowthrall_mouth@24-26(1.00,1.00,0.67)
  - `prefabs/shadowthrall_parasite.lua`: shadowthrall_parasite@22(0.67)
  - `prefabs/shadowthrall_wings.lua`: shadowthrall_wings@26-27(1.00,0.67)
  - `prefabs/shadowwaxwell.lua`: OnAttacked@70
  - `prefabs/shrouden.lua`: shrouden@41-43(1.00,1.00,0.75)
  - `prefabs/smashables.lua`: ?@290(0.01)
  - `prefabs/staff_common.lua`: fns.orange@555
  - `prefabs/stalker.lua`: AtriumLootFn@1019-1024(1,1,1,1,.5,.5); CaveLootFn@1564-1567(1,1,.5,.5); forest_master_postinit@1606(.5)
  - `prefabs/statueruins.lua`: statue_ruins_no_gem@56(1.00)
  - `prefabs/stickheads.lua`: OnFinish@35
  - `prefabs/worm_boss.lua`: worm_boss_shadow@74-76(1.00,0.33,0.33)
  - `prefabs/wx78_shadowdrone_debuffer.lua`: TryToDropRecipeLoot@131
  - `prefabs/wx78_shadowdrone_harvester.lua`: TryToDropRecipeLoot@65

### Marble - `marble` (`prefabs/inv_marble.lua:48`)
- Stack: 40 (inv_marble.lua:32)
- Perishable: none (no perishable component in file)
- Edible: none
- Fuel: none
- Sources (auto-scan of every quoted `"marble"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/chesspieces.lua`: ?@78
  - `prefabs/daywalker_pillar.lua`: daywalker_pillar@22(0.5); OnWorked@480
  - `prefabs/marblepillar.lua`: marble_pillar@14-16(1.00,1.00,0.33)
  - `prefabs/marbleshrub.lua`: ?@30-53
  - `prefabs/marbletree.lua`: marble_tree@16-17(1.0,0.5)
  - `prefabs/resurrectionstone.lua`: fn@381
  - `prefabs/sculptingtable.lua`: ?@17
  - `prefabs/sculptures.lua`: sculptures_loot@3-4(1.0,0.5)
  - `prefabs/slingshotammo.lua`: ?@1379
  - `prefabs/staff_common.lua`: fns.orange@575
  - `prefabs/statue_marble.lua`: statue_marble@44-46(1.0,1.0,0.3)
  - `prefabs/statueglommer.lua`: statueglommer@16-18(1.00,1.00,1.00)
  - `prefabs/statueharp.lua`: statue_harp@17-19(1.0,1.0,0.3)
  - `prefabs/statueharp_hedgespawner.lua`: statue_harp_hedgespawner@39-41(1.0,1.0,0.3)
  - `prefabs/statuemaxwell.lua`: statue_maxwell@19-21(1.00,1.00,0.33)

### Moon Rock - `moonrocknugget` (`prefabs/moonrocknugget.lua:54`)
- Stack: 40 (moonrocknugget.lua:35)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (moonrocknugget.lua:30); hunger 1 (:31); health 10 / sanity 0 edible.lua defaults
- Fuel: none
- Sources (curated, verified in code):
  - Moon boulder `rock_moon`: table rocks.lua:111-119 (rocks 1.0, flint 1.0, moonrocknugget 1.0 x2 + 0.6 + 0.3) ; `rock_moon_shell` (rocks.lua:121-129): moonrocknugget 1.0 x3 + 0.3
- Sources (auto-scan of every quoted `"moonrocknugget"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/alterguardian_phase1.lua`: alterguardian_phase1@40-46(1.00,1.00,1.00,1.00,1.00,0.66,0.66); alterguardian_phase1_lunarrift@51-55(1.00,1.00,1.00,0.66,0.66)
  - `prefabs/alterguardian_phase2.lua`: alterguardian_phase2@27-30(1.00,1.00,0.66,0.66)
  - `prefabs/alterguardian_phase3.lua`: alterguardian_phase3@62-67(1.00,1.00,1.00,1.00,0.66,0.66)
  - `prefabs/alterguardian_phase3dead.lua`: alterguardian_phase3dead@40-47(1.00,1.00,1.00,1.00,0.66,0.66,0.66,0.66)
  - `prefabs/archive_props.lua`: archive_statues@63-64(1.00,0.05)
  - `prefabs/gargoyles.lua`: gargoyles_loot@3-4(1,.5); brokenhound_loot@10(.5); brokenwerepig_loot@17(.5)
  - `prefabs/moonbase.lua`: ToggleMoonCharge@203
  - `prefabs/moonspiderden.lua`: moon_spider_hole@30-31(1.00,0.50)
  - `prefabs/rocks.lua`: rock_moon@115-118(1.00,1.00,0.6,0.3); rock_moon_shell@125-128(1.00,1.00,1.00,0.3)
  - `prefabs/shadowmeteor.lua`: SetSize@243-271
  - `prefabs/tree_rock_data.lua`: MOON_AREA@64; HERMIT_AREA@73; VINE_LOOT_DATA@249
  - `prefabs/vault_pillar_guard.lua`: vault_pillar_guard@73-75(1,1,0.5)
  - `prefabs/walls.lua`: ?@398

### Red Gem - `redgem` (`prefabs/gem.lua:92 (prefab id `redgem`, built by buildgem("red") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"redgem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `emoji_items.lua`: ?@504
  - `prefabs/balatro_machine.lua`: REWARDS@49(1)
  - `prefabs/cave_vents.lua`: cave_vent_rock@59(0.05)
  - `prefabs/chessjunk.lua`: chess_junk@36(0.25)
  - `prefabs/deer.lua`: SCRAPBOOK_OVERRIDEDATA_NORMAL@429
  - `prefabs/dragonfly.lua`: dragonfly@62-69(1.00,1.00)
  - `prefabs/hotspring.lua`: OnGlassedSpringMineFinished@190
  - `prefabs/hound.lua`: hound_fire@120(0.2)
  - `prefabs/houndmound.lua`: hound_mound@26(0.01)
  - `prefabs/sacred_chest.lua`: ?@20
  - `prefabs/staff_common.lua`: fns.orange@567
  - `prefabs/stalagmite.lua`: full_rock@32(0.05)
  - `prefabs/stalagmite_tall.lua`: stalagmite_tall_full_rock@27(0.05)
  - `prefabs/tree_rock_data.lua`: DEFAULT@4; ROCKYLAND_AREA@138; RUINS_AREA@151; RUINS_EXTRA_AREA@164; MOON_GROTTO_AREA@182; VENT_AREA_SHADOW_RIFT@195; VINE_LOOT_DATA@236
  - `prefabs/tumbleweed.lua`: MakeLoot@159(0.02)
  - `prefabs/wanderingtrader.lua`: RANDOM_RARES@35

### Blue Gem - `bluegem` (`prefabs/gem.lua:92 (prefab id `bluegem`, built by buildgem("blue") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"bluegem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/balatro_machine.lua`: REWARDS@49(1)
  - `prefabs/cave_vents.lua`: cave_vent_rock@58(0.05)
  - `prefabs/deer.lua`: SCRAPBOOK_OVERRIDEDATA_NORMAL@428
  - `prefabs/dragonfly.lua`: dragonfly@63-70(1.00,1.00)
  - `prefabs/hotspring.lua`: OnGlassedSpringMineFinished@190
  - `prefabs/hound.lua`: hound_cold@128(0.2)
  - `prefabs/houndmound.lua`: hound_mound@27(0.01)
  - `prefabs/malbatross.lua`: malbatross@406-408(1,1,0.3)
  - `prefabs/staff_common.lua`: fns.orange@567
  - `prefabs/stalagmite.lua`: full_rock@31(0.05)
  - `prefabs/tree_rock_data.lua`: DEFAULT@5; ROCKYLAND_AREA@139; RUINS_AREA@152; RUINS_EXTRA_AREA@165; MOON_GROTTO_AREA@183; VENT_AREA_SHADOW_RIFT@196; VINE_LOOT_DATA@237
  - `prefabs/tumbleweed.lua`: MakeLoot@158(0.04)
  - `prefabs/wanderingtrader.lua`: RANDOM_RARES@38

### Purple Gem - `purplegem` (`prefabs/gem.lua:92 (prefab id `purplegem`, built by buildgem("purple") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"purplegem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/balatro_machine.lua`: REWARDS@49(1)
  - `prefabs/bishop.lua`: bishop@45(1.0); bishop_nightmare@50(1.0)
  - `prefabs/chessjunk.lua`: chess_junk@39(0.05)
  - `prefabs/dragonfly.lua`: dragonfly@64-71(1.00,0.50)
  - `prefabs/sacred_chest.lua`: ?@21-22
  - `prefabs/staff_common.lua`: teleport_func@445; fns.orange@567
  - `prefabs/telebase_gemsocket.lua`: OnLoad@79
  - `prefabs/tree_rock_data.lua`: DEFAULT@6; ROCKYLAND_AREA@140; RUINS_AREA@153; RUINS_EXTRA_AREA@166; MOON_GROTTO_AREA@184; VENT_AREA_SHADOW_RIFT@197; VINE_LOOT_DATA@238
  - `prefabs/tumbleweed.lua`: MakeLoot@157(0.01)

### Green Gem - `greengem` (`prefabs/gem.lua:92 (prefab id `greengem`, built by buildgem("green") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"greengem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/chessjunk.lua`: chess_junk@37(0.05)
  - `prefabs/dragonfly.lua`: dragonfly@67-74(1.00,0.50)
  - `prefabs/eyeofterror.lua`: twinofterror2@619(1.00)
  - `prefabs/minotaur.lua`: minotaur@71
  - `prefabs/rubble.lua`: common_fn@69(0.01)
  - `prefabs/smashables.lua`: ?@287(0.01)
  - `prefabs/staff_common.lua`: fns.orange@567
  - `prefabs/tree_rock_data.lua`: DEFAULT@9; ROCKYLAND_AREA@143; RUINS_AREA@156; RUINS_EXTRA_AREA@169; MOON_GROTTO_AREA@187; VENT_AREA_SHADOW_RIFT@200; VINE_LOOT_DATA@241
  - `prefabs/tumbleweed.lua`: MakeLoot@162(0.02)

### Orange Gem - `orangegem` (`prefabs/gem.lua:92 (prefab id `orangegem`, built by buildgem("orange") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"orangegem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/chessjunk.lua`: chess_junk@40(0.05)
  - `prefabs/dragonfly.lua`: dragonfly@65-72(1.00,0.50)
  - `prefabs/minotaur.lua`: minotaur@70
  - `prefabs/rubble.lua`: common_fn@71(0.01)
  - `prefabs/smashables.lua`: ?@289(0.01)
  - `prefabs/staff_common.lua`: fns.orange@567
  - `prefabs/tree_rock_data.lua`: DEFAULT@8; ROCKYLAND_AREA@142; RUINS_AREA@155; RUINS_EXTRA_AREA@168; MOON_GROTTO_AREA@186; VENT_AREA_SHADOW_RIFT@199; VINE_LOOT_DATA@240
  - `prefabs/tumbleweed.lua`: MakeLoot@160(0.02)

### Yellow Gem - `yellowgem` (`prefabs/gem.lua:92 (prefab id `yellowgem`, built by buildgem("yellow") gem.lua:92-104)`)
- Stack: 40 (gem.lua:79)
- Perishable: none (no perishable component in file)
- Edible: ELEMENTAL (gem.lua:68); hunger 5 (:70); health 10 / sanity 0 edible.lua defaults
- Repairer material GEM (gem.lua:75-76); Fuel: none
- Sources (auto-scan of every quoted `"yellowgem"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/chessjunk.lua`: chess_junk@38(0.05)
  - `prefabs/dragonfly.lua`: dragonfly@66-73(1.00,0.50)
  - `prefabs/eyeofterror.lua`: twinofterror1@549(1.00)
  - `prefabs/malbatross.lua`: malbatross@409(0.05)
  - `prefabs/minotaur.lua`: minotaur@69
  - `prefabs/rubble.lua`: common_fn@70(0.01)
  - `prefabs/smashables.lua`: ?@288(0.01)
  - `prefabs/staff_common.lua`: fns.orange@567
  - `prefabs/tree_rock_data.lua`: DEFAULT@7; ROCKYLAND_AREA@141; RUINS_AREA@154; RUINS_EXTRA_AREA@167; MOON_GROTTO_AREA@185; VENT_AREA_SHADOW_RIFT@198; VINE_LOOT_DATA@239
  - `prefabs/tumbleweed.lua`: MakeLoot@161(0.01)

### Torch - `torch` (`prefabs/torch.lua:530`)
- Not stackable (no stackable component in torch.lua)
- Perishable: none (no perishable component in file)
- Weapon: damage 17 (TORCH_DAMAGE, torch.lua:458); sets target on fire (TORCH_ATTACK_IGNITE_PERCENT=1)
- Fueled (burn time): 75 s (TORCH_FUEL, torch.lua:509), removed when empty (:510); rain rate 1.5; light radius per level TORCH_RADIUS {2,3,4,5}
- Waterproofer 0.2 (WATERPROOFNESS_SMALL, torch.lua:492)
- Sources (auto-scan of every quoted `"torch"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `emoji_items.lua`: ?@669
  - `prefabs/charlie_boss_trial.lua`: TryToFindSpawnPointForHand@292
  - `prefabs/lighterfire_common.lua`: fn@69-70
  - `prefabs/nightstick.lua`: onequip@36; onunequip@60; onequiptomodel@69
  - `prefabs/skilltree_wilson.lua`: ?@5; wilson_torch_1@139-140; wilson_torch_2@152-153; wilson_torch_3@164-165; wilson_torch_4@173-174; wilson_torch_5@187-188; wilson_torch_6@199-200; wilson_torch_lock_1@207-208; wilson_torch_7@223-224
  - `prefabs/skinprefabs.lua`: ?@12622-12787
  - `prefabs/torchfire_common.lua`: fn@78-79

### Axe - `axe` (`prefabs/axe.lua`)
- Not stackable
- Perishable: none (no perishable component in file)
- Tool: CHOP efficiency 1 (axe.lua:108); uses 100 (AXE_USES, axe.lua:45,113-114), 1 use per chop (:116); breaks when empty (:115)
- Weapon: damage 27.2 (AXE_DAMAGE, axe.lua:120)
- Sources (auto-scan of every quoted `"axe"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/possessedaxe.lua`: OnChangeOwner@68
  - `debugkeys.lua`: group_items@599
  - `prefabs/axe.lua`: MakeAxe@39-40; fumarole_master_postinit@291
  - `prefabs/charlie_stage.lua`: REWARDPOOL@158
  - `prefabs/hound.lua`: OnHedgeKilled@883
  - `prefabs/skinprefabs.lua`: ?@1126-1180
  - `prefabskin.lua`: ?@882-886

### Pickaxe - `pickaxe` (`prefabs/pickaxe.lua:244`)
- Not stackable
- Perishable: none (no perishable component in file)
- Tool: MINE efficiency 1 (pickaxe.lua:102); uses 33 (PICKAXE_USES, :39,106-107), 1 use per mine (:109)
- Weapon: damage 27.2 (PICK_DAMAGE, pickaxe.lua:99)
- Sources (auto-scan of every quoted `"pickaxe"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/charlie_stage.lua`: REWARDPOOL@155
  - `prefabs/hound.lua`: OnHedgeKilled@880
  - `prefabs/pickaxe.lua`: MakePickaxe@33-34; fumarole_master_postinit@244
  - `prefabs/skinprefabs.lua`: ?@9152-9217
  - `prefabskin.lua`: ?@929-933

### Shovel - `shovel` (`prefabs/shovel.lua`)
- Not stackable
- Perishable: none (no perishable component in file)
- Tool: DIG (shovel.lua:95); uses 25 (SHOVEL_USES, :39,100-101), 1 use per dig (:103)
- Weapon: damage 17 (SHOVEL_DAMAGE, shovel.lua:108)
- Sources (auto-scan of every quoted `"shovel"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `components/piratespawner.lua`: additem@602
  - `emoji_items.lua`: ?@609
  - `prefabs/charlie_stage.lua`: REWARDPOOL@158
  - `prefabs/hound.lua`: OnHedgeKilled@883
  - `prefabs/shovel.lua`: MakeShovel@33-34; fumarole_master_postinit@249
  - `prefabs/skinprefabs.lua`: ?@11013-11077
  - `prefabskin.lua`: ?@973-977

### Spear - `spear` (`prefabs/spear.lua:79`)
- Not stackable
- Perishable: none (no perishable component in file)
- Weapon: damage 34 (SPEAR_DAMAGE, spear.lua:56); uses 150 (SPEAR_USES, :61-62), removed when empty (:64)
- Sources (auto-scan of every quoted `"spear"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/charlie_stage.lua`: REWARDPOOL@159
  - `prefabs/hound.lua`: OnHedgeKilled@884
  - `prefabs/skilltree_wathgrithr.lua`: wathgrithr_arsenal_spear_1@217; wathgrithr_arsenal_spear_2@227; wathgrithr_arsenal_spear_3@235; wathgrithr_arsenal_spear_4@248; wathgrithr_arsenal_spear_5@254
  - `prefabs/skinprefabs.lua`: ?@11427-11533

### Log Suit - `armorwood` (`prefabs/armor_wood.lua:83`)
- Not stackable
- Perishable: none (no perishable component in file)
- Armor: durability 315 (ARMORWOOD, armor_wood.lua:69); absorption 0.8 = 80% (ARMORWOOD_ABSORPTION, :69); extra damage vs beaver (:70, BEAVER_WOOD_DAMAGE=17)
- Fuel: 180 (LARGE_FUEL, armor_wood.lua:63); equipslot BODY
- Sources (auto-scan of every quoted `"armorwood"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/skinprefabs.lua`: ?@1036-1084

### Football Helmet - `footballhat` (`prefabs/hats.lua:7867 (MakeHat("football"); fn hats.lua:372)`)
- Not stackable (no stackable component in football fn hats.lua:372-396)
- Perishable: none (no perishable component in file)
- Armor: durability 315 (ARMOR_FOOTBALLHAT, hats.lua:380); absorption 0.8 (ARMOR_FOOTBALLHAT_ABSORPTION, :380); equipslot HEAD (hats.lua:227 default simple())
- Fuel: none found
- Sources (auto-scan of every quoted `"footballhat"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/skinprefabs.lua`: ?@5834-6455

### Campfire (structure) - `campfire` (`prefabs/campfire.lua:231`)
- Not an item. Fueled structure: max fuel 270 (CAMPFIRE_FUEL_MAX, campfire.lua:177), starts with 135 (CAMPFIRE_FUEL_START, :185), 4 sections (:180)
- Burn rate while raining = 1 + 2.5 * rain factor (CAMPFIRE_RAIN_RATE, campfire.lua:34); accepts fuel (:178); cooker component (:174)

### Fire Pit (structure) - `firepit` (`prefabs/firepit.lua:266`)
- Not an item. Fueled structure: max fuel 360 (FIREPIT_FUEL_MAX, firepit.lua:214), starts with 180 (FIREPIT_FUEL_START, :222), 4 sections (:217)
- bonusmult 2 (FIREPIT_BONUS_MULT, firepit.lua:218): fueled.lua:210 multiplies each added fuel's fuelvalue by bonusmult, so fuel lasts twice as long as in a campfire
- Rain rate 2 (FIREPIT_RAIN_RATE, firepit.lua:42); cooker (:211); has lootdropper (:203)

### Telltale Heart - `reviver` (`prefabs/reviver.lua:89`)
- Not stackable (no stackable component in reviver.lua)
- Perishable: none (no perishable component in file)
- Edible: none; Fuel: none
- Tag `reviver` (reviver.lua:62). A ghost player accepts only reviver items (player_common.lua:333-336); giving it to a ghost triggers revival (OnGetItem player_common.lua:341-352, event `usereviver`); health penalty applied unless tag `noreviverhealthpenalty` (:346)
- Craftable by anyone: recipes.lua:420 (3 cutgrass + 1 spidergland + 40 health, TECH.NONE, no builder tag)
- Sources (auto-scan of every quoted `"reviver"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/skinprefabs.lua`: ?@10566-10666

### Life Giving Amulet - `amulet` (`prefabs/amulet.lua:625 (function red(), amulet.lua:425)`)
- Not stackable
- Perishable: none (no perishable component in file)
- Equipslot BODY (amulet.lua:410), dapperness DAPPERNESS_SMALL = 0.0333/s (amulet.lua:411)
- finiteuses: 20 (REDAMULET_USES, amulet.lua:444-445), removed at 0 (:443)
- While equipped, every 30 s (REDAMULET_CONVERSION_TIME, :29) if hurt and hunger > 5: +5 health, -5 hunger, 1 use (REDAMULET_CONVERSION=5, amulet.lua:12-17)
- Resurrection: when a player dies wearing it in the BODY slot it is consumed and revives them (stategraphs/SGwilson.lua:15232-15236); the comment at amulet.lua:434 notes the amulet drops on death so it must be haunted/picked up

### Healing Salve - `healingsalve` (`prefabs/healingsalve.lua`)
- Stack: 40 (healingsalve.lua:54)
- Perishable: none (no perishable component in file)
- Healer: restores 20 health (HEALING_MED, healingsalve.lua:57)
- Edible: none; Fuel: none
- Sources (auto-scan of every quoted `"healingsalve"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `prefabs/healingsalve.lua`: fumarolebuff_OnDetached@268

### Honey - `honey` (`prefabs/honey.lua:57`)
- Stack: 40 (honey.lua:39)
- Perishable: 19200 s = 40 days (PERISH_SUPERSLOW, honey.lua:46)
- Edible: foodtype GENERIC (edible.lua default; not set in honey.lua); health 3 (HEALING_SMALL, honey.lua:35); hunger 9.375 (CALORIES_TINY, :36); sanity 0
- Fuel: none
- Sources (auto-scan of every quoted `"honey"` in prefabs/components excluding recipes/strings/prefab-dependency lists; `ctx@line(chances)`; ctx = SetSharedLootTable name / enclosing function or table; caveat: a hit may be a trade, a spawn or a consumer rather than a drop - the ctx names the owner):
  - `cooking.lua`: IsModCookingProduct@97
  - `prefabs/bee.lua`: commonfn@195(1)
  - `prefabs/beebox.lua`: fn@281(6)
  - `prefabs/beehive.lua`: fn@205
  - `prefabs/beequeen.lua`: beequeen@35-38(1.00,1.00,1.00,0.50)
  - `prefabs/beequeenhive.lua`: OnWorked@207
  - `prefabs/hermitcrab.lua`: addhoneyrewards@866
  - `prefabs/slingshotammo.lua`: ?@1395
  - `prefabs/wasphive.lua`: fn@150
  - `preparedfoods.lua`: ?@45-900(3,2,2,2,1,1)
