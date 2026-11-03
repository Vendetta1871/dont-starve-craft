# 02 — Time & Seasons

Sources: `tuning.lua` (Tune header), `components/clock.lua`, `components/seasons.lua`,
`worldsettings_overrides.lua`.

## The clock

Time is measured in **segments of 30 s**; a full day is **16 segments = 480 s**
(`seg_time = 30`, `total_day_time = seg_time*16`, `tuning.lua:18-20`).

The day has three **phases** (`components/clock.lua:15-17`): `day`, `dusk`, `night`.
`TheWorld.state` exposes `cycles` (full days survived), `phase`, and the booleans
`isday` / `isevening` / `isdark`.

Default composition (`tuning.lua:23-27`):

| Phase | Segments | Seconds |
|---|---|---|
| day | 10 | 300 |
| dusk | 4 | 120 |
| night | 2 | 60 |

Per-season day composition (`components/seasons.lua:33-38`, `DEFAULT_CLOCK_SEGS`):

| Season | day | dusk | night |
|---|---|---|---|
| autumn | 8 (240 s) | 6 (180 s) | 2 (60 s) |
| winter | 5 (150 s) | 5 (150 s) | 6 (180 s) |
| spring | 5 (150 s) | 8 (240 s) | 3 (90 s) |
| summer | 11 (330 s) | 1 (30 s) | 4 (120 s) |

## Seasons

Order is fixed: **autumn → winter → spring → summer** (`SEASON_NAMES`,
`components/seasons.lua:17-22`). A new default world starts in **autumn**.

Season length presets (`worldsettings_overrides.lua:15-34` → `tuning.lua:2236-2246`);
spring/autumn use the *friendly* number, winter/summer the *harsh* one:

| Preset | autumn/spring | winter/summer |
|---|---|---|
| veryshort | 5 | 5 |
| short | 12 | 10 |
| **default** | **20** | **15** |
| long | 30 | 22 |
| verylong | 50 | 40 |
| none | 0 (endless mode) | — |

Seasons component modes: `cycle`, `endless`, `always` (`components/seasons.lua:24-28`);
endless mode = 10 preseason days + 10-day temperature ramp + 10 000-day hold
(`ENDLESS_PRE_DAYS`, `ENDLESS_RAMP_DAYS`, `ENDLESS_DAYS`, lines 41-43).

## Moon phases

The clock also carries `moonphase` / `mooniswaxing` net vars (`components/clock.lua:91-92`).
Full moon turns `pig` into `werepig` (`prefabs/pigman.lua`).

## Notes for the mod

- Our islands dimension should reuse the same 480 s day model; DST's `has_celestial`
  is per-dimension, so the islands can run their own clock phase table.
- "Day" for gameplay rules = `TheWorld.state.cycles` increments at dawn, not midnight.
