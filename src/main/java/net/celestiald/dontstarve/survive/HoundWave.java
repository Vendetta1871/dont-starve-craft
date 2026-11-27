package net.celestiald.dontstarve.survive;

import net.celestiald.dontstarve.DSScale;

import java.util.List;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.entity.Hound;
import net.celestiald.dontstarve.init.DSEntities;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Hound waves (DST hounded.lua): while a run is live, periodic waves hunt
 * the living members of the current island's run. DST paces them by player
 * age in days with 5-11 day gaps; our runs are minutes long, so the schedule
 * is compressed to DST's proportions (first wave after ~6 min, then every
 * ~8 min, 2 min warning — the intro wave's 120 s warnduration, hounded.lua:54).
 * The clock only runs while living run members are on the island.
 *
 * Wave sizes follow DST's light..heavy bands (2+random(2) ... 4+random(3)).
 */
public final class HoundWave {

    /** Seconds of run time before the first wave (DST intro wave arrives early). */
    public static final int FIRST_WAVE_SECONDS = 360;
    /** Gap between later waves. */
    public static final int WAVE_GAP_SECONDS = 480;
    /** DST intro warnduration = 120 s of distant howling before the bite. */
    public static final int WARNING_SECONDS = 120;
    public static final String WAVE_TAG = "dontstarve.hound_wave";
    /** hounded.lua SPAWN_DIST: hounds appear 30 DS units from their target. */
    private static final double SPAWN_DIST = 30;

    /** Sends wave number {@code wave} (1-based) at once; used by /ds hound now. */
    public static int forceWave(ServerLevel islands, IslandState state, int wave) {
        state.setHoundWavesSent(wave);
        return spawnWave(islands, state, wave);
    }

    /** Test switch (/ds hound pause): scenarios that jump the clock keep the waves away. Not saved. */
    public static boolean paused;

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (paused || server.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }
        IslandState state = IslandState.get(overworld);
        if (survivors(islands, state).isEmpty()) {
            return; // no living souls, no dinner
        }

        int elapsed = state.runSeconds();
        int sent = state.houndWavesSent();
        int nextWaveAt = FIRST_WAVE_SECONDS + sent * WAVE_GAP_SECONDS;
        if (elapsed < nextWaveAt - WARNING_SECONDS) {
            state.setHoundWarned(false);
            return;
        }
        if (elapsed < nextWaveAt) {
            if (!state.houndWarned()) {
                state.setHoundWarned(true);
                warn(server, islands);
            }
            return;
        }
        spawnWave(islands, state, sent + 1);
        state.setHoundWavesSent(sent + 1);
        state.setHoundWarned(false);
    }

    /** Living members of the current run who are on the island right now. */
    private static List<ServerPlayer> survivors(ServerLevel islands, IslandState state) {
        return islands.players().stream()
                .filter(p -> !GhostState.isGhost(p) && state.isCurrentRun(p))
                .toList();
    }

    /** Hound howl: the warning that a wave is on its way. */
    private static void warn(MinecraftServer server, ServerLevel islands) {
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.dontstarve.hound_warning")
                        .withStyle(ChatFormatting.DARK_RED), false);
        for (ServerPlayer player : islands.players()) {
            player.playSound(SoundEvents.WOLF_GROWL, 1.0F, 0.6F);
        }
    }

    private static int spawnWave(ServerLevel islands, IslandState state, int wave) {
        RandomSource random = islands.getRandom();
        // DST: intro 1, light 2+random(2), med 3+random(3), heavy 4+random(3).
        int count = wave == 1 ? 1 : Math.min(2 + random.nextInt(2 + wave), 7);
        List<ServerPlayer> prey = survivors(islands, state);
        Vec3 fallback = null;
        if (prey.isEmpty()) {
            // Headless test waves (and DST's "hounds hunt the camp, not the
            // player"): arrive around the island plaza instead.
            BlockPos center = IslandState.islandCenter(state.islandIndex());
            fallback = new Vec3(center.getX() + 0.5, IslandState.PLAZA_Y, center.getZ() + 0.5);
        }
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            ServerPlayer target = prey.isEmpty() ? null : prey.get(random.nextInt(prey.size()));
            Vec3 around = target != null ? target.position() : fallback;
            if (spawnHound(islands, target, around, random)) {
                spawned++;
            }
        }
        if (spawned > 0) {
            islands.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.dontstarve.hound_attack", spawned)
                            .withStyle(ChatFormatting.RED), false);
        }
        return spawned;
    }

    private static boolean spawnHound(ServerLevel islands, @Nullable ServerPlayer target,
                                      Vec3 around, RandomSource random) {
        // Arrive from a random bearing at hounded.lua SPAWN_DIST 30 units, on solid island ground.
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = DSScale.blocks(SPAWN_DIST) + random.nextDouble() * 4.0;
            int x = (int) Math.round(around.x + Math.cos(angle) * dist);
            int z = (int) Math.round(around.z + Math.sin(angle) * dist);
            BlockPos pos = islands.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (!islands.getWorldBorder().isWithinBounds(pos)
                    || !islands.getBlockState(pos.below()).isSolidRender(islands, pos.below())) {
                continue;
            }
            Hound hound = DSEntities.HOUND.get().create(islands);
            if (hound == null) {
                return false;
            }
            hound.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            hound.finalizeSpawn(islands, islands.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            // DST hounds never give up and never despawn mid-chase.
            hound.setPersistenceRequired();
            hound.addTag(WAVE_TAG);
            if (target != null) {
                hound.setTarget(target);
            }
            islands.addFreshEntityWithPassengers(hound);
            return true;
        }
        return false;
    }

    private HoundWave() {
    }
}
