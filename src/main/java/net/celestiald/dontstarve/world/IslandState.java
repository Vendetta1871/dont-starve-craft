package net.celestiald.dontstarve.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

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
    /** All-ghost wipe countdown in seconds; -1 means inactive. */
    private int wipeSecondsLeft = -1;
    /** islands-level game time when the current island went active (hound clock). */
    private long runStartGameTime = -1;
    /** Hound waves already sent against the current island. */
    private int houndWavesSent = 0;
    /** Whether the warning howl for the next wave has already been broadcast. */
    private boolean houndWarned = false;

    public static IslandState get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(IslandState::new, IslandState::load),
                NAME);
    }
    public int islandIndex() {
        return islandIndex;
    }

    public int wipeSecondsLeft() {
        return wipeSecondsLeft;
    }

    public void setWipeSecondsLeft(int seconds) {
        if (seconds != wipeSecondsLeft) {
            wipeSecondsLeft = seconds;
            setDirty();
        }
    }

    /** Abandons the current island and activates the next one. */
    private void onIslandChanged() {
        runStartGameTime = -1; // first tick of HoundWave re-anchors it
        houndWavesSent = 0;
        houndWarned = false;
    }

    /** Jumps to an arbitrary island index (used by /ds island goto and tests). */
    public void setIndex(int index) {
        islandIndex = index;
        onIslandChanged();
        setDirty();
    }

    /** Abandons the current island and activates the next one. */
    public int advance() {
        islandIndex++;
        onIslandChanged();
        setDirty();
        return islandIndex;
    }

    /** Game time (islands level) the current run started at; -1 = not anchored. */
    public long runStartGameTime() {
        return runStartGameTime;
    }

    public void setRunStartGameTime(long time) {
        if (time != runStartGameTime) {
            runStartGameTime = time;
            setDirty();
        }
    }

    public int houndWavesSent() {
        return houndWavesSent;
    }

    public boolean houndWarned() {
        return houndWarned;
    }

    public void setHoundWarned(boolean warned) {
        if (warned != houndWarned) {
            houndWarned = warned;
            setDirty();
        }
    }

    public void setHoundWavesSent(int waves) {
        if (waves != houndWavesSent) {
            houndWavesSent = waves;
            setDirty();
        }
    }

    public static BlockPos islandCenter(int index) {
        return new BlockPos(index * SPACING, 0, 0);
    }

    public static Vec3 arrivalPos(int index) {
        BlockPos center = islandCenter(index);
        return new Vec3(center.getX() + 0.5, PLAZA_Y + 1.0, center.getZ() + 0.5);
    }

    /**
     * Pre-generates a 5x5 chunk area around the arrival plaza of the given
     * island so players never fall through ungenerated chunks on arrival.
     */
    public static void pregenArrival(ServerLevel islands, int index) {
        ChunkPos spawnChunk = new ChunkPos(islandCenter(index));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                islands.getChunk(spawnChunk.x + dx, spawnChunk.z + dz, ChunkStatus.FULL, true);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("island_index", islandIndex);
        tag.putInt("wipe_seconds_left", wipeSecondsLeft);
        tag.putLong("run_start_game_time", runStartGameTime);
        tag.putInt("hound_waves_sent", houndWavesSent);
        tag.putBoolean("hound_warned", houndWarned);
        return tag;
    }

    public static IslandState load(CompoundTag tag, HolderLookup.Provider registries) {
        IslandState state = new IslandState();
        state.islandIndex = tag.getInt("island_index");
        state.wipeSecondsLeft = tag.contains("wipe_seconds_left")
                ? tag.getInt("wipe_seconds_left") : -1;
        state.runStartGameTime = tag.getLong("run_start_game_time");
        state.houndWavesSent = tag.getInt("hound_waves_sent");
        state.houndWarned = tag.getBoolean("hound_warned");
        return state;
    }

    private IslandState() {
    }
}
