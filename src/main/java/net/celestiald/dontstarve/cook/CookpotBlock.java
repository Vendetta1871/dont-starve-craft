package net.celestiald.dontstarve.cook;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSFire;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Crock Pot (prefabs/cookpot.lua): right-click an idle pot to fill it, a full pot cooks on the
 * Cook button, a finished dish is harvested by right-click. Hammered in 4 hits (half the recipe back),
 * MakeMediumBurnable(inst, nil, nil, true) + MakeSmallPropagator: burns 20 s, and what is left is a
 * burnt wreck that no longer cooks (DefaultBurntStructureFn), one hammer hit from gone.
 */
public class CookpotBlock extends Block implements EntityBlock, DSBurnable, DSWorkable {

    /** The stewer state: nothing, cooking_loop, idle_full. */
    public enum Phase implements StringRepresentable {
        EMPTY("empty"), COOKING("cooking"), DONE("done");

        private final String id;

        Phase(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
    /** The "burnt" tag of DefaultBurntStructureFn. */
    public static final BooleanProperty BURNT = BooleanProperty.create("burnt");

    private static final VoxelShape SHAPE = box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

    public CookpotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.EMPTY).setValue(BURNT, false));
    }

    /** cookpot.lua: the pot light (radius .6, intensity .5) is on while cooking. */
    static int lightLevel(BlockState state) {
        return state.getValue(PHASE) == Phase.COOKING ? 3 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE, BURNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ---- stewer / container ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookpotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != CookInit.COOKPOT_ENTITY.get()) {
            return null;
        }
        return (l, p, s, be) -> ((CookpotBlockEntity) be).serverTick((ServerLevel) l, p, s);
    }

    /** Right-click: open an idle pot, harvest a finished dish; a cooking or burnt pot does nothing. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(BURNT)) {
            return InteractionResult.PASS;
        }
        switch (state.getValue(PHASE)) {
            case EMPTY -> {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof CookpotBlockEntity be) {
                    player.openMenu(be);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            case DONE -> {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof CookpotBlockEntity be) {
                    be.harvest(player instanceof ServerPlayer sp ? sp : null);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            default -> {
                return InteractionResult.PASS;
            }
        }
    }

    // ---- burnable and propagator (MakeMediumBurnable structure + MakeSmallPropagator) ----

    @Override
    public float burnTime(BlockState state) {
        return state.getValue(BURNT) ? 0.0F : 20.0F;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.SMALL;
    }

    /** MakeMediumBurnable: SetFXLevel(3). */
    @Override
    public int fxLevel(BlockState state) {
        return 3;
    }

    @Override
    public boolean canLight(BlockState state) {
        return !state.getValue(BURNT);
    }

    /** DefaultBurntStructureFn: burnt tag, container emptied and removed, stewer stopped (the dish pops out), one hit left. */
    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CookpotBlockEntity be) {
            be.burnDown(level, pos);
        }
        level.setBlock(pos, state.setValue(BURNT, true).setValue(PHASE, Phase.EMPTY), Block.UPDATE_ALL);
        DSWork.setWorkLeft(level, pos, 1);
    }

    // ---- workable: hammer, 4 hits ----

    @Nullable
    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.HAMMER;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 4;
    }

    /**
     * cookpot.lua onhammered: put out the fire, harvest a finished dish, drop the loaded ingredients,
     * the recipe loot (half; a burnt pot a quarter) and the pot is gone.
     */
    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        DSFire.extinguish(level, pos);
        if (!state.getValue(BURNT) && level.getBlockEntity(pos) instanceof CookpotBlockEntity be) {
            be.dropAll(level, pos);
        }
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            // Removed any other way (creative break): nothing stays loaded in a vanished pot.
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof CookpotBlockEntity be) {
                be.dropAll((ServerLevel) level, pos);
            }
            DSWork.forget(state, level, pos, newState);
            DSFire.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
