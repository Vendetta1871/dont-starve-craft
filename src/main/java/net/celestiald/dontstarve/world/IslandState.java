package net.celestiald.dontstarve.world;

import net.celestiald.dontstarve.init.DSAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
    /**
     * Seconds the current run has been played: counted only while living run
     * members are on the island, so time spent away does not pile up waves.
     */
    private int runSeconds = 0;
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

    /** Resets the per-run clocks when another island becomes current. */
    private void onIslandChanged() {
        runSeconds = 0;
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

    /** Seconds of live play on the current island (hound clock). */
    public int runSeconds() {
        return runSeconds;
    }

    /** Advances the run clock by one second of live play. */
    public void tickRunSecond() {
        runSeconds++;
        setDirty();
    }

    /** Island run the player belongs to, or -1 when they are not part of a run. */
    public static int runOf(Player player) {
        return player.getData(DSAttachments.RUN_ISLAND.get());
    }

    /** Whether the player is a member of the current island's run. */
    public boolean isCurrentRun(Player player) {
        return runOf(player) == islandIndex;
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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("island_index", islandIndex);
        tag.putInt("wipe_seconds_left", wipeSecondsLeft);
        tag.putInt("run_seconds", runSeconds);
        tag.putInt("hound_waves_sent", houndWavesSent);
        tag.putBoolean("hound_warned", houndWarned);
        return tag;
    }

    public static IslandState load(CompoundTag tag, HolderLookup.Provider registries) {
        IslandState state = new IslandState();
        state.islandIndex = tag.getInt("island_index");
        state.wipeSecondsLeft = tag.contains("wipe_seconds_left")
                ? tag.getInt("wipe_seconds_left") : -1;
        state.runSeconds = tag.getInt("run_seconds");
        state.houndWavesSent = tag.getInt("hound_waves_sent");
        state.houndWarned = tag.getBoolean("hound_warned");
        return state;
    }

    private IslandState() {
    }
}
