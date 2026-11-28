package net.celestiald.dontstarve.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.clock.DSClock;
import net.celestiald.dontstarve.clock.RunClock;
import net.celestiald.dontstarve.craft.PrototyperBlock;
import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.revive.ResurrectionStatueBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * DST components/regrowthmanager.lua on the islands. A plant registered with
 * AddToRegrowthManager (carrot, flower, reeds) starts a timer when it is removed
 * (picked, burnt, dug). The type's internal clock advances only while its time multiplier
 * is above 0 (the weather or season allows growth); when a timer is due the manager tries to
 * grow a new plant at a random spot within JITTER_RADIUS 20 of the old one:
 * <ul>
 *   <li>no player within ENTITY_POPOUT_RADIUS (64 x 1.2 = 76.8): otherwise the timer waits;</li>
 *   <li>same ground as the original, the plant can survive there, no other object within
 *       EXCLUDE_RADIUS 3, no structure within BASE_RADIUS 20 (TestForRegrow);</li>
 *   <li>a failed try restarts the timer with the full regrow time.</li>
 * </ul>
 * Timers of abandoned islands are dropped. The manager runs every UPDATE_PERIOD 29 DS seconds of
 * run time (RunClock). Not ported: roads (none on the islands) and mod time multipliers.
 */
public final class FloraRegrowth extends SavedData {

    private static final String NAME = "dontstarve_flora_regrowth";
    /** UPDATE_PERIOD, DS seconds. */
    public static final int UPDATE_PERIOD = 29;
    private static final double JITTER_RADIUS = 20.0;
    private static final int EXCLUDE_RADIUS = 3;
    private static final int BASE_RADIUS = 20;
    /** ENTITY_POPOUT_RADIUS = ENTITY_POPIN_RADIUS (64) * 1.2. */
    private static final double MIN_PLAYER_DISTANCE = 64.0 * 1.2;

    /** A plant type the manager regrows (SetRegrowthForType). */
    private record Type(double regrowSeconds, Function<RandomSource, BlockState> plant, Function<DSClock.State, Double> timeMult) {
    }

    private static final Map<String, Type> TYPES = new HashMap<>();

    static {
        // carrot_planted: CARROT_REGROWTH_TIME = day_time * 20 = 6000 s; grows by day, not in winter or snow.
        TYPES.put("carrot_planted", new Type(300.0 * 20, r -> DSBlocks.CARROT_PLANTED.get().defaultBlockState(),
                clock -> clock.phase() == DSClock.Phase.NIGHT || clock.day().season() == DSClock.Season.WINTER ? 0.0 : 1.0));
        // flower: FLOWER_REGROWTH_TIME 30 s; grows only by day, outside winter, not raining, on wet ground
        // (wetness > 1; doubled in spring). The islands have no weather yet, so the ground is never wet
        // and flowers do not regrow until it is ported.
        TYPES.put("flower", new Type(30.0, r -> DSBlocks.FLOWER.get().randomVariant(r),
                clock -> clock.phase() == DSClock.Phase.NIGHT || clock.day().season() == DSClock.Season.WINTER
                        || wetness() <= 1.0 ? 0.0 : clock.day().season() == DSClock.Season.SPRING ? 2.0 : 1.0));
        // reeds: REEDS_REGROWTH_TIME = total_day_time * 5 = 2400 s; only in spring (REEDS_REGROWTH_TIME_BASE_MULT 0).
        TYPES.put("reeds", new Type(480.0 * 5, r -> DSBlocks.REEDS.get().defaultBlockState(),
                clock -> clock.day().season() == DSClock.Season.SPRING ? 1.0 : 0.0));
    }

    /** TheWorld.state.wetness: the ground never gets wet until weather is ported. */
    private static double wetness() {
        return 0.0;
    }

    private record Timer(double due, BlockPos pos) {
    }

    private final Map<String, Double> internal = new HashMap<>();
    private final Map<String, List<Timer>> lists = new HashMap<>();

