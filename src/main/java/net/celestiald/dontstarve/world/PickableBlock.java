package net.celestiald.dontstarve.world;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DS "pickable" plant (DST components/pickable.lua): right-click picks one
 * product by hand, then the plant regrows after its regrow time. With a cycle
 * limit it turns barren once the cycles are used up (fertilizing is not
 * ported yet). Digging it up needs a shovel and yields the product (if ripe)
 * plus the dug plant, via its loot table. DS seconds are scaled to the
 * Minecraft day (x2.5).
 */
public class PickableBlock extends BushBlock {

    public static final BooleanProperty PICKED = BooleanProperty.create("picked");
    /** Picks left before the plant goes barren; ignored for plants without a cycle limit. */
    public static final IntegerProperty CYCLES = IntegerProperty.create("cycles", 0, 20);

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    private final Supplier<? extends Item> product;
    private final int regrowTicks;
    private final boolean limitedCycles;

    /**
     * @param regrowDsSeconds DS regrow time (e.g. GRASS_REGROW_TIME 1440)
     * @param maxCycles       DS cycle limit, or 0 for unlimited picks
     */
    public PickableBlock(Properties properties, Supplier<? extends Item> product, int regrowDsSeconds, int maxCycles) {
        super(properties);
        this.product = product;
        this.regrowTicks = Math.round(regrowDsSeconds * net.celestiald.dontstarve.item.DSTorch.DS_TIME_SCALE * 20);
        this.limitedCycles = maxCycles > 0;
        registerDefaultState(stateDefinition.any().setValue(PICKED, false)
                .setValue(CYCLES, limitedCycles ? maxCycles : 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        throw new UnsupportedOperationException("PickableBlock is registered in code only");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PICKED, CYCLES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean isRipe(BlockState state) {
        return !state.getValue(PICKED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!isRipe(state)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            popResource(server, pos, new ItemStack(product.get()));
            server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.2F);
            BlockState picked = state.setValue(PICKED, true);
            if (limitedCycles) {
                picked = picked.setValue(CYCLES, Math.max(0, state.getValue(CYCLES) - 1));
            }
            server.setBlock(pos, picked, Block.UPDATE_ALL);
            if (!limitedCycles || picked.getValue(CYCLES) > 0) {
                server.scheduleTick(pos, this, regrowTicks);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Regrowth after the regrow time. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isRipe(state)) {
            level.setBlock(pos, state.setValue(PICKED, false), Block.UPDATE_ALL);
        }
    }
}
