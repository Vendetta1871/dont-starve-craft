package net.celestiald.dontstarve.portal;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.world.IslandState;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
/**
 * The portal block filling the marble frame interior. One-way: walking in
 * confiscates the player's items into the frame's storage and sends them to
 * the current Don't Starve island. Getting back is only possible through the
 * (not yet implemented) sky portal, mirroring the original game's rules.
 */
public class MarblePortalBlock extends Block {

    public MarblePortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.isOnPortalCooldown() || !player.canUsePortal(false)) {
            return;
        }        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        ServerLevel target = player.server.getLevel(DSRefs.ISLANDS);
        if (overworld == null || target == null) {
            return;
        }

        BlockPos anchor = PortalFrame.find(level, pos)
                .map(PortalFrame.Frame::anchor)
                .orElse(pos);
        PortalStorage.get(overworld).store(anchor, player);

        int index = IslandState.get(overworld).islandIndex();
        BlockPos center = IslandState.islandCenter(index);
        // Make sure the arrival plaza exists before sending the player there.
        target.getChunk(center);
        player.setPortalCooldown();
        player.changeDimension(new DimensionTransition(
                target, IslandState.arrivalPos(index), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return;
        }
        Optional<PortalFrame.Frame> frame = PortalFrame.find(level, pos);
        if (frame.isEmpty()) {
            PortalFrame.clearContiguous(server, pos);
        }
    }
}
