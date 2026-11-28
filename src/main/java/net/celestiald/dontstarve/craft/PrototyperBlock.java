package net.celestiald.dontstarve.craft;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.fire.DSFire;
import net.celestiald.dontstarve.fire.DSPropagator;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.celestiald.dontstarve.work.DSAction;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * A DST prototyper (components/prototyper.lua): standing within
 * RESEARCH_MACHINE_DIST of it grants its tech trees (TUNING.PROTOTYPER_TREES).
 * Structures are not hand-breakable in DS: HAMMER, 4 hits, then half the recipe
 * comes back (scienceprototyper.lua onhammered, lootdropper GetRecipeLoot).
 */
public class PrototyperBlock extends Block implements DSWorkable, DSBurnable {

    /** The "burnt" tag of DefaultBurntStructureFn: the machine is a charred wreck that no longer prototypes. */
    public static final BooleanProperty BURNT = BooleanProperty.create("burnt");

    private final Map<String, Integer> trees;
    private final SoundEvent ding;

    public PrototyperBlock(Properties properties, Map<String, Integer> trees, SoundEvent ding) {
        super(properties);
        this.trees = Map.copyOf(trees);
        this.ding = ding;
        registerDefaultState(stateDefinition.any().setValue(BURNT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BURNT);
    }

    // ---- scienceprototyper.lua: MakeLargeBurnable(inst, nil, nil, true) + MakeLargePropagator ----

    /** MakeLargeBurnable's default burn time. */
    @Override
    public float burnTime(BlockState state) {
        return 30.0F;
    }

    @Override
    public DSPropagator propagator(BlockState state) {
        return DSPropagator.LARGE;
    }

    /** DefaultBurntStructureFn: canlight = false once burnt. */
    @Override
    public boolean canLight(BlockState state) {
        return !state.getValue(BURNT);
    }

    /** DefaultBurntStructureFn: tagged burnt, the prototyper is removed, one hammer hit is left. */
    @Override
    public void onBurnt(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BURNT, true), Block.UPDATE_ALL);
        DSWork.setWorkLeft(level, pos, 1);
    }

    public Map<String, Integer> trees() {
        return trees;
    }

    /** prototyper:Activate: the machine reacts to a prototype being built at it. */
    public void activate(Level level, BlockPos pos) {
        level.playSound(null, pos, ding, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public DSAction workAction(BlockState state) {
        return DSAction.HAMMER;
    }

    @Override
    public int maxWork(BlockState state, Level level, BlockPos pos) {
        return 4;
    }

    @Override
    public void onWorkFinished(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker) {
        // onhammered: a burning machine is put out first, then DropLoot (burnt: a quarter of the recipe, maybe charcoal).
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        DSWork.forget(state, level, pos, newState);
        if (!newState.is(this)) {
            DSFire.forget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
