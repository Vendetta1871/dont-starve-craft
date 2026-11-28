package net.celestiald.dontstarve.fire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.clock.DSClock;
import net.celestiald.dontstarve.clock.RunClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The DST fire engine for {@link DSBurnable} blocks on the islands: the burnable component
 * (ignite, burn time, smouldering, extinguish, onburnt) and the propagator component (heat
 * spread, flashpoint, decay, fire damage), with the fire fx light and the fire heater.
 *
 * <p>State lives in {@link FireData} (one entry per burnable holding heat, smouldering or
 * burning). The propagator runs every {@code PROPAGATOR_DT} = 0.5 DS s (25 ticks); entries in
 * unloaded chunks wait until they load. Callers use the static API: {@link #ignite},
 * {@link #extinguish}, {@link #smoulder}, {@link #addHeat}, {@link #light} and the queries.
 */
public final class DSFire {

    /** fire.lua heater heats by fx level (the generic fire fx). */
    public static final double[] FIRE_HEATS = {30, 70, 120, 180, 220, 240};
    /** fire.lua light radius by fx level. */
    public static final double[] FIRE_RADII = {2, 3, 4, 5, 6, 7};

    /** propagator.lua PROPAGATOR_DT in DS seconds. */
    private static final double DT = 0.5;
    private static final int STEP_TICKS = DSScale.ticks(DT);
    /** DS seconds per game tick. */
    public static final double DS_PER_TICK = 1.0 / (20.0 * DSScale.TIME);
    /** propagator.lua TARGET: vertical reach of the planar scan (trees are tall). */
    private static final int SCAN_Y = 4;
    /** burnable.lua SmolderUpdate looks at propagators this far around. */
    private static final double SMOLDER_RADIUS = 12.0;
    /** temperature.lua ZERO_DISTANCE: heaters reach this far. */
    private static final double HEATER_RANGE = 10.0;
    /** tuning.lua SPRING_FIRE_RANGE_MOD. */
    private static final double SPRING_FIRE_RANGE_MOD = 0.67;
    /** health.lua fire_timestart: fire damage starts after this long in the fire (DS s). */
    private static final double FIRE_TIME_START = 1.0;
    /** health.lua FIRE_TIMEOUT: the fire damage run ends this long after the last hit (DS s). */
    private static final double FIRE_TIMEOUT = 0.5;

    /** First and last tick of each creature's current run of fire damage. */
    private static final Map<UUID, long[]> FIRE_RUNS = new HashMap<>();

    // ---- queries ----

    public static boolean isActive(Level level) {
        return level instanceof ServerLevel && level.dimension().equals(DSRefs.ISLANDS);
    }

    /** The block of the burn position at {@code pos} (a tree trunk maps to its base). */
    @Nullable
    private static DSBurnable burnable(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DSBurnable b ? b : null;
    }

    /** The position that holds the fire of the object at {@code pos}, or null when it is not burnable. */
    @Nullable
    public static BlockPos burnPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DSBurnable b)) {
            return null;
        }
        BlockPos key = b.burnPos(state, level, pos);
        return level.getBlockState(key).getBlock() instanceof DSBurnable ? key : null;
    }

    public static boolean isBurning(Level level, BlockPos pos) {
        FireData.Entry e = entry(level, pos);
        return e != null && e.burning;
    }

    public static boolean isSmoldering(Level level, BlockPos pos) {
        FireData.Entry e = entry(level, pos);
        return e != null && e.smoldering();
    }

    /** propagator.currentheat of the object at {@code pos}. */
    public static double heat(Level level, BlockPos pos) {
        FireData.Entry e = entry(level, pos);
        return e == null ? 0.0 : e.heat;
    }

    @Nullable
    private static FireData.Entry entry(Level level, BlockPos pos) {
        if (!isActive(level)) {
            return null;
        }
        BlockPos key = burnPos(level, pos);
        return key == null ? null : FireData.get((ServerLevel) level).at(key);
    }

    /** The "canlight" tag: a burnable that is not burning and may be lit by hand. */
    public static boolean canLight(Level level, BlockPos pos) {
        BlockPos key = isActive(level) ? burnPos(level, pos) : null;
        if (key == null) {
            return false;
        }
        BlockState state = level.getBlockState(key);
        DSBurnable b = (DSBurnable) state.getBlock();
        return b.canLight(state) && (b.burnTime(state) > 0 || b.endless(state)) && !isBurning(level, key);
    }

    // ---- burnable ----

    /**
     * ACTIONS.LIGHT with a lighter (lighter.lua Light): ignites the target when it can be lit.
     * Returns whether it caught fire.
     */
    public static boolean light(ServerLevel level, BlockPos pos, @Nullable Entity doer) {
        return canLight(level, pos) && ignite(level, pos, doer);
    }

    /** Burnable:Ignite. Fails when the object is not burnable, cannot burn in its state or already burns. */
    public static boolean ignite(ServerLevel level, BlockPos pos, @Nullable Entity doer) {
        BlockPos key = isActive(level) ? burnPos(level, pos) : null;
        if (key == null) {
            return false;
        }
        BlockState state = level.getBlockState(key);
        DSBurnable b = (DSBurnable) state.getBlock();
        boolean endless = b.endless(state);
        if (b.burnTime(state) <= 0 && !endless) {
            return false;
        }
        FireData data = FireData.get(level);
        FireData.Entry e = data.getOrCreate(key);
        if (e.burning) {
            return false;
        }
        if (e.smoldering()) {
            // StopSmoldering: the propagator is reset.
            e.smolder = -1;
            e.heat = 0;
            e.paused = false;
        }
        e.burning = true;
        e.burnEnd = endless ? -1 : level.getGameTime() + DSScale.ticks(b.burnTime(state));
        e.warmth = b.warmth(state);
        placeLight(level, key, state, b, e);
        level.playSound(null, key, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
        data.setDirty();
        b.onIgnite(level, key, state);
        return true;
    }

    /** Burnable:Extinguish: puts out the fire (and any smouldering) without burning the object down. */
    public static boolean extinguish(ServerLevel level, BlockPos pos) {
        BlockPos key = isActive(level) ? burnPos(level, pos) : null;
        if (key == null) {
            return false;
        }
        FireData data = FireData.get(level);
        FireData.Entry e = data.at(key);
        if (e == null) {
            return false;
        }
        boolean was = e.burning || e.smoldering();
        if (e.smoldering()) {
            e.smolder = -1;
            e.heat = 0;
            e.paused = false;
        }
        if (e.burning) {
            stopBurning(level, key, e);
            BlockState state = level.getBlockState(key);
            level.playSound(null, key, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
            if (state.getBlock() instanceof DSBurnable b) {
                b.onExtinguish(level, key, state);
            }
        }
        if (FireData.idle(e)) {
            data.remove(key);
        } else {
            data.setDirty();
        }
        return was;
    }

    /** Burnable:StartWildfire: the object starts to smoulder and ignites when the smoulder time is up. */
    public static boolean smoulder(ServerLevel level, BlockPos pos) {
        BlockPos key = isActive(level) ? burnPos(level, pos) : null;
        if (key == null) {
            return false;
        }
        BlockState state = level.getBlockState(key);
        DSBurnable b = (DSBurnable) state.getBlock();
        if (b.burnTime(state) <= 0 && !b.endless(state)) {
            return false;
        }
        FireData data = FireData.get(level);
        FireData.Entry e = data.getOrCreate(key);
        if (e.burning || e.smoldering()) {
            return false;
        }
        // smoldertimeremaining = propagator.flashpoint
        e.smolder = b.propagator(state).roll(key).flashpoint();
        data.setDirty();
        return true;
    }

    /** Propagator:AddHeat: heat from a source other than another fire (a test, an explosion, ...). */
    public static void addHeat(ServerLevel level, BlockPos pos, double amount) {
        BlockPos key = isActive(level) ? burnPos(level, pos) : null;
        if (key != null) {
            addHeat(level, FireData.get(level), key, amount, new HashMap<>());
        }
    }

    /** The block was removed or replaced: forget its fire without any callbacks. */
    public static void forget(Level level, BlockPos pos) {
        if (!isActive(level)) {
            return;
        }
        FireData data = FireData.get((ServerLevel) level);
        FireData.Entry e = data.at(pos);
        if (e != null) {
            removeLight((ServerLevel) level, e);
            data.remove(pos);
        }
    }

    private static void stopBurning(ServerLevel level, BlockPos key, FireData.Entry e) {
        e.burning = false;
        e.burnEnd = -1;
        e.warmth = 0;
        removeLight(level, e);
    }

    // ---- light block of the fire fx ----

    private static int lightLevel(DSBurnable b, BlockState state) {
        return Math.min(15, (int) Math.round(b.lightRadius(state)) + 2);
    }

    private static void placeLight(ServerLevel level, BlockPos key, BlockState state, DSBurnable b, FireData.Entry e) {
        if (b.selfLit()) {
            return;
        }
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel(b, state));
        if (e.light != null && level.getBlockState(e.light).is(Blocks.LIGHT)) {
            return;
        }
        BlockPos[] candidates = {key.above(), key.north(), key.south(), key.east(), key.west(), key.above(2)};
        for (BlockPos p : candidates) {
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p, light, 2);
                e.light = p;
                return;
            }
        }
    }

    private static void removeLight(ServerLevel level, FireData.Entry e) {
        if (e.light != null) {
            if (level.hasChunkAt(e.light) && level.getBlockState(e.light).is(Blocks.LIGHT)) {
                level.setBlock(e.light, Blocks.AIR.defaultBlockState(), 2);
            }
            e.light = null;
        }
    }

    // ---- propagator ----

    /** easing.outCubic(t, 1, 3, 1). */
    private static double outCubic(double t) {
        double u = t - 1.0;
        return 3.0 * (u * u * u + 1.0) + 1.0;
    }

    private static void addHeat(ServerLevel level, FireData data, BlockPos key, double amount, Map<Long, double[]> caps) {
        BlockState state = level.getBlockState(key);
        if (!(state.getBlock() instanceof DSBurnable b) || !b.acceptsHeat(state)) {
            return;
        }
        FireData.Entry e = data.at(key);
        if (e != null && e.paused) {
            return;
        }
        double flashpoint = b.propagator(state).roll(key).flashpoint();
        // CalculateHeatCap: at most flashpoint / outCubic(random) * DT per update.
        double[] cap = caps.computeIfAbsent(key.asLong(),
                k -> new double[] {flashpoint / outCubic(level.random.nextDouble()) * DT, 0.0});
        if (cap[1] >= cap[0]) {
            return;
        }
        double heat = Math.min(amount, cap[0] - cap[1]);
        cap[1] += heat;
        if (e == null) {
            e = data.getOrCreate(key);
        }
        e.heat += heat;
        data.setDirty();
        if (e.heat > flashpoint) {
            e.paused = true;
            // The flashpoint callback is DefaultIgniteFn: StartWildfire (smoulder first).
            if (!e.burning && !e.smoldering() && (b.burnTime(state) > 0 || b.endless(state))) {
                e.smolder = flashpoint;
            }
        }
    }

    // ---- the tick ----

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 5 != 0) {
            return;
        }
        ServerLevel level = server.getLevel(DSRefs.ISLANDS);
        if (level == null) {
            return;
        }
        FireData data = FireData.get(level);
        if (data.entries().isEmpty()) {
            return;
        }
        particles(level, data);
        if (tick % STEP_TICKS == 0) {
            step(level, data);
        }
    }

    private static void particles(ServerLevel level, FireData data) {
        for (Map.Entry<Long, FireData.Entry> en : data.entries().entrySet()) {
            FireData.Entry e = en.getValue();
            if (!e.burning && !e.smoldering()) {
                continue;
            }
            BlockPos pos = BlockPos.of(en.getKey());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DSBurnable b)) {
                continue;
            }
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.6;
            double z = pos.getZ() + 0.5;
            if (e.burning) {
                int fx = b.fxLevel(state);
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1 + fx, 0.25, 0.15, 0.25, 0.01);
                level.sendParticles(ParticleTypes.SMOKE, x, y + 0.5, z, 1 + fx / 2, 0.2, 0.2, 0.2, 0.01);
                if (level.random.nextInt(8) == 0) {
                    level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5F + fx * 0.1F, 1.0F);
                }
            } else {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 1, 0.2, 0.1, 0.2, 0.005);
            }
        }
    }

    private static boolean isSpring(ServerLevel level) {
        return RunClock.state(level.getServer()).day().season() == DSClock.Season.SPRING;
    }

    /** One propagator period: decay, spread, smoulder, burn down, damage. */
    private static void step(ServerLevel level, FireData data) {
        long now = level.getGameTime();
        double rangeMod = isSpring(level) ? SPRING_FIRE_RANGE_MOD : 1.0;
        Map<Long, double[]> caps = new HashMap<>();
        List<Long> keys = new ArrayList<>(data.entries().keySet());

        // Validity and heat decay (Propagator:OnUpdate).
        for (long k : keys) {
            BlockPos pos = BlockPos.of(k);
            FireData.Entry e = data.entries().get(k);
            if (e == null || !level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DSBurnable b) || !b.burnPos(state, level, pos).equals(pos)
                    || e.burning && e.burnEnd < 0 && !b.endless(state)) {
                removeLight(level, e);
                data.remove(pos);
                continue;
            }
            if (e.heat > 0) {
                e.heat = Math.max(0.0, e.heat - DT * DSPropagator.DECAY_RATE);
            }
            if (e.burning) {
                e.warmth = b.warmth(state);
                placeLight(level, pos, state, b, e);
            }
        }

        // Spreading heat and damage from every burning object.
        for (long k : keys) {
            FireData.Entry e = data.entries().get(k);
            BlockPos pos = BlockPos.of(k);
            if (e == null || !e.burning || !level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DSBurnable b) {
                DSBurnable.Spread spread = b.spread(state, pos);
                if (spread != null) {
                    spreadFrom(level, data, pos, spread, rangeMod, caps);
                }
            }
        }

        // Smouldering (burnable.lua SmolderUpdate) and the end of the burn (DoneBurning).
        for (long k : keys) {
            FireData.Entry e = data.entries().get(k);
            BlockPos pos = BlockPos.of(k);
            if (e == null || !level.hasChunkAt(pos)) {
                continue;
            }
            if (e.smoldering() && !e.burning) {
                double nearby = 0.0;
                for (Map.Entry<Long, FireData.Entry> other : data.entries().entrySet()) {
                    BlockPos op = BlockPos.of(other.getKey());
                    double dx = op.getX() - pos.getX();
                    double dz = op.getZ() - pos.getZ();
                    if (dx * dx + dz * dz <= SMOLDER_RADIUS * SMOLDER_RADIUS) {
                        nearby += other.getValue().heat;
                    }
                }
                // Remap(nearbyheat, 20, 90, 1, 2.2) clamped: about twice as fast with lots of heat nearby.
                double heatMod = Math.max(1.0, Math.min(2.2, 1.0 + (nearby - 20.0) / 70.0 * 1.2));
                e.smolder -= DT * heatMod;
                data.setDirty();
                if (e.smolder <= 0 && !ignite(level, pos, null)) {
                    e.smolder = -1; // the object can no longer burn
                }
            } else if (e.burning && e.burnEnd >= 0 && now >= e.burnEnd) {
                finishBurning(level, data, pos, e);
            }
        }

        // Clean up entries that hold nothing any more.
        data.entries().values().removeIf(FireData::idle);
        data.setDirty();
    }

    private static void finishBurning(ServerLevel level, FireData data, BlockPos pos, FireData.Entry e) {
        BlockState state = level.getBlockState(pos);
        stopBurning(level, pos, e);
        if (state.getBlock() instanceof DSBurnable b) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
            b.onBurnt(level, pos, state);
        }
        if (!(level.getBlockState(pos).getBlock() instanceof DSBurnable)) {
            data.remove(pos);
        }
        data.setDirty();
    }

    private static void spreadFrom(ServerLevel level, FireData data, BlockPos pos, DSBurnable.Spread spread,
                                   double rangeMod, Map<Long, double[]> caps) {
        double range = spread.range() * rangeMod;
        if (range <= 0) {
            return;
        }
        double rangeSq = range * range;
        int r = (int) Math.ceil(range);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dsq = dx * dx + dz * dz;
                if (dsq > rangeSq) {
                    continue;
                }
                for (int dy = -SCAN_Y; dy <= SCAN_Y; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) {
                        continue;
                    }
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState state = level.getBlockState(p);
                    if (state.isAir() || !(state.getBlock() instanceof DSBurnable t)
                            || !t.burnPos(state, level, p).equals(p)) {
                        continue;
                    }
                    double percent = Math.max(0.1, 1.0 - dsq / rangeSq);
                    addHeat(level, data, p.immutable(), spread.heat() * percent * DT, caps);
                }
            }
        }
        if (spread.damages()) {
            damage(level, pos, spread.heat() * DT, spread.damageRange() * rangeMod);
        }
    }

    /** Health:DoFireDamage for creatures within the damage range (propagator.lua OnUpdate). */
    private static void damage(ServerLevel level, BlockPos pos, double dsDamage, double range) {
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        AABB box = new AABB(cx - range, pos.getY() - 1.0, cz - range, cx + range, pos.getY() + 3.0, cz + range);
        long now = level.getGameTime();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            double dx = entity.getX() - cx;
            double dz = entity.getZ() - cz;
            if (dx * dx + dz * dz >= range * range || entity.fireImmune()) {
                continue;
            }
            long[] run = FIRE_RUNS.get(entity.getUUID());
            if (run == null || now - run[1] > DSScale.ticks(FIRE_TIMEOUT) + STEP_TICKS) {
                run = new long[] {now, now};
                FIRE_RUNS.put(entity.getUUID(), run);
            }
            run[1] = now;
            if (now - run[0] > DSScale.ticks(FIRE_TIME_START)) {
                entity.invulnerableTime = 0;
                entity.hurt(level.damageSources().inFire(), DSScale.stat(dsDamage));
            }
        }
        if (FIRE_RUNS.size() > 256) {
            FIRE_RUNS.values().removeIf(run -> now - run[1] > 200);
        }
    }

    // ---- heater ----

    /**
     * temperature.lua OnUpdate, heater part: how far the fires around {@code pos} pull the
     * body temperature {@code current} up this second (the sum over heaters of
     * heat * (1 - d^2 / 10^2) - current, for heaters warmer than the body).
     */
    public static double heaterDelta(ServerLevel level, Vec3 pos, double current) {
        if (!level.dimension().equals(DSRefs.ISLANDS)) {
            return 0.0;
        }
        FireData data = FireData.get(level);
        double delta = 0.0;
        for (Map.Entry<Long, FireData.Entry> en : data.entries().entrySet()) {
            FireData.Entry e = en.getValue();
            if (!e.burning || e.warmth <= 0) {
                continue;
            }
            BlockPos p = BlockPos.of(en.getKey());
            double dx = p.getX() + 0.5 - pos.x;
            double dz = p.getZ() + 0.5 - pos.z;
            double factor = 1.0 - (dx * dx + dz * dz) / (HEATER_RANGE * HEATER_RANGE);
            if (factor > 0) {
                double warming = e.warmth * factor;
                if (warming > current) {
                    delta += warming - current;
                }
            }
        }
        return delta;
    }

    /** Ticks as DS seconds, for tests and fuel code. */
    public static double dsSeconds(long ticks) {
        return ticks * DS_PER_TICK;
    }

    private DSFire() {
    }
}
