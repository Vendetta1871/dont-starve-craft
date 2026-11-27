package net.celestiald.dontstarve.clock;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.network.ClockPayload;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Runs the current island's DST clock on the server: advances it while members of
 * the run are on the island, and keeps clients in sync — a {@link ClockPayload}
 * every second, and every tick a time packet that puts the sun where the DS phase
 * is (the islands level otherwise shares the overworld's day time).
 */
public final class RunClock {

    private static DSClock.State cached;
    private static long cachedTicks = -1;

    /** The clock state of the current run. */
    public static DSClock.State state(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        long ticks = overworld == null ? 0 : IslandState.get(overworld).runTicks();
        if (cached == null || cachedTicks != ticks) {
            cached = DSClock.at(ticks);
            cachedTicks = ticks;
        }
        return cached;
    }

    /** DS "isday" for a level: the run clock on the islands, vanilla elsewhere. */
    public static boolean isDay(Level level) {
        if (level.dimension().equals(DSRefs.ISLANDS) && level.getServer() != null) {
            return state(level.getServer()).isDay();
        }
        return level.isDay();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }
        IslandState island = IslandState.get(overworld);
        boolean running = islands.players().stream().anyMatch(island::isCurrentRun);
        if (running) {
            island.tickRun();
        }
        DSClock.State state = state(server);
        long dayTime = DSClock.minecraftDayTime(state);
        boolean sync = server.getTickCount() % 20 == 0;
        for (ServerPlayer player : islands.players()) {
            player.connection.send(new ClientboundSetTimePacket(islands.getGameTime(), dayTime, false));
            if (sync) {
                PacketDistributor.sendToPlayer(player, new ClockPayload(island.runTicks(), running));
            }
        }
    }

    private RunClock() {
    }
}
