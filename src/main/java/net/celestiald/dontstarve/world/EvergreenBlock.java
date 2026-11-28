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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;

/**
 * A trunk block of a living DST evergreen (prefabs/evergreens.lua), "evergreen" or the
 * lumpy "evergreen_sparse". The state carries the growable stage; all trunk blocks of
 * one tree are worked as one object (workable CHOP, counter at the base), burn as one
 * (TREE_BURN_TIME, medium propagator) and grow as one (timer on the base block).
 */
public class EvergreenBlock extends Block implements DSWorkable, DSBurnable, EvergreenTree.Trunk {

    /** growable stage 1..4 (short, normal, tall, old) as 0..3. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    /** TUNING.TREE_BURN_TIME (evergreens.lua MakeLargeBurnable). */
    public static final float TREE_BURN_TIME = 45;

    private final boolean sparse;

    public EvergreenBlock(Properties properties, boolean sparse) {
        super(properties);
        this.sparse = sparse;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    public static BlockState state(Block block, int stage) {
        return block.defaultBlockState().setValue(STAGE, stage);
    }

    @Override
    public boolean sparse() {
        return sparse;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    // ---- work (workable CHOP) --------------------------------------------------------

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.CHOP;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return EvergreenTree.chops(state.getValue(STAGE));
    }

    @Override
    public BlockPos workPos(BlockState state, Level level, BlockPos pos) {
        return EvergreenTree.baseOf(level, pos);
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        EvergreenTree.chopDown(level, EvergreenTree.baseOf(level, pos), state.getValue(STAGE), sparse);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return DSWork.destroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        if (player.isCreative() && !level.isClientSide) {
            EvergreenTree.clear(level, EvergreenTree.baseOf(level, pos), false);
        }
        return DSWork.onDestroyedByPlayer(state, level, pos, player)
                && super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        DSWork.forget(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---- growth (growable) -----------------------------------------------------------

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this) && EvergreenTree.isBase(level, pos)) {
            EvergreenTree.scheduleGrowth(level, pos, state.getValue(STAGE), sparse);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (EvergreenTree.isBase(level, pos)) {
            EvergreenTree.grow(level, pos, state);
        }
    }

    // ---- fire (burnable + propagator) ------------------------------------------------

    @Override
    public float burnTime(BlockState state) {
        return TREE_BURN_TIME;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.MEDIUM;
    }

    @Override
    public BlockPos burnPos(BlockState state, Level level, BlockPos pos) {
        return EvergreenTree.baseOf(level, pos);
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        EvergreenTree.burn(level, EvergreenTree.baseOf(level, pos), state.getValue(STAGE), sparse);
    }
}
