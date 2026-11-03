package dev.dontstarve;

import dev.dontstarve.init.DSBlockEntities;
import dev.dontstarve.init.DSBlocks;
import dev.dontstarve.init.DSCreativeTabs;
import dev.dontstarve.init.DSDataComponents;
import dev.dontstarve.init.DSItems;
import dev.dontstarve.portal.SacrificeHandler;
import dev.dontstarve.revive.ReviveHandler;
import dev.dontstarve.survive.DeathHandler;
import dev.dontstarve.survive.GhostState;
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
        DSBlockEntities.BLOCK_ENTITIES.register(modBus);
        DSDataComponents.DATA_COMPONENTS.register(modBus);
        GhostState.ATTACHMENTS.register(modBus);

        modBus.addListener(DontStarveCraft::registerWorldgen);

        NeoForge.EVENT_BUS.addListener(SacrificeHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(DSCommands::register);

        NeoForge.EVENT_BUS.addListener(DeathHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onPickupItem);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(DeathHandler::onServerTick);

        NeoForge.EVENT_BUS.addListener(ReviveHandler::onInteractWithGhost);
        NeoForge.EVENT_BUS.addListener(ReviveHandler::onItemCrafted);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerWorldgen(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, helper ->
                helper.register(rl("islands"), IslandChunkGenerator.CODEC));
    }
}
