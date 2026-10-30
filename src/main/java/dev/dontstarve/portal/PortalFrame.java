package dev.dontstarve.portal;

import dev.dontstarve.init.DSBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/**
 * Detection and ignition of the "П"-shaped marble portal frame:
 * a horizontal top bar of marble blocks with two vertical legs of equal
 * length dropping from its ends. The bottom is open (no bottom bar).
 * The interior is a flat rectangle of air that gets filled with portal blocks.
 */
public final class PortalFrame {

    private static final int MIN_INTERIOR_WIDTH = 2;
    private static final int MIN_INTERIOR_HEIGHT = 3;
    private static final int MAX_CLEAR_SIZE = 512;

    /**
     * A validated frame. {@code topMin}/{@code topMax} are the two top corners
     * of the top bar (topMin has the smaller coordinate along the axis),
     * {@code bottomY} is the row of the leg bottoms (inclusive).
     */
    public record Frame(BlockPos topMin, BlockPos topMax, int bottomY, Direction.Axis axis) {

        /** Deterministic identifier of this frame, stable from any starting block. */
        public BlockPos anchor() {
            return topMin;
        }

        public List<BlockPos> interior() {
            List<BlockPos> positions = new ArrayList<>();
            int topY = topMin.getY() - 1;
            for (int y = bottomY; y <= topY; y++) {
                if (axis == Direction.Axis.X) {
                    for (int x = topMin.getX() + 1; x < topMax.getX(); x++) {
                        positions.add(new BlockPos(x, y, topMin.getZ()));
                    }
                } else {
                    for (int z = topMin.getZ() + 1; z < topMax.getZ(); z++) {
                        positions.add(new BlockPos(topMin.getX(), y, z));
                    }
                }
            }
            return positions;
        }

        public boolean isLit(Level level) {
            for (BlockPos pos : interior()) {
                if (level.getBlockState(pos).is(DSBlocks.MARBLE_PORTAL.get())) {
                    return true;
                }
            }
            return false;
        }

        /** Ignites the frame if the interior is completely empty. */
        public boolean light(ServerLevel level) {
            List<BlockPos> interior = interior();
            for (BlockPos pos : interior) {
                if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
            // Two-phase ignition: place all portal blocks without neighbor
            // updates first, then notify once the whole interior is portal.
            // Otherwise the first block placed triggers neighborChanged on
            // its still-unlit neighbors, whose frame lookup fails mid-ignition.
            BlockState portal = DSBlocks.MARBLE_PORTAL.get().defaultBlockState();
            for (BlockPos pos : interior) {
                level.setBlock(pos, portal, Block.UPDATE_CLIENTS);
            }
            for (BlockPos pos : interior) {
                level.updateNeighborsAt(pos, portal.getBlock());
            }
            return true;
        }
    }

    public static boolean isFrameBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(DSTags.MARBLE);
    }

    public static boolean isPortal(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(DSBlocks.MARBLE_PORTAL.get());
    }

    /**
     * Finds the frame that the given marble or portal block belongs to.
     * The result is deterministic: the same frame is returned regardless of
     * which of its blocks is passed in.
     */
    public static Optional<Frame> find(Level level, BlockPos start) {
        if (!isFrameBlock(level, start) && !isPortal(level, start)) {
            return Optional.empty();
        }
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Optional<Frame> found = findWithAxis(level, start, axis);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<Frame> findWithAxis(Level level, BlockPos start, Direction.Axis axis) {
        BlockPos cursor = start;
        if (isPortal(level, cursor)) {
            // Walk up through the portal interior to the top bar.
            while (isPortal(level, cursor)) {
                cursor = cursor.above();
            }
            if (!isFrameBlock(level, cursor)) {
                return Optional.empty();
            }
        } else {
            while (isFrameBlock(level, cursor.above())) {
                cursor = cursor.above();
            }
        }
        BlockPos topCursor = cursor;

        Direction dirMin = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction dirMax = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos min = topCursor;
        BlockPos max = topCursor;
        while (isFrameBlock(level, min.relative(dirMin))) {
            min = min.relative(dirMin);
        }
        while (isFrameBlock(level, max.relative(dirMax))) {
            max = max.relative(dirMax);
        }
        int span = axis == Direction.Axis.X ? max.getX() - min.getX() : max.getZ() - min.getZ();
        if (span < MIN_INTERIOR_WIDTH + 1) {
            return Optional.empty();
        }

        BlockPos minFoot = min;
        while (isFrameBlock(level, minFoot.below())) {
            minFoot = minFoot.below();
        }
        BlockPos maxFoot = max;
        while (isFrameBlock(level, maxFoot.below())) {
            maxFoot = maxFoot.below();
        }
        if (minFoot.getY() != maxFoot.getY()) {
            return Optional.empty();
        }
        int bottomY = minFoot.getY();
        if (topCursor.getY() - bottomY < MIN_INTERIOR_HEIGHT) {
            return Optional.empty();
        }

        // The start block must lie within this frame's plane and vertical range.
        if (axis == Direction.Axis.X && start.getZ() != min.getZ()) {
            return Optional.empty();
        }
        if (axis == Direction.Axis.Z && start.getX() != min.getX()) {
            return Optional.empty();
        }
        if (start.getY() > topCursor.getY() || start.getY() < bottomY) {
            return Optional.empty();
        }

        Frame frame = new Frame(min, max, bottomY, axis);
        for (BlockPos pos : frame.interior()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(DSBlocks.MARBLE_PORTAL.get())) {
                return Optional.empty();
            }
        }
        return Optional.of(frame);
    }

    /** Removes the connected cluster of portal blocks containing {@code seed}. */
    public static void clearContiguous(ServerLevel level, BlockPos seed) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(seed);
        seen.add(seed);
        while (!queue.isEmpty() && seen.size() < MAX_CLEAR_SIZE) {
            BlockPos pos = queue.poll();
            if (!isPortal(level, pos)) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
    }

    private PortalFrame() {
    }
}
