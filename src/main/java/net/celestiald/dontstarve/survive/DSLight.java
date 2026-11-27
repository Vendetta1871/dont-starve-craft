package net.celestiald.dontstarve.survive;

import net.celestiald.dontstarve.clock.DSClock;
import net.celestiald.dontstarve.clock.RunClock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;

/**
 * DST's LightWatcher light value (0..1) for a player on the islands: block light,
 * or sky light scaled by the run clock's ambient brightness, at head height.
 * Thresholds (player_common.lua:2446-2448, tuning.lua): in light above 0.075,
 * dark below 0.05 (the grue), sanity "dark" below 0.1 and "light" above 0.6.
 */
public final class DSLight {

    public static final float LIGHT_THRESH = 0.075F;
    public static final float DARK_THRESH = 0.05F;
    public static final float SANITY_LOW_LIGHT = 0.1F;
    public static final float SANITY_HIGH_LIGHT = 0.6F;

    public static float value(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos head = BlockPos.containing(player.getEyePosition());
        float block = level.getBrightness(LightLayer.BLOCK, head) / 15.0F;
        float sky = level.getBrightness(LightLayer.SKY, head) / 15.0F
                * DSClock.ambientLevel(RunClock.state(player.server));
        return Math.max(block, sky);
    }

    private DSLight() {
    }
}
