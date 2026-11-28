package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: open the inventory with the crafting menu on a tab and recipe
 * ("" keeps the default). Sent by {@code /ds ui}; the headless test client uses it to
 * photograph the menu.
 */
public record OpenCraftingPayload(String filter, String recipe) implements CustomPacketPayload {

    public static final Type<OpenCraftingPayload> TYPE = new Type<>(DontStarveCraft.rl("open_crafting"));
    public static final StreamCodec<ByteBuf, OpenCraftingPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenCraftingPayload::filter,
            ByteBufCodecs.STRING_UTF8, OpenCraftingPayload::recipe,
            OpenCraftingPayload::new);

    @Override
    public Type<OpenCraftingPayload> type() {
        return TYPE;
    }
}
