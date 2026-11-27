# Mechanics cheat-sheet (commonly misremembered facts)

Short Q&A, every answer taken from the shipped DST Lua source. Citations are
`file.lua:line` relative to `scripts/`. Numbers written `TUNING.X = n` were resolved
through `tuning.lua` (constants at the top of `Tune()`; `seg=30 s`, day = 480 s).
Code beats the wiki. See also [README](../README.md).

---

## 1. What are ALL sources of `beardhair`? Do hounds drop it?

**Hounds do NOT drop it. No hound variant does.** Hound loot tables
(`hound.lua:110-142`) contain only `monstermeat`, `houndstooth`, `redgem`, `bluegem`,
`redpouch`. A full grep of `scripts/` for `beardhair` (excluding strings/skins) gives
exactly these sources:

| Source | Amount / chance | Cite |
|---|---|---|
| Wilson's beard (shave with razor/after growth) | `beard.prize = "beardhair"`, one per beard bit | `wilson.lua:154`, `components/beard.lua:146-149` |
| Bunnyman killed by an **insane** killer ("beard lord") | guaranteed `beardhair` x2 + `monstermeat` | `bunnyman.lua:41`, `bunnyman.lua:305-310` |
| Bunnyman forced into nightmare form | `nightmarefuel`, `beardhair` x2, `monstermeat` | `bunnyman.lua:42`, `:305-307` |
| Cozy bunnyman (same beard-lord table) | `beardhair` x2 + `monstermeat` | `cozy_bunnyman.lua:25` |
| **Beardling** (insane-form rabbit, `rabbit` prefab) | 1 random item of {`beardhair` w=.5, `monstermeat` w=1, `nightmarefuel` w=1} -> beardhair = 0.5/2.5 = **20 %** | `rabbit.lua:96-104` |
| Forced-nightmare rabbit | `nightmarefuel` guaranteed + 50 % roll for 1 extra random of {beardhair .5, monstermeat 1} | `rabbit.lua:46,106-117` |
| Nightmare/shadow `monkey` | `SetChanceLootTable("monkey")`: `beardhair` 1.0 (guaranteed) (+ smallmeat, cave_banana 1.0, nightmarefuel 0.5). Normal monkey uses plain `LOOT = {smallmeat, cave_banana}` | `monkey.lua:25-30,273-274,305-312` |
| `rabbitking_aggressive` | guaranteed `monstermeat`, `beardhair` x2, `rabbitkingspear` | `rabbitking.lua:161-166,281` |
| `rabbitkingminion_bunnyman` | random 1 of beardhair 3 / monstermeat 3 / carrot 3 / meat 3 / manrabbit_tail 4 (weights, beardhair = 3/16) | `rabbitking.lua:462-467` |
| Bernie decaying (`bernie_inactive`) | `beardhair` + `beefalowool` + `silk` | `bernie_inactive.lua:28-34` |
| Tumbleweed pick | weight `0.02` in a weighted pool (total weight dominated by many `1`/`40` entries) | `tumbleweed.lua:173,213-215` |
| Skeleton-wizard static layouts (ground items) | `type = "beardhair"` placed 3 (ice) / 5 (fire) times | `map/static_layouts/skeleton_wizard_ice.lua:164-186`, `..._fire.lua:219-263` |
| Crafting | not craftable | `recipes.lua` (no `beardhair` output) |

`beardhair` is **consumed** by the Meat Effigy recipe (`recipes.lua:580`: 4 boards, 4 beardhair, 40 HP).

## 2. What does picking a grass tuft yield? Can grass ever yield flint?

- Pick: `pickable:SetUp("cutgrass", TUNING.GRASS_REGROW_TIME = 1440 s = 3 days)`, `max_cycles = cycles_left = TUNING.GRASS_CYCLES = 20`: one `cutgrass` per pick (`grass.lua:248-254`; spawn of single product `components/pickable.lua:493`). After 20 cycles it becomes barren.
- Dig: workable `ACTIONS.DIG`, work left 1 (`grass.lua:267-270`); drops the product if still pickable and `dug_grass` (or `cutgrass` if withered) (`grass.lua:79-84`).
- **Grass never yields flint.** `grass.lua` and `components/pickable.lua` contain no `flint`. 
- The claim "`tuning.lua:3528` = grass drops join the flint weight pool" is **false**. Line 3528 is `EXTRA_STARTING_ITEMS.autumn = { 5x cutgrass, 5x twigs, 1x flint }` (`tuning.lua:3526-3531`), the starter-kit given to a player who joins a world at day >= `EXTRA_STARTING_ITEMS_MIN_DAYS = 10` (`player_common.lua:1484-1485`). It is a per-player inventory handout, not a loot table. (The only other `flint = 10` is in `KITCOON_HIDEANDSEEK_NOT_YOT_REWARDS`, `tuning.lua:5302`, also unrelated.)
- 1 % (`TUNING.GRASSGEKKO_MORPH_CHANCE`) chance per pick/dig outside winter to make nearby grass morph into a `grassgekko` (`grass.lua:72-77,122-127`).

