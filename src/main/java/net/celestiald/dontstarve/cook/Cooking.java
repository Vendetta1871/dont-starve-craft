package net.celestiald.dontstarve.cook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Crock Pot resolver (scripts/cooking.lua): ingredient values by DST prefab name,
 * GetIngredientValues, GetCandidateRecipes (every recipe whose test passes, only the highest
 * priority kept) and CalculateRecipe (weighted random among those). The recipes themselves are
 * {@link CookRecipeData}.
 *
 * <p>Items are mapped to DST prefab names in one place: {@link #prefab(Item)} / {@link #item(String)}.
 * A mod item {@code dontstarve:<prefab>} is the prefab itself, plus the vanilla items the project
 * uses for DST's goldnugget, flint and log.
 */
public final class Cooking {

    /** Ingredient counts or tag sums by name; 0 where Lua has nil. */
    public static final class Counts {
        private final Map<String, Double> values = new HashMap<>();

        public double get(String name) {
            return values.getOrDefault(name, 0.0);
        }

        void add(String name, double value) {
            values.merge(name, value, Double::sum);
        }

        public Map<String, Double> asMap() {
            return values;
        }
    }

    /** GetIngredientValues' result. */
    public record IngredientData(Counts names, Counts tags) {
    }

    /** ingredients[name].tags of cooking.lua. */
    private static final Map<String, Map<String, Double>> INGREDIENTS = new LinkedHashMap<>();

    /** cooking.lua aliases: "our naming conventions aren't completely consistent, sadly". */
    private static final Map<String, String> ALIASES = Map.of(
            "cookedsmallmeat", "smallmeat_cooked",
            "cookedmonstermeat", "monstermeat_cooked",
            "cookedmeat", "meat_cooked");

    /** DST prefabs that are a vanilla item in this project, and DST "log" (any spruce log). */
    private static final Map<String, Item> VANILLA = Map.of(
            "goldnugget", Items.GOLD_NUGGET,
            "flint", Items.FLINT,
            "log", Items.SPRUCE_LOG);

    private static final List<CookRecipe> RECIPES = new ArrayList<>();
    private static final Map<String, CookRecipe> BY_NAME = new LinkedHashMap<>();

    /** AddIngredientValues(names, tags, cancook, candry). */
    private static void values(String[] names, Map<String, Double> tags, boolean cancook, boolean candry) {
        for (String name : names) {
            INGREDIENTS.put(name, new LinkedHashMap<>(tags));
            if (cancook) {
                Map<String, Double> cooked = new LinkedHashMap<>();
                if (!tags.isEmpty()) {
                    cooked.put("precook", 1.0);
                }
                cooked.putAll(tags);
                INGREDIENTS.put(name + "_cooked", cooked);
            }
            if (candry) {
                Map<String, Double> dried = new LinkedHashMap<>();
                if (!tags.isEmpty()) {
                    dried.put("dried", 1.0);
                }
                dried.putAll(tags);
                INGREDIENTS.put(name + "_dried", dried);
            }
        }
    }

    private static void values(String name, Map<String, Double> tags, boolean cancook, boolean candry) {
        values(new String[] {name}, tags, cancook, candry);
    }

    private static Map<String, Double> tags(Object... kv) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        }
        return map;
    }

    static {
        // All tags: fruit, monster, sweetener, veggie, meat, fish, egg, decoration, fat, dairy, inedible, seed, magic
        values(new String[] {"pomegranate", "dragonfruit", "cave_banana"}, tags("fruit", 1), true, false);
        values("wormlight", tags("fruit", 1), false, false);
        values("wormlight_lesser", tags("fruit", .5), false, false);
        values("berries", tags("fruit", .5), true, false);
        values("berries_juicy", tags("fruit", .5), true, false);
        values("fig", tags("fruit", .5), true, false);
        values("durian", tags("fruit", 1, "monster", 1), true, false);

        values(new String[] {"honey", "honeycomb"}, tags("sweetener", 1), true, false);
        values("royal_jelly", tags("sweetener", 3), false, false);

        values(new String[] {"carrot", "corn", "pumpkin", "eggplant", "cutlichen", "asparagus", "onion", "garlic",
                "tomato", "potato", "pepper"}, tags("veggie", 1), true, false);
        values(new String[] {"red_cap", "green_cap", "blue_cap", "moon_cap"}, tags("veggie", .5), true, false);

        values("meat", tags("meat", 1), true, true);
        values("monstermeat", tags("meat", 1, "monster", 1), true, true);
        values(new String[] {"froglegs", "drumstick", "batwing"}, tags("meat", .5), true, false);
        values(new String[] {"smallmeat", "batnose"}, tags("meat", .5), true, true);

        values("eel", tags("meat", .5, "fish", 1), true, false);
        values("fish", tags("meat", 1, "fish", 1), true, false);

        values("pondeel", tags("meat", .5, "fish", 1), true, false);
        values("pondfish", tags("meat", .5, "fish", .5), false, false);
        values("fishmeat_small", tags("meat", .5, "fish", .5), true, true);
        values("fishmeat", tags("meat", 1, "fish", 1), true, true);
        // prefabs/oceanfishdef.lua cooker_ingredient_value: SMALL {meat .5, fish .5}, MEDIUM {meat 1, fish 1},
        // MEDIUM_ICE adds frozen 1; the corn fish (small_5, medium_5) are {veggie 1}.
        for (int i = 1; i <= 9; i++) {
            values("oceanfish_small_" + i + "_inv", i == 5 ? tags("veggie", 1) : tags("meat", .5, "fish", .5), false, false);
            values("oceanfish_medium_" + i + "_inv", i == 5 ? tags("veggie", 1)
                    : i == 8 ? tags("meat", 1, "fish", 1, "frozen", 1) : tags("meat", 1, "fish", 1), false, false);
        }

        values("kelp", tags("veggie", .5), true, true);

        values("mandrake", tags("veggie", 1, "magic", 1), true, false);
        values("egg", tags("egg", 1), true, false);
        values("tallbirdegg", tags("egg", 4), true, false);
        values("bird_egg", tags("egg", 1), true, false);
        values("butterflywings", tags("decoration", 2), false, false);
        values("moonbutterflywings", tags("decoration", 2), false, false);
        values("butter", tags("fat", 1, "dairy", 1), false, false);
        values("twigs", tags("inedible", 1), false, false);
        values("lightninggoathorn", tags("inedible", 1), false, false);

        values("ice", tags("frozen", 1), false, false);
        values("mole", tags("meat", .5), false, false);
        values("cactus_meat", tags("veggie", 1), true, false);
        values("rock_avocado_fruit_ripe", tags("veggie", 1), true, false);
        values("watermelon", tags("fruit", 1), true, false);
        values("cactus_flower", tags("veggie", .5), false, false);
        values("acorn", tags("seed", 1), false, false);
        values("acorn_cooked", tags("seed", 1), false, false);
        values("goatmilk", tags("dairy", 1), false, false);

        values("milkywhites", tags("dairy", 1), false, false);

        values("nightmarefuel", tags("inedible", 1, "magic", 1), false, false);
        values("boneshard", tags("inedible", 1), false, false);

        values("wobster_sheller_land", tags("meat", 1.0, "fish", 1.0), false, false);

        values(new String[] {"barnacle", "barnacle_cooked"}, tags("meat", 0.25, "fish", 0.25), false, false);

        values(new String[] {"plantmeat", "plantmeat_cooked"}, tags("meat", 1), false, false);

        values("refined_dust", tags("decoration", 2), false, false);

        values("forgetmelots", tags("decoration", 1), false, false);

        values(new String[] {"trunk_summer", "trunk_winter", "trunk_cooked"}, tags("meat", 1), false, false);

        values("ancientfruit_nightvision", tags("fruit", 1), true, false);

        // The first time dried prefabs are listed explicitly; the originals are not ingredients.
        values(new String[] {"petals_dried", "foliage_dried", "succulent_picked_dried", "firenettles_dried",
                "tillweed_dried", "moon_tree_blossom_dried", "forgetmelots_dried"}, tags("decoration", 1, "dried", 1), false, false);
        values("petals_evil_dried", tags("decoration", 1, "magic", .5, "dried", 1), false, false);

        values("wx78_foodbrick", tags("inedible", 1), false, false);

        values("mitegland", tags("meat", .5, "monster", .5), true, false);

        CookRecipeData.register(recipe -> {
            RECIPES.add(recipe);
            BY_NAME.put(recipe.name(), recipe);
        });
    }

    // ---- item <-> prefab ----

    /** The mod or vanilla item standing for a DST prefab, null when it does not exist (yet). */
    @Nullable
    public static Item item(String prefab) {
        Item vanilla = VANILLA.get(prefab);
        if (vanilla != null) {
            return vanilla;
        }
        ResourceLocation id = DontStarveCraft.rl(prefab);
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    /** The DST prefab name of an item, null when it is not a DST prefab (vanilla stand-ins excepted). */
    @Nullable
    public static String prefab(Item item) {
        if (item == Items.GOLD_NUGGET) {
            return "goldnugget";
        }
        if (item == Items.FLINT) {
            return "flint";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return DontStarveCraft.MODID.equals(key.getNamespace()) ? key.getPath() : null;
    }

    /** IsCookingIngredient. */
    public static boolean isCookingIngredient(String prefab) {
        return INGREDIENTS.containsKey(ALIASES.getOrDefault(prefab, prefab));
    }

    /** A stack the pot accepts: its item exists and has cooking ingredient values. */
    public static boolean isIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String prefab = prefab(stack.getItem());
        return prefab != null && isCookingIngredient(prefab);
    }

    /** The prefab names of the DST ingredients, for tests. */
    public static java.util.Set<String> ingredientNames() {
        return java.util.Collections.unmodifiableSet(INGREDIENTS.keySet());
    }

    public static Map<String, Double> ingredientTags(String prefab) {
        Map<String, Double> tags = INGREDIENTS.get(ALIASES.getOrDefault(prefab, prefab));
        return tags == null ? null : java.util.Collections.unmodifiableMap(tags);
    }

    // ---- resolver ----

    /** Every recipe in the data, in preparedfoods.lua order. */
    public static List<CookRecipe> recipes() {
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    @Nullable
    public static CookRecipe recipe(String name) {
        return BY_NAME.get(name);
    }

    /** GetIngredientValues: counts every prefab, sums the tags of those that are ingredients. */
    public static IngredientData ingredientData(List<String> prefabs) {
        Counts names = new Counts();
        Counts tags = new Counts();
        for (String raw : prefabs) {
            String name = ALIASES.getOrDefault(raw, raw);
            names.add(name, 1);
            Map<String, Double> data = INGREDIENTS.get(name);
            if (data != null) {
                data.forEach(tags::add);
            }
        }
        return new IngredientData(names, tags);
    }

    /** GetCandidateRecipes: the recipes that pass their test, only the highest priority group. */
    public static List<CookRecipe> candidates(IngredientData data) {
        List<CookRecipe> passing = new ArrayList<>();
        for (CookRecipe recipe : RECIPES) {
            if (recipe.test(data.names(), data.tags())) {
                passing.add(recipe);
            }
        }
        if (passing.isEmpty()) {
            return passing;
        }
        double top = passing.stream().mapToDouble(CookRecipe::priority).max().getAsDouble();
        passing.removeIf(r -> r.priority() < top);
        passing.sort(Comparator.comparing(CookRecipe::name));
        return passing;
    }

    /** CalculateRecipe: a weighted random pick among the top-priority candidates; null if there are none. */
    @Nullable
    public static CookRecipe calculate(List<String> prefabs, RandomSource random) {
        List<CookRecipe> candidates = candidates(ingredientData(prefabs));
        double total = 0;
        for (CookRecipe recipe : candidates) {
            total += recipe.weight();
        }
        double val = random.nextDouble() * total;
        for (CookRecipe recipe : candidates) {
            val -= recipe.weight();
            if (val <= 0) {
                return recipe;
            }
        }
        return null;
    }

    private Cooking() {
    }
}
