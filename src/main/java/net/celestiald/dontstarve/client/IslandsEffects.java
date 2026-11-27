package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.clock.DSClock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Sky and light of the islands: overworld-like sky, but the sky light is tinted
 * and scaled by the DST ambient colour of the run clock (warm day, grey dusk,
 * black night, blue full-moon night), so night is dark except near light sources.
 */
public final class IslandsEffects extends DimensionSpecialEffects {

    public IslandsEffects() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        if (!ClientClock.active()) {
            return;
        }
        float[] ambient = DSClock.ambient(ClientClock.state());
        float block = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        float sky = LightTexture.getBrightness(level.dimensionType(), pixelY);
        colors.set(block, block * ((block * 0.6F + 0.4F) * 0.6F + 0.4F), block * (block * block * 0.6F + 0.4F));
        colors.add(ambient[0] * sky, ambient[1] * sky, ambient[2] * sky);
    }
}
