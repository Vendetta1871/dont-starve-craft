package net.celestiald.dontstarve.network;

import io.netty.buffer.ByteBuf;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: save a screenshot named {@code name}.png in the client's
 * screenshots folder, with or without the HUD. Sent by {@code /ds shot}; the
 * headless test client uses it to photograph scenes for visual checks.
 */
public record ShotPayload(String name, boolean hud) implements CustomPacketPayload {

    public static final Type<ShotPayload> TYPE = new Type<>(DontStarveCraft.rl("shot"));
    public static final StreamCodec<ByteBuf, ShotPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShotPayload::name,
            ByteBufCodecs.BOOL, ShotPayload::hud,
            ShotPayload::new);

    @Override
    public Type<ShotPayload> type() {
        return TYPE;
    }
}
