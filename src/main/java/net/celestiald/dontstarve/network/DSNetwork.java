package net.celestiald.dontstarve.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the mod's custom packets. */
public final class DSNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        // The handler body references client-only code; it only ever runs on a client.
        event.registrar("1").optional().playToClient(ShotPayload.TYPE, ShotPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> net.celestiald.dontstarve.client.ScreenshotTaker.request(payload)))
                .playToClient(ClockPayload.TYPE, ClockPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.ClientClock.receive(payload)))
                .playToClient(StatsPayload.TYPE, StatsPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.StatsHud.receive(payload)));
    }

    private DSNetwork() {
    }
}
