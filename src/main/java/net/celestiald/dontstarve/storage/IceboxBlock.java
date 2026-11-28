package net.celestiald.dontstarve.storage;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.fire.DSFire;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ice Box (DST "icebox", prefabs/icebox.lua): a nine-slot container (containers.lua) that
 * slows spoilage of the food inside to PERISH_FRIDGE_MULT. Hammered in 2 hits; a hit shakes
 * everything out (onhit: DropEverything). It does not burn: the prefab has no burnable.
 */
public class IceboxBlock extends Block implements EntityBlock, DSWorkable {

    private static final VoxelShape SHAPE = box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public IceboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IceboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != DSBlockEntities.ICEBOX.get()) {
            return null;
        }
        return (l, p, s, be) -> ((IceboxBlockEntity) be).serverTick((ServerLevel) l);
    }

    /** Opens the nine slots. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IceboxBlockEntity box) {
            player.openMenu(box);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            if (level.getBlockEntity(pos) instanceof IceboxBlockEntity box) {
                Containers.dropContents(level, pos, box);
            }
            DSWork.forget(state, level, pos, newState);
            DSFire.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---- workable: HAMMER, SetWorkLeft(2) ----

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.HAMMER;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 2;
    }

    /** onhit: the contents spill out. */
    @Override
    public void onWork(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker, int workLeft) {
        if (level.getBlockEntity(pos) instanceof IceboxBlockEntity box) {
            Containers.dropContents(level, pos, box);
            box.clearContent();
        }
    }

    /** onhammered: the recipe loot and everything inside (dropped when the block goes). */
    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        DSWork.dropRecipeLoot(level, pos, this);
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
}
