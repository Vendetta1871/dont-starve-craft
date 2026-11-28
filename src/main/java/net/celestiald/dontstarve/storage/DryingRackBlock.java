package net.celestiald.dontstarve.storage;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSFire;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Drying Rack (DST "meatrack", prefabs/meatrack2.lua with components/dryingrack.lua): three
 * slots, one item each. Right-click with a dryable puts one on the rack (container itemtestfn:
 * only dryables), an empty hand takes an item off. Hammered in 4 hits, medium burnable with a
 * small propagator; when it burns down every dryable on it is dried at once
 * (DefaultBurntStructureFn -> dryingrack:OnBurnt) and the rest drops.
 */
public class DryingRackBlock extends Block implements EntityBlock, DSBurnable, DSWorkable {

    /** The "burnt" tag of DefaultBurntStructureFn. */
    public static final BooleanProperty BURNT = BooleanProperty.create("burnt");

    private static final VoxelShape SHAPE = box(0.0, 0.0, 5.0, 16.0, 15.0, 11.0);

    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BURNT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BURNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ---- container ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != DSBlockEntities.DRYING_RACK.get()) {
            return null;
        }
        return (l, p, s, be) -> ((DryingRackBlockEntity) be).serverTick((ServerLevel) l, p, s);
    }

    /** A dryable goes onto the rack, one item (the container does not accept stacks). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(BURNT) || !DryingRackBlockEntity.dryable(stack) || !level.dimension().equals(DSRefs.ISLANDS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && rack.put(stack) >= 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes the finished product, or else an item still drying (it resumes where it was). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(BURNT) || !(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) || rack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack taken = rack.take();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
                rack.dropAll(server, pos);
            }
            DSWork.forget(state, level, pos, newState);
            DSFire.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---- burnable (MakeMediumBurnable(inst, nil, nil, true) + MakeSmallPropagator) ----

    @Override
    public float burnTime(BlockState state) {
        return 20.0F;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.SMALL;
    }

    /** MakeMediumBurnable: fx level 3 even with the small propagator. */
    @Override
    public int fxLevel(BlockState state) {
        return 3;
    }

    /** DefaultBurntStructureFn: canlight = false once burnt. */
    @Override
    public boolean canLight(BlockState state) {
        return !state.getValue(BURNT);
    }

    /**
     * DefaultBurntStructureFn: tagged burnt, dryingrack:OnBurnt dries everything instantly
     * (InstantDry), the rest drops, one hammer hit is left.
     */
    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            rack.dryAllAndDrop(level, pos);
        }
        level.setBlock(pos, state.setValue(BURNT, true), Block.UPDATE_ALL);
        DSWork.setWorkLeft(level, pos, 1);
    }

    // ---- workable: HAMMER, SetWorkLeft(4) ----

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.HAMMER;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 4;
    }

    /** OnHammered: a burning rack is put out, everything on it drops, DropLoot (a quarter when burnt). */
    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        DSFire.extinguish(level, pos);
        DSWork.dropRecipeLoot(level, pos, this, state.getValue(BURNT));
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