## 3. What does a hound drop? Hound wave (hounded.lua) rules

Loot (`hound.lua`; chance-table entries are each rolled independently, `>= 1.0` = guaranteed: `components/lootdropper.lua:213-225`):

| Prefab | Loot |
|---|---|
| `hound` | `monstermeat` 100 %, `houndstooth` 12.5 % (`hound.lua:110-114`) |
| `firehound` | `monstermeat` 100 %, `houndstooth` 100 %, `redgem` 20 % (`:116-121`); also spawns 3 `houndfire` on death (`:633-642`) |
| `icehound` | `monstermeat` 100 %, `houndstooth` x2 (two 100 % entries), `bluegem` 20 % (`:123-129`) |
| `mutatedhound` | `monstermeat`, `houndstooth` x2 (`:137-142`) |

Stats: hound 150 HP/20 dmg/2 s/run 10 (`tuning.lua:1490-1494`); fire & ice 100 HP/30 dmg/2 s/run 10 (`:1505-1513`).

Wave (`components/hounded.lua`):
- **Time between waves** depends on the *average age of active players* (`:120-138`); delay = base + random variance (`:185-186`), in days of 480 s:
  age < 8: 5 d + rand 0..3 d (`intro`, `:65`); < 25: 5 d + 0..5 d (`light`, `:66`); < 50: 7 d + 0..5 d (`med`, `:67`); < 100: 9 d + 0..5 d (`heavy`, `:68`); else 11 d + 0..5 d (`crazy`, `:69`).
- **Warning** (`warnduration`, `:54-58`): intro 120 s, light 60 s, med 45 s, heavy 30 s, crazy 30 s before the wave (warning state set at `:1088-1089`, speech `ANNOUNCE_HOUNDS`, `:72`).
- **Wave size per player** by that player's age (`CalcPlayerAttackSize`, `:142-149`): < 10 d: 1; < 25 d: 2 + rand(2) = 3-4; < 50 d: 3 + rand(3) = 4-6; < 100 d: 4 + rand(3) = 5-7; else 6 + rand(4) = 7-10. A player who has been in the game less than the warning time or younger than the delay gets none (`:259-264`). Players within 20 tiles (`GROUP_DIST`, `:207`) are grouped and the group total is raised to the power `1/(0.95 + 0.05*n)` (`:208-209,296-305`) so groups get fewer hounds.
- **Variants**: after day 15 / 30 / 50 / 75 of average age the chance a hound is the seasonal one is 0.1 / 0.2 / 0.333 / 0.5 (`TUNING.HOUND_SPECIAL_CHANCE`, `tuning.lua:1481`; `hounded.lua:395-406`), x1.5 in summer; `icehound` in winter/spring, `firehound` in summer/autumn (`:408-421`).
- **Targeting**: each hound is spawned `SPAWN_DIST = 30` from its assigned player (`:19`, `:427-451`) and given `combat:SuggestTarget(player)` (`:464`). Afterwards normal hound AI: retarget every 3 s within `TUNING.HOUND_TARGET_DIST = 20` of anything targetable, except tags `wall`, `houndmound`, `hound`, `houndfriend` (`hound.lua:174-199,558`); no day/night gating.

## 4. Is the Meat Effigy (resurrectionstatue) consumed after a resurrection? Cost?

