package net.celestiald.dontstarve.world;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A planted pinecone (planted_tree.lua "pinecone_sapling", and "lumpy_sapling" for the
 * lumpy build): after PINECONE_GROWTIME it grows into a short evergreen
 * (handler_growfromseed). One DIG with a shovel gives twigs (SetLoot {"twigs"}). It burns
 * as a small burnable (SMALL_BURNTIME 7.5 s) into ash.
 */
public class TreeSaplingBlock extends BushBlock implements DSWorkable, DSBurnable {

    private static final VoxelShape SHAPE = box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);
    /** TUNING.SMALL_BURNTIME. */
    private static final float SMALL_BURNTIME = 7.5F;

    private final boolean sparse;

    public TreeSaplingBlock(Properties properties, boolean sparse) {
        super(properties);
        this.sparse = sparse;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        throw new UnsupportedOperationException("TreeSaplingBlock is registered in code only");
    }

    /** Planted on any solid ground (DEPLOYMODE.PLANT on a land tile), not only on dirt. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, EvergreenTree.saplingTicks(level.random));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        EvergreenTree.growFromSeed(level, pos, sparse);
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
        Block.popResource(level, pos, new ItemStack(DSItems.TWIGS.get()));
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
    public float burnTime(BlockState state) {
        return SMALL_BURNTIME;
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
