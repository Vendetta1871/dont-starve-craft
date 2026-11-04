package net.celestiald.dontstarve.grave;

import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSDataComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds everything a player carried when they died in the islands dimension,
 * plus their experience. Like ground items in Don't Starve, a grave never
 * despawns; it only gives up its contents when a living player digs it open
 * (ghosts can look but not touch, per DST ghost rules). Breaking the grave
 * keeps contents via the block loot table's copy_components function.
 */
public class GraveBlockEntity extends BaseContainerBlockEntity {

    /** 6 rows: a full player inventory plus armor and offhand always fits. */
    public static final int SIZE = 54;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int graveXp;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlockEntities.GRAVE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newItems) {
        items = newItems;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dontstarve.grave");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.sixRows(id, inventory, this);
    }

    /** Fills the grave right after it swallows a dead player's belongings. */
    public void setContents(List<ItemStack> stacks) {
        items.clear();
        int slot = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && slot < SIZE) {
                items.set(slot++, stack.copy());
            }
        }
        setChanged();
    }

    public void setGraveXp(int xp) {
        graveXp = Math.max(0, xp);
        setChanged();
    }

    /** The first living player to open the grave takes the sealed experience. */
    public void claimXp(ServerPlayer player) {
        if (graveXp > 0) {
            int xp = graveXp;
            graveXp = 0;
            setChanged();
            player.giveExperiencePoints(xp);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (graveXp > 0) {
            tag.putInt("GraveXp", graveXp);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        graveXp = tag.getInt("GraveXp");
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        graveXp = input.getOrDefault(DSDataComponents.GRAVE_XP.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (graveXp > 0) {
            builder.set(DSDataComponents.GRAVE_XP.get(), graveXp);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("GraveXp");
    }
}
