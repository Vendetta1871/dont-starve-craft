package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.entity.Hound;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HoundRenderer extends MobRenderer<Hound, HoundModel> {

    private static final ResourceLocation TEXTURE = DontStarveCraft.rl("textures/entity/hound.png");

    public HoundRenderer(EntityRendererProvider.Context context) {
        super(context, new HoundModel(context.bakeLayer(HoundModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Hound hound) {
        return TEXTURE;
    }
}
