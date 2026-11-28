package net.celestiald.dontstarve.craft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The DST recipe book, read from the extracted DST data shipped in the jar
 * ({@code data/dontstarve/dst/recipes.json} and {@code crafting.json}, copied from
 * docs/dont-starve/data at build time).
 * A recipe is in the book once its product and all its ingredients exist in the mod,
 * so recipes appear by themselves as content is ported.
 */
public final class DSRecipes {

    /** DST prefabs that are the same thing as a Minecraft item. */
    private static final Map<String, Item> SAME_AS_VANILLA = Map.of(
            "goldnugget", Items.GOLD_NUGGET,
            "flint", Items.FLINT,
            "torch", Items.TORCH);
    /** DST "log": any tree log; our trees are vanilla spruce. */
    private static final TagKey<Item> LOGS = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("logs"));
    /**
     * Filters that are not plain tabs (recipes_filter.lua: favourites, stations, events, mods,
     * character); EVERYTHING has no list of its own and is built from all recipes.
     */
    private static final Set<String> NOT_TABS = Set.of("FAVORITES", "CRAFTING_STATION", "SPECIAL_EVENT", "MODS", "CHARACTER", "EVERYTHING");
    public static final String EVERYTHING = "EVERYTHING";

    public record Filter(String name, List<DSRecipe> recipes) {
    }

    private record Book(Map<String, DSRecipe> byName, List<DSRecipe> ordered, List<Filter> filters) {
    }

    private static volatile Book book;

    public static DSRecipe get(String name) {
        return book().byName.get(name);
    }

    /** Every recipe in menu order. */
    public static List<DSRecipe> all() {
        return book().ordered;
    }

    /** The menu tabs in DST order; EVERYTHING last. Tabs may be empty. */
    public static List<Filter> filters() {
        return book().filters;
    }

    private static Book book() {
        Book b = book;
        if (b == null) {
            synchronized (DSRecipes.class) {
                if (book == null) {
                    book = load();
                }
                b = book;
            }
        }
        return b;
    }

    private static Book load() {
        JsonObject recipes = read("recipes.json");
        JsonObject crafting = read("crafting.json");
        JsonObject tech = crafting.getAsJsonObject("tech");

        // Tab lists in DST order; a recipe's menu position is its first appearance.
        Map<String, List<String>> tabLists = new LinkedHashMap<>();
        for (JsonElement e : crafting.getAsJsonArray("filters")) {
            JsonObject f = e.getAsJsonObject();
            String name = f.get("name").getAsString();
            if (NOT_TABS.contains(name)) {
                continue;
            }
            List<String> list = new ArrayList<>();
            // The extractor's JSON writer cannot tell an empty Lua table from an empty map: {} = no recipes.
            if (f.get("recipes").isJsonArray()) {
                for (JsonElement r : f.getAsJsonArray("recipes")) {
                    list.add(r.getAsString());
                }
            }
            tabLists.put(name, list);
        }
        Map<String, JsonObject> defs = new TreeMap<>();
        recipes.entrySet().forEach(en -> defs.put(en.getKey(), en.getValue().getAsJsonObject()));

        Map<String, List<String>> filtersOf = new HashMap<>();
        LinkedHashSet<String> order = new LinkedHashSet<>();
        for (var en : tabLists.entrySet()) {
            for (String r : en.getValue()) {
                filtersOf.computeIfAbsent(r, k -> new ArrayList<>()).add(en.getKey());
                order.add(r);
            }
        }
        order.addAll(defs.keySet());

        Map<String, DSRecipe> byName = new LinkedHashMap<>();
        for (String name : order) {
            JsonObject def = defs.get(name);
            if (def == null) {
                continue;
            }
            DSRecipe recipe = resolve(name, def, tech, filtersOf.getOrDefault(name, List.of()));
            if (recipe != null) {
                byName.put(name, recipe);
            }
        }
        List<Filter> filters = new ArrayList<>();
        for (var en : tabLists.entrySet()) {
            List<DSRecipe> list = new ArrayList<>();
            for (String r : en.getValue()) {
                DSRecipe recipe = byName.get(r);
                if (recipe != null && !list.contains(recipe)) {
                    list.add(recipe);
                }
            }
            filters.add(new Filter(en.getKey(), Collections.unmodifiableList(list)));
        }
        List<DSRecipe> ordered = List.copyOf(byName.values());
        filters.add(new Filter(EVERYTHING, ordered));
        DontStarveCraft.LOGGER.info("DS recipe book: {} of {} DST recipes available", byName.size(), defs.size());
        return new Book(Collections.unmodifiableMap(byName), ordered, List.copyOf(filters));
    }

    /** Null when the recipe cannot exist yet (missing items) or is not for Wilson. */
    private static DSRecipe resolve(String name, JsonObject def, JsonObject tech, List<String> filters) {
        if (def.has("deconstruct") || !def.has("tech")) {
            return null;
        }
        JsonObject config = def.has("config") ? def.getAsJsonObject("config") : new JsonObject();
        // Character recipes (builder_tag/skill) and skin unlocks are not Wilson's.
        for (String key : new String[] {"builder_tag", "builder_skill", "unlocks_from_skin", "station_tag", "manufactured"}) {
            if (config.has(key)) {
                return null;
            }
        }
        JsonObject levels = tech.getAsJsonObject(def.get("tech").getAsString());
        if (levels == null) {
            return null;
        }
        Map<String, Integer> level = new TreeMap<>();
        levels.entrySet().forEach(en -> level.put(en.getKey(), en.getValue().getAsInt()));

        String productId = config.has("product") ? config.get("product").getAsString() : name;
        Item product = item(productId);
        if (product == null) {
            return null;
        }
        List<DSRecipe.Ing> ings = new ArrayList<>();
        List<DSRecipe.CharacterIng> chars = new ArrayList<>();
        for (JsonElement e : def.getAsJsonArray("ingredients")) {
            JsonObject o = e.getAsJsonObject();
            String prefab = o.get("item").getAsString();
            int amount = o.get("amount").getAsInt();
            switch (prefab) {
                case "decrease_health" -> chars.add(new DSRecipe.CharacterIng(DSRecipe.CharacterIng.Type.HEALTH, amount));
                case "decrease_sanity" -> chars.add(new DSRecipe.CharacterIng(DSRecipe.CharacterIng.Type.SANITY, amount));
                case "log" -> ings.add(new DSRecipe.Ing(prefab, Ingredient.of(LOGS), amount, Items.SPRUCE_LOG));
                default -> {
                    Item item = item(prefab);
                    if (item == null) {
                        return null; // half_health, sculpting_material, not-yet-ported items
                    }
                    ings.add(new DSRecipe.Ing(prefab, Ingredient.of(item), amount, item));
                }
            }
        }
        int numToGive = config.has("numtogive") ? config.get("numtogive").getAsInt() : 1;
        boolean placer = config.has("placer");
        boolean nounlock = config.has("nounlock") && config.get("nounlock").getAsBoolean();
        double minSpacing = config.has("min_spacing") ? config.get("min_spacing").getAsDouble() : 3.2;
        return new DSRecipe(name, product, numToGive, List.copyOf(ings), List.copyOf(chars),
                Collections.unmodifiableMap(level), placer, nounlock, minSpacing, List.copyOf(filters));
    }

    private static Item item(String prefab) {
        Item same = SAME_AS_VANILLA.get(prefab);
        if (same != null) {
            return same;
        }
        ResourceLocation id = prefab.contains(":") ? ResourceLocation.parse(prefab) : DontStarveCraft.rl(prefab);
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    private static JsonObject read(String file) {
        String path = "/data/dontstarve/dst/" + file;
        try (InputStream in = DSRecipes.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private DSRecipes() {
    }
}
