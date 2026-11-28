package net.celestiald.dontstarve.init;

import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DSCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DontStarveCraft.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dontstarve"))
                    .icon(() -> new ItemStack(DSBlocks.MARBLE.get()))
                    .displayItems((params, output) -> {
                        output.accept(DSBlocks.MARBLE.get());
                        output.accept(DSBlocks.POLISHED_MARBLE.get());
                        output.accept(DSBlocks.MARBLE_BRICKS.get());
                        output.accept(DSItems.WAXWELLJOURNAL.get());
                        output.accept(DSBlocks.GRAVE.get());
                        output.accept(DSBlocks.RESURRECTION_STATUE.get());
                        output.accept(DSItems.CUTGRASS.get());
                        output.accept(DSItems.TWIGS.get());
                        output.accept(DSItems.BOARDS.get());
                        output.accept(DSItems.SPIDERGLAND.get());
                        output.accept(DSItems.SILK.get());
                        output.accept(DSItems.BEARDHAIR.get());
                        output.accept(DSItems.HOUNDSTOOTH.get());
                        output.accept(DSItems.MONSTERMEAT.get());
                        output.accept(DSItems.SPOILED_FOOD.get());
                        output.accept(DSItems.REVIVER.get());
                        output.accept(DSItems.MOONROCKNUGGET.get());
                        output.accept(DSItems.MOONROCKIDOL.get());
                        output.accept(DSBlocks.SKY_PORTAL.get());
                        output.accept(DSItems.DUG_GRASS.get());
                        output.accept(DSItems.DUG_SAPLING.get());
                        output.accept(DSBlocks.ROCK1.get());
                        output.accept(DSBlocks.ROCK2.get());
                        output.accept(DSBlocks.ROCK_MOON.get());
                        output.accept(DSItems.ROCKS.get());
                        output.accept(DSItems.NITRE.get());
                        output.accept(DSBlocks.SPIDER_DEN.get());
                        output.accept(DSItems.CUTSTONE.get());
                        output.accept(DSItems.ROPE.get());
                        output.accept(DSItems.TRANSISTOR.get());
                        output.accept(DSBlocks.RESEARCHLAB.get());
                        output.accept(DSBlocks.RESEARCHLAB2.get());
                        output.accept(DSBlocks.RESEARCHLAB4.get());
                        output.accept(DSBlocks.RESEARCHLAB3.get());
                    })
                    .build());

    private DSCreativeTabs() {
    }
}
