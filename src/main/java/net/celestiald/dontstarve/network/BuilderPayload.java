package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player's learned recipes and buffered (paid, unplaced) structures. */
public record BuilderPayload(List<String> recipes, List<String> buffered) implements CustomPacketPayload {

    public static final Type<BuilderPayload> TYPE = new Type<>(DontStarveCraft.rl("builder"));
    public static final StreamCodec<ByteBuf, BuilderPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BuilderPayload::recipes,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BuilderPayload::buffered,
            BuilderPayload::new);

    @Override
    public Type<BuilderPayload> type() {
        return TYPE;
    }
}
