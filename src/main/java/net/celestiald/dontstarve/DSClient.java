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
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DSEntities.HOUND.get(), HoundRenderer::new);
        event.registerEntityRenderer(DSEntities.SPIDER.get(), DSSpiderRenderer::new);
    }

    private void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HoundModel.LAYER, HoundModel::createLayer);
    }
}
