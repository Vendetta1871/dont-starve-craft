package dev.dontstarve.init;

import dev.dontstarve.DontStarveCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DontStarveCraft.MODID);

    public static final DeferredItem<BlockItem> MARBLE = ITEMS.registerSimpleBlockItem(DSBlocks.MARBLE);
    public static final DeferredItem<BlockItem> POLISHED_MARBLE = ITEMS.registerSimpleBlockItem(DSBlocks.POLISHED_MARBLE);
    public static final DeferredItem<BlockItem> MARBLE_BRICKS = ITEMS.registerSimpleBlockItem(DSBlocks.MARBLE_BRICKS);

    // The portal block is normally never obtained as an item; it is registered
    // for creative testing only.
    public static final DeferredItem<BlockItem> MARBLE_PORTAL = ITEMS.registerItem(
            "marble_portal", properties -> new BlockItem(DSBlocks.MARBLE_PORTAL.get(), properties), new Item.Properties());

    private DSItems() {
    }
}
