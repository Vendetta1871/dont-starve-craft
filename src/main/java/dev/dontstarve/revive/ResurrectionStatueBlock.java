package dev.dontstarve.revive;

import dev.dontstarve.init.DSBlockEntities;
import dev.dontstarve.survive.GhostState;
import java.util.UUID;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Meat Effigy (DST "resurrectionstatue"): a living player attunes it by giving
 * it 5 HP of blood — never enough to die, mirroring resurrectionstatue.lua's
 * "don't die from attunement" clamp. When that player dies as a ghost, they
 * can click their attuned effigy to claw their way back at full health.
 * Attuning a new effigy abandons the old one.
 */
public class ResurrectionStatueBlock extends Block implements EntityBlock {

    /** Blood price of attunement, DST EFFIGY_HEALTH_PENALTY 40/150 scaled to 20 HP. */
    private static final float ATTUNE_BLOOD_COST = 5.0F;

    public ResurrectionStatueBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResurrectionStatueBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof ResurrectionStatueBlockEntity statue)) {
            return InteractionResult.FAIL;
        }
        UUID self = player.getUUID();
        UUID attuned = statue.getAttunedTo();

        if (GhostState.isGhost(player)) {
            if (attuned == null) {
                player.displayClientMessage(
                        Component.translatable("message.dontstarve.effigy_hungry"), true);
            } else if (!attuned.equals(self)) {
                player.displayClientMessage(Component.translatable(
                        "message.dontstarve.effigy_taken", String.valueOf(statue.getAttunedName())), true);
            } else {
                resurrect(serverLevel, pos, (ServerPlayer) player);
            }
            return InteractionResult.SUCCESS;
        }

        // Living player: attune with blood.
        if (attuned != null) {
            player.displayClientMessage(Component.translatable(
                    attuned.equals(self) ? "message.dontstarve.effigy_already"
                            : "message.dontstarve.effigy_taken", String.valueOf(statue.getAttunedName())), true);
            return InteractionResult.SUCCESS;
        }
        attune(serverLevel, pos, (ServerPlayer) player, statue);
        return InteractionResult.SUCCESS;
    }

    private void attune(ServerLevel level, BlockPos pos, ServerPlayer player,
                        ResurrectionStatueBlockEntity statue) {
        EffigyState state = EffigyState.get(level);
        BlockPos previous = state.getPos(player.getUUID());
        if (previous != null && !previous.equals(pos)
                && level.getBlockEntity(previous) instanceof ResurrectionStatueBlockEntity oldStatue
                && player.getUUID().equals(oldStatue.getAttunedTo())) {
            oldStatue.setAttunement(null, null);
        }
        statue.setAttunement(player.getUUID(), player.getGameProfile().getName());
        state.setPos(player.getUUID(), pos);

        // DST: the blood price can never finish you off.
        player.setHealth(Math.max(1.0F, player.getHealth() - ATTUNE_BLOOD_COST));
        player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
        player.displayClientMessage(
                Component.translatable("message.dontstarve.effigy_attuned").withStyle(ChatFormatting.DARK_RED), true);
    }

    private void resurrect(ServerLevel level, BlockPos pos, ServerPlayer player) {
        GhostState.clear(player);
        player.setHealth(player.getMaxHealth());
        player.fallDistance = 0.0F;
        player.connection.teleport(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.05);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.dontstarve.effigy_revived", player.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }
}
