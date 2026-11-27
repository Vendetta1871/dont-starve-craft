package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.item.MonsterMeatItem;
import net.celestiald.dontstarve.portal.CodexUmbraItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DontStarveCraft.MODID);

    public static final DeferredItem<BlockItem> MARBLE = ITEMS.registerSimpleBlockItem(DSBlocks.MARBLE);
    public static final DeferredItem<BlockItem> POLISHED_MARBLE = ITEMS.registerSimpleBlockItem(DSBlocks.POLISHED_MARBLE);
    public static final DeferredItem<BlockItem> MARBLE_BRICKS = ITEMS.registerSimpleBlockItem(DSBlocks.MARBLE_BRICKS);
    public static final DeferredItem<BlockItem> GRAVE = ITEMS.registerSimpleBlockItem(DSBlocks.GRAVE);
    public static final DeferredItem<BlockItem> RESURRECTION_STATUE =
            ITEMS.registerSimpleBlockItem(DSBlocks.RESURRECTION_STATUE);

    // DS material items — canonical DST prefab ids (docs/dont-starve/09).
    // Textures are vanilla placeholders until real art exists.
    public static final DeferredItem<Item> CUTGRASS = ITEMS.registerItem("cutgrass",
            Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<Item> TWIGS = ITEMS.registerItem("twigs",
            Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<Item> BOARDS = ITEMS.registerItem("boards",
            Item::new, new Item.Properties().stacksTo(10));
    public static final DeferredItem<Item> SPIDERGLAND = ITEMS.registerItem("spidergland",
            Item::new, new Item.Properties().stacksTo(20));
    /** Spider loot (spider.lua:645-650); the rope/tent material. */
    public static final DeferredItem<Item> SILK = ITEMS.registerItem("silk",
            Item::new, new Item.Properties().stacksTo(40));
    public static final DeferredItem<Item> BEARDHAIR = ITEMS.registerItem("beardhair",
            Item::new, new Item.Properties().stacksTo(20));
    /** Hound loot (hound.lua:110-113); STACK_SIZE_SMALLITEM = 40. */
    public static final DeferredItem<Item> HOUNDSTOOTH = ITEMS.registerItem("houndstooth",
            Item::new, new Item.Properties().stacksTo(40));
    /** Hound loot; edible with a health penalty, see MonsterMeatItem. */
    /** Rot (DST "spoiled_food", prefabs/spoiledfood.lua): what perished food becomes; SPOILED_HEALTH -1, SPOILED_HUNGER -10. */
    public static final DeferredItem<net.celestiald.dontstarve.item.DSFoodItem> SPOILED_FOOD = ITEMS.registerItem("spoiled_food",
            p -> new net.celestiald.dontstarve.item.DSFoodItem(p, new net.celestiald.dontstarve.item.DSFoodItem.Edible(-10.0F, -1.0F, 0.0F)),
            new Item.Properties().stacksTo(40)
                    .food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build()));

    public static final DeferredItem<MonsterMeatItem> MONSTERMEAT = ITEMS.registerItem("monstermeat",
            MonsterMeatItem::new, new Item.Properties().stacksTo(40)
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()));

    /** Telltale Heart: a living player revives a ghost with it at 25 % HP. */
    public static final DeferredItem<Item> REVIVER = ITEMS.registerItem("reviver",
            Item::new, new Item.Properties().stacksTo(20));

    // Moon escape line: Moon Rock (canonical DST id) -> Moon Rock Idol (key)
    // -> Sky Portal (our escape structure).
    public static final DeferredItem<Item> MOONROCKNUGGET = ITEMS.registerItem("moonrocknugget",
            Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<Item> MOONROCKIDOL = ITEMS.registerItem("moonrockidol",
            Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<BlockItem> MOONROCK_CLUSTER =
            ITEMS.registerSimpleBlockItem(DSBlocks.MOONROCK_CLUSTER);
    public static final DeferredItem<BlockItem> MOON_CRYSTAL =
            ITEMS.registerSimpleBlockItem(DSBlocks.MOON_CRYSTAL);
    public static final DeferredItem<BlockItem> SKY_PORTAL =
            ITEMS.registerSimpleBlockItem(DSBlocks.SKY_PORTAL);
    /** Dug grass tuft / sapling: replant them (DST "dug_grass", "dug_sapling"). */
    public static final DeferredItem<BlockItem> DUG_GRASS = ITEMS.registerItem("dug_grass",
            properties -> new OwnNameBlockItem(DSBlocks.GRASS.get(), properties), new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> DUG_SAPLING = ITEMS.registerItem("dug_sapling",
            properties -> new OwnNameBlockItem(DSBlocks.SAPLING.get(), properties), new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> ROCK1 = ITEMS.registerSimpleBlockItem(DSBlocks.ROCK1);
    public static final DeferredItem<BlockItem> ROCK2 = ITEMS.registerSimpleBlockItem(DSBlocks.ROCK2);
    /** Boulder loot; STACK_SIZE_SMALLITEM = 40 (inv_rocks.lua:55, nitre.lua:39). */
    public static final DeferredItem<Item> ROCKS = ITEMS.registerItem("rocks",
            Item::new, new Item.Properties().stacksTo(40));
    public static final DeferredItem<Item> NITRE = ITEMS.registerItem("nitre",
            Item::new, new Item.Properties().stacksTo(40));
    public static final DeferredItem<BlockItem> SPIDER_DEN =
            ITEMS.registerSimpleBlockItem(DSBlocks.SPIDER_DEN);

    /** Codex Umbra (DST "waxwelljournal"): read it to travel to the islands. */
    public static final DeferredItem<CodexUmbraItem> WAXWELLJOURNAL = ITEMS.registerItem("waxwelljournal",
            CodexUmbraItem::new, new Item.Properties().stacksTo(1));

    /** A block item named after itself (DS "Grass Tuft"), not after the block it plants. */
    private static final class OwnNameBlockItem extends BlockItem {

        OwnNameBlockItem(net.minecraft.world.level.block.Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public String getDescriptionId() {
            return getOrCreateDescriptionId();
        }
    }

    private DSItems() {
    }
}
