package net.celestiald.dontstarve.world;

import java.util.ArrayList;
import java.util.List;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.work.DSWork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The DST evergreen (prefabs/evergreens.lua) as one object made of blocks: a trunk
 * column ({@link EvergreenBlock}) with a crown ({@link EvergreenCrownBlock}), carrying
 * the growable stage short / normal / tall / old. Every block of a tree belongs to the
 * base trunk block, which holds the work counter and the growth timer. A tree is felled
 * into a stump, burns into a burnt tree ({@link BurntEvergreenBlock}).
 *
 * <p>Both builds share the code: "evergreen" and the lumpy "evergreen_sparse"
 * (evergreens.lua {@code builds}), which only differ in loot and pinecones.
 */
public final class EvergreenTree {

    public static final int SHORT = 0;
    public static final int NORMAL = 1;
    public static final int TALL = 2;
    public static final int OLD = 3;
    public static final String[] STAGE_NAMES = {"short", "normal", "tall", "old"};

    /** Trunk blocks of a tree stage (our silhouette: DST trees have no block height). */
    private static final int[] TRUNK = {2, 4, 6, 5};
    /** TUNING.EVERGREEN_CHOPS_SMALL / NORMAL / TALL (evergreens.lua SetShort/SetNormal/SetTall); old: SetWorkLeft(1) (SetOld). */
    private static final int[] CHOPS = {5, 10, 15, 1};
    /** TUNING.EVERGREEN_GROW_TIME {base, random} in DS seconds: 1.5 / 5 / 5 / 1 days (day_time 300) +- 0.5 / 2 / 2 / 0.5 days. */
    private static final double[][] GROW_TIME = {{450, 150}, {1500, 600}, {1500, 600}, {300, 150}};
    /** TUNING.PINECONE_GROWTIME {base=0.75*day_time, random=0.25*day_time} (planted_tree.lua startgrowing). */
    private static final double[] PINECONE_GROWTIME = {225, 75};
    /** TUNING.EVERGREEN_REGROWTH.DEAD_DECAY_TIME = total_day_time * 30 (evergreens.lua OnBurnt / make_stump). */
    private static final double DEAD_DECAY_TIME = 480 * 30;
    /** onpineconetask: a burnt tree drops its chance loot 10 DS seconds after burning (evergreens.lua tree_burnt). */
    private static final double BURNT_CONE_DELAY = 10;

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Crown reach: 2 blocks around the trunk and up to y 7 above the base. */
    private static final int REACH = 2;
    private static final int MAX_Y = 7;

    /** Implemented by the trunk blocks of living and burnt trees: a column that belongs to one base. */
    public interface Trunk {
        boolean sparse();
    }

    // ---- stage data ------------------------------------------------------------------

    public static int chops(int stage) {
        return CHOPS[stage];
    }

    /** Number of trunk blocks of a living tree. */
    public static int trunkHeight(int stage) {
        return TRUNK[stage];
    }

    /** A burnt old tree is drawn as a burnt tall tree (old_anims.burnt = "burnt_tall"). */
    public static int burntHeight(int stage) {
        return TRUNK[stage == OLD ? TALL : stage];
    }

    /**
     * Loot per stage (evergreens.lua builds.normal/sparse: short_loot, normal_loot, tall_loot;
     * SetOld: {"pinecone"} when drop_pinecones else {}).
     */
    public static List<Item> loot(boolean sparse, int stage) {
        Item log = Items.SPRUCE_LOG;
        Item cone = DSItems.PINECONE.get();
        return switch (stage) {
            case SHORT -> List.of(log);
            case NORMAL -> sparse ? List.of(log, log) : List.of(log, log, cone);
            case TALL -> sparse ? List.of(log, log, log) : List.of(log, log, log, cone, cone);
            default -> sparse ? List.of() : List.of(cone);
        };
    }

    /** GetRandomWithVariance(base, random): base +- random. */
    private static double variance(RandomSource random, double base, double spread) {
        return base + random.nextDouble() * 2 * spread - spread;
    }

    // ---- blocks ----------------------------------------------------------------------

    public static Block trunk(boolean sparse) {
        return (sparse ? DSBlocks.EVERGREEN_SPARSE : DSBlocks.EVERGREEN).get();
    }

