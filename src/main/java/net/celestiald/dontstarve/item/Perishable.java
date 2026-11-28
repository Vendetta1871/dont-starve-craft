package net.celestiald.dontstarve.item;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.clock.RunClock;
import net.celestiald.dontstarve.clock.WorldTemperature;
import net.celestiald.dontstarve.init.DSDataComponents;
import net.celestiald.dontstarve.survive.DSStats;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * DST food spoilage (components/perishable.lua, edible.lua) on the islands.
 *
 * <ul>
 *   <li>Freshness counts down in DS seconds for food carried by players and lying on
 *       the ground (x PERISH_GROUND_MULT 1.5); the world below 0 degrees slows it
 *       (x0.75), above 70 speeds it (x1.25).</li>
 *   <li>Fresh above 50 %, stale above 20 %, then spoiled; at 0 the stack becomes its
 *       replacement (rot) with the same count.</li>
 *   <li>Stacks of one food with different freshness merge with the freshness averaged
 *       (stackable.lua), when stacked by hand and when picked up.</li>
 * </ul>
 */
public final class Perishable {

    public static final float PERISH_FRESH = 0.5F;
    public static final float PERISH_STALE = 0.2F;
    private static final float GROUND_MULT = 1.5F;
    private static final float WINTER_MULT = 0.75F;
    private static final float SUMMER_MULT = 1.25F;
    /** Ticked once per Minecraft second: 20 ticks of world time in DS seconds. */
    private static final float DS_SECONDS_PER_UPDATE = 1.0F / DSScale.TIME;

    public enum Freshness { FRESH, STALE, SPOILED }

    public static float perishTime(ItemStack stack) {
        return stack.getItem() instanceof DSFoodItem food ? food.perishTime() : 0.0F;
    }

    public static boolean isPerishable(ItemStack stack) {
        return perishTime(stack) > 0.0F;
    }

    public static float remaining(ItemStack stack) {
        return stack.getOrDefault(DSDataComponents.PERISH.get(), perishTime(stack));
    }

    public static float percent(ItemStack stack) {
        float time = perishTime(stack);
        return time > 0.0F ? remaining(stack) / time : 1.0F;
    }

    public static Freshness freshness(ItemStack stack) {
        float p = percent(stack);
        return p >= PERISH_FRESH ? Freshness.FRESH : p >= PERISH_STALE ? Freshness.STALE : Freshness.SPOILED;
    }

    public static void setPercent(ItemStack stack, float percent) {
        stack.set(DSDataComponents.PERISH.get(), Math.max(0.0F, Math.min(1.0F, percent)) * perishTime(stack));
    }

    /** Edible values after spoilage (edible.lua GetHunger/GetHealth/GetSanity). */
    public static DSFoodItem.Edible effective(ItemStack stack, DSFoodItem.Edible edible) {
        if (!isPerishable(stack)) {
            return edible;
        }
        Freshness f = freshness(stack);
        float hunger = edible.hunger();
        float health = edible.health();
        float sanity = edible.sanity();
        if (f == Freshness.STALE) {
            hunger = hunger < 0 ? hunger : hunger * 0.667F;
            health = health < 0 ? health : health * 0.333F;
            sanity = sanity < 0 ? sanity : 0.0F;
        } else if (f == Freshness.SPOILED) {
            hunger = hunger < 0 ? hunger : hunger * 0.5F;
            health = health < 0 ? health : 0.0F;
            sanity = sanity < 0 ? sanity : -10.0F;
        }
        return new DSFoodItem.Edible(hunger, health, sanity);
    }

    /** Ages a stack; returns the stack to keep in its place (rot once it perished). */
    public static ItemStack age(ItemStack stack, float dsSeconds) {
        float left = remaining(stack) - dsSeconds;
        if (left <= 0.0F && stack.getItem() instanceof DSFoodItem food) {
            return new ItemStack(food.spoilsInto(), stack.getCount());
        }
        stack.set(DSDataComponents.PERISH.get(), left);
        return stack;
    }

    /** TUNING.PERISH_FRIDGE_MULT: food in an Ice Box (perishable.lua: owner tagged "fridge"). */
    public static final float FRIDGE_MULT = 0.5F;

    /**
     * DS seconds a perishable ages in one Minecraft second (20 ticks) at the current world
     * temperature (perishable.lua: x0.75 below 0, x1.25 above OVERHEAT_TEMP); 0 when the islands
     * are not loaded. Containers that tick their own contents use it with their own multiplier.
     */
    public static float updateDt(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (server.getLevel(DSRefs.ISLANDS) == null || overworld == null) {
            return 0.0F;
        }
        double ambient = WorldTemperature.at(RunClock.state(server), IslandState.get(overworld).islandIndex());
        float mult = ambient < 0 ? WINTER_MULT : ambient > DSStats.OVERHEAT_TEMP ? SUMMER_MULT : 1.0F;
        return DS_SECONDS_PER_UPDATE * mult;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        float dt = updateDt(server);
        if (islands == null) {
            return;
        }
        for (ServerPlayer player : islands.players()) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (isPerishable(stack)) {
                    ItemStack aged = age(stack, dt);
                    if (aged != stack) {
                        inventory.setItem(i, aged);
                    }
                }
            }
        }
        for (ItemEntity item : islands.getEntities(EntityType.ITEM, e -> isPerishable(e.getItem()))) {
            ItemStack aged = age(item.getItem(), dt * GROUND_MULT);
            if (aged != item.getItem()) {
                item.setItem(aged);
            }
        }
    }

    private static boolean sameFood(ItemStack a, ItemStack b) {
        return isPerishable(a) && a.getItem() == b.getItem();
    }

    /** Moves up to {@code count} items of {@code from} onto {@code into}, averaging freshness. */
    private static int merge(ItemStack into, ItemStack from, int count) {
        int moved = Math.min(count, Math.min(from.getCount(), into.getMaxStackSize() - into.getCount()));
        if (moved <= 0) {
            return 0;
        }
        float total = remaining(into) * into.getCount() + remaining(from) * moved;
        into.grow(moved);
        into.set(DSDataComponents.PERISH.get(), total / into.getCount());
        from.shrink(moved);
        return moved;
    }

    /** Clicking a carried food onto the same food of another freshness merges them. */
    public static void onStackedOnOther(ItemStackedOnOtherEvent event) {
        ItemStack carried = event.getCarriedItem();
        ItemStack slot = event.getStackedOnItem();
        if (!sameFood(carried, slot) || ItemStack.isSameItemSameComponents(carried, slot)
                || !event.getSlot().mayPlace(carried)) {
            return;
        }
        int count = event.getClickAction() == ClickAction.PRIMARY ? carried.getCount() : 1;
        if (merge(slot, carried, count) > 0) {
            event.getSlot().setChanged();
            event.setCanceled(true);
        }
    }

    /** Picked-up food joins carried stacks of the same food, averaging freshness. */
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack ground = entity.getItem();
        if (!isPerishable(ground) || entity.hasPickUpDelay() || player.level().isClientSide) {
            return;
        }
        Inventory inventory = player.getInventory();
        int before = ground.getCount();
        for (int i = 0; i < inventory.items.size() && !ground.isEmpty(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (sameFood(stack, ground) && !ItemStack.isSameItemSameComponents(stack, ground)) {
                merge(stack, ground, ground.getCount());
            }
        }
        int taken = before - ground.getCount();
        if (taken > 0) {
            player.take(entity, taken);
            if (ground.isEmpty()) {
                entity.discard();
                event.setCanPickup(TriState.FALSE);
            } else {
                entity.setItem(ground);
            }
        }
    }

    private Perishable() {
    }
}