- **Yes, consumed**: `inst:ListenForEvent("activateresurrection", inst.Remove)` (`resurrectionstatue.lua:176`); the event is pushed when a player respawns from it (`player_common_extensions.lua:366,375-376`).
- **Costs**: (a) building: recipe `boards` x4 + `beardhair` x4 + **40 HP** (`CHARACTER_INGREDIENT.HEALTH, TUNING.EFFIGY_HEALTH_PENALTY = 40`; TECH.MAGIC_TWO), `recipes.lua:580`; (b) **attuning** (linking a player to an existing effigy) costs 40 HP **at the moment of attunement**, never kills (fails with `NOHEALTH` if current health <= 40): `resurrectionstatue.lua:61-72`, called from `attunable.lua:98-103`. Placing it does not charge twice: the cost fn is nulled while the builder is linked (`resurrectionstatue.lua:99-111`). Dying/respawning charges nothing extra.
- Attuned players can respawn **from anywhere** (`remoteresurrector`, `resurrectionstatue.lua:169`; `ACTIONS.REMOTERESURRECT`, `actions.lua:506`) -> teleported to the effigy (`player_common_extensions.lua:506-535,607-611`), state `rebirth`.
- Health after respawn: no SetPercent on the respawn path; the ghost's health was set to `RESURRECT_HEALTH = 50` when it died (`player_common_extensions.lua:727`, `tuning.lua:2363`), so you come back with **50 HP** (max health unaffected by effigies).
- Hammer 4 work (`:152-153`); burns to `charcoal` (`:20-37`).

## 5. Telltale Heart (`reviver`): where do you end up, with what health?

- Recipe: `cutgrass` x3, `spidergland` x1, **40 HP**, TECH.NONE (`recipes.lua:420`).
- Use: **another living player** gives it to a ghost (`ACTIONS.GIVETOPLAYER` on a `playerghost` target, `componentactions.lua:1403-1406`; accepted only if the ghost is on passable ground `player_common.lua:333-336`). The heart is **consumed** (`item:Remove()`, `player_common.lua:354`).
- Location: the ghost revives **where the ghost is** (`DoActualRez` `Physics:Teleport(x,y,z)` at current position, `player_common_extensions.lua:354,593-594`), state `reviver_rebirth` (`:411`). It does not move to the giver or the death spot.
- Health: current health stays at the ghost value **50** (`RESURRECT_HEALTH`, `player_common_extensions.lua:727`). In addition max health takes a **penalty of 25 %** (`TUNING.REVIVE_HEALTH_PENALTY = 0.25`, `player_common.lua:357-359`, `health.lua:468-471`, capped at `MAXIMUM_HEALTH_PENALTY = 0.75`, `tuning.lua:2170`; `GetMaxWithPenalty = max - max*penalty`, `health.lua:524-526`). So it is **not** "revive at 25 % HP" (docs 07/10 wrongly say so). The giver gains `REVIVE_OTHER_SANITY_BONUS = 80` sanity (`player_common.lua:360`).

## 6. What do spiders drop exactly?

`spider` (and variants built by the same function): `numrandomloot = 1`, weighted pick, `chancerandomloot` default 1 -> **always exactly one item** (`spider.lua:645-650`; `lootdropper.lua:193-198`):
`monstermeat` weight 1, `silk` .5, `spidergland` .5 => **50 % monstermeat, 25 % silk, 25 % spidergland**. Haunted-loot override: `spidergland` 1 (`:649`). (Doc 10's "silk/gland 50/50" and `spider.lua:647-648` is wrong: monstermeat has weight 1.)

## 7. Are spiders / hounds aggressive at night and neutral in the day?

- **Spider targeting has no day/night condition.** `FindTarget` (`spider.lua:208-224`): within `SPIDER_TARGET_DIST = 4` (6 while investigating, `:227`; `spider_warrior` 10, `:231`) any entity with tags `_combat` + `character`, that is a player or a non-`monster` (`guy.isplayer or not guy:HasTag("monster")`); never `spiderwhisperer`/`spiderdisguise` (Webber) or spider allies. Spring multiplier via `SpringCombatMod`.
- What *is* time-gated: homeless spiders sleep during day (`ShouldSleep = iscaveday and not BasicWakeCheck`, `spider.lua:243-262`; `iscaveday` is `phase == "day"` on the surface too, `components/worldstate.lua:63-79`); spiders with a home den never sleep (`BasicWakeCheck` home), go home at day (`brains/spiderbrain.lua:145-146`), and **dens only release spiders when it is not day** (`spiderden.lua:526-545` start/stop spawning on `iscaveday`).
- **Hounds**: no day/night gating at all (see 3); wave hounds are always hostile to whatever they can target; only `pet_hound`s sleep at night (`hound.lua:160-166`).
