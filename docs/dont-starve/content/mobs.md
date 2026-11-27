# Mobs (forest surface + early game)

Ground truth from shipped DST Lua. Citations are `file.lua:line` relative to `scripts/`; TUNING values are resolved. Hound-wave rules and spider/hound aggression summary: see [mechanics-cheatsheet.md](mechanics-cheatsheet.md).

## Part 1

**How to read.** All paths are relative to `scripts/`. Numbers are resolved TUNING values (`tuning.lua` via `/tmp/dstwork/tuning_resolved.txt`). "Range" = combat attack range / hit range; default is 3 / 3 (`components/combat.lua:29-30`) unless `SetRange` is cited. Default attack period when none is set is 4 s (`components/combat.lua:69`, `min_attack_period`). Default walk speed is 4 (`components/locomotor.lua:300`). Unit of speed/distance = DST world units (1 tile = 4 units); time in seconds (1 day = 480 s, 1 segment = 30 s: `tuning.lua` TOTAL_DAY_TIME / SEG_TIME).

**Loot semantics** (`components/lootdropper.lua:186-240`, `GenerateLoot`): `SetLoot` = guaranteed list. `SetChanceLootTable` / `AddChanceLoot` entries `{item, chance}` are each rolled independently (`chance >= 1.0` always drops, otherwise `math.random() <= chance`, lines 202-228); a repeated item is a separate entry/roll. `AddRandomLoot(item, weight)` + `numrandomloot = N` = N weighted picks (the pick happens with probability `chancerandomloot or 1`, line 193). Chances can be altered by the "luck" system (`GetChance`, line 173); ignore for the port.

---

### hound — prefab `hound` (prefabs/hound.lua)
- Display name: "Hound" (strings.lua:1613). Health 150 (hound.lua:550), damage 20, attack period 2 (hound.lua:556-557). Range default 3/3 (no SetRange).
- Speed: run 10 (hound.lua:522, TUNING.HOUND_SPEED), walk 4 (locomotor default), swim/ocean run 3.5 (hound.lua:304).
- Aggression: retarget every 3 s (hound.lua:558), target any attackable thing within 20 (`HOUND_TARGET_DIST`, hound.lua:198) except tags `wall, houndmound, hound, houndfriend` (hound.lua:174); no day/night restriction. Brain: wild hound without a home chases with radius 100, hound with a home leashes at 20 (`ChaseAndAttack(inst,100)` / `(10,20)`, brains/houndbrain.lua:222-224). Sleeps only if it is a player's pet hound (`pet_hound` tag, night, near home; hound.lua:160-166); wild hounds never sleep.
- Loot (chance table `hound`, hound.lua:110-114): monstermeat 100%, houndstooth 12.5%.
- Spawner: `houndmound` prefab (houndmound.lua) is a worldgen structure (map/forest_map.lua:203): health 300 (houndmound.lua:172), 2-3 hounds (HOUNDMOUND_HOUNDS_MIN/MAX), release every 30 s, regen 180 s (houndmound.lua:178-180); its own loot: 3 houndstooth, 2 boneshard (100%), redgem 1%, bluegem 1% (houndmound.lua:19-27). Guard spawn picks icehound in winter/spring, firehound otherwise, with the "rare child" roll `SEASON_CHANCE`, else plain hound (houndmound.lua:41-48). Hound waves: components/hounded.lua (covered elsewhere); special-hound chance by day: `HOUND_SPECIAL_CHANCE` = chance 0 (day<15), 0.1 (>=15), 0.2 (>=30), 0.333 (>=50), 0.5 (>=75) (tuning.lua, resolved).

### firehound — prefab `firehound` (hound.lua:655-665)
- "Fire Hound" (strings.lua:1615). Health 100, damage 30, period 2, run 10 (hound.lua:658-661). Same targeting/brain as hound; amphibious (hound.lua:652).
- Loot (`hound_fire`, hound.lua:116-121): monstermeat 100%, houndstooth 100%, redgem 20%.
- On death: spawns 3 `houndfire` objects at its position via lootdropper (hound.lua:634-646; fire patches that ignite).

### icehound — prefab `icehound` (hound.lua:700-710)
- "Ice Hound" (strings.lua:2079). Health 100, damage 30, period 2, run 10 (hound.lua:702-705). Same targeting as hound.
- Loot (`hound_cold`, hound.lua:123-129): monstermeat 100%, houndstooth 100% twice (two entries = 2 teeth), bluegem 20%.
- On death: shatter FX and `AddColdness(2)` to every freezable within radius 4 (hound.lua:668-690).

### spider — prefab `spider` (prefabs/spider.lua; also spider_warrior/hider/spitter/dropper/moon/healer/water in same file)
- "Spider" (strings.lua:1706). Health 100, damage 20, period 3 (spider.lua:765-768); range default; walk 3, run 5 (spider.lua:771-772).
- Aggression: retarget every 1 s (spider.lua:769); targets players or any non-`monster` character within 4 (6 when "investigating" a noise) (`SPIDER_TARGET_DIST` / `SPIDER_INVESTIGATETARGET_DIST`, spider.lua:227, 238-248); ignores `spiderwhisperer` (Webber), `spiderdisguise` (spider hat) (spider.lua:224). Calls its den's on-hit to release more spiders when hit (`SummonFriends`, radius 12, spider.lua:285-300).
- Sleep: sleeps when `TheWorld.state.iscaveday` is true and it has no target/home/leader/fire (spider.lua:252-263); wakes when it is not cave-day or has a target. Note: on the surface too `iscaveday` tracks `phase == "day"` (OnPhaseChanged calls OnCavePhaseChanged, components/worldstate.lua:71-79), so homeless spiders sleep by day; den spiders have a home so they stay awake (BasicWakeCheck).
- Loot (all spider variants share it, spider.lua:646-650): 1 weighted pick (weights monstermeat 1, silk 0.5, spidergland 0.5 -> 50% / 25% / 25%).
- Spawner: `spiderden` (below); worldgen `spiders` (map/forest_map.lua:169), customize `cave_spiders` in caves (map/customize.lua:292-293).
- Notables: can be fed monstermeat to become a follower (max 2 per gift `SPIDER_FOLLOWER_COUNT`, spider.lua:146); wears hats; `halloweenmoonmutable` -> spider_moon.

### spider_warrior — prefab `spider_warrior` (spider.lua:787-806)
- "Spider Warrior" (strings.lua:1708). Health 400, damage 20, period 4 + random(0..2), range 6 / hit 3 (a leaping attack), walk 4, run 5 (spider.lua:792-800).
- Retarget every 2 s within 10 (`SPIDER_WARRIOR_TARGET_DIST`); wake radius 6 even in the sleep window (spider.lua:231, 261). Same loot as spider. Summoned by dens as larger spiders and as emergency defenders.

