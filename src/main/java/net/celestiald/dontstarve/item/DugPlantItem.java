package net.celestiald.dontstarve.item;

import javax.annotation.Nullable;
import net.celestiald.dontstarve.world.PickableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A dug-up plant (DST "dug_grass", "dug_sapling", "dug_berrybush"): planting it
 * transplants the plant (plantables.lua, pickable.lua OnTransplant), so it starts
 * barren until fertilized. Named after itself (DS "Grass Tuft"), not after the
 * block it plants.
 */
public class DugPlantItem extends BlockItem {

    public DugPlantItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state != null && getBlock() instanceof PickableBlock plant ? plant.transplantedState() : state;
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }
}
