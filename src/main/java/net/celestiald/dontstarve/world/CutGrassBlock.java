package net.celestiald.dontstarve.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wild cut-grass tuft (DST "grass"): breaking it yields cut grass (and
 * sometimes flint — our stand-in for the flint that DS grass can hide, see
 * tuning.lua:3528 where grass drops join the flint weight pool), and it
 * slowly spreads onto neighbouring grass the way DST tufts regrow
 * (grass.lua, GRASS_REGROW_TIME).
 */
public class CutGrassBlock extends BushBlock {

    private static final MapCodec<CutGrassBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BlockBehaviour.Properties.CODEC.fieldOf("properties")
                    .forGetter(Block::properties)).apply(instance, CutGrassBlock::new));

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    /** Per-tick spread chance; DS grass regrows in ~3 days, this is the spirit. */
    private static final int SPREAD_CHANCE = 64;

    public CutGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPREAD_CHANCE) != 0) {
            return;
        }
        BlockPos next = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        if (level.getBlockState(next).is(Blocks.GRASS_BLOCK)
                && level.getBlockState(next.above()).isAir()) {
            level.setBlockAndUpdate(next.above(), defaultBlockState());
        }
    }
}
