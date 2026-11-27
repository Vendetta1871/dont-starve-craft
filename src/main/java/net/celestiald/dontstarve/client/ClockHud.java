package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.clock.DSClock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The DS clock in the top-right corner: a ring of 16 segments coloured by phase
 * (day, dusk, night) for today's split, a hand for the time of day, and the day
 * number in the middle. Drawn pixel by pixel to stay in the blocky Minecraft look.
 */
public final class ClockHud {

    private static final int RADIUS = 22;
    private static final int SIZE = RADIUS * 2;
    private static final int DAY = 0xFFE3B94B;
    private static final int DUSK = 0xFFB0493A;
    private static final int NIGHT = 0xFF26335E;
    private static final int RIM = 0xFF1B1B1B;
    private static final int FACE = 0xFF3C3C3C;
    private static final int HAND = 0xFFF2F2F2;

    private static int[] pixels;
    private static DSClock.Segs pixelSegs;

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!ClientClock.active()) {
            return;
        }
        DSClock.State state = ClientClock.state();
        if (pixels == null || !state.day().segs().equals(pixelSegs)) {
            pixelSegs = state.day().segs();
            pixels = paint(pixelSegs);
        }
        int left = graphics.guiWidth() - SIZE - 6;
        int top = 6;
        // Draw runs of equal colour per row: a few hundred quads, not 1936.
        for (int y = 0; y < SIZE; y++) {
            int x = 0;
            while (x < SIZE) {
                int colour = pixels[y * SIZE + x];
                int end = x + 1;
                while (end < SIZE && pixels[y * SIZE + end] == colour) {
                    end++;
                }
                if (colour != 0) {
                    graphics.fill(left + x, top + y, left + end, top + y + 1, colour);
                }
                x = end;
            }
        }
        // Hand: clockwise from 12 o'clock through the day.
        double angle = state.dayFraction() * Math.PI * 2.0;
        double cx = left + RADIUS;
        double cy = top + RADIUS;
        for (double r = RADIUS * 0.45; r < RADIUS - 1.5; r += 0.5) {
            int px = (int) Math.floor(cx + Math.sin(angle) * r);
            int py = (int) Math.floor(cy - Math.cos(angle) * r);
            graphics.fill(px, py, px + 2, py + 2, HAND);
        }
        Font font = Minecraft.getInstance().font;
        String number = Integer.toString(state.day().cycles() + 1);
        Component label = Component.translatable("hud.dontstarve.day");
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 7, 0);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, label, -font.width(label) / 2, 0, 0xFFD8D8D8, false);
        graphics.pose().popPose();
        graphics.drawString(font, number, (int) cx - font.width(number) / 2, (int) cy - 2, 0xFFFFFFFF, true);
    }

    private static int[] paint(DSClock.Segs segs) {
        int[] out = new int[SIZE * SIZE];
        int[] segColour = new int[DSClock.NUM_SEGS];
        for (int i = 0; i < DSClock.NUM_SEGS; i++) {
            segColour[i] = i < segs.day() ? DAY : i < segs.day() + segs.dusk() ? DUSK : NIGHT;
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                double dx = x + 0.5 - RADIUS;
                double dy = y + 0.5 - RADIUS;
                double r = Math.sqrt(dx * dx + dy * dy);
                int colour = 0;
                if (r <= RADIUS) {
                    if (r > RADIUS - 1.5) {
                        colour = RIM;
                    } else if (r > RADIUS * 0.45) {
                        double turn = Math.atan2(dx, -dy) / (Math.PI * 2.0);
                        if (turn < 0) {
                            turn += 1.0;
                        }
                        double seg = turn * DSClock.NUM_SEGS;
                        int index = Math.min(DSClock.NUM_SEGS - 1, (int) seg);
                        colour = seg - index < 0.08 ? darker(segColour[index]) : segColour[index];
                    } else if (r > RADIUS * 0.45 - 1.0) {
                        colour = RIM;
                    } else {
                        colour = FACE;
                    }
                }
                out[y * SIZE + x] = colour;
            }
        }
        return out;
    }

    private static int darker(int argb) {
        int r = (argb >> 16 & 0xFF) * 3 / 5;
        int g = (argb >> 8 & 0xFF) * 3 / 5;
        int b = (argb & 0xFF) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private ClockHud() {
    }
}
