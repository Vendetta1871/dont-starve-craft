package net.celestiald.dontstarve.storage;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.item.DSFoodItem;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The container of an Ice Box (params.icebox in containers.lua: 3x3 slots, stacking allowed).
 * Its food perishes at {@link Perishable#FRIDGE_MULT} of the normal rate (perishable.lua: an owner
 * tagged "fridge"), on top of the world temperature multiplier; the box ages its own
 * contents once per second. It takes perishable food and rot only (itemtestfn: fresh/stale/spoiled
 * edibles and "icebox_valid" items such as spoiled food).
 */
public class IceboxBlockEntity extends RandomizableContainerBlockEntity {

    public static final int SIZE = 9;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public IceboxBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlockEntities.ICEBOX.get(), pos, state);
    }

    /** containers.lua params.icebox.itemtestfn. */
    public static boolean accepts(ItemStack stack) {
        return Perishable.isPerishable(stack) || stack.is(DSItems.SPOILED_FOOD.get());
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0 || !level.dimension().equals(DSRefs.ISLANDS)) {
            return;
        }
        float dt = Perishable.updateDt(level.getServer()) * Perishable.FRIDGE_MULT;
        boolean dirty = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (Perishable.isPerishable(stack)) {
                items.set(i, Perishable.age(stack, dt));
                dirty = true;
            }
        }
        if (dirty) {
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dontstarve.icebox");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new IceboxMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
    }
}
