package dev.dontstarve.grave;

import dev.dontstarve.survive.GhostState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A marble headstone over the belongings of a player who died in the islands.
 * Living players open it to reclaim their gear (and the first one to dig also
 * frees the sealed experience); ghosts are reminded that the dead cannot eat.
 */
public class GraveBlock extends Block implements EntityBlock {

    public GraveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) {
            return InteractionResult.FAIL;
        }
        if (GhostState.isGhost(player)) {
            player.displayClientMessage(
                    Component.translatable("message.dontstarve.ghost_no_touch"), true);
            return InteractionResult.FAIL;
        }
        player.openMenu(grave);
        if (player instanceof ServerPlayer serverPlayer) {
            grave.claimXp(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
