package net.celestiald.dontstarve.world;

import net.celestiald.dontstarve.DSRefs;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Vanilla blocks used as DS scenery must not leak vanilla loot on the
 * islands: evergreen leaves (vanilla spruce leaves) drop no saplings or
 * sticks — DS trees give nothing from their crown.
 */
public final class DSBlockDrops {

    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getLevel().dimension().equals(DSRefs.ISLANDS) && event.getState().is(BlockTags.LEAVES)) {
            event.getDrops().clear();
        }
    }

    private DSBlockDrops() {
    }
}
