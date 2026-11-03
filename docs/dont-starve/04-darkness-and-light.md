# 04 — Darkness & Light

Light value is a float 0…1 per entity (`LightWatcher`, native component; thresholds in
Lua). Key thresholds (`tuning.lua:2180-2181`):

| Light value | Meaning |
|---|---|
| > 0.6 (`SANITY_HIGH_LIGHT`) | safe at night: sanity drain −0.083/s |
| 0.1 … 0.6 | dim: same −0.083/s |
| < 0.1 (`SANITY_LOW_LIGHT`) | **dark**: −0.833/s sanity **and** the Grue starts hunting |

## The Grue — darkness damage (`components/grue.lua`)

Internal name for "Charlie". Attached to players; runs whenever the entity is in
darkness (`not IsInLight()`, not invincible, not dead, no night vision,
`grue.lua:CheckForStart`).

- First attack after `5 + rand·5` seconds (`grue.lua:Start`).
- Attack: **100 damage** (`GRUEDAMAGE = wilson_health·0.667`, `tuning.lua:762`),
  damage type `"darkness"`, unblockable (`combat:GetAttacked(nil, damage, nil, "darkness")`,
  `grue.lua:187`) **plus −20 sanity** (`SANITY_MEDLARGE`, `grue.lua:217`).
- Repeats with escalating frequency while in darkness; pushes `"attackedbygrue"`.
- Immunity sources: `"light"`, `"nightvision"`, `"invincible"`, named
  `grueimmunearea` zones (`grue.lua:1-30`).
- World option `NONLETHAL_DARKNESS` (default false; true in some presets,
  `worldsettings_overrides.lua:3589`): damage cannot take you below
  `NONLETHAL_PERCENT = 0.2` of max HP (`grue.lua:173-185`).

## Light sources (canonical prefabs)

| Prefab | Notes |
|---|---|
| `campfire` | 3 cut grass + 2 logs; fueled component, burns out and leaves `ash`; provides light + heat (`radiantheat`); can cook food on it |
| `firepit` | 2 logs + 12 rocks; cut stone ring, longer fuel capacity |
| `torch` | portable, fuel-based, extinguishes in rain (DST) |
| `minerhat` | permanent head light, no fuel (`hats.lua:7865`) |
| `lantern` | portable, refillable with fuel |
| `nightlight` | nightmare-fuel lamp, purple light |
| `fireflies` / `glowberry` | dim sources |

## Rules of thumb for the mod

- Complete darkness is lethal in ~10 s of real damage time — our islands must give a
  day-1 light path (torch recipe or campfire) exactly like DS does.
- Sanity drain in darkness (−0.833/s) kills sanity in ~4 min from full — darkness is
  the sanity engine of the game.
