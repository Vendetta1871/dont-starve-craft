package net.celestiald.dontstarve.init;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-player data attachments of the islands run. */
public final class DSAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DontStarveCraft.MODID);

    /** DST "playerghost" tag; synced so client overlays can react to it. */
    public static final Supplier<AttachmentType<Boolean>> GHOST = ATTACHMENTS.register("ghost",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    /**
     * Index of the island run the player joined through the marble portal;
     * -1 = not part of any run (never entered, already home, or an admin
     * teleport); set when the player reads the Codex Umbra. A player whose run is not the current island is stale: the
     * run ended (wipe) while they were offline.
     */
    public static final Supplier<AttachmentType<Integer>> RUN_ISLAND = ATTACHMENTS.register("run_island",
            () -> AttachmentType.builder(() -> -1)
                    .serialize(Codec.INT)
                    .build());

    /** Where the player read the Codex Umbra: the way home leads back there. */
    public static final Supplier<AttachmentType<GlobalPos>> RETURN_POINT = ATTACHMENTS.register("return_point",
            () -> AttachmentType.builder(() -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
                    .serialize(GlobalPos.CODEC)
                    .build());

    /** The free starting Codex Umbra was handed out (kept through deaths). */
    public static final Supplier<AttachmentType<Boolean>> CODEX_GIVEN = ATTACHMENTS.register("codex_given",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    /** Last position a living player stood on solid ground in the islands (void-death graves). */
    public static final Supplier<AttachmentType<BlockPos>> LAST_GROUND = ATTACHMENTS.register("last_ground",
            () -> AttachmentType.builder(() -> BlockPos.ZERO)
                    .serialize(BlockPos.CODEC)
                    .build());

    /** DS hunger and sanity on the islands (health is the vanilla health bar). */
    public static final Supplier<AttachmentType<net.celestiald.dontstarve.survive.DSStats>> STATS = ATTACHMENTS.register("stats",
            () -> AttachmentType.builder(() -> net.celestiald.dontstarve.survive.DSStats.FULL)
                    .serialize(net.celestiald.dontstarve.survive.DSStats.CODEC)
                    .build());

    private DSAttachments() {
    }
}
