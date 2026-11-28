package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.item.DSToolItem;
import net.celestiald.dontstarve.item.MonsterMeatItem;
import net.celestiald.dontstarve.work.DSAction;
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
    public static final DeferredItem<BlockItem> SKY_PORTAL =
            ITEMS.registerSimpleBlockItem(DSBlocks.SKY_PORTAL);
    /** Dug grass tuft / sapling: replant them (DST "dug_grass", "dug_sapling"). */
    public static final DeferredItem<BlockItem> DUG_GRASS = ITEMS.registerItem("dug_grass",
            properties -> new net.celestiald.dontstarve.item.DugPlantItem(DSBlocks.GRASS.get(), properties), new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> DUG_SAPLING = ITEMS.registerItem("dug_sapling",
            properties -> new net.celestiald.dontstarve.item.DugPlantItem(DSBlocks.SAPLING.get(), properties), new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> ROCK1 = ITEMS.registerSimpleBlockItem(DSBlocks.ROCK1);
    public static final DeferredItem<BlockItem> ROCK2 = ITEMS.registerSimpleBlockItem(DSBlocks.ROCK2);
    public static final DeferredItem<BlockItem> ROCK_MOON = ITEMS.registerSimpleBlockItem(DSBlocks.ROCK_MOON);
    /** Boulder loot; STACK_SIZE_SMALLITEM = 40 (inv_rocks.lua:55, nitre.lua:39). */
    public static final DeferredItem<Item> ROCKS = ITEMS.registerItem("rocks",
            Item::new, new Item.Properties().stacksTo(40));
    public static final DeferredItem<Item> NITRE = ITEMS.registerItem("nitre",
            Item::new, new Item.Properties().stacksTo(40));
    public static final DeferredItem<BlockItem> SPIDER_DEN =
            ITEMS.registerSimpleBlockItem(DSBlocks.SPIDER_DEN);

    // Prototyper structures: built through the crafting menu's placer, never carried in survival.
    public static final DeferredItem<BlockItem> RESEARCHLAB = ITEMS.registerSimpleBlockItem(DSBlocks.RESEARCHLAB);
    public static final DeferredItem<BlockItem> RESEARCHLAB2 = ITEMS.registerSimpleBlockItem(DSBlocks.RESEARCHLAB2);
    public static final DeferredItem<BlockItem> RESEARCHLAB4 = ITEMS.registerSimpleBlockItem(DSBlocks.RESEARCHLAB4);
    public static final DeferredItem<BlockItem> RESEARCHLAB3 = ITEMS.registerSimpleBlockItem(DSBlocks.RESEARCHLAB3);

    // Fueled fires: built through the crafting menu's placer (campfire, firepit recipes).
    public static final DeferredItem<BlockItem> CAMPFIRE = ITEMS.registerSimpleBlockItem(DSBlocks.CAMPFIRE);
    public static final DeferredItem<BlockItem> FIREPIT = ITEMS.registerSimpleBlockItem(DSBlocks.FIREPIT);

    // Refined materials (REFINE tab). Stack sizes: cutstone/transistor STACK_SIZE_LARGEITEM 10,
    // rope the stackable default STACK_SIZE_MEDITEM 20.
    public static final DeferredItem<Item> CUTSTONE = ITEMS.registerItem("cutstone",
            Item::new, new Item.Properties().stacksTo(10));
    public static final DeferredItem<Item> ROPE = ITEMS.registerItem("rope",
            Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<Item> TRANSISTOR = ITEMS.registerItem("transistor",
            Item::new, new Item.Properties().stacksTo(10));

    /** What burnt objects leave behind (DefaultBurntFn); STACK_SIZE_SMALLITEM 40. */
    public static final DeferredItem<Item> ASH = ITEMS.registerItem("ash",
            Item::new, new Item.Properties().stacksTo(40));

    /** DST pinecone: plant it on the ground to grow an evergreen (pinecone.lua deployable PLANT); STACK_SIZE_SMALLITEM 40. */
    public static final DeferredItem<BlockItem> PINECONE = ITEMS.registerItem("pinecone",
            properties -> new net.celestiald.dontstarve.item.DugPlantItem(DSBlocks.PINECONE_SAPLING.get(), properties), new Item.Properties().stacksTo(40));
    /** DST charcoal (burnt trees, campfires); STACK_SIZE_SMALLITEM 40. */
    public static final DeferredItem<Item> CHARCOAL = ITEMS.registerItem("charcoal",
            Item::new, new Item.Properties().stacksTo(40));

    // DST tools (prefabs/axe.lua, pickaxe.lua, shovel.lua, hammer.lua, razor.lua, pitchfork.lua).
    // TUNING: AXE_USES 100, PICKAXE_USES 33, SHOVEL_USES 25, HAMMER_USES 75, PITCHFORK_USES 25;
    // golden tools wear 1/GOLDENTOOLFACTOR (4) per action and per attack; pitchfork 1/8 per turf.
    // Damage: AXE/PICK_DAMAGE wilson_attack * .8 = 27.2, HAMMER/SHOVEL/PITCHFORK_DAMAGE * .5 = 17.
    public static final DeferredItem<DSToolItem> AXE = tool("axe", DSAction.CHOP, 100, 1, 1, 27.2);
    public static final DeferredItem<DSToolItem> GOLDENAXE = tool("goldenaxe", DSAction.CHOP, 100, 0.25, 0.25, 27.2);
    public static final DeferredItem<DSToolItem> PICKAXE = tool("pickaxe", DSAction.MINE, 33, 1, 1, 27.2);
    public static final DeferredItem<DSToolItem> GOLDENPICKAXE = tool("goldenpickaxe", DSAction.MINE, 33, 0.25, 0.25, 27.2);
    public static final DeferredItem<DSToolItem> SHOVEL = tool("shovel", DSAction.DIG, 25, 1, 1, 17);
    public static final DeferredItem<DSToolItem> GOLDENSHOVEL = tool("goldenshovel", DSAction.DIG, 25, 0.25, 0.25, 17);
    public static final DeferredItem<DSToolItem> HAMMER = tool("hammer", DSAction.HAMMER, 75, 1, 1, 17);
    public static final DeferredItem<DSToolItem> PITCHFORK = tool("pitchfork", DSAction.TERRAFORM, 25, 0.125, 1, 17);
    public static final DeferredItem<DSToolItem> GOLDENPITCHFORK = tool("goldenpitchfork", DSAction.TERRAFORM, 25, 0.125 / 4, 0.25, 17);
    /** The razor has no finiteuses and no weapon component. */
    public static final DeferredItem<DSToolItem> RAZOR = tool("razor", DSAction.SHAVE, 0, 1, 0, 0);

    private static DeferredItem<DSToolItem> tool(String id, DSAction action, double uses, double consumption,
                                                 double attackWear, double damage) {
        return ITEMS.registerItem(id, p -> new DSToolItem(p, action, uses, consumption, attackWear, damage),
                new Item.Properties());
    }

    /** Codex Umbra (DST "waxwelljournal"): read it to travel to the islands. */
    public static final DeferredItem<CodexUmbraItem> WAXWELLJOURNAL = ITEMS.registerItem("waxwelljournal",
            CodexUmbraItem::new, new Item.Properties().stacksTo(1));

    // DS-3 forest flora (berrybush.lua, carrot.lua, flower.lua, reeds.lua, veggies.lua, petals.lua).
    // Foods perish into rot; PERISH_FAST 6 days = 2880 s, PERISH_MED 10 days = 4800 s (day 480 s);
    // calories: TINY 9.375, SMALL 12.5; HEALING_TINY 1. Textures are vanilla placeholders.
    /** Berries (veggies.lua:85): hunger CALORIES_TINY, no health, PERISH_FAST. */
    public static final DeferredItem<net.celestiald.dontstarve.item.DSFoodItem> BERRIES = food("berries", 9.375F, 0.0F, 2880.0F);
    /** Carrot (veggies.lua:45): hunger CALORIES_SMALL, health HEALING_TINY, PERISH_MED. */
    public static final DeferredItem<net.celestiald.dontstarve.item.DSFoodItem> CARROT = food("carrot", 12.5F, 1.0F, 4800.0F);
    /** Petals (petals.lua:96-118): health HEALING_TINY, no hunger, PERISH_FAST. */
    public static final DeferredItem<net.celestiald.dontstarve.item.DSFoodItem> PETALS = food("petals", 0.0F, 1.0F, 2880.0F);
    /** Cut reeds (cutreeds.lua): STACK_SIZE_SMALLITEM 40; roughage, not edible by players. */
    public static final DeferredItem<Item> CUTREEDS = ITEMS.registerItem("cutreeds",
            Item::new, new Item.Properties().stacksTo(40));
    /** Dug berry bushes: planting transplants them (STACK_SIZE_LARGEITEM 10, plantables.lua:85). */
    public static final DeferredItem<BlockItem> DUG_BERRYBUSH = ITEMS.registerItem("dug_berrybush",
            properties -> new net.celestiald.dontstarve.item.DugPlantItem(DSBlocks.BERRYBUSH.get(), properties),
            new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> DUG_BERRYBUSH2 = ITEMS.registerItem("dug_berrybush2",
            properties -> new net.celestiald.dontstarve.item.DugPlantItem(DSBlocks.BERRYBUSH2.get(), properties),
            new Item.Properties().stacksTo(10));
    public static final DeferredItem<BlockItem> BERRYBUSH = ITEMS.registerSimpleBlockItem(DSBlocks.BERRYBUSH);
    public static final DeferredItem<BlockItem> BERRYBUSH2 = ITEMS.registerSimpleBlockItem(DSBlocks.BERRYBUSH2);
    public static final DeferredItem<BlockItem> CARROT_PLANTED = ITEMS.registerSimpleBlockItem(DSBlocks.CARROT_PLANTED);
    public static final DeferredItem<BlockItem> FLOWER = ITEMS.registerSimpleBlockItem(DSBlocks.FLOWER);
    public static final DeferredItem<BlockItem> REEDS = ITEMS.registerSimpleBlockItem(DSBlocks.REEDS);

    private static DeferredItem<net.celestiald.dontstarve.item.DSFoodItem> food(String id, float hunger, float health,
                                                                                float perishDsSeconds) {
        return ITEMS.registerItem(id,
                p -> new net.celestiald.dontstarve.item.DSFoodItem(p,
                        new net.celestiald.dontstarve.item.DSFoodItem.Edible(hunger, health, 0.0F),
                        perishDsSeconds, () -> SPOILED_FOOD.get()),
                new Item.Properties().stacksTo(40)
                        .food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build()));
    }

    private DSItems() {
    }
}
