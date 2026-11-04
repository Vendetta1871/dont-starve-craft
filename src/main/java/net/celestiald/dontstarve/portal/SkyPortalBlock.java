package net.celestiald.dontstarve.portal;

import net.celestiald.dontstarve.init.DSItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The escape route home (our invention, DS-flavored after the Moon Portal:
 * docs/dont-starve/08). A living player places the frame, wakes it with a
 * Moon Rock Idol — the idol melts into starlight — and steps through to the
 * overworld, back to the marble portal they came through (vault included).
 * One-way: nothing walks out of the overworld side, and ghosts cannot use it.
 */
public class SkyPortalBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SkyPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (state.getValue(LIT)) {
            escape(serverLevel, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        ItemStack idol = player.getMainHandItem().is(DSItems.MOONROCKIDOL.get())
                ? player.getMainHandItem() : player.getOffhandItem();
        if (!idol.is(DSItems.MOONROCKIDOL.get())) {
            player.displayClientMessage(
                    Component.translatable("message.dontstarve.skyportal_needs_idol"), true);
            return InteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) {
            idol.shrink(1);
        }
        serverLevel.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        serverLevel.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1.0F, 1.6F);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        player.displayClientMessage(
                Component.translatable("message.dontstarve.skyportal_lit").withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    private void escape(ServerLevel islands, ServerPlayer player) {
        ServerLevel overworld = islands.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        ReturnPoints.Target target = ReturnPoints.of(overworld, player.getUUID());
        DimensionTransition.PostDimensionTransition post = target.portalCooldown()
                ? moved -> moved.setPortalCooldown() : DimensionTransition.DO_NOTHING;
        player.changeDimension(new DimensionTransition(overworld, target.position(), Vec3.ZERO,
                player.getYRot(), player.getXRot(), post));
        islands.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.dontstarve.escaped", player.getDisplayName())
                        .withStyle(ChatFormatting.AQUA), false);
    }
}
