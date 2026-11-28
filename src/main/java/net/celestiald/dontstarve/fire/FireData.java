package net.celestiald.dontstarve.fire;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-level fire state: one entry for every burnable that holds heat, smoulders or burns
 * (the burnable + propagator component fields: currentheat, pauseheating, burning,
 * smoldertimeremaining, the burn task). Keyed by the packed burn position. Saved with the level,
 * so fires, smouldering and accumulated heat survive a restart.
 */
public final class FireData extends SavedData {

    private static final String NAME = "dontstarve_fire";

    /** The fire fields of one burnable. */
    public static final class Entry {
        /** propagator.currentheat. */
        public double heat;
        /** propagator.pauseheating: heat above the flashpoint was reached, no more heat is accepted. */
        public boolean paused;
        /** burnable.burning. */
        public boolean burning;
        /** Game time at which the burn task finishes (burnable DoneBurning); -1 = burns until put out. */
        public long burnEnd = -1;
        /** burnable.smoldertimeremaining in DS seconds, negative when not smouldering. */
        public double smolder = -1;
        /** The invisible light block this fire placed. */
        @Nullable
        public BlockPos light;
        /** Warming temperature of the burning fire, refreshed by the engine (not saved). */
        public double warmth;

        public boolean smoldering() {
            return smolder >= 0;
        }

        boolean idle() {
            return !burning && !smoldering() && heat <= 0 && !paused;
        }
    }

    private final Map<Long, Entry> entries = new HashMap<>();

    public static FireData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FireData::new, FireData::load), NAME);
    }

    public Map<Long, Entry> entries() {
        return entries;
    }

    @Nullable
    public Entry at(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    public Entry getOrCreate(BlockPos pos) {
        setDirty();
        return entries.computeIfAbsent(pos.asLong(), k -> new Entry());
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    static boolean idle(Entry entry) {
        return entry.idle();
    }

    private static FireData load(CompoundTag tag, HolderLookup.Provider registries) {
        FireData data = new FireData();
        for (Tag t : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Entry e = new Entry();
            e.heat = c.getDouble("heat");
            e.paused = c.getBoolean("paused");
            e.burning = c.getBoolean("burning");
            e.burnEnd = c.getLong("burnEnd");
            e.smolder = c.getDouble("smolder");
            if (c.contains("light")) {
                e.light = BlockPos.of(c.getLong("light"));
            }
            data.entries.put(c.getLong("pos"), e);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> en : entries.entrySet()) {
            Entry e = en.getValue();
            CompoundTag c = new CompoundTag();
            c.putLong("pos", en.getKey());
            c.putDouble("heat", e.heat);
            c.putBoolean("paused", e.paused);
            c.putBoolean("burning", e.burning);
            c.putLong("burnEnd", e.burnEnd);
            c.putDouble("smolder", e.smolder);
            if (e.light != null) {
                c.putLong("light", e.light.asLong());
            }
            list.add(c);
        }
        tag.put("entries", list);
        return tag;
    }
}
