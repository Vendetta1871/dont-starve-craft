package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: place a buffered structure at a block. */
public record PlacePayload(String recipe, BlockPos pos) implements CustomPacketPayload {

    public static final Type<PlacePayload> TYPE = new Type<>(DontStarveCraft.rl("place"));
    public static final StreamCodec<ByteBuf, PlacePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlacePayload::recipe,
            BlockPos.STREAM_CODEC, PlacePayload::pos,
            PlacePayload::new);

    @Override
    public Type<PlacePayload> type() {
        return TYPE;
    }
}
