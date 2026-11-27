package net.celestiald.dontstarve.survive;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Strips a player of everything they carry (portal confiscation, death). */
public final class Belongings {

    /** Removes and returns the whole inventory: main slots, armor and offhand. */
    public static List<ItemStack> strip(ServerPlayer player) {
        List<ItemStack> taken = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (List<ItemStack> slots : List.of(inventory.items, inventory.armor, inventory.offhand)) {
            for (int i = 0; i < slots.size(); i++) {
                ItemStack stack = slots.get(i);
                if (!stack.isEmpty()) {
                    taken.add(stack.copy());
                    slots.set(i, ItemStack.EMPTY);
                }
            }
        }
        player.inventoryMenu.broadcastChanges();
        return taken;
    }

    /** Hands items back; whatever does not fit drops at the player's feet. */
    public static void give(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private Belongings() {
    }
}
