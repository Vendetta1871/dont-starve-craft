package net.celestiald.dontstarve.network;

import net.celestiald.dontstarve.craft.Builder;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the mod's custom packets. */
public final class DSNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        // Client-bound handler bodies reference client-only code; they only ever run on a client.
        event.registrar("1").optional().playToClient(ShotPayload.TYPE, ShotPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> net.celestiald.dontstarve.client.ScreenshotTaker.request(payload)))
                .playToClient(ClockPayload.TYPE, ClockPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.ClientClock.receive(payload)))
                .playToClient(StatsPayload.TYPE, StatsPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.StatsHud.receive(payload)))
                .playToClient(BuilderPayload.TYPE, BuilderPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.ClientBuilder.receive(payload)))
                .playToClient(OpenCraftingPayload.TYPE, OpenCraftingPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> net.celestiald.dontstarve.client.CraftingScreen.open(payload)))
                .playToServer(CraftPayload.TYPE, CraftPayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> Builder.makeFromMenu((ServerPlayer) context.player(), payload.recipe())))
                .playToServer(PlacePayload.TYPE, PlacePayload.CODEC,
                        (payload, context) -> context.enqueueWork(
                                () -> Builder.placeAt((ServerPlayer) context.player(), payload.recipe(), payload.pos())));
    }

    private DSNetwork() {
    }
}
