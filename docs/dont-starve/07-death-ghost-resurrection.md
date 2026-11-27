# 07 — Death, Ghosts & Resurrection

Sources: `prefabs/player_common.lua`, `prefabs/player_common_extensions.lua`,
`prefabs/resurrectionstatue.lua`, `prefabs/reviver.lua`, `prefabs/ghost.lua`.

## Death flow (DST)

`OnPlayerDeath` (`player_common_extensions.lua:191`):

1. Ghosts cannot die again (early return).
2. If the player has a **revivable corpse** (`revivablecorpse` component — default in
   DST): inventory is **hidden, kept on the corpse** (`inst.components.inventory:Hide()`).
   Otherwise (or after corpse-looting): `inventory:DropEverything()`
   (`player_common.lua:1440`) — items scatter on the ground and **never despawn**.
3. `last_death_position` / `last_death_shardid` recorded; death announced
   (`TheNet:AnnounceDeath`).
4. Player enters **ghost** state (`"playerghost"` tag, `ghostenabled`).

## Ghost

- Flies, passes walls, invisible to most, cannot interact with the world except
  actions flagged `action.ghost_valid` (`player_common_extensions.lua:56-68`).
- Can travel to a Meat Effigy and **resurrect** there, or be revived by another player.
- **No ghost timer in DST** — a lone ghost with no effigy waits forever until someone
  revives them or the server restarts. **[DS1]** single-player: death = game over →
  new world.
- Our mod's rule (all players ghosted → 120 s countdown → world wipe) is **our own
  invention** blending DS1's "new world on death" with DST's ghost window — document it
  as such in-game, don't cite DS for it.

## Resurrection paths (canonical)

| Path | Prefab | Cost / effect |
|---|---|---|
| **Meat Effigy** | `resurrectionstatue` | Recipe: 4 `boards` + 4 `beardhair` + **40 player HP** (`CHARACTER_INGREDIENT.HEALTH`, `recipes.lua:580`, TECH.MAGIC_TWO). The builder is linked for free on build — the recipe already took the HP (`resurrectionstatue.lua:99-106`); anyone else attuning pays 40 HP, never lethal (`resurrectionstatue.lua:61-72`). Ghost touches it → respawns there and the effigy is **consumed** (`ListenForEvent("activateresurrection", inst.Remove)`, `resurrectionstatue.lua:176`). |
| **Telltale Heart** | `reviver` | Recipe: 3 `cutgrass` + 1 `spidergland` + **40 player HP** (`recipes.lua:420`, TECH.NONE). Another player gives it to the ghost (heart consumed, `player_common.lua:354`) → revived **where the ghost is** with current health `RESURRECT_HEALTH` = 50 (`player_common_extensions.lua:727`); `REVIVE_HEALTH_PENALTY = 0.25` (`tuning.lua:2178`) is a **max-health** penalty (`player_common.lua:358`), not the revive health. |
| Touch effigy as ghost | — | effigy must be attuned by a living player first |

## Mapping to our mod (islands run)

| DS concept | Canonical id | Our mod |
|---|---|---|
| death drop scatter | `DropEverything` | grave BE (permanent, matches "never despawn") |
| ghost | `playerghost` tag | ghost state in `ds:islands` only |
| effigy revive | `resurrectionstatue` | block; builder linked free, others pay 5 HP (40 × 2/15); consumed on use (`revive/ResurrectionStatueBlock`) |
| telltale revive | `reviver` | item; revives at 50/150 of max health; the ghost is pulled to the reviver (MC ghosts fly, may hover over void); no max-health penalty yet (`revive/ReviveHandler`) |
| death over the void | — (DS has no void) | ours: grave at the player's last ground, ghost raised to it (`survive/DeathHandler`) |
| wipe on hopeless | — (DS1: game over) | 120 s all-ghost timer → `IslandState.advance()` |
