package net.celestiald.dontstarve;

import net.celestiald.dontstarve.init.DSBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = DontStarveCraft.MODID, dist = Dist.CLIENT)
public final class DSClient {

    public DSClient(IEventBus modBus) {
        modBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                DSBlocks.MARBLE_PORTAL.get(), RenderType.translucent()));
    }}
