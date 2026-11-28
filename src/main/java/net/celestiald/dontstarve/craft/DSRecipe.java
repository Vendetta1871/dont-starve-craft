package net.celestiald.dontstarve.craft;

import java.util.List;
import java.util.Map;
import net.celestiald.dontstarve.item.DSTorch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * One DST crafting recipe (recipe.lua Recipe2) resolved to Minecraft items.
 *
 * @param name       recipe name (DST prefab id of the recipe)
 * @param product    the crafted item; for placer recipes the structure's block item
 * @param numToGive  stack size handed out (config.numtogive)
 * @param level      tech levels the recipe needs, zero levels left out (TECH.*)
 * @param placer     a structure placed in the world after crafting (config.placer)
 * @param nounlock   never learned: every build needs the prototyper (config.nounlock)
 * @param minSpacing free radius around a placed structure (config.min_spacing, default 3.2)
 * @param filters    crafting menu tabs that list the recipe (recipes_filter.lua)
 */
public record DSRecipe(String name, Item product, int numToGive, List<Ing> ingredients,
                       List<CharacterIng> characterIngredients, Map<String, Integer> level,
                       boolean placer, boolean nounlock, double minSpacing, List<String> filters) {

    /** An item ingredient: a DST prefab matched by Minecraft items. */
    public record Ing(String prefab, Ingredient match, int amount, Item icon) {
    }

    /** DST CHARACTER_INGREDIENT: a cost paid from the crafter's stats, in DS units. */
    public record CharacterIng(Type type, int amount) {
        public enum Type { HEALTH, SANITY }
    }

    /** A fresh product stack. The DS torch carries its fuel gauge from the start. */
    public ItemStack result() {
        if (product == Items.TORCH) {
            return DSTorch.crafted();
        }
        return new ItemStack(product, numToGive);
    }

    /** NONE tech: every survivor knows it from the start (builder.lua KnowsRecipe without bonuses). */
    public boolean noTech() {
        return level.isEmpty();
    }
}
