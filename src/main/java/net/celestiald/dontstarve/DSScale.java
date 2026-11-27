package net.celestiald.dontstarve;

/**
 * The one place that converts Don't Starve Together numbers (scripts/tuning.lua,
 * extracted to docs/dont-starve/data/tuning.json) into Minecraft units.
 *
 * <ul>
 *   <li><b>Stats</b> (health, damage, hunger, sanity) scale by the player's health
 *       ratio: Wilson's 150 HP is Minecraft's 20, so x2/15.</li>
 *   <li><b>World time</b> (day clock, regrowth, fuel, spoilage, spawner timers) scales
 *       by day length: a DS day is 480 s, a Minecraft day 1200 s, so x2.5. Combat
 *       timings (attack periods) are real time and stay unscaled.</li>
 *   <li><b>Distance</b>: one DST tile (TILE_SCALE 4 units) is 4x4 blocks, so one DS unit
 *       is one block. Speeds carry over too: WILSON_RUN_SPEED 6 units/s is 6 blocks/s,
 *       about a vanilla sprint (5.6), so the player keeps vanilla movement and mobs
 *       move at their DST speed in blocks/s.</li>
 * </ul>
 */
public final class DSScale {

    /** Stats: Minecraft per DS point (Wilson 150 HP -> 20 HP). */
    public static final float STAT = 2.0F / 15.0F;
    /** World time: Minecraft seconds per DS second (1200 s day / 480 s day). */
    public static final float TIME = 1200.0F / 480.0F;
    /** Blocks per DS distance unit. */
    public static final double BLOCKS_PER_UNIT = 1.0;
    /** Blocks along one side of a DST ground tile (4 DS units). */
    public static final int BLOCKS_PER_TILE = 4;

    /**
     * Measured with {@code /ds speed}: a navigating mob covers 43.17 x attribute^2
     * blocks/s (the move input and the speed both equal the attribute; zombie 0.23 ->
     * 2.28 b/s, spider 0.45 -> 8.2 b/s).
     */
    private static final double MOB_SPEED_K = 43.17;

    /** The movement-speed attribute that makes a mob move at a DS speed (units/s). */
    public static double mobSpeed(double dsUnitsPerSecond) {
        return Math.sqrt(blocks(dsUnitsPerSecond) / MOB_SPEED_K);
    }

    /** Goal speed modifier for moving at {@code dsSpeed} on a mob whose attribute is {@code mobSpeed(baseDsSpeed)}. */
    public static double speedModifier(double dsSpeed, double baseDsSpeed) {
        return mobSpeed(dsSpeed) / mobSpeed(baseDsSpeed);
    }

    /** A DS stat value (health, damage, hunger, sanity) in Minecraft points. */
    public static float stat(double ds) {
        return (float) (ds * STAT);
    }

    /** A DS world-time duration in Minecraft seconds. */
    public static int seconds(double dsSeconds) {
        return (int) Math.round(dsSeconds * TIME);
    }

    /** A DS world-time duration in Minecraft ticks. */
    public static int ticks(double dsSeconds) {
        return (int) Math.round(dsSeconds * TIME * 20.0);
    }

    /** A DS distance in blocks. */
    public static double blocks(double dsUnits) {
        return dsUnits * BLOCKS_PER_UNIT;
    }

    private DSScale() {
    }
}
