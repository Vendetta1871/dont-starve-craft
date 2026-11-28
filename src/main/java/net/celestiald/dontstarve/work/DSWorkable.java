package net.celestiald.dontstarve.work;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with the DST workable component (components/workable.lua): it takes
 * {@link #maxWork} hits of its {@link #workAction} and then finishes. The work
 * left is kept per position by {@link DSWork}; a block spanning several
 * positions (a tree) reports one {@link #workPos} for all of them.
 *
 * <p>Blocks wire it up by delegating {@code getDestroyProgress} to
 * {@link DSWork#destroyProgress} and {@code onDestroyedByPlayer} to
 * {@link DSWork#onDestroyedByPlayer}.
 */
public interface DSWorkable {

    /** The action that works this block in this state, or null when it cannot be worked. */
    @Nullable
    DSAction workAction(BlockState state);

    /** workable:SetWorkLeft for a fresh target. */
    int maxWork(BlockState state, Level level, BlockPos pos);

    /** The position that holds the work counter (the tree base for every trunk block). */
    default BlockPos workPos(BlockState state, Level level, BlockPos pos) {
        return pos;
    }

    /** workable.onwork: called after each hit that did not finish the work. */
    default void onWork(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker, int workLeft) {
    }

    /** workable.onfinish: the work is done; the block removes itself and drops its loot here. */
    void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker);
}
