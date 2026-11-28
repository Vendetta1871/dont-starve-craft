package net.celestiald.dontstarve.mixin;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSRefs;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No vanilla crafting on the islands: the crafting grid (inventory 2x2 and crafting
 * table alike) never yields a result there. DS crafting goes through the DS menu.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
    private static void dontstarve$noGridOnIslands(AbstractContainerMenu menu, Level level, Player player,
                                                   CraftingContainer craftSlots, ResultContainer resultSlots,
                                                   @Nullable RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        if (level.isClientSide || !level.dimension().equals(DSRefs.ISLANDS)) {
            return;
        }
        resultSlots.setItem(0, ItemStack.EMPTY);
        menu.setRemoteSlot(0, ItemStack.EMPTY);
        ((ServerPlayer) player).connection.send(
                new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        ci.cancel();
    }
}
