package net.celestiald.dontstarve.world;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * A DST boulder (prefabs/rocks.lua baserock_fn): MINE, ROCKS_MINE 6 pickaxe
 * hits, then it drops its loot table and is gone. The full/med/low look of a
 * partly mined boulder is not ported yet.
 */
public class BoulderBlock extends Block implements DSWorkable {

    /** TUNING.ROCKS_MINE. */
    public static final int ROCKS_MINE = 6;

    public BoulderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.MINE;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return ROCKS_MINE;
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        dropResources(state, level, pos, null, worker, worker != null ? worker.getMainHandItem() : net.minecraft.world.item.ItemStack.EMPTY);
        level.destroyBlock(pos, false, worker);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return DSWork.destroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        return DSWork.onDestroyedByPlayer(state, level, pos, player)
                && super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        DSWork.forget(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
