package net.celestiald.dontstarve;

import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.init.DSEntities;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = DontStarveCraft.MODID, dist = Dist.CLIENT)
public final class DSClient {

    public DSClient(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterRenderers);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                DSBlocks.MARBLE_PORTAL.get(), RenderType.translucent()));
    }

    /** Hairy Hounds wear the wolf skin until DS art lands. */
    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DSEntities.HAIRY_HOUND.get(), WolfRenderer::new);
    }
}
