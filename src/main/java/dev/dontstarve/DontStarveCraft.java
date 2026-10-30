package dev.dontstarve;

import dev.dontstarve.init.DSBlocks;
import dev.dontstarve.init.DSCreativeTabs;
import dev.dontstarve.init.DSItems;
import dev.dontstarve.portal.SacrificeHandler;
import dev.dontstarve.world.IslandChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(DontStarveCraft.MODID)
public final class DontStarveCraft {

    public static final String MODID = "dontstarve";

    public DontStarveCraft(IEventBus modBus) {
        DSBlocks.BLOCKS.register(modBus);
        DSItems.ITEMS.register(modBus);
        DSCreativeTabs.TABS.register(modBus);

        modBus.addListener(DontStarveCraft::registerWorldgen);

        NeoForge.EVENT_BUS.addListener(SacrificeHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(DSCommands::register);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerWorldgen(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, helper ->
                helper.register(rl("islands"), IslandChunkGenerator.CODEC));
    }
}
