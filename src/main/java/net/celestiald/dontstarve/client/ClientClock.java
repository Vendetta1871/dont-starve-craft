package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.clock.DSClock;
import net.celestiald.dontstarve.network.ClockPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** The client's copy of the run clock: synced every second, ticked locally in between. */
public final class ClientClock {

    private static long runTicks;
    private static boolean running;
    private static boolean known;
    private static DSClock.State cached;

    public static void receive(ClockPayload payload) {
        runTicks = payload.runTicks();
        running = payload.running();
        known = true;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(DSRefs.ISLANDS)) {
            known = false;
            return;
        }
        if (known && running && !mc.isPaused()) {
            runTicks++;
        }
    }

    /** Whether the player is on the islands and the clock has been received. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return known && mc.level != null && mc.level.dimension().equals(DSRefs.ISLANDS);
    }

    public static DSClock.State state() {
        if (cached == null || cached.runTicks() != runTicks) {
            cached = DSClock.at(runTicks);
        }
        return cached;
    }

    private ClientClock() {
    }
}
