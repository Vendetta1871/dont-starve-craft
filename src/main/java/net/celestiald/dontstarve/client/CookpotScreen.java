package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.cook.CookpotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Crock Pot widget: four ingredient slots and a Cook button (placeholder hopper art). */
public class CookpotScreen extends AbstractContainerScreen<CookpotMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/hopper.png");
    private Button cook;

    public CookpotScreen(CookpotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 133;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Covers the hopper's fifth slot; the Cook button is only enabled with all four slots filled (CanCook).
        cook = addRenderableWidget(Button.builder(Component.translatable("gui.dontstarve.cook"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CookpotMenu.COOK_BUTTON))
                .bounds(leftPos + 116, topPos + 20, 52, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        cook.active = menu.canCook();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
