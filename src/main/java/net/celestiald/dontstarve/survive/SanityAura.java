package net.celestiald.dontstarve.survive;

import net.minecraft.world.entity.player.Player;

/**
 * DST components/sanityaura.lua: an entity that changes the sanity of players near
 * it, in DS sanity per DS second at distance 1, falling off with distance squared.
 */
public interface SanityAura {

    float sanityAura(Player observer);
}