    public static FloraRegrowth get(ServerLevel islands) {
        return islands.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FloraRegrowth::new, FloraRegrowth::load), NAME);
    }

    /** OnBeginRegrowth: a registered plant was removed at {@code pos}. */
    public static void begin(ServerLevel level, String type, BlockPos pos) {
        if (!level.dimension().equals(DSRefs.ISLANDS) || !TYPES.containsKey(type)) {
            return;
        }
        get(level).add(type, pos);
    }

    private void add(String type, BlockPos pos) {
        double now = internal.getOrDefault(type, 0.0);
        lists.computeIfAbsent(type, k -> new ArrayList<>()).add(new Timer(now + TYPES.get(type).regrowSeconds, pos));
        setDirty();
    }

    /** Timers waiting for a type. */
    public int pending(String type) {
        return lists.getOrDefault(type, List.of()).size();
    }

    /** Runs the manager once per UPDATE_PERIOD of run time. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }
        IslandState island = IslandState.get(overworld);
        long run = island.runTicks();
        int period = DSScale.ticks(UPDATE_PERIOD);
        if (run > 0 && run % period == 0 && islands.players().stream().anyMatch(island::isCurrentRun)) {
            get(islands).update(islands, RunClock.state(server), island.islandIndex(), UPDATE_PERIOD);
        }
    }

    /** RegrowthManager:LongUpdate(dt), dt in DS seconds. */
    public void update(ServerLevel level, DSClock.State clock, int islandIndex, double dt) {
        for (Map.Entry<String, List<Timer>> entry : lists.entrySet()) {
            String key = entry.getKey();
            Type type = TYPES.get(key);
            List<Timer> list = entry.getValue();
            // Timers of an island that is no longer current can never be tended.
            list.removeIf(t -> Math.round((double) t.pos.getX() / IslandState.SPACING) != islandIndex);
            double mult = type.timeMult.apply(clock);
            if (mult <= 0.0 || list.isEmpty()) {
                continue;
            }
            double now = internal.merge(key, dt * mult, Double::sum);
            List<Timer> restarted = new ArrayList<>();
            for (Iterator<Timer> it = list.iterator(); it.hasNext(); ) {
                Timer timer = it.next();
                if (timer.due > now) {
                    continue;
                }
                Status status = regrow(level, type, timer.pos);
                if (status == Status.SUCCESS) {
                    it.remove();
                } else if (status == Status.FAILED) {
                    it.remove();
                    restarted.add(new Timer(now + type.regrowSeconds, timer.pos));
                }
                // CACHE: stays due, tried again next update.
            }
            list.addAll(restarted);
            setDirty();
        }
    }

    private enum Status { SUCCESS, FAILED, CACHE }

    /** DoRegrowth. */
    private static Status regrow(ServerLevel level, Type type, BlockPos origin) {
        RandomSource random = level.random;
        double theta = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * JITTER_RADIUS;
        int x = (int) Math.floor(origin.getX() + 0.5 + radius * Math.cos(theta));
        int z = (int) Math.floor(origin.getZ() + 0.5 - radius * Math.sin(theta));
        for (Player player : level.players()) {
            double dx = player.getX() - x;
            double dy = player.getY() - origin.getY();
            double dz = player.getZ() - z;
            if (dx * dx + dy * dy + dz * dz < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) {
                return Status.CACHE;
            }
        }
        int y = columnTop(level, x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        if (y == Integer.MIN_VALUE) {
            return Status.CACHE;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState plant = type.plant.apply(random);
        Block originGround = level.getBlockState(origin.below()).getBlock();
        if (level.getBlockState(pos.below()).getBlock() != originGround || !plant.canSurvive(level, pos)
                || !level.getBlockState(pos).isAir() || crowded(level, x, z) || structureNear(level, x, z)) {
            return Status.FAILED;
        }
        level.setBlock(pos, plant, Block.UPDATE_ALL);
        return Status.SUCCESS;
    }

    /** The first open block above the ground of a loaded column, or Integer.MIN_VALUE. */
    private static int columnTop(ServerLevel level, int x, int z, Heightmap.Types map) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        // ChunkAccess.getHeight is the Y of the topmost block, the open block is one above it.
        return chunk == null ? Integer.MIN_VALUE : chunk.getHeight(map, x & 15, z & 15) + 1;
    }

    private static boolean naturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.DIRT)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.STONE) || state.is(DSBlocks.POLISHED_MARBLE.get());
    }

    /** TheSim:FindEntities within EXCLUDE_RADIUS: any plant, tree, boulder or other object nearby. */
    private static boolean crowded(ServerLevel level, int x, int z) {
        for (int dx = -EXCLUDE_RADIUS; dx <= EXCLUDE_RADIUS; dx++) {
            for (int dz = -EXCLUDE_RADIUS; dz <= EXCLUDE_RADIUS; dz++) {
                if (dx * dx + dz * dz > EXCLUDE_RADIUS * EXCLUDE_RADIUS) {
                    continue;
                }
                int top = columnTop(level, x + dx, z + dz, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                BlockPos at = new BlockPos(x + dx, top, z + dz);
                if (!level.getBlockState(at).isAir() || !naturalGround(level.getBlockState(at.below()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** REGROWBLOCKER_ONEOF_TAGS structure/wall within BASE_RADIUS: prototypers and the meat effigy. */
    private static boolean structureNear(ServerLevel level, int x, int z) {
        for (int dx = -BASE_RADIUS; dx <= BASE_RADIUS; dx++) {
            for (int dz = -BASE_RADIUS; dz <= BASE_RADIUS; dz++) {
                if (dx * dx + dz * dz > BASE_RADIUS * BASE_RADIUS) {
                    continue;
                }
                int top = columnTop(level, x + dx, z + dz, Heightmap.Types.WORLD_SURFACE);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                Block block = level.getBlockState(new BlockPos(x + dx, top - 1, z + dz)).getBlock();
                if (block instanceof PrototyperBlock || block instanceof ResurrectionStatueBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag times = new CompoundTag();
        internal.forEach(times::putDouble);
        tag.put("internal", times);
        ListTag timers = new ListTag();
        lists.forEach((type, list) -> list.forEach(t -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("type", type);
            entry.putDouble("due", t.due);
            entry.putLong("pos", t.pos.asLong());
            timers.add(entry);
        }));
        tag.put("timers", timers);
        return tag;
    }

    public static FloraRegrowth load(CompoundTag tag, HolderLookup.Provider registries) {
        FloraRegrowth state = new FloraRegrowth();
        CompoundTag times = tag.getCompound("internal");
        for (String key : times.getAllKeys()) {
            state.internal.put(key, times.getDouble(key));
        }
        for (Tag raw : tag.getList("timers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            state.lists.computeIfAbsent(entry.getString("type"), k -> new ArrayList<>())
                    .add(new Timer(entry.getDouble("due"), BlockPos.of(entry.getLong("pos"))));
        }
        return state;
    }

    private FloraRegrowth() {
    }
}
