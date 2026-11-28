package net.celestiald.dontstarve.world;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DS "pickable" plant (DST components/pickable.lua): right-click picks one
 * product by hand, then the plant regrows after its regrow time.
 *
 * <ul>
 *   <li>Cycles (pickable.lua Pick): a pick only uses up a cycle when the plant was
 *       transplanted (dug up and replanted); wild plants never run out. A transplanted
 *       plant starts barren (ontransplantfn MakeBarren) and stays so until fertilized
 *       (pickable.lua Fertilize: cycles back to the maximum, then empty and regrowing).
 *       Fertilizer items do not exist yet; {@link #fertilize} is the hook.</li>
 *   <li>Dig: DIG with a shovel, one hit (workable 1), via the block's loot table.</li>
 *   <li>Burnable per prefab ({@link #burnable}); burning leaves ash (DefaultBurntFn).</li>
 *   <li>{@link #removedWhenPicked}: pickable.remove_when_picked (carrot, flower); the plant
 *       is gone and, if registered with {@link FloraRegrowth}, the world grows a new one.</li>
 * </ul>
 * DS seconds are scaled to the Minecraft day (x2.5).
 */
public class PickableBlock extends BushBlock implements DSWorkable, DSBurnable {

    /** The plant is empty: picked and regrowing, or barren. */
    public static final BooleanProperty PICKED = BooleanProperty.create("picked");
    /** pickable:IsBarren: no cycles left, never regrows until fertilized. Implies picked. */
    public static final BooleanProperty BARREN = BooleanProperty.create("barren");
    /** pickable.transplanted: placed from a dug-up item, so picks use up cycles. */
    public static final BooleanProperty TRANSPLANTED = BooleanProperty.create("transplanted");
    /** pickable.cycles_left of a transplanted plant with a cycle limit. */
    public static final IntegerProperty CYCLES = IntegerProperty.create("cycles", 0, 20);

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    /** Regrow time in ticks from the cycles already used and a random source. */
    public interface Regrow {
        int ticks(int cyclesPassed, RandomSource random);
    }

    private final Supplier<? extends Item> product;
    private Regrow regrow;
    private final int maxCycles;
    private int extraCycles;
    private float burnTime = 20.0F;
    private DSPropagator propagator = DSPropagator.SMALL;
    @Nullable
    private String regrowthType;
    private boolean removedWhenPicked;
    private boolean diggable = true;

    /**
     * @param regrowDsSeconds DS regrow time (e.g. GRASS_REGROW_TIME 1440)
     * @param maxCycles       DS cycle limit (max_cycles), or 0 for unlimited picks
     */
    public PickableBlock(Properties properties, Supplier<? extends Item> product, int regrowDsSeconds, int maxCycles) {
        super(properties);
        this.product = product;
        this.regrow = (passed, random) -> DSScale.ticks(regrowDsSeconds);
        this.maxCycles = maxCycles;
        registerDefaultState(stateDefinition.any().setValue(PICKED, false).setValue(BARREN, false)
                .setValue(TRANSPLANTED, false).setValue(CYCLES, maxCycles));
    }

    /** pickable.getregentimefn. */
    public PickableBlock regrow(Regrow regrow) {
        this.regrow = regrow;
        return this;
    }

    /** max_cycles = base + math.random(extra) (berrybush.lua normal_postinit), rolled per position. */
    public PickableBlock extraCycles(int extra) {
        this.extraCycles = extra;
        return this;
    }

    /** MakeSmall/Medium/LargeBurnable time (DS seconds) and the propagator preset. */
    public PickableBlock burnable(float burnTimeDsSeconds, DSPropagator propagator) {
        this.burnTime = burnTimeDsSeconds;
        this.propagator = propagator;
        return this;
    }

    /** AddToRegrowthManager: when removed, the world regrows one of this type later. */
    public PickableBlock regrowth(String type) {
        this.regrowthType = type;
        return this;
    }

    /** pickable.remove_when_picked. */
    public PickableBlock removedWhenPicked() {
        this.removedWhenPicked = true;
        return this;
    }

    /** No workable component (reeds cannot be dug). */
    public PickableBlock notDiggable() {
        this.diggable = false;
        return this;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        throw new UnsupportedOperationException("PickableBlock is registered in code only");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PICKED, BARREN, TRANSPLANTED, CYCLES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean isRipe(BlockState state) {
        return !state.getValue(PICKED);
    }

    /** The cycle limit of this plant at a position (max_cycles). */
    public int maxCycles(BlockPos pos) {
        return maxCycles + (extraCycles > 0 ? 1 + Math.floorMod(pos.hashCode(), extraCycles) : 0);
    }

    private boolean limited() {
        return maxCycles > 0;
    }

    /** The state of a plant placed from a dug-up item: transplanted and barren (pickable.lua OnTransplant). */
    public BlockState transplantedState() {
        return defaultBlockState().setValue(TRANSPLANTED, true).setValue(PICKED, true).setValue(BARREN, true)
                .setValue(CYCLES, 0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!isRipe(state)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            pick(server, pos, state, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** pickable:Pick: the product drops, the plant is emptied and regrows (or is gone, or barren). */
    public boolean pick(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player picker) {
        if (!isRipe(state)) {
            return false;
        }
        popResource(level, pos, new ItemStack(product.get()));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.2F);
        onPicked(level, pos, state, picker);
        if (removedWhenPicked) {
            level.removeBlock(pos, false);
            return true;
        }
        BlockState picked = state.setValue(PICKED, true);
        if (state.getValue(TRANSPLANTED) && limited()) {
            picked = picked.setValue(CYCLES, Math.max(0, state.getValue(CYCLES) - 1));
        }
        boolean barren = state.getValue(TRANSPLANTED) && limited() && picked.getValue(CYCLES) == 0;
        level.setBlock(pos, picked.setValue(BARREN, barren), Block.UPDATE_ALL);
        if (!barren) {
            level.scheduleTick(pos, this, regrowTicks(level, pos, picked));
        }
        return true;
    }

    /** pickable.onpickedfn of the prefab; the product has already dropped. */
    protected void onPicked(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player picker) {
    }

    private int regrowTicks(ServerLevel level, BlockPos pos, BlockState state) {
        int passed = state.getValue(TRANSPLANTED) && limited()
                ? Math.max(0, maxCycles(pos) - state.getValue(CYCLES)) : 0;
        return regrow.ticks(passed, level.random);
    }

    /**
     * pickable:Fertilize: cycles back to the maximum, then the plant is emptied and starts
     * regrowing (MakeEmpty). Meant for fertilizer items once they exist.
     */
    public boolean fertilize(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(BARREN)) {
            return false;
        }
        BlockState empty = state.setValue(BARREN, false).setValue(PICKED, true)
                .setValue(CYCLES, limited() ? maxCycles(pos) : 0);
        level.setBlock(pos, empty, Block.UPDATE_ALL);
        level.scheduleTick(pos, this, regrowTicks(level, pos, empty));
        return true;
    }

    @Override
    @Nullable
    public DSAction workAction(BlockState state) {
        return diggable ? DSAction.DIG : null;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 1;
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        dropResources(state, level, pos, null, worker, worker != null ? worker.getMainHandItem() : ItemStack.EMPTY);
        level.destroyBlock(pos, false, worker);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return DSWork.destroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        return DSWork.onDestroyedByPlayer(state, level, pos, player)
                && super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    /**
     * A plant that is placed, or full again, has no regrow timer: drop a pending regrow tick left
     * from a plant that stood here before (a scheduled tick outlives a replacement by the same
     * block, and would end a later regrow too early).
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && (!oldState.is(this) || !state.getValue(PICKED))) {
            server.getBlockTicks().clearArea(net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(pos, pos));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        DSWork.forget(state, level, pos, newState);
        if (regrowthType != null && level instanceof ServerLevel server && !newState.is(this)) {
            FloraRegrowth.begin(server, regrowthType, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Regrowth after the regrow time (pickable.lua Regen). Barren plants never regrow. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PICKED) && !state.getValue(BARREN)) {
            level.setBlock(pos, state.setValue(PICKED, false), Block.UPDATE_ALL);
        }
    }

    // DSBurnable: Make*Burnable / Make*Propagator of the prefab; DefaultBurntFn leaves ash.

    @Override
    public float burnTime(BlockState state) {
        return burnTime;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return propagator;
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        DSBurnable.defaultBurnt(level, pos);
    }
}
