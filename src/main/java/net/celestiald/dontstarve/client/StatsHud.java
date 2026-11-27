package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.network.StatsPayload;
import net.celestiald.dontstarve.survive.DSStats;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * DS stat badges under the clock — health, hunger, sanity — as square Minecraft-style
 * panels that fill from the bottom, with the DS value below. On the islands they
 * replace the vanilla hearts and hunger bar.
 */
public final class StatsHud {

    private static final ResourceLocation HEALTH = DontStarveCraft.rl("hud/health");
    private static final ResourceLocation HUNGER = DontStarveCraft.rl("hud/hunger");
    private static final ResourceLocation SANITY = DontStarveCraft.rl("hud/sanity");
    private static final int SIZE = 20;
    private static final int GAP = 4;

    private static StatsPayload stats;

    public static void receive(StatsPayload payload) {
        stats = payload;
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (!ClientClock.active() || stats == null || player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        int right = graphics.guiWidth() - 6;
        int top = 6 + 44 + 4;
        int left = right - 3 * SIZE - 2 * GAP;
        float healthDs = player.getHealth() / DSScale.STAT;
        float maxHealthDs = player.getMaxHealth() / DSScale.STAT;
        badge(graphics, left, top, HEALTH, healthDs, maxHealthDs, 0xFFB52A2A);
        badge(graphics, left + SIZE + GAP, top, HUNGER, stats.hunger(), DSStats.MAX_HUNGER, 0xFFC9822E);
        badge(graphics, left + 2 * (SIZE + GAP), top, SANITY, stats.sanity(), DSStats.MAX_SANITY,
                stats.sane() ? 0xFFB0609E : 0xFF5A2D7A);
    }

    private static void badge(GuiGraphics graphics, int x, int y, ResourceLocation icon, float value, float max, int colour) {
        // Grey Minecraft panel: light top-left edge, dark bottom-right edge.
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SIZE, y + SIZE, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFF8B8B8B);
        int inner = SIZE - 4;
        int filled = Math.round(inner * Math.max(0.0F, Math.min(1.0F, value / max)));
        graphics.fill(x + 2, y + 2 + inner - filled, x + 2 + inner, y + 2 + inner, colour);
        graphics.blitSprite(icon, x + (SIZE - 9) / 2, y + (SIZE - 9) / 2, 9, 9);
        Font font = Minecraft.getInstance().font;
        String text = Integer.toString(Math.round(value));
        graphics.pose().pushPose();
        graphics.pose().translate(x + SIZE / 2.0F, y + SIZE + 2, 0);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private static final ResourceLocation FROST = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    /** widgets/iceover.lua: four frost steps below 5, 0, -5, -10 degrees. */
    private static final float[] FROST_STEPS = {5.0F, 0.0F, -5.0F, -10.0F};

    /** The DS ice overlay: the screen frosts over in four steps as the player freezes. */
    public static void renderFrost(GuiGraphics graphics, DeltaTracker delta) {
        if (!ClientClock.active() || stats == null) {
            return;
        }
        int step = 0;
        while (step < FROST_STEPS.length && stats.temperature() < FROST_STEPS[step]) {
            step++;
        }
        if (step == 0) {
            return;
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, step / 4.0F);
        graphics.blit(FROST, 0, 0, -90, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(),
                graphics.guiWidth(), graphics.guiHeight());
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /** The badges replace the vanilla hearts and hunger bar on the islands. */
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (ClientClock.active() && stats != null
                && (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL))) {
            event.setCanceled(true);
        }
    }

    private StatsHud() {
    }
}
