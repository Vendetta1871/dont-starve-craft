package net.celestiald.dontstarve.revive;

import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.survive.GhostState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The second resurrection path, ported from DST's Telltale Heart ("reviver"):
 * a living player presses the heart against a ghost and brings them back at
 * a third of their health (RESURRECT_HEALTH 50 of 150). Like DST, crafting
 * blood-magic costs the crafter 5 HP — the recipe ingredients alone are not
 * the whole price.
 */
public final class ReviveHandler {

    /**
     * DST revives at RESURRECT_HEALTH = 50 of Wilson's 150 (player_common_extensions.lua:727).
     * REVIVE_HEALTH_PENALTY 0.25 is a max-health penalty in DST, not ported yet.
     */
    private static final float REVIVE_HEALTH_FRACTION = 50.0F / 150.0F;

    /** DST CHARACTER_INGREDIENT.HEALTH baked into effigy/heart recipes. */
    private static final float BLOOD_CRAFT_COST = 5.0F;

    public static void onInteractWithGhost(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer user)
                || !(event.getTarget() instanceof ServerPlayer ghost)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(DSItems.REVIVER.get()) || !GhostState.isGhost(ghost) || GhostState.isGhost(user)) {
            return;
        }

        GhostState.clear(ghost);
        ghost.setHealth(ghost.getMaxHealth() * REVIVE_HEALTH_FRACTION);
        // The spirit may hover high or over the void: it is pulled down to the
        // living player holding the heart, so it never falls to a second death.
        ghost.connection.teleport(user.getX(), user.getY(), user.getZ(), ghost.getYRot(), ghost.getXRot());
        ghost.fallDistance = 0.0F;
        if (!user.getAbilities().instabuild) {
            held.shrink(1);
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        level.playSound(null, ghost.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD,
                ghost.getX(), ghost.getY() + 1.0, ghost.getZ(), 30, 0.4, 0.8, 0.4, 0.04);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.dontstarve.reviver_used",
                        user.getDisplayName(), ghost.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Blood-magic recipes (DST CHARACTER_INGREDIENT.HEALTH) drink the crafter's HP. */
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Item crafted = event.getCrafting().getItem();
        if (crafted != DSItems.REVIVER.get() && crafted != DSItems.RESURRECTION_STATUE.get()) {
            return;
        }
        Player player = event.getEntity();
        player.setHealth(Math.max(1.0F, player.getHealth() - BLOOD_CRAFT_COST));
        player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 0.8F);
        player.displayClientMessage(
                Component.translatable("message.dontstarve.craft_blood_cost").withStyle(ChatFormatting.DARK_RED),
                true);
    }

    private ReviveHandler() {
    }
}