    public static Block crown(boolean sparse) {
        return (sparse ? DSBlocks.EVERGREEN_SPARSE_CROWN : DSBlocks.EVERGREEN_CROWN).get();
    }

    public static Block burnt(boolean sparse) {
        return (sparse ? DSBlocks.EVERGREEN_SPARSE_BURNT : DSBlocks.EVERGREEN_BURNT).get();
    }

    public static Block stump(boolean sparse) {
        return (sparse ? DSBlocks.EVERGREEN_SPARSE_STUMP : DSBlocks.EVERGREEN_STUMP).get();
    }

    /** The lowest block of the trunk column at {@code pos}: it holds the work counter and the timers. */
    public static BlockPos baseOf(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (level.getBlockState(cursor.move(0, -1, 0)).getBlock() instanceof Trunk) {
            pos = cursor.immutable();
        }
        return pos;
    }

    public static boolean isBase(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return !(level.getBlockState(pos.below()).getBlock() instanceof Trunk);
    }

    // ---- silhouette ------------------------------------------------------------------

    /** Layers of the crown per stage: {y, shape}; shape 0 = 3x3, 1 = 3x3 plus the 5-wide cross, 2 = one block. */
    private static final int[][][] LAYERS = {
            {{1, 0}, {2, 0}},
            {{2, 0}, {3, 0}, {4, 1}, {5, 0}},
            {{3, 0}, {4, 0}, {5, 1}, {6, 1}, {7, 0}},
            {{4, 0}, {5, 2}},
    };