### other spiders (same file, brief)
- `spider_hider` "Cave Spider" (strings.lua:1331): health 225, damage 20, period 3, walk 3, run 5 (spider.lua:822-829); shell absorb 0.75 (TUNING.SPIDER_HIDER_SHELL_ABSORB; use not read).
- `spider_spitter` "Spitter" (strings.lua:1332): health 350, melee 20, ranged 20 (spit weapon, range 5+4, spider.lua:419-420), period 5 + random(0..2), range 5 / hit 3, walk 4, run 5 (spider.lua:856-864).
- `spider_dropper` "Dangling Depth Dweller" (strings.lua:2207): warrior stats (health 400, damage 20, range 6/3, spider.lua:885-895); drops from cave ceilings.
- `spider_healer` (spider.lua:970-985): health 400, damage 10, heals other spiders within 8 for 150 (`SPIDER_HEALING_*`). `spider_moon` "Shattered Spider": health 250, damage 25 (spider.lua:945-948). `spider_water`: health 200, damage 20 (spider.lua:1059-1078).
- Spider den (`spiderden`, prefabs/spiderden.lua, tuning SPIDERDEN_*): 3 stages, health 250/500/1000, max spiders 3/6/9 (child type spider, warriors from stage 2), emergency warriors 0/4/8 within radius 10/15/20, regen 90 s, release 5 s, creep radius 5/9/9, growth 2400/2400/6000 s; stage 3 can become a spiderqueen (`AttemptMakeQueen`, spiderden.lua:263; stage `queen` growth multiplier `SPIDERDEN_GROW_TIME_QUEEN * 2`, :572; `SPIDERDEN_QUEEN_RANGE_CHECK` 60, `SPIDERDEN_QUEEN_CAP` 4; the exact cap logic after line 285 was not read). Den loot tables `LOOT_DATA` (spiderden.lua:181-213, not itemized here).

### spiderqueen — prefab `spiderqueen` (prefabs/spiderqueen.lua)
- "Spider Queen" (strings.lua:1876). Health 2500, damage 80, period 3, range 5 (spiderqueen.lua:165-173); walk 1.75 (:191), run not set.
- Aggression: retarget every 3 s, only while awake; targets non-monster characters or players within 10 (spiderqueen.lua:36-52), ignoring `spiderwhisperer`/`spiderdisguise`. Shares attacker with other spiders within 30, up to 2 (spiderqueen.lua:62-70). Sleeper resistance 4, wakes to light (`watchlight`, :143-145). Sanity aura -`SANITYAURA_HUGE` (6.67) except for spiderwhisperer (0) (:59).
- Loot (guaranteed list, spiderqueen.lua:22-33): monstermeat x4, silk x4, spidereggsack x1, spiderhat x1.
- Mechanic: births babies through `incrementalproducer` every 10 s (`SPIDERQUEEN_GIVEBIRTHPERIOD`, :206); cap = `RoundBiasedDown((numPlayersWithin20 * 20)^(1/1.4))`, increment `RoundBiasedUp(n*0.5)` (:102-112). Baby = spider; if she has a target, 45% (`SPIDERQUEEN_SPAWN_BETTER_SPIDER_CHANCE`) it is instead warrior (50%) or healer (50%) (:77-84). Babies become her followers (leader cap `SPIDERQUEEN_FOLLOWERS` 16).
- Spawner: grown from a large spider den (see above) via `SpawnQueen` (spiderden.lua:229).

### pig — prefab id `pigman` (prefabs/pigman.lua; also `pigguard`, `moonpig`; prefab list at pigman.lua:1047-1049)
- Display name "Pig Man" (strings.lua:1678); guard variant "Guard Pig" (:1459). Health 250, damage 33, period 3, walk 3, run 5 (pigman.lua:347-352, 362; range default 3/3).
- Aggression: passive toward players. Normal-pig retarget every 3 s (pigman.lua:363) picks only things tagged `monster`, `wonkey`, `pirate` (or `merm` if the pig is not a merm) within 16 (`PIG_TARGET_DIST`) that are standing in light, excluding playerghost/limbo (pigman.lua:236-266); keep target while it is valid and still in light and not transforming (:268-273). Retaliates when attacked (shares target with other pigs within 30, up to a max, pigman.lua:194-216). Sleeps via `DefaultSleepTest` (night); followers only sleep near a campfire in light (pigman.lua:275-281).
- Loot (pigman.lua:357-360): 1 weighted pick, meat weight 3 / pigskin weight 1 -> 75% meat, 25% pigskin.
- Guard pig (pigman.lua:462-488): health 600, damage 33, period 1.5; never sleeps; defends the pig king, then its house, within 20 (`PIG_GUARD_DEFEND_DIST`), targets invaders within 8; same loot.
- Followers: accept meat/veggies for loyalty (`PIG_LOYALTY_MAXTIME` 1200 s, +19.2 s per hunger, pigman.lua:80-127); veggies make poop (:136-140).
- Spawner: `pighouse` (worldgen `pigs`, map/forest_map.lua:173) uses a `spawner` component `Configure("pigman", PIGHOUSE_SPAWN_TIME)` = 1920 s respawn (pighouse.lua:474; tuning `PIGHOUSE_SPAWN_TIME`).

### werepig — transformation of `pigman` (pigman.lua:557-590; component components/werebeast.lua)
- Not a separate prefab: the same entity gets `werepig` tag, brain `werepigbrain`, stategraph `SGwerepig`, build `werepig_build` (`SetWerePig`, pigman.lua:557-562). A `werebeast` component is added to normal pigs with `triggerlimit` 4 (pigman.lua:852-854). Stats: health 525, damage 40, period 2, walk 3, run 7 (pigman.lua:566-577; tuning WEREPIG_*); never sleeps (pigman.lua:546-552); sanity aura -`SANITYAURA_LARGE` (pigman.lua:57).
- Transformation rules (werebeast.lua): (1) Full moon: on `isfullmoon` becoming true each non-limbo pig transforms after a random ~1 s (±2) delay and stays in were-state for `max(weretime=120 s, rest of that day + ~1 s)`; it reverts when the full moon ends (werebeast.lua:1-48). (2) Eating monster meat: any `FOODTYPE.MEAT` food with health value < 0 adds 1 trigger; at 4 triggers it turns for 120 s (SEG_TIME*4) (pigman.lua:136-146, werebeast.lua:100-106, :113-128). (3) Haunt: ghost haunt with `HAUNT_CHANCE_OCCASIONAL` forces were-state until the day ends (pigman.lua:735-741).
- Aggression: retarget every 3 s within 16 (`PIG_TARGET_DIST`), any `_combat` target except `werepig`, `alwaysblock`, `wereplayer` and things mid-transform (pigman.lua:497-510); keep target the same way (:512-516). Shares targets with other werepigs within 30 (:211-212, :222-228). Guard pigs go back to guard state afterwards (`SetOnNormalFn`, pigman.lua:942, 965).
- Loot (guaranteed, pigman.lua:555, 574-575): meat x2, pigskin x1 (`numrandomloot = 0`).

