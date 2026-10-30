package dev.dontstarve.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;import net.minecraft.world.phys.Vec3;

/**
 * Tracks which Don't Starve island is the current one. Instead of deleting
 * and recreating a dimension on death, every run gets a fresh island far away
 * along the X axis inside one persistent islands level; the portal always
 * sends players to the island of the current index. Old islands are simply
 * abandoned in the void.
 */
public final class IslandState extends SavedData {

    private static final String NAME = "dontstarve_islands";

    /** Distance between island centers along the X axis. */
    public static final int SPACING = 8192;
    /** Radius of the generated island disc. */
    public static final int RADIUS = 96;
    /** Y of the top block of the spawn plaza; deterministic arrival height. */
    public static final int PLAZA_Y = 74;

    private int islandIndex = 0;

    public static IslandState get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(IslandState::new, IslandState::load),
                NAME);
    }
    public int islandIndex() {
        return islandIndex;
    }

    /** Abandons the current island and activates the next one. */
    public int advance() {
        islandIndex++;
        setDirty();
        return islandIndex;
    }

    public static BlockPos islandCenter(int index) {
        return new BlockPos(index * SPACING, 0, 0);
    }

    public static Vec3 arrivalPos(int index) {
        BlockPos center = islandCenter(index);
        return new Vec3(center.getX() + 0.5, PLAZA_Y + 1.0, center.getZ() + 0.5);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("island_index", islandIndex);
        return tag;
    }

    public static IslandState load(CompoundTag tag, HolderLookup.Provider registries) {
        IslandState state = new IslandState();
        state.islandIndex = tag.getInt("island_index");
        return state;
    }

    private IslandState() {
    }
}
