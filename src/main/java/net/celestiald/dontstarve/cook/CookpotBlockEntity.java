package net.celestiald.dontstarve.cook;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The stewer component (components/stewer.lua) of the Crock Pot with its four-slot container:
 * idle (fill it), cooking (the dish is chosen at the start by {@link Cooking#calculate}, takes
 * recipe cooktime x TUNING.BASE_COOK_TIME), done (harvest it; it spoils in the pot after the
 * recipe's perishtime, scaled by the freshness of the ingredients, and becomes rot).
 */
public class CookpotBlockEntity extends BaseContainerBlockEntity {

    /** The pot's container widget: 4 slots (widgetsetup "cookpot"). */
    public static final int SIZE = 4;
    /** stewer.spoiledproduct. */
    public static final String SPOILED = "spoiled_food";

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** stewer.product: the recipe name, or {@link #SPOILED}. */
    @Nullable
    private String product;
    /** stewer.product_spoilage: freshness of the dish, from the ingredients. */
    private float productSpoilage = 1.0F;
    /** Ticks until the dish is done while cooking. */
    private int cookTicks;
    /** The pot spoil timer of a finished dish: ticks left and the total it started with (0 = never spoils). */
    private int spoilTicks;
    private int spoilTotal;

    public CookpotBlockEntity(BlockPos pos, BlockState state) {
        super(CookInit.COOKPOT_ENTITY.get(), pos, state);
    }

    public CookpotBlock.Phase phase() {
        return getBlockState().getValue(CookpotBlock.PHASE);
    }

    private void setPhase(CookpotBlock.Phase phase) {
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(CookpotBlock.PHASE, phase), Block.UPDATE_ALL);
        }
        setChanged();
    }

    @Nullable
    public String product() {
        return product;
    }

    public int cookTicks() {
        return cookTicks;
    }

    public int spoilTicks() {
        return spoilTotal > 0 ? spoilTicks : -1;
    }

    public float productSpoilage() {
        return productSpoilage;
    }

    /** The ingredients as DST prefab names. */
    public List<String> ingredientPrefabs() {
        List<String> prefabs = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                String prefab = Cooking.prefab(stack.getItem());
                prefabs.add(prefab != null ? prefab : "unknown");
            }
        }
        return prefabs;
    }

    // ---- container ----

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Only cooking ingredients, and only while the pot is idle (container.canbeopened). */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return phase() == CookpotBlock.Phase.EMPTY && !getBlockState().getValue(CookpotBlock.BURNT)
                && Cooking.isIngredient(stack);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newItems) {
        items = newItems;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dontstarve.cookpot");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CookpotMenu(id, inventory, this);
    }

    /** stewer:CanCook: the container is full. */
    public boolean isFull() {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int itemCount() {
        int n = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    // ---- stewer ----

    /** Why cooking did not start, null when it did. */
    @Nullable
    public String startCooking(@Nullable Player doer) {
        if (level == null || level.isClientSide || phase() != CookpotBlock.Phase.EMPTY
                || getBlockState().getValue(CookpotBlock.BURNT)) {
            return "busy";
        }
        if (!isFull()) {
            return "not full";
        }
        CookRecipe recipe = Cooking.calculate(ingredientPrefabs(), level.random);
        if (recipe == null || Cooking.item(recipe.name()) == null) {
            // A product that is no item (the bat nose hat) cannot come out of the pot yet: wet goop.
            recipe = Cooking.recipe("wetgoop");
        }
        product = recipe.name();
        if (recipe.perishTime() > 0) {
            // stewer.lua StartCooking: average freshness of the perishable ingredients, refreshed by half.
            float total = 0.0F;
            int n = 0;
            for (ItemStack stack : items) {
                if (Perishable.isPerishable(stack)) {
                    n++;
                    total += Perishable.percent(stack);
                }
            }
            productSpoilage = n <= 0 ? 1.0F : 1.0F - (1.0F - total / n) * 0.5F;
        } else {
            productSpoilage = 1.0F;
        }
        cookTicks = Math.max(1, DSScale.ticks(T.BASE_COOK_TIME * recipe.cookTime()));
        spoilTicks = 0;
        spoilTotal = 0;
        items.clear();
        for (int i = 0; i < SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setPhase(CookpotBlock.Phase.COOKING);
        return null;
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        CookpotBlock.Phase phase = state.getValue(CookpotBlock.PHASE);
        if (phase == CookpotBlock.Phase.COOKING) {
            if (--cookTicks <= 0) {
                done();
            }
            setChanged();
        } else if (phase == CookpotBlock.Phase.DONE && spoilTotal > 0 && --spoilTicks <= 0) {
            // dospoil -> spoilfn: the dish in the pot turns into rot.
            product = SPOILED;
            spoilTotal = 0;
            spoilTicks = 0;
            setChanged();
        }
    }

    /** stewer:LongUpdate: advances cooking, then the spoil timer, by {@code dt} ticks at once. */
    public void longUpdate(int dt) {
        if (phase() == CookpotBlock.Phase.COOKING) {
            if (cookTicks > dt) {
                cookTicks -= dt;
                dt = 0;
            } else {
                dt -= cookTicks;
                cookTicks = 0;
                done();
            }
        }
        if (dt > 0 && phase() == CookpotBlock.Phase.DONE && spoilTotal > 0) {
            spoilTicks -= dt;
            if (spoilTicks <= 0) {
                product = SPOILED;
                spoilTotal = 0;
                spoilTicks = 0;
            }
        }
        setChanged();
    }

    /** dostew: the dish is done; it starts spoiling in the pot after perishtime x freshness. */
    private void done() {
        CookRecipe recipe = product == null ? null : Cooking.recipe(product);
        double perish = recipe == null ? 0 : recipe.potPerishTime();
        if (perish > 0) {
            spoilTotal = Math.max(1, DSScale.ticks(perish * productSpoilage));
            spoilTicks = spoilTotal;
        }
        cookTicks = 0;
        setPhase(CookpotBlock.Phase.DONE);
    }

    /** The finished dish as the stack stewer:Harvest gives (freshness = spoilage x spoil time left). */
    @Nullable
    public ItemStack dish() {
        if (phase() != CookpotBlock.Phase.DONE || product == null) {
            return null;
        }
        Item item = SPOILED.equals(product) ? DSItems.SPOILED_FOOD.get() : Cooking.item(product);
        if (item == null) {
            return null;
        }
        CookRecipe recipe = Cooking.recipe(product);
        ItemStack stack = new ItemStack(item, recipe != null ? recipe.stackSize() : 1);
        if (spoilTotal > 0 && Perishable.isPerishable(stack)) {
            Perishable.setPercent(stack, productSpoilage * spoilTicks / spoilTotal);
        }
        return stack;
    }

    /** stewer:Harvest into the harvester's inventory (dropped at their feet when full); null when nothing is done. */
    @Nullable
    public ItemStack harvest(@Nullable ServerPlayer harvester) {
        ItemStack dish = dish();
        if (dish == null) {
            return null;
        }
        resetProduct();
        setPhase(CookpotBlock.Phase.EMPTY);
        ItemStack given = dish.copy();
        if (harvester != null) {
            harvester.getInventory().placeItemBackInInventory(dish);
        } else if (level != null) {
            Block.popResource(level, worldPosition, dish);
        }
        return given;
    }

    private void resetProduct() {
        product = null;
        productSpoilage = 1.0F;
        cookTicks = 0;
        spoilTicks = 0;
        spoilTotal = 0;
    }

    /** The pot is going away (hammered, broken, burnt): the loaded ingredients and a finished dish drop out. */
    public void dropAll(ServerLevel level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
        ItemStack dish = dish();
        if (dish != null) {
            Block.popResource(level, pos, dish);
        }
        resetProduct();
        clearContent();
        setChanged();
    }

    /** DefaultBurntStructureFn + stewer:StopCooking("fire"): the dish being cooked pops out, the pot is a wreck. */
    public void burnDown(ServerLevel level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
        clearContent();
        if (product != null && phase() != CookpotBlock.Phase.EMPTY) {
            Item item = SPOILED.equals(product) ? DSItems.SPOILED_FOOD.get() : Cooking.item(product);
            if (item != null) {
                Block.popResource(level, pos, new ItemStack(item));
            }
        }
        resetProduct();
        setChanged();
    }

    // ---- persistence (stewer OnSave/OnLoad) ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (product != null) {
            tag.putString("Product", product);
            tag.putFloat("Spoilage", productSpoilage);
            tag.putInt("CookTicks", cookTicks);
            tag.putInt("SpoilTicks", spoilTicks);
            tag.putInt("SpoilTotal", spoilTotal);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        product = tag.contains("Product") ? tag.getString("Product") : null;
        productSpoilage = tag.contains("Spoilage") ? tag.getFloat("Spoilage") : 1.0F;
        cookTicks = tag.getInt("CookTicks");
        spoilTicks = tag.getInt("SpoilTicks");
        spoilTotal = tag.getInt("SpoilTotal");
    }
}
