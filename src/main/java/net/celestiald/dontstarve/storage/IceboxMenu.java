package net.celestiald.dontstarve.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The 3x3 chest screen on the Ice Box: its slots only take what the icebox itemtestfn allows.
 * The client opens the plain 9x3 menu; the server's slots enforce the rule.
 */
public class IceboxMenu extends ChestMenu {

    public IceboxMenu(int id, Inventory inventory, Container container) {
        super(MenuType.GENERIC_9x3, id, inventory, container, 3);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (slot.container instanceof IceboxBlockEntity) {
            return super.addSlot(new Slot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return IceboxBlockEntity.accepts(stack);
                }
            });
        }
        return super.addSlot(slot);
    }
}
