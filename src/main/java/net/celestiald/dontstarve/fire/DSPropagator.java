package net.celestiald.dontstarve.fire;

import net.minecraft.util.RandomSource;

/**
 * standardcomponents.lua MakeSmall/Medium/LargePropagator: how much heat a
 * burning block gives off, how far it reaches and how much heat a block needs
 * before it catches fire. Each value is {@code base + random * spread}, rolled
 * per object in DST. Ranges are in DS units (1 unit = 1 block, DSScale).
 */
public enum DSPropagator {
    SMALL(5, 5, 3, 2, 3, 2, 2),
    MEDIUM(15, 10, 5, 2, 5, 3.5, 3),
    LARGE(45, 10, 6, 2, 6, 3.5, 3);

    /** propagator.decayrate for all three presets. */
    public static final double DECAY_RATE = 0.5;

    private final double flashpoint;
    private final double flashpointSpread;
    private final double range;
    private final double rangeSpread;
    private final double heat;
    private final double heatSpread;
    /** propagator.damagerange: burning hurts creatures this close. */
    public final double damageRange;

    DSPropagator(double flashpoint, double flashpointSpread, double range, double rangeSpread,
                 double heat, double heatSpread, double damageRange) {
        this.flashpoint = flashpoint;
        this.flashpointSpread = flashpointSpread;
        this.range = range;
        this.rangeSpread = rangeSpread;
        this.heat = heat;
        this.heatSpread = heatSpread;
        this.damageRange = damageRange;
    }

    public double flashpoint(RandomSource random) {
        return flashpoint + random.nextDouble() * flashpointSpread;
    }

    public double propagateRange(RandomSource random) {
        return range + random.nextDouble() * rangeSpread;
    }

    public double heatOutput(RandomSource random) {
        return heat + random.nextDouble() * heatSpread;
    }

    /**
     * The values DST rolls once per object at creation (standardcomponents.lua Make*Propagator:
     * flashpoint, then range, then heat output). A block has no object identity, so the roll is
     * seeded by its position: the same block always has the same values.
     */
    public Roll roll(net.minecraft.core.BlockPos pos) {
        RandomSource random = RandomSource.create(pos.asLong() * 0x9E3779B97F4A7C15L + ordinal());
        double flash = flashpoint(random);
        double reach = propagateRange(random);
        double output = heatOutput(random);
        return new Roll(flash, reach, output, damageRange);
    }

    /** One object's rolled propagator values. */
    public record Roll(double flashpoint, double range, double heat, double damageRange) {
    }
}
