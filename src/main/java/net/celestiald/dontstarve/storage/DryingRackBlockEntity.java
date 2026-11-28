package net.celestiald.dontstarve.storage;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.fire.DSFire;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSDataComponents;
import net.celestiald.dontstarve.item.DSFoodItem;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The dryingrack component (components/dryingrack.lua) of a Drying Rack: three slots, one item
 * each. A dryable item dries for its dry time (DS seconds) and is then replaced by its product;
 * while it dries it does not perish (the rack's preserver rate multiplier is 0 for dryables),
 * every other item on the rack perishes at the normal rate. An item taken off early remembers
 * its time left ({@code dryingrack_drytime}) and resumes it when put back (at least 10 DS s).
 * The rack keeps drying while its chunk is unloaded (LongUpdate). Rain, which pauses drying in
 * DST, is not ported: the mod has no rain yet.
 */
public class DryingRackBlockEntity extends BlockEntity {

    public static final int SLOTS = 3;
    /** dryingrack.lua OnGetItem: a resumed dry time is at least this many seconds. */
    private static final float MIN_RESUME = 10.0F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** DS seconds of drying left per slot; meaningful while the slot holds a dryable. */
    private final float[] dryLeft = new float[SLOTS];
    private long lastTick = -1;
    /** Game ticks since the non-drying items last aged (they age once per second like carried food). */
    private int ageTicks;

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(DSBlockEntities.DRYING_RACK.get(), pos, state);
    }

    /** dryable component present: the item has a dried product. */
    public static boolean dryable(ItemStack stack) {
        return stack.getItem() instanceof DSFoodItem food && food.driedInto() != null;
    }

    public ItemStack get(int slot) {
        return items.get(slot);
    }

    /** DS seconds left to dry in {@code slot}; negative when the slot does not hold a drying item. */
    public float dryLeft(int slot) {
        return dryable(items.get(slot)) ? dryLeft[slot] : -1.0F;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public int used() {
        return (int) items.stream().filter(s -> !s.isEmpty()).count();
    }

    /** Puts one item of {@code stack} on the first free slot; returns the slot or -1 (not dryable or full). */
    public int put(ItemStack stack) {
        if (!dryable(stack)) {
            return -1;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                ItemStack one = stack.copyWithCount(1);
                DSFoodItem food = (DSFoodItem) one.getItem();
                float time = food.dryTime();
                Float resumed = one.remove(DSDataComponents.DRY_LEFT.get());
                if (resumed != null) {
                    time = Math.min(Math.max(MIN_RESUME, resumed), time);
                }
                items.set(i, one);
                dryLeft[i] = time;
                changed();
                return i;
            }
        }
        return -1;
    }

    /** The slot an empty hand takes from: a finished product first, else the first item. */
    public int takeSlot() {
        int first = -1;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!dryable(stack)) {
                return i;
            }
            if (first < 0) {
                first = i;
            }
        }
        return first;
    }

    /** Takes the item out of {@link #takeSlot}; an item still drying remembers its time left. */
    public ItemStack take() {
        int slot = takeSlot();
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (dryable(stack)) {
            stack.set(DSDataComponents.DRY_LEFT.get(), dryLeft[slot]);
        }
        dryLeft[slot] = 0.0F;
        changed();
        return stack;
    }

    /** Drops everything (a hammered or removed rack). */
    public void dropAll(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (dryable(stack)) {
                    stack.set(DSDataComponents.DRY_LEFT.get(), dryLeft[i]);
                }
                Block.popResource(level, pos, stack);
                items.set(i, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    /** dryingrack:OnBurnt (InstantDry) and the container drop: dryables become their product, all of it drops. */
    public void dryAllAndDrop(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (dryable(stack)) {
                Item product = ((DSFoodItem) stack.getItem()).driedInto();
                Block.popResource(level, pos, new ItemStack(product));
            } else {
                Block.popResource(level, pos, stack);
            }
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.dimension().equals(DSRefs.ISLANDS) || state.getValue(DryingRackBlock.BURNT)) {
            return;
        }
        long now = level.getGameTime();
        if (lastTick < 0) {
            lastTick = now;
        }
        long elapsed = now - lastTick;
        lastTick = now;
        if (elapsed <= 0 || isEmpty()) {
            return;
        }
        float dt = (float) (elapsed * DSFire.DS_PER_TICK);
        boolean dirty = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !dryable(stack)) {
                continue;
            }
            dryLeft[i] -= dt;
            if (dryLeft[i] <= 0.0F) {
                done(i, -dryLeft[i]);
                dirty = true;
            }
        }
        ageTicks += (int) elapsed;
        if (ageTicks >= 20) {
            // Non-drying items perish at the normal rate (the preserver multiplier is nil for them).
            float perTick = Perishable.updateDt(level.getServer()) / 20.0F;
            for (int i = 0; i < SLOTS; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty() && !dryable(stack) && Perishable.isPerishable(stack)) {
                    items.set(i, Perishable.age(stack, perTick * ageTicks));
                    dirty = true;
                }
            }
            ageTicks = 0;
        }
        if (dirty) {
            changed();
        }
    }

    /** OnDoneDrying: the item becomes its product in the same slot; the time past the finish ages the product. */
    private void done(int slot, float overshoot) {
        Item product = ((DSFoodItem) items.get(slot).getItem()).driedInto();
        ItemStack dried = new ItemStack(product);
        if (overshoot > 0.0F && Perishable.isPerishable(dried)) {
            dried = Perishable.age(dried, overshoot);
        }
        items.set(slot, dried);
        dryLeft[slot] = 0.0F;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        ListTag left = new ListTag();
        for (float f : dryLeft) {
            left.add(FloatTag.valueOf(f));
        }
        tag.put("dry_left", left);
        tag.putLong("last_tick", lastTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, items, registries);
        ListTag left = tag.getList("dry_left", Tag.TAG_FLOAT);
        for (int i = 0; i < SLOTS && i < left.size(); i++) {
            dryLeft[i] = left.getFloat(i);
        }
        lastTick = tag.contains("last_tick") ? tag.getLong("last_tick") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
