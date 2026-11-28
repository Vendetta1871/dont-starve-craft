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

    /** edible.temperaturedelta / temperatureduration: the warming or chill the food leaves in the belly (DS degrees, DS seconds). */
    public record Belly(float delta, float duration) {
    }

    private final Edible edible;
    private final float perishTime;
    private final Supplier<? extends Item> spoilsInto;
    private final Belly belly;

    public DSFoodItem(Properties properties, Edible edible) {
        this(properties, edible, 0.0F, null);
    }

    public DSFoodItem(Properties properties, Edible edible, float perishTime, Supplier<? extends Item> spoilsInto) {
        this(properties, edible, perishTime, spoilsInto, null);
    }

    public DSFoodItem(Properties properties, Edible edible, float perishTime, Supplier<? extends Item> spoilsInto,
                      Belly belly) {
        super(properties);
        this.edible = edible;
        this.perishTime = perishTime;
        this.spoilsInto = spoilsInto;
        this.belly = belly;
    }

    /** The temperature effect of eating this food, or null. */
    public Belly belly() {
        return belly;
    }

    /** components/cookable.lua product: what one item becomes on a fire; null when it cannot be cooked. */
    private Supplier<? extends Item> cooked;
    /** components/dryable.lua product and dry time (DS seconds); null when it cannot be dried. */
    private Supplier<? extends Item> dried;
    private float dryTime;

    /** Makes this food cookable (cookable.product). */
    public DSFoodItem cookInto(Supplier<? extends Item> product) {
        this.cooked = product;
        return this;
    }

    /** Makes this food dryable (dryable:SetProduct / SetDryTime, DS seconds). */
    public DSFoodItem dryInto(Supplier<? extends Item> product, float dsSeconds) {
        this.dried = product;
        this.dryTime = dsSeconds;
        return this;
    }

    /** The cooked product, or null. */
    public Item cookedInto() {
        return cooked == null ? null : cooked.get();
    }

    /** The dried product, or null. */
    public Item driedInto() {
        return dried == null ? null : dried.get();
    }

    /** DS seconds a dryable takes to dry on a rack. */
    public float dryTime() {
        return dryTime;
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
