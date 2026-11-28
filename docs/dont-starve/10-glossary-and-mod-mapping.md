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
| Codex Umbra | `waxwelljournal` (Maxwell's book) | **our entry**: reading it outside the islands drops the player's belongings on the spot and sends them to the current island; consumed. One free copy on first join; recipe book + wither rose (ours) |
| Sky (post-death limbo) [DS1] | — | our planned ghost-hold state, not a dimension |
| Moon Portal | `multiplayer_portal_moonrock` (tag `moonportal`) | **Sky Portal** (escape, one-way) |
| Moon Rock Idol | `moonrockidol` (key: `moonrocknugget` + `purplegem`) | escape key item — keep ids |
| Moon Rock shard | `moonrocknugget` | our `moonrocknugget` |
| Meat Effigy | `resurrectionstatue` | `resurrectionstatue` block (in-run revive; builder linked free, others −5 HP = 40 × 2/15; consumed on use) |
| Telltale Heart | `reviver` | `reviver` item (revive at 50/150 of max health) |
| Ghost | `playerghost` tag, `ghost.lua` | ghost state in islands dim |
| Charlie / darkness killer | `grue` component | planned: darkness damage in islands dim |
| Sanity | `sanity` component | deferred (overworld: none, per design) |
| Hunger | `hunger` component | deferred |
| Spoilage | `perishable` component | deferred; DS items never despawn (MC ground items do — belongings dropped by the Codex Umbra are not protected) |
| Science tiers | `researchlab`/`researchlab2`/`researchlab4`/`researchlab3` | future overworld tech for DS materials |
| Werepig full moon | `moonphase` clock + `werepig` | future |
| Hound waves | `hounded` component (`hounded.lua:54-57`) | `survive/HoundWave` — compressed schedule, 120 s warning, clock runs only while living run members are on the island; `/ds hound now` for tests |
| Hound | `hound` (150 HP / 20 dmg, `tuning.lua:1490-1491`) | own `hound` mob (`entity/Hound`, placeholder wolf-shaped model); loot monstermeat 100 % + houndstooth 12.5 % (`hound.lua:110-114`). No hound drops beardhair — see `content/mechanics-cheatsheet.md` |
| Grass tuft | `grass` (`grass.lua:248-254`) | `grass` block, pickable by hand: 1 cutgrass, regrows in 1440 DS s, 20 cycles then barren; dig (shovel) → cutgrass if ripe + `dug_grass` |
| Sapling | `sapling` (twig bush, `sapling.lua:141`) | `sapling` block, pickable: twigs every 1920 DS s, unlimited; dig → twigs if ripe + `dug_sapling` |
| Evergreen | `evergreen`, `evergreen_sparse` (`evergreens.lua`) | own blocks: trunk column with the growable stage (short/normal/tall/old), crown, burnt tree, stump; worldgen places random stages; sapling blocks for pinecones. Placeholder spruce textures. Leif, regrowth and petrification: not ported |
| Boulder | `rock1`, `rock2` (`rocks.lua:65-85`) | `rock1`/`rock2` blocks (pickaxe): rocks ×3, nitre/gold, flint + chance rolls; the island's flint source (no gravel) |
| Spider den | `spiderden` (`spiderden.lua`) | `spider_den` tier 1: childspawner, 3 spiders, regen 90 DS s, releases only at night, hit → 2 defenders, destroyed → all out; loot silk ×2. Tiers 2-3/queen not ported |
| Spider | `spider` (`spider.lua`) | own `spider` mob (placeholder vanilla look): 100→13.3 HP, 20→2.7 dmg, 3 s period, target radius 4, homeless sleep by day, den spiders go home at dawn |
| Spider loot | one item: monstermeat 50 %, silk 25 %, spidergland 25 % (`spider.lua:645-650`) | `dontstarve:entities/spider` loot table |
| Boards | `boards` = 4 logs (`recipes.lua:459`) | `boards` item + shapeless recipe (logs tag) |
| Torch | `torch` = 2 cutgrass + 2 twigs (`recipes.lua:425`); TORCH_FUEL 75 s | the **vanilla torch** (user-approved): on the islands it cannot be placed, burns in hand (durability = fuel, 188 MC s), lights the head block, ignites on hit; vanilla stick+coal recipe locked on the islands (`not_in_islands_shaped`) |

## Naming rules for our registry

1. Ported 1:1 → DS prefab id: `moonrocknugget`, `moonrockidol`, `resurrectionstatue`,
   `reviver`, `monstermeat`, `smallmeat`, `spoiled_food`, `beefalowool`, `pigskin`,
   `blowdart_sleep`, `armor_sanity`, `footballhat`, `minerhat`, `cane`, `bugnet`,
   `spider_eggs`, `caged_bird`.
2. Our inventions get DS-flavored but distinct ids: `islands`
   (dimension), `sky_portal` (escape), `ghost` state (internal).
3. Display strings copy DS `STRINGS.NAMES` wording ("Moon Rock", "Telltale Heart",
   "Meat Effigy") so players recognize them; en_us + ru_ru.
4. Never reuse a DS id for non-DS behavior (e.g. don't name our sacrifice mechanic
   `sacrificer` — it would imply a ported component that doesn't exist).

## Design-decision provenance (what is NOT from DS)

- 120 s all-ghost wipe timer — ours (blend of DS1 permadeath + DST ghost window).
- Codex Umbra as the entry (belongings dropped where the book is read) — ours (MC
  adaptation of "you arrive with nothing"); DS seconds are scaled ×2.5 to the MC day.
- Islands-in-one-dimension-at-fixed-X trick — ours (engineering choice, see
  `world/IslandState.java`).