    /** Crown cells {dx, y, dz} relative to the base (the trunk column cells are left out). */
    static List<int[]> crownCells(int stage) {
        List<int[]> cells = new ArrayList<>();
        for (int[] layer : LAYERS[stage]) {
            int y = layer[0];
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    int adx = Math.abs(dx);
                    int adz = Math.abs(dz);
                    boolean inside = switch (layer[1]) {
                        case 0 -> adx <= 1 && adz <= 1;
                        case 1 -> (adx <= 1 && adz <= 1) || (adx == 0 || adz == 0);
                        default -> adx == 0 && adz == 0;
                    };
                    if (inside && !(dx == 0 && dz == 0 && y < TRUNK[stage])) {
                        cells.add(new int[]{dx, y, dz});
                    }
                }
            }
        }
        return cells;
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    // ---- building --------------------------------------------------------------------

    /**
     * Places a whole tree with its trunk base at {@code base}; false (nothing placed) if the
     * trunk column is blocked.
     */
    public static boolean place(LevelAccessor level, BlockPos base, int stage, boolean sparse) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= TRUNK[stage]; y++) {
            if (!replaceable(level.getBlockState(cursor.set(base.getX(), base.getY() + y, base.getZ())))) {
                return false;
            }
        }
        level.setBlock(base, EvergreenBlock.state(trunk(sparse), stage), FLAGS);
        buildAbove(level, base, stage, sparse);
        scheduleGrowth(level, base, stage, sparse);
        return true;
    }

    /** Upper trunk blocks and the crown of a tree whose base block is already placed. */
    private static void buildAbove(LevelAccessor level, BlockPos base, int stage, boolean sparse) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState trunk = EvergreenBlock.state(trunk(sparse), stage);
        for (int y = 1; y < TRUNK[stage]; y++) {
            cursor.set(base.getX(), base.getY() + y, base.getZ());
            if (replaceable(level.getBlockState(cursor))) {
                level.setBlock(cursor, trunk, FLAGS);
            }
        }
        for (int[] c : crownCells(stage)) {
            cursor.set(base.getX() + c[0], base.getY() + c[1], base.getZ() + c[2]);
            if (replaceable(level.getBlockState(cursor))) {
                level.setBlock(cursor, EvergreenCrownBlock.state(crown(sparse), c[0], c[1], c[2]), FLAGS);
            }
        }
    }

    /** Removes every block of the tree at {@code base} (no drops); the base block too unless {@code keepBase}. */
    public static void clear(LevelAccessor level, BlockPos base, boolean keepBase) {
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= MAX_Y; y++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    cursor.set(base.getX() + dx, base.getY() + y, base.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    boolean mine;
                    if (dx == 0 && dz == 0) {
                        mine = state.getBlock() instanceof Trunk && (y > 0 || !keepBase)
                                || EvergreenCrownBlock.belongsTo(state, dx, y, dz);
                    } else {
                        mine = EvergreenCrownBlock.belongsTo(state, dx, y, dz);
                    }
                    if (mine) {
                        level.setBlock(cursor, air, FLAGS);
                    }
                }
            }
        }
    }

    // ---- growth ----------------------------------------------------------------------

    /** growable: a stage lasts GetRandomWithVariance(grow_times[stage]) (evergreens.lua growth_stages). */
    public static int growTicks(int stage, RandomSource random) {
        return Math.max(1, DSScale.ticks(variance(random, GROW_TIME[stage][0], GROW_TIME[stage][1])));
    }

    public static void scheduleGrowth(LevelAccessor level, BlockPos base, int stage, boolean sparse) {
        Block block = trunk(sparse);
        if (!level.getBlockTicks().hasScheduledTick(base, block)) {
            level.scheduleTick(base, block, growTicks(stage, level.getRandom()));
        }
    }

    /**
     * growable DoGrowth: the next stage, and after old the loop back to short
     * (growable.loopstages). SetStage resets the chops left to the stage's count.
     */
    public static void grow(ServerLevel level, BlockPos base, BlockState baseState) {
        boolean sparse = ((Trunk) baseState.getBlock()).sparse();
        int stage = (baseState.getValue(EvergreenBlock.STAGE) + 1) % 4;
        DSWork.setWorkLeft(level, base, -1);
        clear(level, base, true);
        level.setBlock(base, baseState.setValue(EvergreenBlock.STAGE, stage), FLAGS);
        buildAbove(level, base, stage, sparse);
        level.scheduleTick(base, baseState.getBlock(), growTicks(stage, level.getRandom()));
    }

    /**
     * planted_tree.lua growtree + handler_growfromseed: a sapling becomes a short tree. The
     * sapling's own block is the base.
     */
    public static void growFromSeed(ServerLevel level, BlockPos pos, boolean sparse) {
        level.setBlock(pos, EvergreenBlock.state(trunk(sparse), SHORT), FLAGS);
        buildAbove(level, pos, SHORT, sparse);
        scheduleGrowth(level, pos, SHORT, sparse);
    }

    /** PINECONE_GROWTIME of a freshly planted sapling. */
    public static int saplingTicks(RandomSource random) {
        return Math.max(1, DSScale.ticks(variance(random, PINECONE_GROWTIME[0], PINECONE_GROWTIME[1])));
    }

    /** The decay timer of stumps and burnt trees: DEAD_DECAY_TIME +- 50 %. */
    public static int decayTicks(RandomSource random) {
        return Math.max(1, DSScale.ticks(variance(random, DEAD_DECAY_TIME, DEAD_DECAY_TIME * 0.5)));
    }

    // ---- felling and burning ---------------------------------------------------------

    /**
     * chop_down_tree: loot drops, the whole tree goes and a stump stays (make_stump).
     * The chopper is the player that landed the last chop.
     */
    public static void chopDown(ServerLevel level, BlockPos base, int stage, boolean sparse) {
        clear(level, base, false);
        level.setBlock(base, stump(sparse).defaultBlockState(), Block.UPDATE_ALL);
        for (Item item : loot(sparse, stage)) {
            Block.popResource(level, base, new net.minecraft.world.item.ItemStack(item));
        }
    }

    /**
     * tree_burnt / OnBurnt: the tree turns into a burnt tree of the same height; 10 DS seconds
     * later it may drop a pinecone (build with drop_pinecones), and it decays after
     * DEAD_DECAY_TIME.
     */
    public static void burn(ServerLevel level, BlockPos base, int stage, boolean sparse) {
        DSWork.setWorkLeft(level, base, -1);
        clear(level, base, false);
        BlockState state = BurntEvergreenBlock.state(burnt(sparse), stage);
        for (int y = 0; y < burntHeight(stage); y++) {
            level.setBlock(base.above(y), state, Block.UPDATE_ALL);
        }
    }

    /** Ticks until the burnt tree's pinecone task (or decay for builds without pinecones). */
    public static int burntConeTicks() {
        return DSScale.ticks(BURNT_CONE_DELAY);
    }

    private EvergreenTree() {
    }
}
