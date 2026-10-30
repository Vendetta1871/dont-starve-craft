package dev.dontstarve.portal;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Portal ignition by sacrifice: when any mob dies close to an unlit, valid
 * marble frame, the frame lights up. Mirrors the Don't Starve ritual feel —
 * the door hungers for death, whatever caused it.
 */
public final class SacrificeHandler {

    private static final int SACRIFICE_RADIUS = 8;

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!server.dimension().equals(Level.OVERWORLD)) {
            return;
        }
        if (!(entity instanceof Mob)) {
            return;
        }
        BlockPos origin = entity.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SACRIFICE_RADIUS; dx <= SACRIFICE_RADIUS; dx++) {
            for (int dy = -SACRIFICE_RADIUS; dy <= SACRIFICE_RADIUS; dy++) {
                for (int dz = -SACRIFICE_RADIUS; dz <= SACRIFICE_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!PortalFrame.isFrameBlock(level, cursor)) {
                        continue;
                    }
                    Optional<PortalFrame.Frame> frame = PortalFrame.find(level, cursor);
                    if (frame.isPresent() && frame.get().light(server)) {
                        playIgnitionFeedback(server, frame.get());
                        return;
                    }
                }
            }
        }
    }

    private static void playIgnitionFeedback(ServerLevel level, PortalFrame.Frame frame) {
        BlockPos anchor = frame.anchor();
        level.playSound(null, anchor, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);
        List<BlockPos> interior = frame.interior();
        for (BlockPos pos : interior) {
            for (int i = 0; i < 4; i++) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        pos.getX() + level.random.nextDouble(),
                        pos.getY() + level.random.nextDouble(),
                        pos.getZ() + level.random.nextDouble(),
                        0, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private SacrificeHandler() {
    }
}
