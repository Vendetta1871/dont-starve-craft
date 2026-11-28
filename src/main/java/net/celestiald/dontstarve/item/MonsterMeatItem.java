package net.celestiald.dontstarve.item;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Monster Meat (DST "monstermeat", prefabs/meats.lua:284-293): hunger
 * CALORIES_MEDSMALL 18.75, health -HEALING_MED 20, sanity -SANITY_MED 15; perishes
 * in PERISH_FAST 6 days into rot (meats.lua:196-199). On the
 * islands the DS stats apply (Survival); elsewhere it is weak vanilla food that
 * still hurts by the same 20 DS health.
 */
public class MonsterMeatItem extends DSFoodItem {

    public static final Edible EDIBLE = new Edible(18.75F, -20.0F, -15.0F);

    private final Edible edible;

    public MonsterMeatItem(Properties properties) {
        this(properties, EDIBLE, 6 * 480.0F);
    }

    /** Cooked monster meat and monster jerky: same monster food, other values and perish times. */
    public MonsterMeatItem(Properties properties, Edible edible, float perishTime) {
        super(properties, edible, perishTime, () -> net.celestiald.dontstarve.init.DSItems.SPOILED_FOOD.get());
        this.edible = edible;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide && !level.dimension().equals(DSRefs.ISLANDS)) {
            eater.hurt(level.damageSources().magic(), DSScale.stat(-edible.health()));
        }
        return super.finishUsingItem(stack, level, eater);
    }
}
