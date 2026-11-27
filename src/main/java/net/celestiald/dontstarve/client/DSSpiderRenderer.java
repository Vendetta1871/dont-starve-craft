package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.entity.DSSpider;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Placeholder look: the vanilla spider mesh and skin until DS art exists. */
public class DSSpiderRenderer extends MobRenderer<DSSpider, SpiderModel<DSSpider>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");

    public DSSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
    }

    @Override
    protected float getFlipDegrees(DSSpider spider) {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DSSpider spider) {
        return TEXTURE;
    }
}
