package net.celestiald.dontstarve.item;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/**
 * A DS food (components/edible.lua): eating it on the islands changes hunger, health
 * and sanity by these DS values, scaled by its freshness (see {@link Perishable}).
 * A perishable food spoils over {@code perishTime} DS seconds into
 * {@code spoilsInto} (components/perishable.lua onperishreplacement).
 */
public class DSFoodItem extends Item {

    public record Edible(float hunger, float health, float sanity) {
    }

    private final Edible edible;
    private final float perishTime;
    private final Supplier<? extends Item> spoilsInto;

    public DSFoodItem(Properties properties, Edible edible) {
        this(properties, edible, 0.0F, null);
    }

    public DSFoodItem(Properties properties, Edible edible, float perishTime, Supplier<? extends Item> spoilsInto) {
        super(properties);
        this.edible = edible;
        this.perishTime = perishTime;
        this.spoilsInto = spoilsInto;
    }

    public Edible edible() {
        return edible;
    }

    /** DS seconds from fresh to spoiled away; 0 = does not perish. */
    public float perishTime() {
        return perishTime;
    }

    public Item spoilsInto() {
        return spoilsInto.get();
    }
}
