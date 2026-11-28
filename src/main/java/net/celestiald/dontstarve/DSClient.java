package net.celestiald.dontstarve;

import net.celestiald.dontstarve.client.HoundModel;
import net.celestiald.dontstarve.client.DSSpiderRenderer;
import net.celestiald.dontstarve.client.HoundRenderer;
import net.celestiald.dontstarve.client.ScreenshotTaker;
import net.celestiald.dontstarve.init.DSEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = DontStarveCraft.MODID, dist = Dist.CLIENT)
public final class DSClient {

    public DSClient(IEventBus modBus) {
        modBus.addListener(this::onRegisterRenderers);
        modBus.addListener(this::onRegisterLayers);
        NeoForge.EVENT_BUS.addListener(ScreenshotTaker::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.ClientClock::onClientTick);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.StatsHud::onRenderLayer);
        modBus.addListener(DSClient::onRegisterDimensionEffects);
        modBus.addListener(DSClient::onRegisterGuiLayers);
        modBus.addListener(DSClient::onRegisterBlockColors);
        modBus.addListener(net.celestiald.dontstarve.client.FreshnessDisplay::onRegisterDecorations);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.FreshnessDisplay::onTooltip);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.CraftingScreen::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.CraftingScreen::onToast);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.Placement::onClientTick);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.Placement::onInteraction);
        NeoForge.EVENT_BUS.addListener(net.celestiald.dontstarve.client.Placement::onRenderLevel);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DSEntities.HOUND.get(), HoundRenderer::new);
        event.registerEntityRenderer(DSEntities.SPIDER.get(), DSSpiderRenderer::new);
    }

    private static void onRegisterDimensionEffects(
            net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(DontStarveCraft.rl("islands"), new net.celestiald.dontstarve.client.IslandsEffects());
    }

    /** Placeholder spruce_leaves art is grayscale: tint the evergreen crowns (spruce green; the lumpy build a bit yellower). */
    private static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> 0x619961, net.celestiald.dontstarve.init.DSBlocks.EVERGREEN_CROWN.get());
        event.register((state, level, pos, tint) -> 0x7a9a4e, net.celestiald.dontstarve.init.DSBlocks.EVERGREEN_SPARSE_CROWN.get());
    }

    private static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAboveAll(DontStarveCraft.rl("clock"), net.celestiald.dontstarve.client.ClockHud::render);
        event.registerAboveAll(DontStarveCraft.rl("stats"), net.celestiald.dontstarve.client.StatsHud::render);
        event.registerAboveAll(DontStarveCraft.rl("placement"), net.celestiald.dontstarve.client.Placement::renderHud);
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CAMERA_OVERLAYS, DontStarveCraft.rl("frost"),
                net.celestiald.dontstarve.client.StatsHud::renderFrost);
    }

    private void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HoundModel.LAYER, HoundModel::createLayer);
    }
}
