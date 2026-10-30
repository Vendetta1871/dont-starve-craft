package dev.dontstarve.portal;

import dev.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class DSTags {

    /** Any block that can form the marble portal frame. */
    public static final TagKey<Block> MARBLE =
            TagKey.create(Registries.BLOCK, DontStarveCraft.rl("marble"));

    private DSTags() {
    }
}
