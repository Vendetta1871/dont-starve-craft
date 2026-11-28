package net.celestiald.dontstarve.item;

import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.work.DSAction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * A DST tool (components/tool.lua + finiteuses.lua + weapon.lua): it performs
 * one work action, wears by its consumption per action and by its attack wear
 * per hit, and breaks when used up. Durability counts the smallest wear step,
 * so fractional DS consumption (golden tools 1/4, pitchfork 1/8) stays exact:
 * max damage = uses / consumption.
 */
public class DSToolItem extends Item {

    private final DSAction action;
    private final int attackWear;

    /**
     * @param uses        TUNING.*_USES; 0 for a tool without finiteuses (razor)
     * @param consumption finiteuses:SetConsumption for the action
     * @param attackWear  weapon.attackwear (1 by default, 1/GOLDENTOOLFACTOR for golden tools)
     * @param dsDamage    weapon:SetDamage, or 0 for a tool without the weapon component
     */
    public DSToolItem(Properties properties, DSAction action, double uses, double consumption,
                      double attackWear, double dsDamage) {
        super(configure(properties, uses, consumption, dsDamage));
        this.action = action;
        this.attackWear = uses > 0 ? (int) Math.round(attackWear / consumption) : 0;
    }

    private static Properties configure(Properties properties, double uses, double consumption, double dsDamage) {
        properties = properties.stacksTo(1);
        if (uses > 0) {
            properties = properties.durability((int) Math.round(uses / consumption));
        }
        if (dsDamage > 0) {
            properties = properties.attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(DontStarveCraft.rl("tool_damage"),
                                    DSScale.stat(dsDamage) - 1.0, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .build());
        }
        return properties;
    }

    public DSAction action() {
        return action;
    }

    /** Does {@code stack} perform {@code action}? */
    public static boolean canDo(ItemStack stack, DSAction action) {
        return stack.getItem() instanceof DSToolItem tool && tool.action == action;
    }

    /** finiteuses:OnUsedAsItem for one work action. */
    public static void use(ItemStack stack, LivingEntity user, DSAction action) {
        if (stack.isDamageableItem() && canDo(stack, action)) {
            stack.hurtAndBreak(1, user, EquipmentSlot.MAINHAND);
        }
    }

    /** weapon attack wear. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return attackWear > 0;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attackWear > 0 && !(attacker instanceof Player player && player.getAbilities().instabuild)) {
            stack.hurtAndBreak(attackWear, attacker, EquipmentSlot.MAINHAND);
        }
    }
}
