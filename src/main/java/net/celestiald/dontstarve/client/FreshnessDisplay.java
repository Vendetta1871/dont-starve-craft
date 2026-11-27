package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Shows food freshness: a bar along the bottom of the slot (green fresh, yellow
 * stale, red spoiled; DST tints the slot instead) and a tooltip line.
 */
public final class FreshnessDisplay {

    public static void onRegisterDecorations(RegisterItemDecorationsEvent event) {
        event.register(DSItems.MONSTERMEAT.get(), (graphics, font, stack, x, y) -> {
            if (!Perishable.isPerishable(stack)) {
                return false;
            }
            float percent = Perishable.percent(stack);
            int colour = switch (Perishable.freshness(stack)) {
                case FRESH -> 0xFF5BC236;
                case STALE -> 0xFFE0C23A;
                case SPOILED -> 0xFFC0392B;
            };
            int width = Math.round(13 * percent);
            graphics.fill(x + 2, y + 13, x + 15, y + 15, 0xFF000000);
            graphics.fill(x + 2, y + 13, x + 2 + width, y + 14, colour);
            return false;
        });
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!Perishable.isPerishable(stack)) {
            return;
        }
        Perishable.Freshness freshness = Perishable.freshness(stack);
        ChatFormatting colour = switch (freshness) {
            case FRESH -> ChatFormatting.GREEN;
            case STALE -> ChatFormatting.YELLOW;
            case SPOILED -> ChatFormatting.RED;
        };
        event.getToolTip().add(Component.translatable("tooltip.dontstarve." + freshness.name().toLowerCase(java.util.Locale.ROOT),
                Math.round(Perishable.percent(stack) * 100)).withStyle(colour));
    }

    private FreshnessDisplay() {
    }
}
