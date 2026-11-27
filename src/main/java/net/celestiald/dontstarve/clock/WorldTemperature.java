package net.celestiald.dontstarve.clock;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * DST components/worldtemperature.lua for the current run: season curve + day/night
 * swing + slow noise, in degrees.
 *
 * <ul>
 *   <li>Season (progress = 1 - remaining/total days): autumn lerps 55 -> 5, winter
 *       is sin(pi p) * (-25 - 5) + 5, spring lerps 5 -> 55, summer sin(pi p) * 40 + 55.</li>
 *   <li>Phase: day +5 sin(pi t), night -6 sin(pi t) over the phase, dusk 0.</li>
 *   <li>Noise: +-8 degrees, perlin over noise time x 0.025 (noise time = DS seconds of
 *       the run). DST's native perlin is replaced by Minecraft's ImprovedNoise seeded
 *       per island.</li>
 * </ul>
 */
public final class WorldTemperature {

    private static final double NOISE_SCALE = 0.025;
    private static final double NOISE_MAG = 8.0;
    private static final double MIN_TEMPERATURE = -25.0;
    private static final double MAX_TEMPERATURE = 95.0;
    private static final double WINTER_CROSSOVER = 5.0;
    private static final double SUMMER_CROSSOVER = 55.0;

    private static int noiseIsland = Integer.MIN_VALUE;
    private static ImprovedNoise noise;

    public static double season(DSClock.Day day) {
        double p = 1.0 - (day.totalDays() > 0 ? day.remainingDays() / (double) day.totalDays() : 0.0);
        return switch (day.season()) {
            case WINTER -> Math.sin(Math.PI * p) * (MIN_TEMPERATURE - WINTER_CROSSOVER) + WINTER_CROSSOVER;
            case SPRING -> WINTER_CROSSOVER + (SUMMER_CROSSOVER - WINTER_CROSSOVER) * p;
            case SUMMER -> Math.sin(Math.PI * p) * (MAX_TEMPERATURE - SUMMER_CROSSOVER) + SUMMER_CROSSOVER;
            case AUTUMN -> SUMMER_CROSSOVER + (WINTER_CROSSOVER - SUMMER_CROSSOVER) * p;
        };
    }

    public static double phase(DSClock.State state) {
        double t = state.phaseLength() > 0 ? state.phaseTicks() / (double) state.phaseLength() : 0.0;
        return switch (state.phase()) {
            case DAY -> 5.0 * Math.sin(t * Math.PI);
            case NIGHT -> -6.0 * Math.sin(t * Math.PI);
            case DUSK -> 0.0;
        };
    }

    public static synchronized double noise(int islandIndex, long runTicks) {
        if (noise == null || noiseIsland != islandIndex) {
            noise = new ImprovedNoise(RandomSource.create(0x5EA50L ^ islandIndex));
            noiseIsland = islandIndex;
        }
        double dsSeconds = runTicks / (20.0 * net.celestiald.dontstarve.DSScale.TIME);
        double perlin01 = (noise.noise(0.0, 0.0, dsSeconds * NOISE_SCALE) + 1.0) * 0.5;
        return 2.0 * NOISE_MAG * perlin01 - NOISE_MAG;
    }

    /** The world temperature of the run at this clock state. */
    public static double at(DSClock.State state, int islandIndex) {
        return noise(islandIndex, state.runTicks()) + season(state.day()) + phase(state);
    }

    private WorldTemperature() {
    }
}
