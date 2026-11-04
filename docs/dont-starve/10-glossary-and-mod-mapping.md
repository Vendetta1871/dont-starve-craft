# 10 — Glossary & Mod Mapping

Canonical DS term → canonical prefab/component id → our mod name. **Rule:** when our
content is a 1:1 port, our registry id copies the DS prefab id verbatim (`snake_case`,
no extra underscores). When it's our own invention, the id must not collide with a DS
id and the docs must say it's ours.

## Terms

| DS term | Canonical id(s) | Our mod |
|---|---|---|
| World / surface level | `forest` level | `dontstarve:islands` dimension, one island per run |
| Run wipe ("new world") | [DS1] game-over → new world | `IslandState.advance()` (index++, new island) |
| Maxwell's Door [DS1] | adventure-mode door, marble frame, `key_to_city` | **Marble П-portal** (`marble_portal`) — flavor source, mechanics original |
| Sacrifice to light portal | — (no `sacrificer` in DST) | ours: mob death within 8 blocks of unlit frame |
| Sky (post-death limbo) [DS1] | — | our planned ghost-hold state, not a dimension |
| Moon Portal | `multiplayer_portal_moonrock` (tag `moonportal`) | **Sky Portal** (escape, one-way) |
| Moon Rock Idol | `moonrockidol` (key: `moonrocknugget` + `purplegem`) | escape key item — keep ids |
| Moon Rock shard | `moonrocknugget` | our `moonrocknugget` |
| Meat Effigy | `resurrectionstatue` | `resurrectionstatue` block (in-run revive, −5 HP attune) |
| Telltale Heart | `reviver` | `reviver` item (revive at 25 % HP) |
| Ghost | `playerghost` tag, `ghost.lua` | ghost state in islands dim |
| Charlie / darkness killer | `grue` component | planned: darkness damage in islands dim |
| Sanity | `sanity` component | deferred (overworld: none, per design) |
| Hunger | `hunger` component | deferred |
| Spoilage | `perishable` component | deferred; DS items never despawn — vault items safe |
| Science tiers | `researchlab`/`researchlab2`/`researchlab4`/`researchlab3` | future overworld tech for DS materials |
| Werepig full moon | `moonphase` clock + `werepig` | future |
| Hound waves | `hounded` component (`hounded.lua:54-57`) | `survive/HoundWave` — compressed schedule, 120 s warning; `/ds hound now` for tests |
| Hound | `hound` (150 HP / 20 dmg, `tuning.lua:1490-1491`) | wave hounds are wolves; **`hairy_hound`** (ours, distinct id) carries the beard hair (`hound.lua`) |
| Grass tuft | `grass` (`grass.lua`, `GRASS_REGROW_TIME`) | `cut_grass` block — drops cutgrass + 25 % flint, spreads on random tick |
| Sapling / tree | `sapling`, `tree` | `sapling` block; DS-silhouette tree placed by custom grow code (`TreeGrower` is final in 1.21.1) |
| Spider den | `spiderden` | `spider_den` block + BE — keeps ≤ 3 spiders nearby (40 s checks) |
| Spider loot | silk/gland 50/50 (`spider.lua:647-648`) | vanilla string stands in for silk; `spidergland` real — `init/DSLoot` |
| Boards | `boards` = 4 logs (`recipes.lua:459`) | `boards` item + shapeless recipe (logs tag) |
| Torch | `torch` = 2 cutgrass + 2 twigs (`recipes.lua:425`) | `torch` item placing a vanilla torch |

## Naming rules for our registry

1. Ported 1:1 → DS prefab id: `moonrocknugget`, `moonrockidol`, `resurrectionstatue`,
   `reviver`, `monstermeat`, `smallmeat`, `spoiled_food`, `beefalowool`, `pigskin`,
   `blowdart_sleep`, `armor_sanity`, `footballhat`, `minerhat`, `cane`, `bugnet`,
   `spider_eggs`, `caged_bird`.
2. Our inventions get DS-flavored but distinct ids: `marble_portal`, `islands`
   (dimension), `sky_portal` (escape), `ghost` state (internal).
3. Display strings copy DS `STRINGS.NAMES` wording ("Moon Rock", "Telltale Heart",
   "Meat Effigy") so players recognize them; en_us + ru_ru.
4. Never reuse a DS id for non-DS behavior (e.g. don't name our sacrifice mechanic
   `sacrificer` — it would imply a ported component that doesn't exist).

## Design-decision provenance (what is NOT from DS)

- 120 s all-ghost wipe timer — ours (blend of DS1 permadeath + DST ghost window).
- Portal item vault (confiscate on entry, eject on right-click) — ours (MC adaptation
  of "you arrive naked").
- Islands-in-one-dimension-at-fixed-X trick — ours (engineering choice, see
  `world/IslandState.java`).
- Marble П-frame + mob-death ignition — ours, DS-flavored.