### merm — prefab `merm` (prefabs/merm.lua; guards `mermguard`, plus shadow/lunar/mummy variants)
- "Merm" (strings.lua:2092). Health 500, damage 30; with a Merm King alive anywhere: health 560, damage 40 (`UpdateDamageAndHealth`, merm.lua:481-512; TUNING MERM_HEALTH/DAMAGE/*_KINGBONUS). Period 3, walk 3, run 8 (merm.lua:1473-1485); range default 3/3.
- Merm guard: health 200 / damage 20 without king ("puny"), 660 / 50 with king; period 3, walk 3, run 8 (merm.lua:488-507, 1307-1318); never sleeps (merm.lua:590-596).
- Aggression: retarget every 1 s (merm.lua:1481) within `MERM_TARGET_DIST` 10 of its home (if home within 30; guards 15 targeting dist / 40 defend dist) or itself: any `character` that is not a merm, but only while no Merm King exists in the world and it is not the follower of a player (merm.lua:115-146). Keeps target only while target and itself stay within 30 of home (merm.lua:149-163). Shares target with up to 5 merms within 40 (guards 8 within 60, merm.lua:166-228). Sleeping: night sleep test (`NocturnalSleepTest`) when leaderless and not a king candidate (merm.lua:598-606).
- Loot (merm.lua:70-78, 1489): guaranteed pondfish + froglegs (guard list identical, merm.lua:1325). Lunar merm variant adds `tentaclespots` 25% (merm.lua:86-89, 1727).
- Spawner: `mermhouse` (worldgen `merm`, map/forest_map.lua:196): max 3 children, release 10 s, regen 1920 s, emergency 3 within 15 (TUNING MERMHOUSE_*, mermhouse.lua:273-336); spawning only when it is not winter (mermhouse.lua:123). `mermhouse_crafted` (player-built) max 1, regen half period (mermhouse.lua:405-416).
- Notables: players with Wurt can hire merms (loyalty 1440 s, 2 per hire, radius 8: merm.lua:311-326); Merm King (`mermkingmanager`) turns all merms friendly to non-monsters and buffs stats.

### tentacle — prefab `tentacle` (prefabs/tentacle.lua)
- "Tentacle" (strings.lua:1749). Health 500, damage 34, period 2, range 4 (tentacle.lua:109-114). Immobile: no locomotor component (tentacle.lua:80-97).
- Aggression: retarget every 2±0.5 s within 4 (`TENTACLE_ATTACK_DIST`) for anything with health/combat that is visible, not another tentacle, not `prey`, and is a `character`/`monster`/`animal` or already targeting it (tentacle.lua:23-34); drops the target when it is further than 6 (`TENTACLE_STOPATTACK_DIST`, :36-43); switches to a new attacker only if its current target has not hit it within 2 s (:48-72). Sanity aura -`SANITYAURA_MED` (:121).
- Loot (chance table `tentacle`, tentacle.lua:15-21): monstermeat 100% x2, tentaclespike 50%, tentaclespots 20%.
- Spawner: worldgen only, marsh rooms (`tentacles` -> `tentacle`, map/forest_map.lua:171; weights in map/rooms/forest/terrain_marsh.lua:22, 46, 66, 94). No respawn component in the prefab. (Big-tentacle pillar arms are in tuning TENTACLE_PILLAR_*, not covered.)

### beefalo — prefab `beefalo` (prefabs/beefalo.lua; also `beefalo_carry`, `babybeefalo`)
- "Beefalo" (strings.lua:1638). Health 1000 (beefalo.lua:1163), damage 34 (:1158; domesticated 50 ornery / 20 pudgy / 25 rider, :597), period 4 (default), range 3/3. Walk 1.0 (`BEEFALO_WALK_SPEED`, :1222), run 7 (:1223; domesticated tendency 7 / 6.5 / 8).
- Aggression: passive normally. Retarget every 1 s (:1159) only while its herd is **in mood** (rut): target = any `_combat` thing within 5 (`BEEFALO_TARGET_DIST`) except beefalo, walls, and players riding beefalo (:313-330). Keep target unless herd is in mood and the target is further than 30 from the herd (`BEEFALO_CHASE_DIST`, :332-335). Also fights back (shares with nearby beefalo within 30, up to 5: :343, 367).
- Mood: `beefaloherd` prefab has a `mood` component: 3 days in mood, 20 days wait, active in spring (`BEEFALO_MATING_SEASON_LENGTH` 3 / `_WAIT` 20, `SetMoodSeason(SPRING)`, beefaloherd.lua:125-130). Herd radius 40, max 16 in range (`BEEFALOHERD_RANGE/MAX_IN_RANGE`); herd spawns `babybeefalo` periodically (beefaloherd.lua:135). Worldgen: map/forest_map.lua:176.
- Loot (chance table, beefalo.lua:49-58): meat x4 (100% each), beefalowool x3 (100% each), horn 33%.
- Other: poop periodically (:1188); regen 6.94 hp / 10 s (`BEEFALO_HEALTH_REGEN`); wool regrows `BEEFALO_HAIR_GROWTH_DAYS` 3; domestication/obedience/rider mechanics (BEEFALO_DOMESTICATION_*, tendency table beefalo.lua:565-610); buck time 50-800 s (`BEEFALO_MIN/MAX_BUCK_TIME`); sleeps/wakes by default sleeper rules.

### koalefant — prefabs `koalefant_summer`, `koalefant_winter` (prefabs/koalefant.lua)
- "Koalefant" for both (strings.lua:2047-2048). Health 1000, damage 50 (koalefant.lua:112, 117), period 4 default, range 3/3, walk 1.5, run 7 (:141-142).
- Aggression: no retarget function = never starts a fight. When attacked it targets the attacker and shares with other koalefants within 30, max 5 (koalefant.lua:57-66). Keeps the target while within 30 (`KOALEFANT_CHASE_DIST`, :54-56). Sleep: `DefaultSleepTest` and no player within 14; wakes if any player comes within 10 (:41-51).
- Loot (guaranteed, koalefant.lua:22-24): meat x8 + trunk_summer (summer variant) or trunk_winter (winter variant); if burning/burnt: meat x8 + `trunk` (converted to cooked trunk) (:68-72).
- Spawner: no worldgen spawn and no den: created only by the hunting system (`components/hunter.lua:389-390`: koalefant_winter in winter, otherwise koalefant_summer; a hunt can instead yield warg/lightninggoat/other beasts).
- Notables: poops every 40-60 s (`SetRandomTimes(40,60)`, :126).

### rabbit — prefab `rabbit` (prefabs/rabbit.lua)
- "Rabbit" (strings.lua:1684). Health 25 (:404), run 5 (:375), walk 4 default; no combat (prey animal).
- Behaviour (brains/rabbitbrain.lua): runs away from players / hunters at 5 (stops at 10; hard-avoid at 3), eats nearby bait within 20, wanders within 20 of home. On attack, up to 5 rabbits within 30 go home (rabbit.lua:296-311). Winter: turns white `BecomeWinterRabbit` on `iswinter` (rabbit.lua:206-240). Sleeps by default sleeper with `watchlight` (:423-424).
- Loot (normal): smallmeat guaranteed (rabbit.lua:45, 90-94). Cooks into cookedsmallmeat (:275).
- Spawner: `rabbithole` (worldgen `rabbits`, map/forest_map.lua:174) with `spawner:Configure("rabbit", RABBIT_RESPAWN_TIME)` = 1200 s (rabbithole.lua:265); `RABBITHOLE_REGROWTH_TIME` 2400 s for the hole itself.

### beardling — same prefab `rabbit` (separate display only)
- There is **no separate prefab**. The "beardling" is the same `rabbit` entity with a different look/loot:
  1. **Insanity (per observer)**: a player in insanity mode whose sanity <= `BEARDLING_SANITY` 0.4 of max (0.3 for `dappereffects` characters, `DAPPER_BEARDLING_SANITY`) sees the rabbit with the `beard_monster` build/inventory icon (client-side override, rabbit.lua:81-84, 359-364); rabbit's sanity aura becomes -`SANITYAURA_MED` (0.67) for that observer (rabbit.lua:271-273); cooks into cookedmonstermeat (:275-277).
  2. **Forced nightmare (global)**: event `ms_forcenightmarestate` (used by components/corruption.lua, monkey, bunnyman, shadow_trap) sets timer `forcenightmare`, switches the build to `beard_monster` for that duration and then reverts (rabbit.lua:170-205, 436-437).
- Loot (decided at death by `LootSetupFunction`, rabbit.lua:283-292):
  - Killer (or killer's leader) is "crazy" (sanity <= 0.4, rule 1): 1 weighted pick, beardhair weight 0.5, monstermeat 1, nightmarefuel 1 -> beardhair 20%, monstermeat 40%, nightmarefuel 40% (rabbit.lua:96-104).
  - Forced nightmare rabbit (rule 2): nightmarefuel guaranteed; with 50% (luck roll) one extra weighted pick of beardhair 0.5 / monstermeat 1 -> beardhair 1/3 of those (about 16.7% overall) (rabbit.lua:106-115).
  - Otherwise smallmeat.

### crow, robin, robin_winter, canary — prefabs from `makebird(...)` (prefabs/birds.lua:191-441; also puffin, quagmire_pigeon)
- Names: "Crow" (strings.lua:1647), "Redbird" `robin` (:1648), "Snowbird" `robin_winter` (:1649), "Canary" (:1645; poisoned variant "Canary (Volatile)" :1646). `robin_winter` uses the "junco" sound/build (birds.lua:438).
- All identical stats: health 25 (`BIRD_HEALTH`, birds.lua:355); flight-based movement (no ground locomotion); flees when a threat comes within 5 (`BIRD_SEE_THREAT_DISTANCE`; 6 for water birds) (birds.lua:361-363); does not fight. Sleeps by `DefaultSleepTest` when not flying (birds.lua:13-15).
- Loot (birds.lua:319-327): 1 weighted pick, `feather_<name>` weight 1 (canary 0.1) vs smallmeat weight 1 -> crow/robin/robin_winter 50% feather / 50% smallmeat; canary 9% feather_canary / 91% smallmeat.
- Spawner: world-level `components/birdspawner.lua`: tile table crow = rocky/dirt/marsh/forest/savanna, robin = grass/forest/savanna (birdspawner.lua:17-30); in winter robin spawns as robin_winter (:249). Max `BIRD_SPAWN_MAX` 4 birds near a player, delay 5-15 s (`BIRD_SPAWN_DELAY`), rain slows spawning to 0.25x (`BIRD_RAIN_FACTOR`). Canary: no world spawner found in map/ or birdspawner; only birdcage/toadstoolspawner hooks (birds.lua:228, 401).
- Notables: birds drop items/seeds while they land when a player is near (`SpawnPrefabChooser`: young players get flint/twigs/cutgrass or seeds, birds.lua:50-102; `BIRD_DROP_ITEM_BASE_CHANCE` 0.35 minus 0.1 per day-age year, seeds chance 0.8 / 0.6 afterwards); can be trapped or caged; `BIRD_PERISH_TIME` 2400 s as pets.

### frog — prefab `frog` (prefabs/frog.lua; `lunarfrog` variant in same file)
- "Frog" (strings.lua:1816). Health 100, damage 10, period 1, range default 3/3, walk 4, run 8 (frog.lua:157-158, 205-208).
- Aggression: retarget every 3 s within 4 (`FROG_TARGET_DIST`): any living thing with an inventory (players, pigs, etc.), excluding `merm` (frog.lua:61-77). Shares target with other frogs within 30, up to 5 (frog.lua:85-91). Sleeps at night only when it has no home; with a home it never sleeps (frog.lua:79-85).
- Loot (guaranteed, frog.lua:180): froglegs x1.
- Lunar frog (frog.lua:228-252): health 150, damage 10 + 5 planar damage, period 1, target dist 4, steals items, ignores merms and `lunar_aligned`, 50% chance of an additional froglegs (`LUNARFROG_*`).
- Spawner: `pond` prefab spawns up to `FROG_POND_CHILDREN` 3-4 frogs during the day only (`dayspawn = true`), not in winter; release 75 s, regen 150 s (pond.lua:346-366, 287-292); frogs also fall during rain (`FROG_RAIN_*`: 16% chance, up to 300 frogs; rain-frog code not read).

## Part 2

Paths are relative to `scripts/`. Resolved TUNING values are in brackets. "Default range" = attackrange 3 / hitrange 3 (components/combat.lua:29-30). `SetRange(a)` sets hit range = a (components/combat.lua:148-151). Locomotor defaults are walk 4 / run 6 (components/locomotor.lua:300-301) where a prefab does not override them.

### Mosquito — `mosquito` (prefabs/mosquito.lua)
- Name: Mosquito (strings.lua:1838). Tags: insect, flying, smallcreature (mosquito.lua:163-168).
- Health 100 (TUNING.MOSQUITO_HEALTH), damage 3 (mosquito.lua:232,237), attack period 7, range 1.75 (hit 1.75) (mosquito.lua:238-239).
- Speed: walk 8, run 12 (mosquito.lua:192-193).
- Aggro: retarget every 2 s (mosquito.lua:240), radius 20 (SpringCombatMod(20), mosquito.lua:100). Targets anything with `_combat`+`_health` and tag character/animal/monster, excluding insects. Ignores anyone carrying mosquito musk and allies (mosquito.lua:93-110). Shares target to allies within 30, max 10 (mosquito.lua:134-140, consts at :29-30).
- Brain: chase max 8 dist / 10 s; leash to home 20, wander 6 (brains/mosquitobrain.lua:18-24); goes home when ShouldGoHome. Runs away from "player" 4..8 when not chasing (mosquitobrain.lua:20-21).
- Blood drinking: each drink adds a belly size; `maxdrinks` 4 (mosquito.lua:244); on drink 5 it bursts (`toofat`, kills itself, burst damage 34, burst range 3.5: TUNING.MOSQUITO_BURST_DAMAGE/RANGE; used by the explode state, not re-verified in SG).
- Loot: `mosquitosack` 0.5 (chance table, mosquito.lua:25-28).
- Spawner: ponds (prefabs/pond.lua:313-328): childname "mosquito", 3-4 children (`math.random(min,max)` of {min=3,max=4}), spawn period 75 s, regen 150 s.
- Sleeps (watchlight=true) (mosquito.lua:260-261).

### Bee / Killer Bee — `bee`, `killerbee` (prefabs/bee.lua)
- Names: Bee / Killer Bee (strings.lua:1834,1836). Tags insect, flying, smallcreature (bee.lua:153-158); killer variant adds "killer","scarytoprey" (bee.lua:290), worker adds "worker","pollinator" (bee.lua:264).
- Both: health 100, damage 10, attack period 2, attack range 0.6 (bee.lua:212,274-276,303-305). Speed: no override in bee.lua (locomotor default walk 4 / run 6; SGbee has no speed override: not found). Extra +10 damage vs targets tagged `allergictobees` (bee.lua:56-57, TUNING.BEE_ALLERGY_EXTRADAMAGE).
- Worker bee: retargets only in spring, radius 4 (bee.lua:126-132; retarget every 2 s, :277). Brain: pollinates flowers, goes home at night/when full of pollen (brains/beebrain.lua:70-75), chase max-time/dist wrapped in SpringCombatMod (beebrain.lua:64).
- Killer bee: retarget radius 8 (SpringCombatMod(8)) every 2 s (bee.lua:115-124,306), anything with `_combat`/`_health` and tag character/animal/monster, not insects/plantkin. Brain chase max 25 dist / 10 s (brains/killerbeebrain.lua:9-10).
- Loot (both): 1 weighted random pick: honey weight 1, stinger weight 5 (bee.lua:195-198) -> stinger 5/6, honey 1/6.
- Hive relationship: `beehive` (prefabs/beehive.lua) childname "bee", 5 bees (BEEHIVE_BEES), regen 30 s, release period 50 s (beehive.lua:102-108; spring x1.33 more bees/faster), emergency spawn 8 bees within radius 20 (beehive.lua:192-194). Hitting the hive releases ALL children as killerbees (beehive.lua:88-91). Hive loot: honey x3 + honeycomb (beehive.lua:205). Bee boxes: 4 bees, honey 300 s (TUNING.BEEBOX_*).

### Butterfly — `butterfly` (prefabs/butterfly.lua)
- Name: Butterfly (strings.lua:1843). Passive, flying.
- Health 1 (butterfly.lua:141). Has a combat component but no attack stats (butterfly.lua:144-145); damage: not found / none.
- Speed: no override found (locomotor default).
- Behaviour: panics; RunAway from threats; at night or when full of pollen goes home; otherwise finds/pollinates a flower and wanders around the nearest flower (brains/butterflybrain.lua:49-57).
- Loot: 1 weighted pick: butter weight 0.1, butterflywings weight 5 (butterfly.lua:160-161) -> wings ~98%, butter ~2%.
- Spawner: components/butterflyspawner.lua - runs only when day and not winter (:81), per player every `10 + rand*10` s (:68; first spawn 0.3 s + rand*10), cap `TUNING.MAX_BUTTERFLIES`=4 within 64 of the player (:22,:45-46); spawns on a random flower between 6 and 25 from the player (`mindistance=36` is squared, rad=25, :33-48).
- Perish time as pet 1440 (TUNING.BUTTERFLY_PERISH_TIME, butterfly.lua:190).

### Tallbird — `tallbird` (prefabs/tallbird.lua)
- Name: Tallbird (strings.lua:1848). Territorial, nest-bound.
- Health 800, damage 50, attack period 2, range 3 (hit 3) (tallbird.lua:205-215). Walk speed 7 (tallbird.lua:196); run speed: not found.
- Retarget (every 3 s, tallbird.lua:213): (1) near its nest, anything within 12 (TALLBIRD_DEFEND_DIST) of the nest tagged character/animal/monster; (2) any pigman within 8 (TALLBIRD_TARGET_DIST), not werepig; (3) any character/monster within 8; never other tallbirds; smallbirds only if companion (tallbird.lua:25-58).
- Keep target: stays within 32 of home, or 256 if the target is the egg thief (and the nest egg was taken) (tallbird.lua:16-17,60-72). Brain chase time 20 (brains/tallbirdbrain.lua:6,69). Aggro timeout 2 s before switching to a new attacker (tallbird.lua:108).
- Sleep: sleeps at night when it has no target; resistance 3 (tallbird.lua:74-84,232-237).
- Loot: SetLoot guaranteed 2x meat (tallbird.lua:15,201).
- Nest (`tallbirdnest`, prefabs/tallbirdnest.lua): childspawner childname "tallbird", max 1, regen 5*16*SEG_TIME = 2400 s (tallbirdnest.lua:175-181); lays an egg after 1440 + rand*960 s (:46; TALLBIRD_LAY_EGG_TIME_MIN/VAR). Picking the egg sets the picker as thief and all tallbirds outside target them (:52-62). Spring: a smallbird spawns at the nest after 960-3840 s (nest:132; MIN/MAX_SPRING_SMALL_BIRD_SPAWN_TIME). A homeless tallbird can build a new nest on rocky/dirt turf if no other tallbird is within 2 (tallbird.lua:125-143).

### Smallbird / Teenbird — `smallbird`, `teenbird` (prefabs/smallbird.lua)
- Names: Smallbird / Smallish Tallbird (strings.lua:1849-1850). Hatched from `tallbirdegg` (hatch time 1440, TUNING.SMALLBIRD_HATCH_TIME), a pet that follows a leader and needs feeding (hunger component, smallbird.lua:326).
- Smallbird: health 50, damage 10, period 1, range 3, walk 6 (smallbird.lua:407-416,335). Hunger max 20, starves in 480 s, then takes starvation damage over 120 s (smallbird.lua:409-411). No retarget fn (smallbird.lua:417 commented out). Loot: smallmeat (smallbird.lua:420).
- Grows to teen after 4800 s (SMALLBIRD_GROW_TIME, smallbird.lua:258), to full tallbird after 8640 s (TEENBIRD_GROW_TIME, :262).
- Teenbird: health 600, damage 37.5 / period 2 (peck mode: damage 2 / period 4), range 3, hunger max 60, starve 480 s + 240 s kill (smallbird.lua:473-481, tuning TEENBIRD_*). Retarget radius 8: monsters (or tallbirds if leaderless) that are NOT in light; a starving teen may attack its leader (smallbird.lua:172-186). Keep-target requires the target in light (:188-190). Loot: meat (smallbird.lua:488).

### Krampus — `krampus` (prefabs/krampus.lua)
- Name: Krampus (strings.lua:1621). Tags monster, hostile, scarytoprey (krampus.lua:65-68).
- Health 300, damage 50, attack period 1.2, range default 3/3 (krampus.lua:104-109). Run speed 7 (krampus.lua:89).
- Behaviour (brains/krampusbrain.lua:83-95): ChaseAndAttack(100) whenever it has a combat target; leaves (exit state) once it carries `greed` items (greed = 2 + rand(4)); every >=10 s steals a loose item within 30 (not heavy/irreplaceable/containers/birds) or hammers an empty chest within 30; otherwise runs away from players 8..15 and follows/wanders around the player it was spawned for (`spawnedforplayer`). No retarget function in krampus.lua.
- Loot (chance table, krampus.lua:18-24): monstermeat 1.0, charcoal 1.0, charcoal 1.0, krampus_sack 0.01.
- Spawner `kramped` (components/kramped.lua; added to the forest world at prefabs/forest.lua:649):
  - Listens to the player's "killed" event; naughtiness values from `NAUGHTY_VALUE` (constants.lua:2271+): pigman 3 (0 for werepig/franken), babybeefalo 6, smallbird 6, catcoon 5, beefalo 4, deer 4, dustmoth 4, bunnyman 3 (0 if vampire), teenbird 2, robin 2, robin_winter 2, canary 2, tallbird 2, penguin 2, crow 1, butterfly 1, moonbutterfly 1, rabbit 1, mole 1, lightflier 1, glommer 50, friendlyfruitfly 20.
  - Per player: threshold = 30 + random(20) (31..50) (kramped.lua OnNaughtyAction; TUNING.KRAMPUS_THRESHOLD/_VARIANCE). Counter (`actions`) decays by 1 every 60 s without new naughtiness (KRAMPUS_NAUGHTINESS_DECAY_PERIOD; kramped.lua OnUpdate). When actions >= threshold: counter resets to 0, new threshold rolled, krampii spawn.
  - Count: world cycles <= 50: 1; > 50 (KRAMPUS_INCREASE_LVL1): 1 + random(2) = 2..3; > 100 (LVL2): 2 + random(2) = 3..4.
  - Spawn point: 30 from the player (SPAWN_DIST=30), walkable, not near holes.
  - Warning: below threshold, a warning prefab `krampuswarning_lvl1/2/3` spawns at the player 1-3 s later when remaining score < 20 (lvl3 <5, lvl2 <15, lvl1 else).

### MacTusk / Wee MacTusk — `walrus`, `little_walrus` (prefabs/walrus.lua), camp `walrus_camp`
- Names: MacTusk / Wee MacTusk (strings.lua:1851,1855). Tags character, walrus, houndfriend (walrus.lua:139-141). Fires a `blowdart_walrus` projectile from a hidden equipped blowdart (walrus.lua:88-104).
- MacTusk: health 300, damage 33, period 3, range 15 (hit 15), walk 2 / run 4 (walrus.lua:153-154,171-178). Retarget radius 10 every 1 s, excludes hound/walrus tags, needs animal/character/monster (walrus.lua:46-56); keeps target while within 30 (walrus.lua:58-60). When attacked it targets the attacker and shares with walruses within 30 (max 5) (walrus.lua:36-43).
- Wee MacTusk: health 100, damage 22, period 5.1, range 15, walk 3 / run 5; follows MacTusk (walrus.lua:224-235; TUNING.LITTLE_WALRUS_*).
- Brain: goes home at night when leaderless and not fighting (brains/walrusbrain.lua:78-82); if asleep/off-screen at night it despawns back into the camp (walrus.lua:62-82). Stalks players within 40, leash 40/80 (walrusbrain.lua:21-24).
- Loot (walrus.lua:23-29): meat 1.0, blowdart_pipe 1.0, walrushat 0.25, walrus_tusk 0.50. Wee MacTusk: meat 1.0 (walrus.lua:31-34).
- Camp (prefabs/walrus_camp.lua): party spawns only when the camp is occupied and it is winter (:264-269): 1 walrus, 1 little_walrus, `NUM_HOUNDS`=2 icehounds (:18,208-246; ice hounds follow the wee one). Re-spawn trigger when a member's new target is within 10 of the camp (AGGRO_SPAWN_PARTY_RADIUS=10, :19,272-276) or at day start (:287-289). Each member killed starts a 1200 s regen timer (WALRUS_REGEN_PERIOD, :132,199-200). At night/asleep only the hounds are spawned (:281). Outside winter the camp clears itself once no children are awake (:86-98). Also may investigate megaflares with chance 0.6 (WALRUS_INVESTIGATE_MEGAFLARE_CHANCE, :406).

### Gobbler — `perd` (prefabs/perd.lua)
- Name: Gobbler (strings.lua:2004). Tags character, berrythief, perd.
- Health 50, damage 20, attack period 3, range default 3/3 (perd.lua:150-152); walk 3, run 8 (perd.lua:132-133). Mostly flees; no retarget.
- Brain (brains/perdbrain.lua:31-49): goes home at night; eats food (radius 20) when no scary player within 5; RunAway from "scarytoprey" 5..10; picks berries within 20 (search bushes within 40); can seek `perdshrine` within 30 and loiter (30/4-6 consts :10-17).
- Loot: SetLoot 2x drumstick (perd.lua:17-20); plus `redpouch` 0.1 chance if its "offeringcooldown" timer is absent (perd.lua:76-83).
- Spawner: picking a berry bush (berrybush.lua:100-107): if the picker is not tagged `berrythief` and the bush is not `_noperd`, `TryLuckRoll(PERD_SPAWNCHANCE=0.1)` -> 3-6 s later a perd appears next to the bush (berrybush.lua:75-83) with the bush as home.

### Moleworm — `mole` (prefabs/mole.lua)
- Name: Moleworm (strings.lua:2340). Prey animal: tags animal, prey, smallcreature, baitstealer (mole.lua:177-190). No combat.
- Health 30 (mole.lua:221), walk 2.75 (mole.lua:208). Loot: smallmeat (mole.lua:232). Can be captured as pet (perish 960 = 2 days, mole.lua:276).
- Brain (brains/molebrain.lua): digs a new home if none; flees "scarytoprey" 0..6 when `flee`; peeks above ground periodically; steals bait items (tag `molebait`) within 20; goes home at night (below ground) and when inventory full, when acid raining; above ground by day (molebrain.lua:93-114).
- Spawner: `molehill` (prefabs/molehill.lua): spawner component "mole" respawn 1200 s (MOLE_RESPAWN_TIME, :85,122-123). Digging the hill gives rocks (4), nitre (1.5), goldnugget (0.5), flint (1.5) weights (molehill.lua:111-114).

### Catcoon — `catcoon` (prefabs/catcoon.lua)
- Name: Catcoon (strings.lua:2354); den prefab named "Hollow Stump" (strings.lua:12255 context). Tags smallcreature, animal, catcoon, trader.
- Health 150, damage 25, period 2, range 4 (hit 4), walk 3 (catcoon.lua:391-396,438).
- Retarget radius 25 every 3 s (catcoon.lua:218-240): monsters and smallcreatures, airborne cat toys (`cattoyairborne`), and non-allied other catcoons; never its leader/ally ghost. Chase max 4 s / 12 dist; avoids players 3..10 when idle; goes home when raining (brains/catcoonbrain.lua:9-21,138-155).
- Naps: nap interval 30-120 s, nap length 20-40 s (catcoon.lua:249,259,432-433). Hairball gifts every 360-720 s base, 120-480 s when friendly (catcoon.lua:416-417; TUNING.CATCOON_*_HAIRBALL_TIME).
- Befriending: picks up/accepts items, +48 s loyalty per item, max 480 s (TUNING.CATCOON_LOYALTY_PER_ITEM/MAXTIME, catcoon.lua:295,407); pickup chance 0.67.
- Loot (catcoon.lua:72-76): meat 1.0, coontail 0.33.
- Den: `catcoonden` childspawner max 1, regen 120 s, release 15 s (TUNING.CATCOONDEN_*); a destroyed den repairs after 7200 + rand(2400) s outside summer (catcoonden.lua:84-90; TUNING.CATCOONDEN_REPAIR_TIME).

### Buzzard — `buzzard` (prefabs/buzzard.lua), spawner `buzzardspawner`
- Name: Buzzard (strings.lua:2325). Tags buzzard, animal, scarytoprey (buzzard.lua:88-90).
- Health 250, damage 15, period 2, range 2 (buzzard.lua:103-111). Walk 4 / run 8 (buzzard.lua:149-150). Sleep resistance 4 (buzzard.lua:122).
- Aggro: no retarget function; targets whoever attacked it (OnAttacked, buzzard.lua:32-35 area) and keeps the target only within 7.5 (KeepTargetFn, buzzard.lua:26-29). Brain: eats food/corpses within 15, flees threats within 7.5 (brains/buzzardbrain.lua:34-36), goes home (flies off) when no food.
- Loot (buzzard.lua:19-25): drumstick 1.0, smallmeat 0.33, feather_crow 0.33.
- Spawner (prefabs/buzzardspawner.lua, placed on desert in worldgen map/forest_map.lua:152,197): childspawner max `math.random(1,2)` buzzards, spawn period 45 +- 15 s, regen 1440 s (buzzardspawner.lua:343-347; TUNING.BUZZARD_*). Spawning stops in winter, during lunar hail, and at night (children are recalled at night) (buzzardspawner.lua:208-223,226-231). Buzzards arrive from the sky (y=30) toward visible food (not already hunted, 3-radius) found every 2-4 s with a 25% skip (buzzardspawner.lua:90-107,156). Flares can also summon them (:164-171).

### Treeguard — `leif`, `leif_sparse` (prefabs/leif.lua)
- Name: Treeguard (strings.lua:1981-1982). Tags epic, smallepic, monster, hostile, leif, tree, evergreens, largecreature, plantcreature (leif.lua:106-114).
- Base stats: health 3000, damage 150 (player damage x0.33 = 49.5 - LEIF_DAMAGE_PLAYER_PERCENT), attack period 3, range 3/3, walk 1.5 (leif.lua:142,161-172). Scaled by `leifscale`: health, damage, range 3, walk all x scale (leif.lua:21-34). Hit recovery 1.5 (TUNING.LEIF_HIT_RECOVERY).
- Aggro: no retarget fn; brain is AttackWall -> ChaseAndAttack -> Wander (brains/leifbrain.lua:12-19). Spawns targeting the chopper (spawn_leif suggests chopper as target, evergreens.lua:442-456). Sleeps (resistance 3); a tree can wake nearby leifs within 20 when chopped (evergreens.lua:411-415; sleeper hibernate saved, leif.lua:46-53). Sanity aura -0.667 idle / -1.667 fighting per second (leif.lua:79).
- Flammability 0.333; burn time 10 x PLANTMOB_BURNTIME_MULT (leif.lua:154,163).
- Loot: guaranteed 6x livinglog + 1x monstermeat (leif.lua:184). `leif_sparse` (Lumpy tree variant) uses the same stats, different build "leif_lumpy_build" (leif.lua:212-214).
- Spawning from trees (prefabs/evergreens.lua): chopping the LAST work of an evergreen (`GetBuild(inst).leif` set: normal tree -> `leif`, sparse tree -> `leif_sparse`, evergreens.lua:70,84): if the chopper's age >= 3 days (LEIF_MIN_DAY) roll chance 0.01333 (LEIF_PERCENT_CHANCE; x1.5 for tag woodcutter, x0 for beaver) (evergreens.lua:527-541). On success: 1 leif if age <= 30 days, else `random(2)` if <= 80 days, else `random(3)` (evergreens.lua:540). Each picks another standing tree within 15 (LEIF_MAXSPAWNDIST) that is not leif/stump/burnt and transforms it after 1-4 s (evergreens.lua:489-493,541-543). Scale by tree stage: short 0.7, normal 1, tall 1.25 (evergreens.lua:357,364,371). Haunting a tree also can spawn one with chance HAUNT_CHANCE_SUPERRARE (evergreens.lua:801-807). Leif idols (`leif_idol`) spawn 2 within 10 (LEIF_IDOL_NUM_SPAWNS/RADIUS).

### Deerclops — `deerclops` (prefabs/deerclops.lua), spawner components/deerclopsspawner.lua
- Name: Deerclops (strings.lua:2066). Tags epic, monster, hostile, deerclops, scarytoprey, largecreature (deerclops.lua:386-391).
- Health 4000, damage 150 (x0.5 vs players = 75), attack period 4, attack range 8 (hit 8), AOE range 12 (TUNING.DEERCLOPS_AOE_RANGE), walk 3 (deerclops.lua:420,439,505-509). Run speed: not found. Never sleeps (ShouldSleep false, deerclops.lua:152-158). Sanity aura -6.67 fighting / -1.67 idle (deerclops.lua:100).
- Aggro: retarget radius 16 (TARGET_DIST), targets a valid enemy that attacked it or is within `physicsradius + 8` (deerclops.lua:82,118-132). Brain chase 20 s / 32 dist; destroys structures near its target base within 40 (brains/deerclopsbrain.lua:9-12,22-24,146). After destroying 5 structures (STRUCTURES_PER_HARASS) it is "sated" and leaves if it has no target and has lived >= 120 s; it always leaves when it is not winter (deerclops.lua:83-97).
- Loot (deerclops.lua:329-340): meat x8 (each 1.0), deerclops_eyeball 1.0, chesspiece_deerclops_sketch 1.0.
- Spawn rule (components/deerclopsspawner.lua): allowed only when `cycles > NO_BOSS_TIME`=26 and season == "winter" (unless DEERCLOPS_ATTACKS_OFF_SEASON, default false) (:54-60). `TUNING.DEERCLOPS_ATTACKS_PER_SEASON`=4 attacks per winter; attack delay = (winterlength - 1) * TOTAL_DAY_TIME(480) / (attacks + 1) (:298); after one is killed the next delay x6 (HASSLER_KILLED_DELAY_MULT, :19,119). A random eligible player is picked, preferring ones with >= 4 structures within 20 (:70-101). A 60 s warning (`deerclopswarning_lvl*` sounds) precedes it (:33). It spawns at camera-see distance from the target, only one deerclops at a time, remembering the nearest structure within 40 as `targetbase` (:151-195). A stored (retreated) deerclops is reused.

### Ghost (hostile) — `ghost` (prefabs/ghost.lua)
- Name: Ghost (strings.lua:1665). Tags monster, hostile, ghost, flying, noauradamage (ghost.lua:66-70). Emits light (0.6 intensity, radius 0.5).
- Health 200, speed walk = run = 2 (ghost.lua:86-87,98). Damage via aura: 15 every 1.2 s to anything within radius 1.5, player damage x1 (ghost.lua:101-107; TUNING.GHOST_*). Aura skips tags abigail, ghostlyfriend, ghost_ally unless they target it. Sanity aura -0.667/s (ghost.lua:93). Default combat range (no SetRange).
- Aggro/brain (brains/ghostbrain.lua): follows a living `character` within 10 (keeps within 900 sq = 30 distance, TUNING.GHOST_FOLLOW_DSQ) at 0.375..1.5 distance; with no follow target it wanders for 10 s and then dissipates (ghostbrain.lua:56-66). Attacked -> targets attacker (ghost.lua:24-30).
- Loot: no lootdropper in ghost.lua (none).
- Spawner: digging a `mound` (grave) has `ghost_of_a_chance` 0.1 (mound.lua:205) to spawn a ghost instead of loot (mound.lua:57-79; `SpawnPrefab("ghost")`, :59). Gravestones spawn the friendly `smallghost` (5% per day, TUNING.GHOST_GRAVESTONE_CHANCE) instead (gravestone.lua:22-40). Wendy decorated graves spawn `graveguard_ghost` (ghost.lua:308).

### Grue / Charlie - darkness attack (not a prefab) — components/grue.lua
- Added to every player at prefabs/player_common.lua:2812-2813 (sounds `dontstarve/charlie/warn`, `.../attack`). The attacker is called Charlie in announcements: `ANNOUNCE_CHARLIE` "That presence... it's familiar! Hello?", `ANNOUNCE_CHARLIE_ATTACK` "OW! Something bit me!" (speech_wilson.lua:581-582); after the Shrouded Queen/Charlie boss is defeated the `ANNOUNCE_GRUE_NOCHARLIE*` lines are used instead (speech_wilson.lua:1271-1272; components/wisecracker.lua:107-116). Name string `CHARLIE = "Charlie"` (strings.lua:2271).
- Active only while the player is in darkness (not `IsInLight`, no night vision, not invincible, not dead, not sleeping) (grue.lua:101-105,123-126; events enterdark/enterlight/nightvision/invincibletoggle/grueimmunearea, :1-52). Other immunities: lunar plant hat (hats.lua:3825), Winona skill (skilltree_winona.lua:400), vault rooms (virtualroomset).
- Timing: first hit after 5 + rand*5 s of darkness; every subsequent hit after 5 + rand*6 s (grue.lua:129-135 Start; OnUpdate). A warning sound plays at 40-80% of the countdown, then `heargrue` fires `warndelay`=1 s later (grue.lua:190-203).
- Each tick the `level` counter increments; if level > resistance (default 0; Winona has resistance 1, winona.lua:200) it attacks: `GetAttacked(nil, TUNING.GRUEDAMAGE = 100.05, ..., "darkness")` and sanity -20 (SANITY_MEDLARGE) (grue.lua:177-190, 210-219); otherwise `resistedgrue`. Damage bypass: nonlethal option (default false, NONLETHAL_PERCENT 0.2) would cap damage to leave 20% health (grue.lua:150-162).
