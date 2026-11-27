package net.celestiald.dontstarve.portal;

import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.survive.GhostState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The way home from the islands (sky portal escape, all-ghost wipe, stale
 * run): back to the spot where the player read the Codex Umbra — where their
 * belongings were dropped — or the world spawn when that place is unknown.
 */
public final class ReturnPoints {

    /** Sends the player home and ends their run membership. */
    public static void sendHome(ServerPlayer player) {
        GhostState.clear(player);
        player.setData(DSAttachments.RUN_ISLAND.get(), -1);

        ServerLevel target = null;
        Vec3 landing = null;
        if (player.hasData(DSAttachments.RETURN_POINT.get())) {
            GlobalPos point = player.getData(DSAttachments.RETURN_POINT.get());
            target = player.server.getLevel(point.dimension());
            landing = Vec3.atBottomCenterOf(point.pos());
        }
        if (target == null) {
            target = player.server.getLevel(Level.OVERWORLD);
            BlockPos spawn = target.getLevelData().getSpawnPos();
            landing = Vec3.atBottomCenterOf(target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn));
        }
        player.changeDimension(new DimensionTransition(target, landing, Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        player.fallDistance = 0.0F;
    }

    private ReturnPoints() {
    }
}
