package net.celestiald.dontstarve.fire;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The fuel component (components/fuel.lua) of DST items: fuelvalue in DS seconds of burn time
 * (tuning.lua TINY_FUEL ... HUGE_FUEL, in segments of 30 s). Keyed by DST prefab id and resolved
 * lazily against the item registry, so items other parts of the mod add later count as soon as
 * they are registered. DST {@code log} is {@code minecraft:spruce_log}.
 */
public final class FuelValues {

    /** TUNING.TINY_FUEL = seg_time * .25. */
    public static final double TINY = 7.5;
    /** TUNING.SMALL_FUEL = seg_time * .5. */
    public static final double SMALL = 15;
    /** TUNING.MED_FUEL = seg_time * 1.5. */
    public static final double MED = 45;
    /** TUNING.MED_LARGE_FUEL = seg_time * 3. */
    public static final double MED_LARGE = 90;
    /** TUNING.LARGE_FUEL = seg_time * 6. */
    public static final double LARGE = 180;
    /** TUNING.HUGE_FUEL = seg_time * 9. */
    public static final double HUGE = 270;

    /** prefab id to fuelvalue, from each prefab's fuel component. */
    private static final Map<String, Double> BY_PREFAB = new HashMap<>();

    static {
        // Materials and plants (log.lua, cutgrass.lua, twigs.lua, boards.lua, rope.lua, pinecone.lua, charcoal.lua, ...).
        BY_PREFAB.put("log", MED);
        BY_PREFAB.put("cutgrass", SMALL);
        BY_PREFAB.put("twigs", SMALL);
        BY_PREFAB.put("boards", LARGE);
        BY_PREFAB.put("rope", MED);
        BY_PREFAB.put("pinecone", SMALL);
        BY_PREFAB.put("charcoal", MED);
        BY_PREFAB.put("petals", TINY);
        BY_PREFAB.put("petals_evil", TINY);
        BY_PREFAB.put("foliage", TINY);
        BY_PREFAB.put("cutreeds", SMALL);
        BY_PREFAB.put("papyrus", SMALL);
        BY_PREFAB.put("livinglog", MED);
        BY_PREFAB.put("nightmarefuel", LARGE);
        BY_PREFAB.put("nitre", MED_LARGE);
        for (String mushroom : new String[] {"red_cap", "green_cap", "blue_cap", "moon_cap"}) {
            BY_PREFAB.put(mushroom, TINY); // mushrooms.lua:393
        }
        // Dug plants (plantables.lua:92).
        for (String dug : new String[] {"dug_grass", "dug_sapling", "dug_berrybush", "dug_berrybush2",
                "dug_berrybush_juicy", "dug_marsh_bush", "dug_rock_avocado_bush", "dug_bananabush",
                "dug_monkeytail", "dug_reeds"}) {
            BY_PREFAB.put(dug, LARGE);
        }
        // Creature drops and found items.
        BY_PREFAB.put("beardhair", MED);       // beardhair.lua:35
        BY_PREFAB.put("beefalowool", MED);     // beefalowool.lua:40
        BY_PREFAB.put("furtuft", MED);         // furtuft.lua:48
        BY_PREFAB.put("poop", MED);            // poop.lua:109
        BY_PREFAB.put("guano", MED);           // guano.lua:103
        BY_PREFAB.put("spoiled_food", SMALL);  // spoiledfood.lua:136
        BY_PREFAB.put("tentaclespots", SMALL); // tentaclespots.lua:34
        BY_PREFAB.put("slurtleslime", MED);    // slurtleslime.lua:64
        BY_PREFAB.put("steelwool", MED);       // steelwool.lua:36
        BY_PREFAB.put("spidereggsack", LARGE); // spidereggsack.lua:59
        BY_PREFAB.put("driftwood_log", MED);   // driftwood.lua:39
        BY_PREFAB.put("palmcone_scale", MED);  // palmcone_scale.lua:35
        BY_PREFAB.put("papyrus", SMALL);       // papyrus.lua:38
        BY_PREFAB.put("waxpaper", SMALL);      // waxpaper.lua:37
        BY_PREFAB.put("lightbulb", MED_LARGE); // lightbulb.lua:57
        BY_PREFAB.put("glommerwings", MED_LARGE);
        BY_PREFAB.put("glommerflower", MED_LARGE);
        BY_PREFAB.put("glommerfuel", LARGE);
        BY_PREFAB.put("horrorfuel", LARGE * 2);
        // Wearables and tools made of plant matter.
        BY_PREFAB.put("armorgrass", LARGE);    // armor_grass.lua:63
        BY_PREFAB.put("armorwood", LARGE);     // armor_wood.lua:63
        BY_PREFAB.put("bedroll_straw", LARGE); // bedroll.lua:71
        BY_PREFAB.put("umbrella", LARGE);      // umbrella.lua:160
        BY_PREFAB.put("walking_stick", LARGE);
        BY_PREFAB.put("cutless", MED);         // cutless.lua:65
        BY_PREFAB.put("blueprint", SMALL);     // blueprint.lua:151
        BY_PREFAB.put("featherpencil", SMALL);
    }

    private static Map<Item, Double> resolved;

    /** The fuelvalue in DS seconds of one item of this stack, or empty when it is not fuel. */
    public static OptionalDouble of(ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalDouble.empty();
        }
        Double value = resolve().get(stack.getItem());
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    private static synchronized Map<Item, Double> resolve() {
        if (resolved == null) {
            Map<Item, Double> map = new HashMap<>();
            for (Map.Entry<String, Double> en : BY_PREFAB.entrySet()) {
                Item item = en.getKey().equals("log") ? Items.SPRUCE_LOG
                        : BuiltInRegistries.ITEM.getOptional(DontStarveCraft.rl(en.getKey())).orElse(null);
                if (item != null) {
                    map.put(item, en.getValue());
                }
            }
            resolved = map;
        }
        return resolved;
    }

    /** An item of the mod by prefab id, or null when it is not registered (yet). */
    public static Item optionalItem(String prefab) {
        ResourceLocation id = DontStarveCraft.rl(prefab);
        return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    private FuelValues() {
    }
}
