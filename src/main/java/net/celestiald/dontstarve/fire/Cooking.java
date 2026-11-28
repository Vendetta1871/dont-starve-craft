package net.celestiald.dontstarve.fire;

import java.util.Optional;
import net.celestiald.dontstarve.item.DSFoodItem;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The COOK action (actions.lua ACTIONS.COOK, components/cooker.lua, components/cookable.lua):
 * one item of the held stack is cooked on a burning fire and its product goes to the player.
 * The campfire and the fire pit are the cookers (their fueled section above 0 stands for
 * "fuel not empty"); a cooked product keeps half of the lost freshness.
 */
public final class Cooking {

    /** cooker:CanCook: the fire still has fuel and the item is cookable. */
    public static boolean canCook(ServerLevel level, BlockPos pos, ItemStack item) {
        return fireBurning(level, pos) && cookedItem(item) != null;
    }

    /** The fire at {@code pos} is a cooker with fuel left. */
    public static boolean fireBurning(net.minecraft.world.level.Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof FueledFireBlock && state.getValue(FueledFireBlock.SECTION) > 0;
    }

    /** cookable.product of the stack's item, or null when it is not cookable. */
    public static Item cookedItem(ItemStack item) {
        return item.getItem() instanceof DSFoodItem food ? food.cookedInto() : null;
    }

    /**
     * cookable:Cook for one item of {@code stack}: the product keeps
     * {@code 1 - (1 - percent) * 0.5} of its freshness. The cooked item is given to the player
     * (dropped when the inventory is full); creative players keep the raw item.
     */
    public static Optional<ItemStack> cook(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        if (!canCook(level, pos, stack)) {
            return Optional.empty();
        }
        ItemStack product = new ItemStack(cookedItem(stack));
        if (Perishable.isPerishable(stack) && Perishable.isPerishable(product)) {
            Perishable.setPercent(product, 1.0F - (1.0F - Perishable.percent(stack)) * 0.5F);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.4F);
        ItemStack given = product.copy();
        if (!player.getInventory().add(given)) {
            player.drop(given, false);
        }
        return Optional.of(product);
    }

    private Cooking() {
    }
}
