package net.celestiald.dontstarve.world;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.survive.Survival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The DST "flower" (prefabs/flower.lua): a pickable that is removed when picked
 * (remove_when_picked) and gives petals. Picking restores TUNING.SANITY_TINY sanity
 * (onpickedfn); the one-in-a-hundred rose (ROSE_CHANCE .01) pricks the picker for
 * ROSE_DAMAGE. The plant has ten look variants f1..f10 plus the rose, here {@link #VARIANT}
 * 0..9 and 10.
 */
public class FlowerBlock extends PickableBlock {

    /** flower.lua animname: 0..9 are f1..f10, 10 is the rose. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 10);
    public static final int ROSE = 10;
    /** flower.lua ROSE_CHANCE. */
    public static final double ROSE_CHANCE = 0.01;
    /** TUNING.SANITY_TINY, given to the picker. */
    public static final float PICK_SANITY = 5.0F;
    /** TUNING.ROSE_DAMAGE, DS health. */
    public static final float ROSE_DAMAGE = 1.0F;

    public FlowerBlock(Properties properties, Supplier<? extends Item> product) {
        super(properties, product, 0, 0);
        registerDefaultState(defaultBlockState().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    /** setflowertype: a rose at ROSE_CHANCE, else one of the ten flowers. */
    public BlockState randomVariant(RandomSource random) {
        return defaultBlockState().setValue(VARIANT,
                random.nextDouble() < ROSE_CHANCE ? ROSE : random.nextInt(ROSE));
    }

    @Override
    protected void onPicked(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player picker) {
        if (picker == null) {
            return;
        }
        Survival.set(picker, Survival.stats(picker).withSanity(Survival.stats(picker).sanity() + PICK_SANITY));
        if (state.getValue(VARIANT) == ROSE && picker instanceof ServerPlayer) {
            picker.hurt(level.damageSources().cactus(), DSScale.stat(ROSE_DAMAGE));
        }
    }
}
