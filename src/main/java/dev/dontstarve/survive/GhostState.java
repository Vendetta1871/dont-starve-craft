package dev.dontstarve.survive;

import com.mojang.serialization.Codec;
import dev.dontstarve.DontStarveCraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/**
 * Per-player ghost flag, mirroring DST's "playerghost" tag (docs/dont-starve/07).
 * A player who dies inside the islands dimension does not respawn: they linger as
 * an invisible, invulnerable, flying spirit until revived (planned: Meat Effigy /
 * Telltale Heart) or until the all-ghost wipe ejects them to the overworld.
 * The flag is synced to clients so future overlays/effects can react to it.
 */
public final class GhostState {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DontStarveCraft.MODID);

    public static final Supplier<AttachmentType<Boolean>> GHOST = ATTACHMENTS.register("ghost",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static boolean isGhost(Player player) {
        return player.getData(GHOST.get());
    }

    public static void becomeGhost(ServerPlayer player) {
        player.setData(GHOST.get(), true);
        player.setHealth(player.getMaxHealth());
        applyGhostPerks(player);
        // Mobs that were chasing the player before death lose interest in the spirit.
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(32.0), m -> m.getTarget() == player)) {
            mob.setTarget(null);
            mob.setAggressive(false);
        }
    }

    public static void clear(ServerPlayer player) {
        player.setData(GHOST.get(), false);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    /**
     * Re-assert ghost perks; abilities and effects can be lost on relog or
     * dimension change, and ghosts have no way to re-apply them by themselves.
     */
    public static void upkeep(ServerPlayer player) {
        if (isGhost(player) && (!player.hasEffect(MobEffects.INVISIBILITY)
                || !player.getAbilities().mayfly)) {
            applyGhostPerks(player);
        }
    }

    private static void applyGhostPerks(ServerPlayer player) {
        // Infinite (duration -1), silent, no icon: ghosts are felt, not seen.
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false, false));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private GhostState() {
    }
}
