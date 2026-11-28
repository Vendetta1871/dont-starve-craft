package net.celestiald.dontstarve.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** builder.lua OnSave: the learned recipes and the structures crafted but not yet placed. */
public record BuilderData(List<String> recipes, List<String> buffered) {

    public static final BuilderData EMPTY = new BuilderData(List.of(), List.of());

    public static final Codec<BuilderData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("recipes").forGetter(BuilderData::recipes),
            Codec.STRING.listOf().fieldOf("buffered").forGetter(BuilderData::buffered)
    ).apply(i, BuilderData::new));

    public boolean knows(String recipe) {
        return recipes.contains(recipe);
    }

    public boolean isBuffered(String recipe) {
        return buffered.contains(recipe);
    }

    public BuilderData withRecipe(String recipe) {
        return knows(recipe) ? this : new BuilderData(add(recipes, recipe), buffered);
    }

    public BuilderData withBuffered(String recipe, boolean on) {
        Set<String> set = new LinkedHashSet<>(buffered);
        if (on) {
            set.add(recipe);
        } else {
            set.remove(recipe);
        }
        return new BuilderData(recipes, List.copyOf(set));
    }

    private static List<String> add(List<String> list, String value) {
        Set<String> set = new LinkedHashSet<>(list);
        set.add(value);
        return List.copyOf(set);
    }
}
