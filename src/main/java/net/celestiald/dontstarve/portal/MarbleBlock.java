package net.celestiald.dontstarve.portal;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Any marble block. Doubles as the eject point for the portal's storage:
 * right-clicking a frame block of a lit portal drops the items that were
 * confiscated from players entering it. Players can never put items in.
 */
public class MarbleBlock extends Block {

    public MarbleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        Optional<PortalFrame.Frame> frame = PortalFrame.find(level, pos);
        if (frame.isEmpty() || !frame.get().isLit(level)) {
            return InteractionResult.PASS;
        }
        PortalStorage storage = PortalStorage.get(server);
        List<ItemStack> items = storage.take(frame.get().anchor());
        if (items.isEmpty()) {
            return InteractionResult.PASS;
        }
        for (ItemStack stack : items) {
            popResource(level, pos.above(), stack);
        }
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        return InteractionResult.CONSUME;
    }
}
