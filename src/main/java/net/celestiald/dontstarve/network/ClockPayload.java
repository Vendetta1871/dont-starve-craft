package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the current run's clock (run ticks and whether it is running).
 * The client derives phase, season, moon and ambient light with DSClock.
 */
public record ClockPayload(long runTicks, boolean running) implements CustomPacketPayload {

    public static final Type<ClockPayload> TYPE = new Type<>(DontStarveCraft.rl("clock"));
    public static final StreamCodec<ByteBuf, ClockPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ClockPayload::runTicks,
            ByteBufCodecs.BOOL, ClockPayload::running,
            ClockPayload::new);

    @Override
    public Type<ClockPayload> type() {
        return TYPE;
    }
}
