package net.celestiald.dontstarve.revive;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Tracks which Meat Effigy each player is currently attuned to, so attuning a
 * new effigy de-attunes the old one (DST rule, resurrectionstatue.lua).
 * Stored inside the islands level.
 */
public final class EffigyState extends SavedData {

    private static final String NAME = "dontstarve_effigies";

    private final Map<UUID, BlockPos> byPlayer = new HashMap<>();

    public static EffigyState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EffigyState::new, EffigyState::load),
                NAME);
    }

    @Nullable
    public BlockPos getPos(UUID player) {
        return byPlayer.get(player);
    }

    public void setPos(UUID player, BlockPos pos) {
        byPlayer.put(player, pos);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag effigies = new ListTag();
        for (Map.Entry<UUID, BlockPos> entry : byPlayer.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("player", entry.getKey());
            entryTag.putInt("x", entry.getValue().getX());
            entryTag.putInt("y", entry.getValue().getY());
            entryTag.putInt("z", entry.getValue().getZ());
            effigies.add(entryTag);
        }
        tag.put("effigies", effigies);
        return tag;
    }

    public static EffigyState load(CompoundTag tag, HolderLookup.Provider registries) {
        EffigyState state = new EffigyState();
        for (Tag entryTag : tag.getList("effigies", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entryTag;
            state.byPlayer.put(compound.getUUID("player"),
                    new BlockPos(compound.getInt("x"), compound.getInt("y"), compound.getInt("z")));
        }
        return state;
    }

    private EffigyState() {
    }
}
