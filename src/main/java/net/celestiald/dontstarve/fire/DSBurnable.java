package net.celestiald.dontstarve.fire;

import net.celestiald.dontstarve.init.DSItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with the DST burnable + propagator components
 * (standardcomponents.lua Make*Burnable / Make*Propagator). The fire system
 * ({@code fire} package, {@link DSFire}) ignites it, keeps it burning for {@link #burnTime}
 * DS seconds, spreads heat from it, and then calls {@link #onBurnt}.
 */
public interface DSBurnable {

    /** burnable:SetBurnTime in DS seconds; 0 or less: cannot burn in this state (e.g. already burnt). */
    float burnTime(BlockState state);

    /** The propagator preset of this block. */
    DSPropagator propagator(BlockState state);

    /** The position that burns for every block of a multi-block object (the tree base). */
    default BlockPos burnPos(BlockState state, Level level, BlockPos pos) {
        return pos;
    }

    /** burnable onignite. */
    default void onIgnite(ServerLevel level, BlockPos pos, BlockState state) {
    }

    /** burnable onextinguish: the fire was put out before the block burnt. */
    default void onExtinguish(ServerLevel level, BlockPos pos, BlockState state) {
    }

    /** burnable onburnt: the fire burnt out; turn into the burnt form or {@link #defaultBurnt}. */
    void onBurnt(ServerLevel level, BlockPos pos, BlockState state);

    /** DefaultBurntFn (standardcomponents.lua): the object is gone and leaves ash. */
    static void defaultBurnt(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(DSItems.ASH.get()));
    }

    // ---- additive defaults used by the fire engine (all optional) ----

    /**
     * The "canlight" tag: whether the LIGHT action (a torch) may start this fire. A burnt structure
     * keeps its burnable but cannot be lit by hand (DefaultBurntStructureFn: canlight = false).
     */
    default boolean canLight(BlockState state) {
        return true;
    }

    /** propagator.acceptsheat: false for a fire that must not catch from its neighbours (the campfire). */
    default boolean acceptsHeat(BlockState state) {
        return true;
    }

    /** A burnable with no burn time that burns until put out (fueled fires: campfire, fire pit). */
    default boolean endless(BlockState state) {
        return false;
    }

    /** The fire fx level while burning (MakeSmall/Medium/LargeBurnable: 2/3/4); sets light and warmth. */
    default int fxLevel(BlockState state) {
        return switch (propagator(state)) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 4;
        };
    }

    /** The block emits its own light (blockstate light level), so the engine adds no light block. */
    default boolean selfLit() {
        return false;
    }

    /**
     * What this fire gives off while burning: null when it has no propagator (the fire pit).
     * The default is the preset's per-object roll, with damage on (Make*Propagator).
     */
    default Spread spread(BlockState state, BlockPos burnPos) {
        DSPropagator.Roll roll = propagator(state).roll(burnPos);
        return new Spread(roll.range(), roll.heat(), true, roll.damageRange());
    }

    /** The warming temperature of the burning fire (heater component), by fx level for generic fires (fire.lua heats). */
    default double warmth(BlockState state) {
        return DSFire.FIRE_HEATS[Math.max(0, Math.min(DSFire.FIRE_HEATS.length, fxLevel(state)) - 1)];
    }

    /** The light radius in blocks of the burning fire (fire.lua level radius). */
    default double lightRadius(BlockState state) {
        return DSFire.FIRE_RADII[Math.max(0, Math.min(DSFire.FIRE_RADII.length, fxLevel(state)) - 1)];
    }

    /** propagator range, heat output and damage of a burning object. */
    record Spread(double range, double heat, boolean damages, double damageRange) {
    }
}
