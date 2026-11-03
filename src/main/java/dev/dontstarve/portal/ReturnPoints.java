package dev.dontstarve.portal;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Where a player returning from the islands lands: inside the marble portal
 * frame they originally entered through (so they can open its vault), or the
 * overworld spawn when that frame is gone or unknown.
 */
public final class ReturnPoints {

    public record Target(Vec3 position, boolean portalCooldown) {
    }

    public static Target of(ServerLevel overworld, UUID player) {
        BlockPos anchor = PortalStorage.get(overworld).entryOf(player);
        if (anchor != null) {
            Optional<PortalFrame.Frame> frame = PortalFrame.find(overworld, anchor);
            if (frame.isPresent() && !frame.get().interior().isEmpty()) {
                // interior() starts at the bottom row; landing there drops the
                // player safely onto the ground under the frame.
                return new Target(Vec3.atBottomCenterOf(frame.get().interior().get(0)), true);
            }
        }
        BlockPos spawn = overworld.getLevelData().getSpawnPos();
        return new Target(Vec3.atBottomCenterOf(
                overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn)), false);
    }

    private ReturnPoints() {
    }
}
