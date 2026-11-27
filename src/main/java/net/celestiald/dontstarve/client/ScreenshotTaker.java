package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.network.ShotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Takes the screenshot requested by {@link ShotPayload}. It waits a few frames so the
 * HUD toggle and freshly loaded chunks are on screen, then saves
 * {@code screenshots/<name>.png} and restores the HUD setting.
 */
public final class ScreenshotTaker {

    /** Frames rendered between the request and the capture. */
    private static final int SETTLE_FRAMES = 20;

    private static String pending;
    private static int framesLeft;
    private static boolean hideGuiBefore;

    public static void request(ShotPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (pending == null) {
            hideGuiBefore = mc.options.hideGui;
        }
        mc.options.hideGui = !payload.hud();
        pending = payload.name();
        framesLeft = SETTLE_FRAMES;
    }

    public static void onRenderFrame(RenderFrameEvent.Post event) {
        if (pending == null || --framesLeft > 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, pending + ".png", mc.getMainRenderTarget(), message -> {
        });
        mc.options.hideGui = hideGuiBefore;
        pending = null;
    }

    private ScreenshotTaker() {
    }
}
