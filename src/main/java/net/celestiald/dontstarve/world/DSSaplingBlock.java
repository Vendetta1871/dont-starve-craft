package net.celestiald.dontstarve.world;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A DS sapling that grows into the stylised Don't Starve tree silhouette:
 * a straight 4-5 block trunk with a flat-topped leaf crown (no vanilla oak
 * blob). Grows on random tick and instantly under bone meal.
 */
public class DSSaplingBlock extends BushBlock implements BonemealableBlock {

    private static final MapCodec<DSSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BlockBehaviour.Properties.CODEC.fieldOf("properties")
                    .forGetter(Block::properties)).apply(instance, DSSaplingBlock::new));

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
    private static final int GROW_CHANCE = 16;

    public DSSaplingBlock(BlockBehaviour.Properties properties) {
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
        if (random.nextInt(GROW_CHANCE) == 0) {
            growTree(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        growTree(level, pos, random);
    }

    /** Places the DS-style tree with its trunk base at {@code pos}. */
    public static void growTree(Level level, BlockPos pos, RandomSource random) {
        growTree(level, level, pos, random);
    }

    /** Worldgen overload: {@link WorldGenLevel} is a reader and writer, not a Level. */
    public static void growTree(LevelReader level, LevelWriter writer, BlockPos pos, RandomSource random) {
        int trunk = 4 + random.nextInt(2);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // The trunk column must be clear (air or replaceable) up to the crown.
        for (int y = 1; y <= trunk + 2; y++) {
            BlockState above = level.getBlockState(cursor.set(pos.getX(), pos.getY() + y, pos.getZ()));
            if (!above.isAir() && !above.canBeReplaced()) {
                return;
            }
        }
        writer.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        for (int y = 1; y <= trunk; y++) {
            writer.setBlock(cursor.set(pos.getX(), pos.getY() + y, pos.getZ()),
                    Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        // Crown: two 3x3 layers, a 5x5 cross, flat 3x3 top — the DS silhouette.
        int crownY = trunk;
        fillLayer(level, writer, cursor, pos, crownY - 1, 1);
        fillLayer(level, writer, cursor, pos, crownY, 1);
        fillCross(level, writer, cursor, pos, crownY + 1);
        fillLayer(level, writer, cursor, pos, crownY + 2, 1);
    }

    private static void fillLayer(LevelReader level, LevelWriter writer, BlockPos.MutableBlockPos cursor,
                                  BlockPos base, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                placeLeafIfFree(level, writer,
                        cursor.set(base.getX() + dx, base.getY() + y, base.getZ() + dz));
            }
        }
    }

    private static void fillCross(LevelReader level, LevelWriter writer,
                                  BlockPos.MutableBlockPos cursor, BlockPos base, int y) {
        fillLayer(level, writer, cursor, base, y, 1);
        for (int d = -2; d <= 2; d++) {
            placeLeafIfFree(level, writer, cursor.set(base.getX() + d, base.getY() + y, base.getZ()));
            placeLeafIfFree(level, writer, cursor.set(base.getX(), base.getY() + y, base.getZ() + d));
        }
    }

    private static void placeLeafIfFree(LevelReader level, LevelWriter writer, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) {
            writer.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
