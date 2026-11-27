package net.celestiald.dontstarve.survive;

import net.celestiald.dontstarve.init.DSAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player ghost flag, mirroring DST's "playerghost" tag (docs/dont-starve/07).
 * A player who dies inside the islands dimension does not respawn: they linger as
 * an invisible, invulnerable, flying spirit until revived (Meat Effigy /
 * Telltale Heart) or until the all-ghost wipe ejects them to the overworld.
 */
public final class GhostState {

    public static boolean isGhost(Player player) {
        return player.getData(DSAttachments.GHOST.get());
    }

    public static void becomeGhost(ServerPlayer player) {
        player.setData(DSAttachments.GHOST.get(), true);
        Survival.onBecomeGhost(player);
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
        player.setData(DSAttachments.GHOST.get(), false);
        player.removeEffect(MobEffects.INVISIBILITY);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
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
