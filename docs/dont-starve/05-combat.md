# 05 — Combat

Core: `components/combat.lua`. Damage is a plain number plus a **damage-type string**
(`"fire"`, `"electric"`, `"darkness"`, default = physical). There are no levels or
crits; balance comes from damage, period, and aggro.

## Player baseline

| Stat | Value | Source |
|---|---|---|
| Fist damage | 34 (`wilson_attack`) | `tuning.lua:36` |
| Attack period | 0.4 s (`wilson_attack_period`) | `tuning.lua:45` |
| Spear | 34 dmg, 150 uses | `tuning.lua:355-356` |
| Dark Sword | high dmg, sanity drain on equip | `prefabs/darksword.lua` |

Weapons override via `weapon:SetDamage()` / `combat:SetAttackPeriod(TUNING.X)`
(`combat.lua:120`); default mob attack period is **4 s** (`combat.lua:69`).

## Damage pipeline (`Combat:CalcDamage`, combat.lua:871)

```
final = base(weapon or defaultdamage)
      × damagemultiplier (entity, or mount's)
      × externaldamagemultipliers
      × damagetypebonus (entity + weapon, vs target tags)
      × call-multiplier
      × playerdamagepercent   (NPCs vs players only)
      × pvp_damagemod         (player vs player)
```

- Electric: `ELECTRIC_DAMAGE_MULT + ELECTRIC_WET_DAMAGE_MULT · wetness`
  (`combat.lua:1198-1210`).
- `alwaysblock` tag → damage 0.
- **No backstab / sleeping multiplier exists in current DST code** — do not port one
  from old wiki pages.

## Armor (`components/armor.lua`)

Absorption % with durability pool; some convert damage to sanity
(`ARMOR_SANITY_DMG_AS_SANITY = 0.10`). Verified examples (`tuning.lua:1832-1865`):

| Armor | Durability | Absorption |
|---|---|---|
| Football Helmet (`footballhat`) | 150·3·0.7 = **315** | **80 %** |
| Night Armor (`armor_sanity`) | 150·5·0.7 = **525** | **95 %**, 10 % of blocked damage drains sanity |
| Carved Wood Hat | 150·2.5·0.7 = 262.5 | 70 % |

(`multiplayer_armor_durability_modifier = 0.7`, `tuning.lua:32`.)

## Aggro & targeting

- Mobs acquire targets via brains (`scripts/brains/`) + behaviours
  (`AttackWall`, `ChaseAndAttack`, `RunAway`); retaliation is explicit per mob
  (`combat:GetAttacked` sets `lastattacker`, brains read it).
- Tags gate targeting: `"monster"`, `"player"`, `"veggie"`, `"prey"`…
- `combat:SetPlayerStunlock()` controls attack-stun locking (`combat.lua:83,157`) —
  players can be stun-locked unless a mob opts out.

## Hound waves

`components/houndspawner.lua` — attacks scale with days survived; wave size/count grow,
special waves (red/ice hounds) mixed in. This is the game's pacing whip; our islands
should reuse the same "days-survived pressure curve".

## Bosses

Boss aggression is event/season-driven (Deerclops winter, Bearger autumn, Dragonfly
summer, Bee Queen spring in default worlds — see `map/rooms/forest/giants.lua` and
`worldsettings_overrides.lua` difficulty overrides). Stats table: [09](09-reference-entities.md).
