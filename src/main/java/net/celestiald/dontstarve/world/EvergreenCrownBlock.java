package net.celestiald.dontstarve.world;

import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.work.DSWork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;

/**
 * A crown block of a DST evergreen. It has no behaviour of its own: its state stores
 * the offset to the tree's base trunk block, and working or burning it works or burns
 * that tree (the whole tree is one DST object). It drops nothing: DST trees give
 * nothing from the crown.
 */
public class EvergreenCrownBlock extends Block implements DSBurnable {

    /** dx + 2, dz + 2 and dy of this block relative to the base trunk block. */
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 4);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 4);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 7);

    private final boolean sparse;

    public EvergreenCrownBlock(Properties properties, boolean sparse) {
        super(properties);
        this.sparse = sparse;
        registerDefaultState(stateDefinition.any().setValue(DX, 2).setValue(DZ, 2).setValue(DY, 0));
    }

    public static BlockState state(Block block, int dx, int dy, int dz) {
        return block.defaultBlockState().setValue(DX, dx + 2).setValue(DZ, dz + 2).setValue(DY, dy);
    }

    /** Whether {@code state} is a crown block whose base is at offset {@code -(dx, dy, dz)} from its position. */
    static boolean belongsTo(BlockState state, int dx, int dy, int dz) {
        return state.getBlock() instanceof EvergreenCrownBlock
                && state.getValue(DX) == dx + 2 && state.getValue(DZ) == dz + 2 && state.getValue(DY) == dy;
    }

    public BlockPos baseOf(BlockState state, BlockPos pos) {
        return pos.offset(2 - state.getValue(DX), -state.getValue(DY), 2 - state.getValue(DZ));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DZ, DY);
    }

    /** The trunk base block state if the tree is intact, else null. */
    private BlockState baseState(BlockGetter level, BlockState state, BlockPos pos) {
        BlockState base = level.getBlockState(baseOf(state, pos));
        return base.getBlock() instanceof EvergreenBlock ? base : null;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState base = baseState(level, state, pos);
        return base != null
                ? DSWork.destroyProgress(base, player, level, baseOf(state, pos))
                : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        BlockPos basePos = baseOf(state, pos);
        BlockState base = baseState(level, state, pos);
        if (base == null) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        if (player.isCreative()) {
            if (!level.isClientSide) {
                EvergreenTree.clear(level, basePos, false);
            }
            return true;
        }
        return DSWork.onDestroyedByPlayer(base, level, basePos, player)
                && super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    // ---- fire: the crown burns with its tree -----------------------------------------

    @Override
    public float burnTime(BlockState state) {
        return EvergreenBlock.TREE_BURN_TIME;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.MEDIUM;
    }

    @Override
    public BlockPos burnPos(BlockState state, Level level, BlockPos pos) {
        return baseOf(state, pos);
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos basePos = baseOf(state, pos);
        BlockState base = level.getBlockState(basePos);
        if (base.getBlock() instanceof EvergreenBlock tree) {
            tree.onBurnt(level, basePos, base);
        } else {
            level.removeBlock(pos, false);
        }
    }
}
