package net.celestiald.dontstarve.clock;

import net.celestiald.dontstarve.DSScale;

/**
 * The DST world clock and season cycle as a pure function of run time, so the
 * server and every client compute the same state from one synced tick count.
 *
 * <ul>
 *   <li>A day is 16 segments of 30 DS s (components/clock.lua); 30 DS s is 1500
 *       Minecraft ticks, so a day is 24000 ticks like a Minecraft day.</li>
 *   <li>Each day is day, dusk, night segments. The split follows the season and
 *       slides towards the neighbouring season as in seasons.lua
 *       PushSeasonClockSegs; it changes at dawn.</li>
 *   <li>Seasons advance at dawn (seasons.lua OnAdvanceSeason). A new world starts
 *       in autumn with 20 of 40 "total" days left, i.e. mid-autumn (seasons.lua:441).</li>
 *   <li>The moon follows a 20-day cycle advancing at dawn (clock.lua:30-50, 358).</li>
 *   <li>Ambient light lerps to each phase's colour (ambientlighting.lua).</li>
 * </ul>
 */
public final class DSClock {

    public static final int NUM_SEGS = 16;
    public static final int SEG_TICKS = DSScale.ticks(30);
    public static final int DAY_TICKS = SEG_TICKS * NUM_SEGS;

    public enum Phase { DAY, DUSK, NIGHT }

    /**
     * DST seasons with their clock segments (seasons.lua DEFAULT_CLOCK_SEGS) and the
     * default lengths in days. The DS stage of the port has autumn and winter only;
     * spring and summer (Reign of Giants) have length 0, which DST treats as absent.
     */
    public enum Season {
        AUTUMN(8, 6, 2, 20),
        WINTER(5, 5, 6, 15),
        SPRING(5, 8, 3, 0),
        SUMMER(11, 1, 4, 0);

        public final Segs segs;
        public final int length;

        Season(int day, int dusk, int night, int length) {
            this.segs = new Segs(day, dusk, night);
            this.length = length;
        }

        Season next() {
            Season s = this;
            do {
                s = values()[(s.ordinal() + 1) % 4];
            } while (s.length == 0 && s != this);
            return s;
        }

        Season prev() {
            Season s = this;
            do {
                s = values()[(s.ordinal() + 3) % 4];
            } while (s.length == 0 && s != this);
            return s;
        }
    }

    public enum MoonPhase { NEW, QUARTER, HALF, THREEQUARTER, FULL }

    private static final MoonPhase[] MOON_CYCLE = buildMoonCycle();

    public record Segs(int day, int dusk, int night) {
        int of(Phase phase) {
            return switch (phase) {
                case DAY -> day;
                case DUSK -> dusk;
                case NIGHT -> night;
            };
        }
    }

    /** Season bookkeeping for one day (cycles = days completed before it). */
    public record Day(int cycles, Season season, int totalDays, int remainingDays, int elapsedDays, Segs segs) {
    }

    /**
     * Clock state at a run time.
     *
     * @param phaseTicks  ticks into the current phase
     * @param phaseLength ticks the current phase lasts
     * @param dayFraction 0..1 through the whole day (the HUD clock hand)
     */
    public record State(long runTicks, Day day, Phase phase, int phaseTicks, int phaseLength,
                        float dayFraction, MoonPhase moon, boolean waxing) {

        public boolean isDay() {
            return phase == Phase.DAY;
        }

        public boolean isFullMoonNight() {
            return phase == Phase.NIGHT && moon == MoonPhase.FULL;
        }
    }

    public static State at(long runTicks) {
        int cycles = (int) (runTicks / DAY_TICKS);
        int inDay = (int) (runTicks % DAY_TICKS);
        Day day = day(cycles);
        Phase phase = Phase.DAY;
        int start = 0;
        for (Phase p : Phase.values()) {
            int length = day.segs().of(p) * SEG_TICKS;
            if (inDay < start + length) {
                phase = p;
                break;
            }
            start += length;
        }
        int length = day.segs().of(phase) * SEG_TICKS;
        int moonIndex = cycles % MOON_CYCLE.length;
        return new State(runTicks, day, phase, inDay - start, length, inDay / (float) DAY_TICKS,
                MOON_CYCLE[moonIndex], moonIndex < MOON_CYCLE.length / 2);
    }

    /** Replays OnAdvanceSeason from the first day (cycle mode). */
    public static Day day(int cycles) {
        Season season = Season.AUTUMN;
        int total = season.length * 2;
        int remaining = season.length;
        int elapsed = 0;
        for (int i = 0; i < cycles; i++) {
            elapsed++;
            if (remaining > 1) {
                remaining--;
            } else {
                season = season.next();
                total = season.length;
                elapsed = 0;
                remaining = total;
            }
        }
        return new Day(cycles, season, total, remaining, elapsed, segs(season, total, remaining));
    }

