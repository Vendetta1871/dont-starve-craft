package net.celestiald.dontstarve.world;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The stump of a felled DST evergreen (evergreens.lua make_stump): one DIG with a shovel
 * (SetWorkLeft(1), dig_up_stump) gives a log. It burns as a small burnable (10 s, small
 * propagator) into ash (DefaultBurntFn), and decays after DEAD_DECAY_TIME (timer "decay").
 */
public class EvergreenStumpBlock extends Block implements DSWorkable, DSBurnable {

    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    /** MakeSmallBurnable default burn time. */
    private static final float STUMP_BURN_TIME = 10;

    public EvergreenStumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.DIG;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 1;
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(Items.SPRUCE_LOG));
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

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, EvergreenTree.decayTicks(level.random));
        }
    }

    /** timerdone "decay". */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    public float burnTime(BlockState state) {
        return STUMP_BURN_TIME;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.SMALL;
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        DSBurnable.defaultBurnt(level, pos);
    }
}
