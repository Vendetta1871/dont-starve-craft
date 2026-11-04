package net.celestiald.dontstarve;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Registry keys for the Don't Starve islands dimension and its content.
 */
public final class DSRefs {

    public static final ResourceKey<Level> ISLANDS =
            ResourceKey.create(Registries.DIMENSION, DontStarveCraft.rl("islands"));

    public static final ResourceKey<DimensionType> ISLANDS_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, DontStarveCraft.rl("islands"));

    public static final ResourceKey<Biome> THE_WILDS =
            ResourceKey.create(Registries.BIOME, DontStarveCraft.rl("the_wilds"));

    private DSRefs() {
    }
}
