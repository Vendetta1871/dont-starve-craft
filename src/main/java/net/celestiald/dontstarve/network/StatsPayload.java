package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player's DS hunger and sanity (DS units), the sanity rate per DS s and the temperature. */
public record StatsPayload(float hunger, float sanity, boolean sane, float sanityRate, float temperature) implements CustomPacketPayload {

    public static final Type<StatsPayload> TYPE = new Type<>(DontStarveCraft.rl("stats"));
    public static final StreamCodec<ByteBuf, StatsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StatsPayload::hunger,
            ByteBufCodecs.FLOAT, StatsPayload::sanity,
            ByteBufCodecs.BOOL, StatsPayload::sane,
            ByteBufCodecs.FLOAT, StatsPayload::sanityRate,
            ByteBufCodecs.FLOAT, StatsPayload::temperature,
            StatsPayload::new);

    @Override
    public Type<StatsPayload> type() {
        return TYPE;
    }
}
