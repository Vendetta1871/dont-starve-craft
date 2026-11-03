# 01 — Game Architecture

How DST's Lua layer is put together. Knowing this prevents naming mistakes: almost every
"thing" in DS is a **prefab** composed of **components**, and the ids below are the ids
the game itself uses.

## Entity / component / prefab

- **Entities** are C++ objects (`EntityScript` wrappers in `scripts/entities/*.lua`,
  e.g. `lightwatcher.lua`). Lua code almost never touches them directly.
- **Components** (`scripts/components/*.lua`, 835 files) are behavior units attached to
  an entity: `inst:AddComponent("hunger")`, then `inst.components.hunger.max = …`.
  A component is a `Class(function(self, inst) … end)` (see `scripts/class.lua`) with
  lifecycle hooks: `OnLoad`, `OnSave`, `Update(dt)`, `OnRemoveFromEntity`, plus event
  callbacks registered in `OnLoad`.
- **Prefabs** (`scripts/prefabs/*.lua`, 1621 files) are factories registered through the
  global `Prefab()` from `scripts/prefabs.lua` into `scripts/prefablist.lua`:

  ```lua
  local prefab = AddPrefab("pig", fn, Asset("ANIM", "anim/pig.zip"), …)
  --             ^ prefab id   ^ fn(inst) builds the entity:
  --               adds components, tags, sets up master-side logic
  ```

  `fn(inst)` runs on **both** sides; code that mutates state must be guarded:

  ```lua
  if TheWorld.ismastersim then
      inst:DoServerThing()          -- or: local master_postinit = function(inst) … end
  end
  ```

  Prefabs can alias: `pigman.lua` registers `pig`, `pig_guard`, `werepig` — one file,
  several ids. `AddPrefab` may also be called `RegisterPrefab`/via `prefabs.lua` helpers.

## Globals you will see everywhere

| Global | What it is |
|---|---|
| `TheWorld` | The master world entity (`prefabs/world.lua`). Holds `TheWorld.state` (`cycles`, `phase`, `season`, `isday/isevening/isdark`, `temperature`, …) and pushes world events (`TheWorld:PushEvent("ms_pausecombat", …)`). |
| `ThePlayer` | Local player. `ThePlayer.userid`, `:HasTag("player")`. |
| `TheNet` | Server/session facade (`TheNet:AnnounceDeath`, client table). |
| `TUNING` | All balance numbers, built in `scripts/tuning.lua` inside `Tune()`. |
| `STRINGS` | All display text, `scripts/strings.lua`. |
| `TheInventory`/`TheSim` | Client-side helpers, time (`TheSim:GetGameTime()`). |

## Networking model

Master (server) / client split. State crosses the boundary via **net variables**
declared in entity scripts (`scripts/entities/*.lua`) with `net_int`, `net_float`,
`net_bool`, `net_tinybyte`, `net_ushortint`, `net_string8`, … plus dirty-callback events
(`"cyclesdirty"`, `"phasedirty"` in `components/clock.lua`). Components read them
client-side and never write authoritative state there.

## Events

- Entity events: `inst:PushEvent(name, data)` / `inst:ListenForEvent(name, cb, source)`.
- Canonical names in `scripts/events.lua` (`"death"`, `"attacked"`, `"killed"`,
  `"ms_respawnedfromghost"`, `"sanitydelta"`, `"hungerdelta"`, `"startfreezing"`, …).
- World events go through `TheWorld` (`"ms_"` prefix = master-side commands,
  e.g. `ms_unlockchesspiece` pushed by `statuemaxwell.lua:46`).

## AI: brains, behaviours, stategraphs

- **Stategraphs** (`scripts/stategraphs/SG*.lua`) drive animation + action states of
  players and mobs (`SGwilson`, `SGpig`, …).
- **Brains** (`scripts/brains/*.lua`) are per-mob AI programs composed of
  **behaviours** (`scripts/behaviours/*.lua`) — e.g. `AttackWall`, `ChaseAndAttack`,
  `RunAway`. Hostility is emergent from the brain, not a flag on the mob.

## Actions

`scripts/actions.lua` defines every player interaction (`ACTIONS.MINE`, `ACTIONS.PICK`,
`ACTIONS.EAT`, `ACTIONS.ATTACK`, `ACTIONS.CHOP`, …) with `fn`, `string`, `mathode`.
Ghost-usable actions are filtered via `action.ghost_valid`
(`prefabs/player_common_extensions.lua:56`).

## Naming conventions (the important part)

- Prefab ids: `snake_case`, stable forever (savefiles reference them).
- Component names: `snake_case`, used as `inst.components.<name>`.
- Display names: `STRINGS.NAMES.<ID_UPPER>`; descriptions under
  `STRINGS.CHARACTERS.GENERIC.DESCRIBE.<ID_UPPER>`.
- Tags: `inst:AddTag("player")`, `"monster"`, `"fleshy"`, `"structure"`, `"fire"`,
  `"sanityaura"`, `"maxwell"`, `"statue"`, `"moonportal"`… — tags are the cross-system
  contract; check them before assuming hostility or interactivity.
- Events: lowercase, `snake_case`; master commands prefixed `ms_`.

## Where the numbers come from

`tuning.lua` `Tune()` header (lines ~17–45) defines the base units every other constant
is computed from:

```lua
local seg_time = 30                 -- one clock segment, seconds
local total_day_time = seg_time*16  -- 480 s full day
local day_segs, dusk_segs, night_segs = 10, 4, 2   -- default composition
local wilson_attack = 34
local wilson_health, wilson_hunger, wilson_sanity = 150, 150, 200
local calories_per_day = 75         -- hunger burn basis
local wilson_attack_period = 0.4
```

Any `TUNING.X` cited in these docs is resolved through this header; re-resolve after
game updates.
