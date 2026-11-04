package net.celestiald.dontstarve;

import net.celestiald.dontstarve.entity.HairyHound;
import net.celestiald.dontstarve.init.DSBlockEntities;
import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.init.DSCreativeTabs;
import net.celestiald.dontstarve.init.DSDataComponents;
import net.celestiald.dontstarve.init.DSEntities;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.init.DSLoot;
import net.celestiald.dontstarve.portal.SacrificeHandler;
import net.celestiald.dontstarve.revive.ReviveHandler;
import net.celestiald.dontstarve.survive.DeathHandler;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.survive.HoundWave;
import net.celestiald.dontstarve.world.IslandChunkGenerator;
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
        DSEntities.ENTITIES.register(modBus);
        GhostState.ATTACHMENTS.register(modBus);

        modBus.addListener(DontStarveCraft::registerWorldgen);
        modBus.addListener(DontStarveCraft::createAttributes);

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

        NeoForge.EVENT_BUS.addListener(HoundWave::onServerTick);
        NeoForge.EVENT_BUS.addListener(DSLoot::onLootTableLoad);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerWorldgen(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, helper ->
                helper.register(rl("islands"), IslandChunkGenerator.CODEC));
    }

    private static void createAttributes(
            net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(DSEntities.HAIRY_HOUND.get(), HairyHound.createAttributes().build());
    }
}
