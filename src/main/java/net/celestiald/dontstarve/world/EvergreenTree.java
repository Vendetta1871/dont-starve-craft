package net.celestiald.dontstarve.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The DS evergreen (DST "evergreen") built from vanilla spruce blocks in the
 * stylised Don't Starve silhouette: a straight 4-5 block trunk with a
 * flat-topped crown. Leaves are persistent (no decay) and drop nothing on
 * the islands (see DSBlockDrops). Growth stages, stumps and pinecones are
 * not ported yet.
 */
public final class EvergreenTree {

    private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.SPRUCE_LEAVES.defaultBlockState()
            .setValue(LeavesBlock.PERSISTENT, true);

    /** Places the tree with its trunk base at {@code pos}; no-op if the column is blocked. */
    public static void place(LevelReader level, LevelWriter writer, BlockPos pos, RandomSource random) {
        int trunk = 4 + random.nextInt(2);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= trunk + 2; y++) {
            BlockState above = level.getBlockState(cursor.set(pos.getX(), pos.getY() + y, pos.getZ()));
            if (!above.isAir() && !above.canBeReplaced()) {
                return;
            }
        }
        for (int y = 0; y < trunk; y++) {
            writer.setBlock(cursor.set(pos.getX(), pos.getY() + y, pos.getZ()), LOG, Block.UPDATE_KNOWN_SHAPE);
        }
        // Crown: two 3x3 layers, a 5x5 cross, flat 3x3 top — the DS silhouette.
        int crownY = trunk - 1;
        layer(level, writer, cursor, pos, crownY - 1);
        layer(level, writer, cursor, pos, crownY);
        layer(level, writer, cursor, pos, crownY + 1);
        for (int d = -2; d <= 2; d++) {
            leaf(level, writer, cursor.set(pos.getX() + d, pos.getY() + crownY + 1, pos.getZ()));
            leaf(level, writer, cursor.set(pos.getX(), pos.getY() + crownY + 1, pos.getZ() + d));
        }
        layer(level, writer, cursor, pos, crownY + 2);
    }

    private static void layer(LevelReader level, LevelWriter writer, BlockPos.MutableBlockPos cursor,
                              BlockPos base, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                leaf(level, writer, cursor.set(base.getX() + dx, base.getY() + y, base.getZ() + dz));
            }
        }
    }

    private static void leaf(LevelReader level, LevelWriter writer, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced()) {
            writer.setBlock(pos, LEAVES, Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private EvergreenTree() {
    }
}
