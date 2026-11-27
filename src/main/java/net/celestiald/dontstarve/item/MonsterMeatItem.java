package net.celestiald.dontstarve.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Monster Meat (DST "monstermeat", prefabs/meats.lua:284-293): filling but
 * harmful — hunger CALORIES_MEDSMALL 18.75, health -HEALING_MED 20, scaled to
 * Minecraft by x2/15 (hunger 2.5, health -2.67). Sanity is not ported yet.
 */
public class MonsterMeatItem extends Item {

    private static final float HEALTH_PENALTY = net.celestiald.dontstarve.DSScale.stat(20);

    public MonsterMeatItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!level.isClientSide) {
            eater.hurt(level.damageSources().magic(), HEALTH_PENALTY);
        }
        return super.finishUsingItem(stack, level, eater);
    }
}
