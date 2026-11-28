package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the build/prototype button of the crafting menu for a recipe. */
public record CraftPayload(String recipe) implements CustomPacketPayload {

    public static final Type<CraftPayload> TYPE = new Type<>(DontStarveCraft.rl("craft"));
    public static final StreamCodec<ByteBuf, CraftPayload> CODEC =
            ByteBufCodecs.STRING_UTF8.map(CraftPayload::new, CraftPayload::recipe);

    @Override
    public Type<CraftPayload> type() {
        return TYPE;
    }
}
