package net.celestiald.dontstarve.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.craft.Builder;
import net.celestiald.dontstarve.craft.BuilderData;
import net.celestiald.dontstarve.craft.DSRecipe;
import net.celestiald.dontstarve.craft.DSRecipes;
import net.celestiald.dontstarve.craft.TechTrees;
import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.network.CraftPayload;
import net.celestiald.dontstarve.network.OpenCraftingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The inventory on the islands: the DS crafting menu takes the place of the vanilla
 * 2x2 grid. Left of the inventory, in grey Minecraft panels: the filter tabs
 * (recipes_filter.lua), the recipe grid coloured by build state (craftingmenu_hud.lua)
 * and the details of the selected recipe with its ingredients and the
 * Build / Prototype / Place button, or the hint for where to prototype it.
 */
public class CraftingScreen extends EffectRenderingInventoryScreen<InventoryMenu> {

    private static final int PANEL_W = 150;
    private static final int GAP = 2;
    private static final int TAB = 20;
    private static final int TAB_STEP = 22;
    private static final int TABS_PER_COLUMN = 7;
    private static final int CELL = 18;
    private static final int GRID_ROWS = 3;
    /** Inventory menu slots 0-4: the vanilla crafting result and 2x2 grid, unused here. */
    private static final int GRID_SLOTS = 5;
    private static final ResourceLocation HEALTH = DontStarveCraft.rl("hud/health");
    private static final ResourceLocation SANITY = DontStarveCraft.rl("hud/sanity");

    private static String rememberedFilter = "";
    private static String rememberedRecipe = "";

    private final List<DSRecipes.Filter> tabs = new ArrayList<>();
    private final Map<DSRecipe, Builder.State> states = new HashMap<>();
    private List<DSRecipe> shown = List.of();
    private String filter = "";
    private DSRecipe selected;
    private int scrollRow;
    private TechTrees.Access access = TechTrees.Access.NONE;
    private Button button;
    private float xMouse;
    private float yMouse;

