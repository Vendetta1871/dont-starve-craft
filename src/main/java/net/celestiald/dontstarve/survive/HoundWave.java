package net.celestiald.dontstarve.survive;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSEntities;
import net.celestiald.dontstarve.world.IslandState;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Hound waves (DST hounded.lua): while a run is live, periodic waves hunt
 * everyone on the island. DST paces them by player age in days with 5-11 day
 * gaps; our runs are minutes long, so the schedule is compressed to DST's
 * proportions (first wave after ~6 min, then every ~8 min, 2 min warning —
 * the intro wave's 120 s warnduration, hounded.lua:54).
 *
 * Wave sizes follow DST's light..heavy bands (2+random(2) ... 4+random(3)),
 * and from the second wave on some hounds are Hairy Hounds — the beard hair
 * that Meat Effigies hunger for.
 */
public final class HoundWave {

    /** Seconds of run time before the first wave (DST intro wave arrives early). */
    public static final int FIRST_WAVE_SECONDS = 360;
    /** Gap between later waves. */
    public static final int WAVE_GAP_SECONDS = 480;
    /** DST intro warnduration = 120 s of distant howling before the bite. */
    public static final int WARNING_SECONDS = 120;

    /** Sends wave number {@code wave} (1-based) at once; used by /ds hound now. */
    public static int forceWave(ServerLevel islands, IslandState state, int wave) {
        state.setHoundWavesSent(wave);
        return spawnWave(islands, state, wave);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }
        IslandState state = IslandState.get(overworld);
        List<ServerPlayer> survivors = new java.util.ArrayList<>(islands.players());
        survivors.removeIf(GhostState::isGhost);
        if (survivors.isEmpty()) {
            return; // no living souls, no dinner
        }
        if (state.runStartGameTime() < 0) {
            state.setRunStartGameTime(islands.getGameTime());
        }

        long elapsedSeconds = (islands.getGameTime() - state.runStartGameTime()) / 20;
        int sent = state.houndWavesSent();
        long nextWaveAt = FIRST_WAVE_SECONDS + (long) sent * WAVE_GAP_SECONDS;
        if (elapsedSeconds < nextWaveAt - WARNING_SECONDS) {
            state.setHoundWarned(false);
            return;
        }
        if (elapsedSeconds < nextWaveAt) {
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

    /** Hairy Hound howl: the warning that a wave is on its way. */
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
        List<ServerPlayer> prey = islands.players().stream()
                .filter(p -> !GhostState.isGhost(p)).toList();
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
            if (spawnHound(islands, target, around, random, wave >= 2 && random.nextFloat() < 0.3F)) {
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
                                      Vec3 around, RandomSource random, boolean hairy) {
        // Arrive from a random bearing, just off-screen, on solid island ground.
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 24.0 + random.nextDouble() * 12.0;
            int x = (int) Math.round(around.x + Math.cos(angle) * dist);
            int z = (int) Math.round(around.z + Math.sin(angle) * dist);
            BlockPos pos = islands.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    new BlockPos(x, 0, z));
            if (!islands.getWorldBorder().isWithinBounds(pos) || !isSolid(islands, pos.below())) {
                continue;
            }
            Wolf hound = hairy
                    ? DSEntities.HAIRY_HOUND.get().create(islands)
                    : EntityType.WOLF.create(islands);
            if (hound == null) {
                return false;
            }
            hound.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            hound.finalizeSpawn(islands, islands.getCurrentDifficultyAt(pos),
                    MobSpawnType.EVENT, null);
            // DST hounds never give up and never despawn mid-chase.
            hound.setPersistenceRequired();
            hound.addTag("dontstarve.hound_wave");
            if (target != null) {
                hound.setTarget(target);
            }
            islands.addFreshEntityWithPassengers(hound);
            return true;
        }
        return false;
    }

    private static boolean isSolid(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }

    private HoundWave() {
    }
}
