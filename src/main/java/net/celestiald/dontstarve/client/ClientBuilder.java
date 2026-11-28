package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.craft.BuilderData;
import net.celestiald.dontstarve.network.BuilderPayload;

/** The client's copy of the player's builder state (learned and buffered recipes). */
public final class ClientBuilder {

    private static BuilderData data = BuilderData.EMPTY;

    public static void receive(BuilderPayload payload) {
        data = new BuilderData(payload.recipes(), payload.buffered());
    }

    public static BuilderData data() {
        return data;
    }

    private ClientBuilder() {
    }
}