    public CraftingScreen(Player player) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("container.crafting"));
    }

    /** Test hook: open the menu on a tab and recipe, or with filter "PLACE" start placing a buffered recipe. */
    public static void open(OpenCraftingPayload payload) {
        if (payload.filter().equals("PLACE")) {
            Minecraft.getInstance().setScreen(null);
            Placement.start(payload.recipe());
            return;
        }
        if (!payload.filter().isEmpty()) {
            rememberedFilter = payload.filter();
        }
        if (!payload.recipe().isEmpty()) {
            rememberedRecipe = payload.recipe();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new CraftingScreen(mc.player));
        }
    }

    /** On the islands the inventory key opens this screen instead of the vanilla inventory. */
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getNewScreen() instanceof InventoryScreen && mc.player != null && mc.level != null
                && mc.level.dimension().equals(DSRefs.ISLANDS)) {
            event.setNewScreen(new CraftingScreen(mc.player));
        }
    }

    /** Vanilla recipe-book toasts make no sense where the grid does nothing. */
    public static void onToast(net.neoforged.neoforge.client.event.ToastAddEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getToast() instanceof net.minecraft.client.gui.components.toasts.RecipeToast
                && mc.level != null && mc.level.dimension().equals(DSRefs.ISLANDS)) {
            event.setCanceled(true);
        }
    }

    // ---- layout ----

    private int panelX() {
        return leftPos - PANEL_W - GAP;
    }

    private int tabColumns() {
        return Math.max(1, (tabs.size() + TABS_PER_COLUMN - 1) / TABS_PER_COLUMN);
    }

    private int tabX(int i) {
        return panelX() + 5 + (i / TABS_PER_COLUMN) * TAB_STEP;
    }

    private int tabY(int i) {
        return topPos + 5 + (i % TABS_PER_COLUMN) * TAB_STEP;
    }

    private int gridX() {
        return panelX() + 5 + tabColumns() * TAB_STEP + 2;
    }

    private int gridY() {
        return topPos + 5;
    }

    private int gridCols() {
        return (panelX() + PANEL_W - 5 - gridX()) / CELL;
    }

    /** Details and the button sit right of the tab column, under the grid. */
    private int detailsW() {
        return panelX() + PANEL_W - 5 - gridX();
    }

    private int detailsY() {
        return gridY() + GRID_ROWS * CELL + 6;
    }

    private int ingredientsY() {
        return detailsY() + 38;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(PANEL_W + GAP, (width - imageWidth - PANEL_W - GAP) / 2 + PANEL_W + GAP);
        filter = rememberedFilter;
        selected = null;
        button = addRenderableWidget(Button.builder(Component.empty(), b -> build())
                .bounds(panelX() + 6, topPos + imageHeight - 23, PANEL_W - 12, 18).build());
        // refresh() moves the button next to the tab column once the tabs are known.
        refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    /** Re-evaluates tech, recipe states, tabs and the selection (craftingmenu_hud.lua RebuildRecipes). */
    private void refresh() {
        Player player = minecraft.player;
        access = TechTrees.at(player.level(), player.position());
        BuilderData data = ClientBuilder.data();
        states.clear();
        for (DSRecipe r : DSRecipes.all()) {
            states.put(r, Builder.state(player, data, access, r));
        }
        tabs.clear();
        for (DSRecipes.Filter f : DSRecipes.filters()) {
            if (f.name().equals(DSRecipes.EVERYTHING) || f.recipes().stream().anyMatch(r -> states.get(r).visible())) {
                tabs.add(f);
            }
        }
        DSRecipes.Filter current = tabs.stream().filter(f -> f.name().equals(filter)).findFirst().orElse(tabs.get(0));
        filter = current.name();
        rememberedFilter = filter;
        // EVERYTHING also lists the hidden recipes (craftingmenu_widget.lua show_hidden).
        boolean showHidden = filter.equals(DSRecipes.EVERYTHING);
        shown = current.recipes().stream().filter(r -> showHidden || states.get(r).visible()).toList();
        if (selected == null || !shown.contains(selected)) {
            selected = shown.stream().filter(r -> r.name().equals(rememberedRecipe)).findFirst()
                    .orElse(shown.isEmpty() ? null : shown.get(0));
        }
        rememberedRecipe = selected == null ? "" : selected.name();
        int rows = (shown.size() + gridCols() - 1) / gridCols();
        scrollRow = Math.max(0, Math.min(scrollRow, rows - GRID_ROWS));
        if (selected != null && shown.indexOf(selected) / gridCols() >= scrollRow + GRID_ROWS) {
            scrollRow = shown.indexOf(selected) / gridCols() - GRID_ROWS + 1;
        }
        button.setX(gridX());
        button.setWidth(detailsW());
        updateButton();
    }

    private void updateButton() {
        if (selected == null) {
            button.visible = false;
            return;
        }
        Builder.State st = states.get(selected);
        button.visible = st == Builder.State.HAS_INGREDIENTS || st == Builder.State.NO_INGREDIENTS
                || st == Builder.State.PROTOTYPE || st == Builder.State.BUFFERED;
        button.active = Builder.canBuild(minecraft.player, st, selected);
        button.setMessage(Component.translatable(switch (st) {
            case PROTOTYPE -> "dontstarve.crafting.prototype";
            case BUFFERED -> "dontstarve.crafting.place";
            default -> "dontstarve.crafting.build";
        }));
    }

    private void build() {
        if (selected == null) {
            return;
        }
        Builder.State st = states.get(selected);
        if (!Builder.canBuild(minecraft.player, st, selected)) {
            return;
        }
        if (st != Builder.State.BUFFERED) {
            PacketDistributor.sendToServer(new CraftPayload(selected.name()));
        }
        if (selected.placer()) {
            Placement.start(selected.name());
            onClose();
        }
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderPanelTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
        xMouse = mouseX;
        yMouse = mouseY;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = leftPos;
        int j = topPos;
        g.blit(INVENTORY_LOCATION, i, j, 0, 0, imageWidth, imageHeight);
        g.fill(i + 96, j + 6, i + 172, j + 62, 0xFFC6C6C6); // where the vanilla 2x2 grid was
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, i + 26, j + 8, i + 75, j + 78, 30, 0.0625F,
                xMouse, yMouse, minecraft.player);
        renderPanel(g);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The active prototyper where the "Crafting" title was.
        if (access.prototyper() != null) {
            Component name = minecraft.level.getBlockState(access.prototyper()).getBlock().getName();
            // One line, shrunk to fit the 72 px where the "Crafting" title was.
            float scale = Math.min(1.0F, 72.0F / font.width(name));
            g.pose().pushPose();
            g.pose().translate(98, 8, 0);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, name, 0, 0, 0x404040, false);
            g.pose().popPose();
        }
    }

    private void renderPanel(GuiGraphics g) {
        int px = panelX();
        int py = topPos;
        panel(g, px, py, PANEL_W, imageHeight);
        for (int t = 0; t < tabs.size(); t++) {
            tab(g, tabX(t), tabY(t), tabs.get(t), tabs.get(t).name().equals(filter));
        }
        int cols = gridCols();
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = (scrollRow + row) * cols + col;
                int x = gridX() + col * CELL;
                int y = gridY() + row * CELL;
                if (idx < shown.size()) {
                    DSRecipe r = shown.get(idx);
                    cell(g, x, y, r, states.get(r));
                } else {
                    slotBg(g, x, y, 0xFF8B8B8B);
                }
            }
        }
        int sy = detailsY() - 4;
        g.fill(gridX(), sy, px + PANEL_W - 4, sy + 1, 0xFF8B8B8B);
        g.fill(gridX(), sy + 1, px + PANEL_W - 4, sy + 2, 0xFFFFFFFF);
        if (selected != null) {
            details(g, px, selected, states.get(selected));
        }
    }

    private void details(GuiGraphics g, int panelX, DSRecipe r, Builder.State st) {
        int px = gridX() - 6;
        int w = detailsW();
        int y = detailsY();
        boolean hidden = st == Builder.State.HINT || st == Builder.State.HIDE;
        g.renderItem(new ItemStack(r.product()), px + 6, y);
        g.drawString(font, r.product().getDescription(), px + 26, y + 4, 0x404040, false);
        List<FormattedCharSequence> desc = font.split(Component.translatable("dontstarve.recipe_desc." + r.name()), w);
        for (int k = 0; k < Math.min(2, desc.size()); k++) {
            g.drawString(font, desc.get(k), px + 6, y + 18 + k * 9, 0x555555, false);
        }
        int iy = ingredientsY();
        int x = px + 6;
        Player player = minecraft.player;
        for (DSRecipe.Ing ing : r.ingredients()) {
            slotBg(g, x, iy, 0xFF8B8B8B);
            g.renderItem(new ItemStack(ing.icon()), x + 1, iy + 1);
            int have = Builder.count(player.getInventory(), ing);
            count(g, x, iy, have + "/" + ing.amount(), have >= ing.amount());
            x += 22;
        }
        for (DSRecipe.CharacterIng c : r.characterIngredients()) {
            slotBg(g, x, iy, 0xFF8B8B8B);
            g.blitSprite(c.type() == DSRecipe.CharacterIng.Type.HEALTH ? HEALTH : SANITY, x + 1, iy + 1, 16, 16);
            count(g, x, iy, String.valueOf(c.amount()), Builder.hasCharacterIngredient(player, c));
            x += 22;
        }
        if (hidden) {
            List<FormattedCharSequence> hint = font.split(Component.translatable(hintKey(r)), w);
            for (int k = 0; k < Math.min(2, hint.size()); k++) {
                g.drawString(font, hint.get(k), px + 6, topPos + imageHeight - 24 + k * 9, 0x8B2020, false);
            }
        }
    }

    /** Ingredient count under its slot, red when short. */
    private void count(GuiGraphics g, int x, int y, String text, boolean enough) {
        g.pose().pushPose();
        g.pose().translate(x + 9, y + 19, 0);
        g.pose().scale(0.75F, 0.75F, 1.0F);
        g.drawCenteredString(font, text, 0, 0, enough ? 0xFFFFFF : 0xFF5555);
        g.pose().popPose();
    }

    private void cell(GuiGraphics g, int x, int y, DSRecipe r, Builder.State st) {
        int inner = switch (st) {
            case PROTOTYPE -> 0xFF4F78B0;
            case BUFFERED -> 0xFF4F9A4F;
            case NO_INGREDIENTS -> 0xFF7A5A5A;
            default -> 0xFF8B8B8B;
        };
        slotBg(g, x, y, inner);
        g.renderItem(new ItemStack(r.product()), x + 1, y + 1);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (st == Builder.State.HINT || st == Builder.State.HIDE) {
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xD0202020);
            g.drawCenteredString(font, "?", x + CELL / 2, y + 5, 0xC0C0C0);
        } else if (st == Builder.State.NO_INGREDIENTS) {
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x60000000);
        }
        if (r == selected) {
            g.renderOutline(x, y, CELL, CELL, 0xFFFFFFFF);
        }
        g.pose().popPose();
    }

    private void tab(GuiGraphics g, int x, int y, DSRecipes.Filter f, boolean active) {
        // Raised grey button, pressed (inset) when it is the open tab.
        g.fill(x, y, x + TAB, y + TAB, 0xFF000000);
        g.fill(x + 1, y + 1, x + TAB - 1, y + TAB - 1, active ? 0xFF555555 : 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + TAB - 1, y + TAB - 1, active ? 0xFFFFFFFF : 0xFF555555);
        g.fill(x + 2, y + 2, x + TAB - 2, y + TAB - 2, active ? 0xFF8B8B8B : 0xFFC6C6C6);
        if (f.name().equals(DSRecipes.EVERYTHING)) {
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    g.fill(x + 5 + a * 4, y + 5 + b * 4, x + 8 + a * 4, y + 8 + b * 4, 0xFF404040);
                }
            }
            return;
        }
        f.recipes().stream().filter(r -> states.get(r).visible()).findFirst()
                .ifPresent(r -> g.renderItem(new ItemStack(r.product()), x + 2, y + 2));
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 3, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 3, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    private static void slotBg(GuiGraphics g, int x, int y, int inner) {
        g.fill(x, y, x + CELL, y + CELL, 0xFFFFFFFF);
        g.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF373737);
        g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, inner);
    }

    private void renderPanelTooltip(GuiGraphics g, int mx, int my) {
        for (int t = 0; t < tabs.size(); t++) {
            if (inside(mx, my, tabX(t), tabY(t), TAB, TAB)) {
                g.renderTooltip(font, Component.translatable("dontstarve.filter." + tabs.get(t).name()), mx, my);
                return;
            }
        }
        int idx = cellAt(mx, my);
        if (idx >= 0) {
            g.renderTooltip(font, shown.get(idx).product().getDescription(), mx, my);
            return;
        }
        if (selected != null) {
            int x = gridX();
            for (DSRecipe.Ing ing : selected.ingredients()) {
                if (inside(mx, my, x, ingredientsY(), CELL, CELL)) {
                    g.renderTooltip(font, ing.icon().getDescription(), mx, my);
                    return;
                }
                x += 22;
            }
            for (DSRecipe.CharacterIng c : selected.characterIngredients()) {
                if (inside(mx, my, x, ingredientsY(), CELL, CELL)) {
                    g.renderTooltip(font, Component.translatable("dontstarve.crafting."
                            + (c.type() == DSRecipe.CharacterIng.Type.HEALTH ? "health" : "sanity")), mx, my);
                    return;
                }
                x += 22;
            }
        }
    }

    /**
     * The prototyper to name in the hint (craftingmenu_details.lua): among the machines
     * that would allow the recipe, the one whose tree matches it most closely.
     */
    private static String hintKey(DSRecipe r) {
        record Machine(String key, Map<String, Integer> trees) {
        }
        List<Machine> machines = List.of(
                new Machine("needsciencemachine", DSBlocks.RESEARCHLAB.get().trees()),
                new Machine("needalchemyengine", DSBlocks.RESEARCHLAB2.get().trees()),
                new Machine("needprestihatitator", DSBlocks.RESEARCHLAB4.get().trees()),
                new Machine("needshadowmanipulator", DSBlocks.RESEARCHLAB3.get().trees()));
        Machine best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Machine m : machines) {
            if (!TechTrees.canPrototype(r.level(), new TechTrees.Access(m.trees(), null))) {
                continue;
            }
            int score = 0;
            Set<String> keys = new TreeSet<>(r.level().keySet());
            keys.addAll(m.trees().keySet());
            for (String k : keys) {
                int rv = r.level().getOrDefault(k, 0);
                int pv = m.trees().getOrDefault(k, 0);
                if (rv == pv && (rv > 0 || pv > 0)) {
                    score++;
                } else if (rv < pv) {
                    score -= pv - rv;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return "dontstarve.crafting." + (best == null ? "cantresearch" : best.key());
    }

    // ---- input ----

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int cellAt(double mx, double my) {
        int cols = gridCols();
        if (!inside(mx, my, gridX(), gridY(), cols * CELL, GRID_ROWS * CELL)) {
            return -1;
        }
        int idx = (scrollRow + (int) (my - gridY()) / CELL) * cols + (int) (mx - gridX()) / CELL;
        return idx < shown.size() ? idx : -1;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int mouseButton) {
        if (mouseButton == 0) {
            for (int t = 0; t < tabs.size(); t++) {
                if (inside(mx, my, tabX(t), tabY(t), TAB, TAB)) {
                    filter = tabs.get(t).name();
                    selected = null;
                    scrollRow = 0;
                    refresh();
                    click();
                    return true;
                }
            }
            int idx = cellAt(mx, my);
            if (idx >= 0) {
                selected = shown.get(idx);
                rememberedRecipe = selected.name();
                updateButton();
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (inside(mx, my, gridX(), gridY(), gridCols() * CELL, GRID_ROWS * CELL)) {
            int rows = (shown.size() + gridCols() - 1) / gridCols();
            scrollRow = Math.max(0, Math.min(scrollRow - (int) Math.signum(scrollY), rows - GRID_ROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int mouseButton) {
        return super.hasClickedOutside(mx, my, left, top, mouseButton)
                && !inside(mx, my, panelX(), topPos, PANEL_W, imageHeight);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null && slot.index < GRID_SLOTS) {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        if (slot.index >= GRID_SLOTS) {
            super.renderSlot(g, slot);
        }
    }

    @Override
    protected void renderSlotHighlight(GuiGraphics g, Slot slot, int mouseX, int mouseY, float partialTick) {
        if (slot.index >= GRID_SLOTS) {
            super.renderSlotHighlight(g, slot, mouseX, mouseY, partialTick);
        }
    }
}
