package net.celestiald.dontstarve.world;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;

/**
 * A burnt DST evergreen (evergreens.lua OnBurnt, prefab "evergreen_burnt"): a bare dark
 * trunk of the stage's height that is chopped down in one hit (SetWorkLeft(1),
 * chop_down_burnt_tree) leaving charcoal and, for the normal build, a 10 % pinecone
 * (AddChanceLoot("pinecone", 0.1)). 10 DS seconds after burning it may drop that
 * pinecone by itself (onpineconetask); after DEAD_DECAY_TIME it is gone (timer "decay").
 * It cannot burn again.
 */
public class BurntEvergreenBlock extends Block implements DSWorkable, DSBurnable, EvergreenTree.Trunk {

    public static final IntegerProperty STAGE = EvergreenBlock.STAGE;
    /** burntcone: the pinecone task already ran. */
    public static final BooleanProperty CONE = BooleanProperty.create("cone");

    /** AddChanceLoot("pinecone", 0.1). */
    private static final float CONE_CHANCE = 0.1F;

    private final boolean sparse;

    public BurntEvergreenBlock(Properties properties, boolean sparse) {
        super(properties);
        this.sparse = sparse;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(CONE, false));
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
        builder.add(STAGE, CONE);
    }

    // ---- work: one chop, charcoal ----------------------------------------------------

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.CHOP;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 1;
    }

    @Override
    public BlockPos workPos(BlockState state, Level level, BlockPos pos) {
        return EvergreenTree.baseOf(level, pos);
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        BlockPos base = EvergreenTree.baseOf(level, pos);
        EvergreenTree.clear(level, base, false);
        Block.popResource(level, base, new ItemStack(DSItems.CHARCOAL.get()));
        if (!sparse && level.random.nextFloat() < CONE_CHANCE) {
            Block.popResource(level, base, new ItemStack(DSItems.PINECONE.get()));
        }
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

    // ---- timers: the pinecone task and decay -----------------------------------------

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this) && EvergreenTree.isBase(level, pos)
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, sparse ? EvergreenTree.decayTicks(level.random) : EvergreenTree.burntConeTicks());
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!EvergreenTree.isBase(level, pos)) {
            return;
        }
        if (!sparse && !state.getValue(CONE)) {
            // onpineconetask: DropLoot with only the 10 % pinecone chance loot.
            if (random.nextFloat() < CONE_CHANCE) {
                Block.popResource(level, pos, new ItemStack(DSItems.PINECONE.get()));
            }
            for (int y = 0; y < EvergreenTree.burntHeight(state.getValue(STAGE)); y++) {
                BlockPos p = pos.above(y);
                if (level.getBlockState(p).is(this)) {
                    level.setBlock(p, level.getBlockState(p).setValue(CONE, true), Block.UPDATE_CLIENTS);
                }
            }
            level.scheduleTick(pos, this, EvergreenTree.decayTicks(random));
        } else {
            // timerdone "decay": the tree is removed.
            EvergreenTree.clear(level, pos, false);
        }
    }

    // ---- fire: burnt trees do not burn -----------------------------------------------

    @Override
    public float burnTime(BlockState state) {
        return 0;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.SMALL;
    }

    @Override
    public BlockPos burnPos(BlockState state, Level level, BlockPos pos) {
        return EvergreenTree.baseOf(level, pos);
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
    }
}
