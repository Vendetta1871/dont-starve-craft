package net.celestiald.dontstarve.fire;

import java.util.OptionalDouble;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DST fueled fire: the campfire (prefabs/campfire.lua) or the fire pit (prefabs/firepit.lua).
 * The fueled component lives in {@link FueledFireBlockEntity}; the block state carries the current
 * fueled section (0 = out, 1 embers ... 4 high) and the light of the fire fx for the model, the
 * light and the fire engine. Right-clicking with a fuel item adds fuel ({@link FuelValues}).
 */
public class FueledFireBlock extends Block implements EntityBlock, DSBurnable, DSWorkable {

    /** fueled:GetCurrentSection, sections = 4. */
    public static final int SECTIONS = 4;
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, SECTIONS);
    /** The light level of the fire fx at the current fuel (0 when out). */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 7);

    /** campfirefire.lua firelevels radius per fx level (= section). */
    private static final double[] FX_RADII = {2, 3, 4, 5};
    /** campfirefire.lua heats per fx level. */
    private static final double[] FX_HEATS = {70, 85, 100, 115};

    /** The two prefabs and their fueled / burnable settings. */
    public enum Kind {
        /** campfire.lua: CAMPFIRE_FUEL_MAX (night+dusk)*1.5, START *.75; propagator ranges/outputs per section; burns out. */
        CAMPFIRE(270, 135, 1.0, true, 0, new double[] {1, 2, 3, 4}, new double[] {2, 5, 5, 10}),
        /** firepit.lua: FIREPIT_FUEL_MAX (night+dusk)*2, START night+dusk, FIREPIT_BONUS_MULT 2, hammered in 4 hits, no propagator. */
        FIREPIT(360, 180, 2.0, false, 4, null, null);

        final double maxFuel;
        final double startFuel;
        final double bonusMult;
        final boolean burnsOut;
        final int hammerHits;
        @Nullable
        final double[] ranges;
        @Nullable
        final double[] outputs;

        Kind(double maxFuel, double startFuel, double bonusMult, boolean burnsOut, int hammerHits,
             @Nullable double[] ranges, @Nullable double[] outputs) {
            this.maxFuel = maxFuel;
            this.startFuel = startFuel;
            this.bonusMult = bonusMult;
            this.burnsOut = burnsOut;
            this.hammerHits = hammerHits;
            this.ranges = ranges;
            this.outputs = outputs;
        }
    }

    private static final VoxelShape CAMPFIRE_SHAPE = box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0);
    private static final VoxelShape FIREPIT_SHAPE = box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);

    public final Kind kind;

    public FueledFireBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(SECTION, 0).setValue(LIGHT, 0));
    }

    /** Block properties of a fueled fire: it lights itself from the blockstate. */
    public static Properties properties(net.minecraft.world.level.material.MapColor color,
                                        net.minecraft.world.level.block.SoundType sound) {
        return Properties.of()
                .mapColor(color)
                .strength(2.0F, 6.0F)
                .sound(sound)
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(LIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SECTION, LIGHT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.CAMPFIRE ? CAMPFIRE_SHAPE : FIREPIT_SHAPE;
    }

    // ---- fueled component ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FueledFireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != DSBlockEntities.FUELED_FIRE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((FueledFireBlockEntity) be).serverTick((ServerLevel) l, p, s);
    }

    /** ACTIONS.ADDFUEL: right-click with a fuel item feeds the fire (fueled:TakeFuelItem). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        OptionalDouble value = FuelValues.of(stack);
        if (value.isEmpty() || !level.dimension().equals(DSRefs.ISLANDS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        return feed((ServerLevel) level, pos, player, stack)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** fueled:TakeFuelItem with one item of {@code stack}; false when it is not fuel or the fire does not take it. */
    public static boolean feed(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        OptionalDouble value = FuelValues.of(stack);
        if (value.isEmpty() || !(level.getBlockEntity(pos) instanceof FueledFireBlockEntity fire)
                || !fire.takeFuel(level, pos, value.getAsDouble())) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 1.2F);
        return true;
    }

    /**
     * What a campfire leaves behind (campfire.lua onfuelchange): charcoal when it was ever fed to the
     * top section, else ash. Without the charcoal item (registered by another part of the mod) the
     * ash drops instead.
     */
    static void dropCampfireLeftovers(ServerLevel level, BlockPos pos, boolean charcoal) {
        Item item = charcoal ? FuelValues.optionalItem("charcoal") : null;
        Block.popResource(level, pos, new ItemStack(item != null ? item : DSItems.ASH.get()));
    }

    /** firepit.lua: lootdropper:SpawnLootPrefab("charcoal"); nothing when the item does not exist (yet). */
    static void dropFirepitCharcoal(ServerLevel level, BlockPos pos) {
        Item item = FuelValues.optionalItem("charcoal");
        if (item != null) {
            Block.popResource(level, pos, new ItemStack(item));
        }
    }

    // ---- burnable and propagator ----

    @Override
    public float burnTime(BlockState state) {
        return state.getValue(SECTION) > 0 ? 1.0F : 0.0F;
    }

    @Override
    public boolean endless(BlockState state) {
        return state.getValue(SECTION) > 0;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.MEDIUM;
    }

    /** The fire only starts from fuel; a torch cannot light it (lighter.lua: "fueldepleted" blocks it, a lit one is not "canlight"). */
    @Override
    public boolean canLight(BlockState state) {
        return false;
    }

    /** The campfire's propagator has acceptsheat false; the fire pit has none. */
    @Override
    public boolean acceptsHeat(BlockState state) {
        return false;
    }

    @Override
    public int fxLevel(BlockState state) {
        return state.getValue(SECTION);
    }

    @Override
    public boolean selfLit() {
        return true;
    }

    @Override
    public double warmth(BlockState state) {
        int section = state.getValue(SECTION);
        return section > 0 ? FX_HEATS[section - 1] : 20.0;
    }

    @Override
    public Spread spread(BlockState state, BlockPos burnPos) {
        int section = state.getValue(SECTION);
        if (kind.ranges == null || section <= 0) {
            return null;
        }
        // campfire.lua PROPAGATE_RANGES / HEAT_OUTPUTS by section; the campfire propagator does not damage.
        return new Spread(kind.ranges[section - 1], kind.outputs[section - 1], false, 0);
    }

    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
    }

    /** onextinguish: InitializeFuelLevel(0), the fuel is gone. */
    @Override
    public void onExtinguish(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof FueledFireBlockEntity fire) {
            fire.initializeFuel(level, pos, 0.0);
        }
    }

    /** Light radius lerped within the section (firefx.lua UpdateRadius) as a light level. */
    static int lightFor(int section, double percent) {
        if (section <= 0) {
            return 0;
        }
        double high = FX_RADII[section - 1];
        double low = section > 1 ? FX_RADII[section - 2] : 0.0;
        double radius = percent * (high - low) + low;
        return (int) Math.min(7, Math.round(radius) + 2);
    }

    // ---- workable ----

    @Nullable
    @Override
    public DSAction workAction(BlockState state) {
        return kind.hammerHits > 0 ? DSAction.HAMMER : null;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return kind.hammerHits;
    }

    /** firepit.lua onhammered: the recipe loot, an ash heap and the pit is gone. */
    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        DSWork.dropRecipeLoot(level, pos, this);
        Block.popResource(level, pos, new ItemStack(DSItems.ASH.get()));
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
            DSWork.forget(state, level, pos, newState);
            DSFire.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The registry id path, for commands. */
    public String id() {
        return BuiltInRegistries.BLOCK.getKey(this).getPath();
    }
}
