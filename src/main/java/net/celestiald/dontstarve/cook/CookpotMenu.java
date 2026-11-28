package net.celestiald.dontstarve.cook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Crock Pot widget (widgetsetup "cookpot", ui_cookpot_1x4): four ingredient slots that take one
 * cooking ingredient each, and a Cook button (button id 0, the widget's cook button).
 */
public class CookpotMenu extends AbstractContainerMenu {

    public static final int COOK_BUTTON = 0;

    private final Container pot;

    /** Client side. */
    public CookpotMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CookpotBlockEntity.SIZE));
    }

    public CookpotMenu(int id, Inventory inventory, Container pot) {
        super(CookInit.COOKPOT_MENU.get(), id);
        checkContainerSize(pot, CookpotBlockEntity.SIZE);
        this.pot = pot;
        pot.startOpen(inventory.player);
        for (int i = 0; i < CookpotBlockEntity.SIZE; i++) {
            addSlot(new IngredientSlot(pot, i, 44 + i * 18, 20));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 109));
        }
    }

    /** One cooking ingredient per slot (the pot does not accept stacks). */
    private static final class IngredientSlot extends Slot {
        IngredientSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return Cooking.isIngredient(stack) && container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** The Cook button is enabled when the pot is full (stewer CanCook). */
    public boolean canCook() {
        for (int i = 0; i < CookpotBlockEntity.SIZE; i++) {
            if (!slots.get(i).hasItem()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == COOK_BUTTON && pot instanceof CookpotBlockEntity be && player instanceof ServerPlayer) {
            if (be.startCooking(player) == null) {
                player.closeContainer();
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < CookpotBlockEntity.SIZE) {
                if (!moveItemStackTo(stack, CookpotBlockEntity.SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CookpotBlockEntity.SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return pot.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pot.stopOpen(player);
    }
}
