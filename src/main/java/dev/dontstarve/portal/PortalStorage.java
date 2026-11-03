package dev.dontstarve.portal;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-portal vault in the overworld that holds the items confiscated from
 * players entering the Don't Starve world. Items can only be taken out
 * (right-click a frame block of a lit portal), never put in. Stored items
 * never despawn, unlike drops on the ground.
 */
public final class PortalStorage extends SavedData {

    private static final String NAME = "dontstarve_portal_storage";
    private static final Codec<List<ItemStack>> LIST_CODEC = ItemStack.OPTIONAL_CODEC.listOf();

    private final Map<String, List<ItemStack>> byPortal = new HashMap<>();
    /** Which portal each player entered the islands through (for the way back). */
    private final Map<UUID, String> entryByPlayer = new HashMap<>();

    public static PortalStorage get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PortalStorage::new, PortalStorage::load),
                NAME);
    }
    /** Moves the player's whole inventory (including armor) into the vault. */
    public void store(BlockPos anchor, ServerPlayer player) {
        List<ItemStack> list = byPortal.computeIfAbsent(key(anchor), k -> new ArrayList<>());
        Inventory inventory = player.getInventory();
        stealAll(inventory.items, list);
        stealAll(inventory.armor, list);
        stealAll(inventory.offhand, list);
        player.inventoryMenu.broadcastChanges();
        entryByPlayer.put(player.getUUID(), key(anchor));
        setDirty();
    }

    /** The frame the given player entered through, or null if unknown. */
    @Nullable
    public BlockPos entryOf(UUID player) {
        String raw = entryByPlayer.get(player);
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return null;
        }
        return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    /** Removes and returns everything stored for the given portal. */
    public List<ItemStack> take(BlockPos anchor) {
        List<ItemStack> removed = byPortal.remove(key(anchor));
        if (removed != null) {
            setDirty();
        }
        return removed == null ? List.of() : removed;
    }

    private static void stealAll(List<ItemStack> from, List<ItemStack> into) {
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (!stack.isEmpty()) {
                into.add(stack.copy());
                from.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag portals = new ListTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Map.Entry<String, List<ItemStack>> entry : byPortal.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("portal", entry.getKey());
            LIST_CODEC.encodeStart(ops, entry.getValue())
                    .result()
                    .ifPresent(items -> entryTag.put("items", items));
            portals.add(entryTag);
        }
        tag.put("portals", portals);
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, String> entry : entryByPlayer.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("player", entry.getKey());
            entryTag.putString("portal", entry.getValue());
            entries.add(entryTag);
        }
        tag.put("entries", entries);
        return tag;
    }

    public static PortalStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        PortalStorage storage = new PortalStorage();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Tag entryTag : tag.getList("portals", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entryTag;
            List<ItemStack> items = compound.contains("items")
                    ? LIST_CODEC.decode(ops, compound.get("items")).result()
                            .map(com.mojang.datafixers.util.Pair::getFirst).orElse(List.of())
                    : List.of();            storage.byPortal.put(compound.getString("portal"), new ArrayList<>(items));
        }
        for (Tag entryTag : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entryTag;
            storage.entryByPlayer.put(compound.getUUID("player"), compound.getString("portal"));
        }
        return storage;
    }

    private PortalStorage() {
    }
}