    /** seasons.lua PushSeasonClockSegs. */
    static Segs segs(Season season, int total, int remaining) {
        double p = 1.0 - (total > 0 ? remaining / (double) total : 0.0);
        Season to = p < 0.5 ? season.prev() : season.next();
        if (to == season) {
            return season.segs;
        }
        Segs from = season.segs;
        double t = 0.5 - Math.sin(Math.PI * p) * 0.5;
        int day = (int) Math.floor(from.day() + (to.segs.day() - from.day()) * t + 0.5);
        int night = (int) Math.floor(from.night() + (to.segs.night() - from.night()) * t + 0.5);
        return new Segs(day, NUM_SEGS - day - night, night);
    }

    // Ambient colours (ambientlighting.lua NORMAL_COLOURS default) and lerp times in DS s.
    private static final float[] DAY_COLOUR = {1.0F, 230 / 255F, 158 / 255F};
    private static final float[] DUSK_COLOUR = {150 / 255F, 150 / 255F, 150 / 255F};
    private static final float[] NIGHT_COLOUR = {0F, 0F, 0F};
    private static final float[] FULL_MOON_COLOUR = {84 / 255F, 122 / 255F, 156 / 255F};

    private static float[] target(Phase phase, MoonPhase moon) {
        return switch (phase) {
            case DAY -> DAY_COLOUR;
            case DUSK -> DUSK_COLOUR;
            case NIGHT -> moon == MoonPhase.FULL ? FULL_MOON_COLOUR : NIGHT_COLOUR;
        };
    }

    private static int lerpTicks(Phase phase) {
        return DSScale.ticks(switch (phase) {
            case DAY -> 4;
            case DUSK -> 6;
            case NIGHT -> 8;
        });
    }

    /** The ambient light colour (r, g, b in 0..1) at this state, lerping after each phase change. */
    public static float[] ambient(State state) {
        float[] to = target(state.phase(), state.moon());
        int lerp = lerpTicks(state.phase());
        if (state.phaseTicks() >= lerp) {
            return to;
        }
        // The previous non-empty phase (the night before dawn belongs to yesterday).
        Phase prev = state.phase();
        Segs segs = state.day().segs();
        MoonPhase prevMoon = state.moon();
        do {
            if (prev == Phase.DAY) {
                prev = Phase.NIGHT;
                int cycles = state.day().cycles();
                prevMoon = MOON_CYCLE[Math.floorMod(cycles - 1, MOON_CYCLE.length)];
                segs = cycles > 0 ? day(cycles - 1).segs() : segs;
            } else {
                prev = Phase.values()[prev.ordinal() - 1];
            }
        } while (segs.of(prev) == 0 && prev != state.phase());
        float[] from = state.day().cycles() == 0 && state.phase() == Phase.DAY ? DAY_COLOUR : target(prev, prevMoon);
        float t = state.phaseTicks() / (float) lerp;
        return new float[] {
                from[0] + (to[0] - from[0]) * t,
                from[1] + (to[1] - from[1]) * t,
                from[2] + (to[2] - from[2]) * t};
    }

    /** Ambient brightness 0..1 (mean of the channels): day ~0.84, dusk 0.59, full moon 0.47, night 0. */
    public static float ambientLevel(State state) {
        float[] c = ambient(state);
        return (c[0] + c[1] + c[2]) / 3.0F;
    }

    /**
     * A Minecraft day time that puts the sun where the DS phase is: day maps to
     * 0..11000 (sun up), dusk to 11000..13000 (sunset), night to 13000..24000.
     */
    public static long minecraftDayTime(State state) {
        float f = state.phaseLength() > 0 ? state.phaseTicks() / (float) state.phaseLength() : 0F;
        long time = switch (state.phase()) {
            case DAY -> (long) (f * 11000);
            case DUSK -> 11000 + (long) (f * 2000);
            case NIGHT -> 13000 + (long) (f * 11000);
        };
        return state.day().cycles() * 24000L + time;
    }

    /** Run ticks at which the given phase of the given day (0-based cycles) begins. */
    public static long startOf(int cycles, Phase phase) {
        Segs segs = day(cycles).segs();
        long start = (long) cycles * DAY_TICKS;
        for (Phase p : Phase.values()) {
            if (p == phase) {
                break;
            }
            start += (long) segs.of(p) * SEG_TICKS;
        }
        return start;
    }

    private static MoonPhase[] buildMoonCycle() {
        int[] lengths = {1, 3, 3, 3, 1};
        java.util.List<MoonPhase> cycle = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            for (int x = 0; x < lengths[i]; x++) {
                cycle.add(MoonPhase.values()[i]);
            }
        }
        for (int i = 3; i >= 1; i--) {
            for (int x = 0; x < lengths[i]; x++) {
                cycle.add(MoonPhase.values()[i]);
            }
        }
        return cycle.toArray(new MoonPhase[0]);
    }

    private DSClock() {
    }
}
