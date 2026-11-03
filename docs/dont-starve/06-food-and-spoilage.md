# 06 — Food & Spoilage

Two components per food item (`components/edible.lua`, `components/perishable.lua`).

## Edible

Fields: `hungervalue`, `healthvalue`, `sanityvalue` (defaults 10/10/0,
`edible.lua:21-23`), `foodtype` (+ `secondaryfoodtype`).
Types: `MEAT`, `VEGGIE`, `FRUIT`, `INGREDIENT`, `ELEMENTAL`, `ROUGHAGE`, `WOOD`,
`HORRIBLE`, `MONSTER`, `BURNT`. Monster food (`monstermeat`) carries secondary type
`MONSTER`: sanity-negative, refused by pig king / some pots.

Canonical value ladder (`tuning.lua:2021-2063`), **hunger is in hunger units**
(150 = full bar):

| Constant | Hunger | | Constant | Health | | Constant | Sanity |
|---|---|---|---|---|---|---|---|
| CALORIES_TINY | 9.4 | | HEALING_TINY | 1 | | SANITY_TINY | 5 |
| CALORIES_SMALL | 12.5 | | HEALING_SMALL | 3 | | SANITY_SMALL | 10 |
| CALORIES_MEDSMALL | 18.75 | | HEALING_MEDSMALL | 8 | | SANITY_MED | 15 |
| CALORIES_MED | 25 | | HEALING_LARGE | 40 | | SANITY_LARGE | 33 |
| CALORIES_LARGE | 37.5 | | HEALING_HUGE | 60 | | SANITY_HUGE | 50 |
| CALORIES_HUGE | 75 | | | | | SANITY_MEDLARGE | 20 |

Verified examples (`prefabs/meats.lua`, `veggies.lua`, `preparedfoods.lua`):

| Item | Hunger | Health | Sanity | Perish |
|---|---|---|---|---|
| `meat` | +25 | +1 | −10 | 6 d |
| `smallmeat` ("Morsel") | +12.5 | 0 | −10 | 6 d |
| `monstermeat` | +18.75 | −20 | −15 | 6 d |
| `berries` | +9.4 | 0 | 0 | 6 d |
| `carrot` | +12.5 | +1 | 0 | 10 d |
| `corn` | +25 | +3 | 0 | 10 d |
| `dragonpie` | +75 | +40 | +5 | 15 d |
| `baconeggs` | +75 | +20 | +5 | 20 d |
| `spoiled_food` ("Rot") | −10 | −1 | 0 | — |

## Perishable

- `SetPerishTime(days × 480 s)`; ladder (`tuning.lua:2045-2051`):
  **3 / 6 / 10 / 15 / 20 / 40 days** (`SUPERFAST…SUPERSLOW`).
- On expire → `onperishreplacement` (`"spoiled_food"`, `meats.lua:199`).
- Storage multipliers: fridge (`icebox`) **×0.5** (`PERISH_FRIDGE_MULT`,
  `tuning.lua:2079`), saltbox **×0.25** (`PERISH_SALTBOX_MULT`, `tuning.lua:2081`,
  via `components/preserver.lua`).
- **Cooking produces a different prefab** (`meat` → `meat_cooked`), and halves the
  spoiled fraction (`cookable.lua:32`). There is no generic "+50 % while cooked"
  modifier — each cooked item defines its own values.
- Ground items **never despawn** in DS — no 5-minute timer like Minecraft. Our graves
  can be plain dropped stacks + a marker, or a BE; either matches DS semantics.

## Farming (for later)

`components/grower.lua` + `components/farmland.lua`; crops have seasons, need
fertilizing, produce via `components/productivity.lua`. Crops wither in darkness
(`CROP_DARK_WITHER_TIME = 1.5 days`, `tuning.lua:2277`).
