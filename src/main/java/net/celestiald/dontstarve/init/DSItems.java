package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
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
    public static final DeferredItem<Item> BEARDHAIR = ITEMS.registerItem("beardhair",
            Item::new, new Item.Properties().stacksTo(20));

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

    // The portal block is normally never obtained as an item; it is registered
    // for creative testing only.
    public static final DeferredItem<BlockItem> MARBLE_PORTAL = ITEMS.registerItem(
            "marble_portal", properties -> new BlockItem(DSBlocks.MARBLE_PORTAL.get(), properties), new Item.Properties());

    private DSItems() {
    }
}
