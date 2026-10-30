package dev.dontstarve.init;

import dev.dontstarve.DontStarveCraft;
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
                        output.accept(DSBlocks.MARBLE_PORTAL.get());
                    })
                    .build());

    private DSCreativeTabs() {
    }
}
