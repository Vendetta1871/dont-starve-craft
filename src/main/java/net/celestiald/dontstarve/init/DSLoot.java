package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * DS spider loot (spider.lua:647-648): half a chance of silk, half of a gland.
 * Silk is represented by vanilla string, the gland is the DS item itself —
 * together they feed the Telltale Heart and (later) rope recipes.
 */
public final class DSLoot {

    private static final ResourceLocation SPIDER =
            ResourceLocation.withDefaultNamespace("entities/spider");

    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().equals(SPIDER)) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(DSItems.SPIDERGLAND.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                .when(LootItemRandomChanceCondition.randomChance(0.5F))
                .build());
        event.getTable().addPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(Items.STRING)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                .when(LootItemRandomChanceCondition.randomChance(0.5F))
                .build());
    }

    private DSLoot() {
    }
}
