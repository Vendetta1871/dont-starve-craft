package net.celestiald.dontstarve.cook;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.item.DSFoodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry of the Crock Pot (prefabs/cookpot.lua): the pot block, its block entity and menu, and
 * every prepared food of preparedfoods.lua as an item {@code dontstarve:<prefab>}.
 */
public final class CookInit {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DontStarveCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DontStarveCraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DontStarveCraft.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DontStarveCraft.MODID);

    public static final DeferredBlock<CookpotBlock> COOKPOT = BLOCKS.register("cookpot", () -> new CookpotBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(CookpotBlock::lightLevel)));
    public static final DeferredItem<BlockItem> COOKPOT_ITEM = ITEMS.registerSimpleBlockItem(COOKPOT);

    public static final Supplier<BlockEntityType<CookpotBlockEntity>> COOKPOT_ENTITY = BLOCK_ENTITIES.register("cookpot",
            () -> BlockEntityType.Builder.of(CookpotBlockEntity::new, COOKPOT.get()).build(null));

    public static final Supplier<MenuType<CookpotMenu>> COOKPOT_MENU = MENUS.register("cookpot",
            () -> new MenuType<>(CookpotMenu::new, FeatureFlags.DEFAULT_FLAGS));

    /** Every prepared food that is a prefab, by DST name (preparedfoods.lua prefabs/preparedfoods.lua). */
    public static final Map<String, DeferredItem<DSFoodItem>> FOODS = new LinkedHashMap<>();

    static {
        for (CookRecipe recipe : Cooking.recipes()) {
            if (!recipe.noPrefab()) {
                FOODS.put(recipe.name(), food(recipe));
            }
        }
    }

    /**
     * prefabs/preparedfoods.lua: edible hunger/health/sanity from the recipe, perishable into rot
     * when it has a perishtime, temperature and duration when given. Roughage and elemental food
     * (beefalo feed, dust meringue) is not for a player's mouth: no food component.
     */
    private static DeferredItem<DSFoodItem> food(CookRecipe recipe) {
        DSFoodItem.Edible edible = new DSFoodItem.Edible((float) recipe.hunger(), (float) recipe.health(), (float) recipe.sanity());
        float perish = (float) recipe.perishTime();
        DSFoodItem.Belly belly = recipe.temperature() != 0 && recipe.temperatureDuration() != 0
                ? new DSFoodItem.Belly((float) recipe.temperature(), (float) recipe.temperatureDuration()) : null;
        Item.Properties properties = new Item.Properties().stacksTo(40);
        String type = recipe.foodType();
        if (!"ROUGHAGE".equals(type) && !"ELEMENTAL".equals(type)) {
            properties.food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build());
        }
        return ITEMS.registerItem(recipe.name(),
                p -> new DSFoodItem(p, edible, perish, perish > 0 ? () -> DSItems.SPOILED_FOOD.get() : null, belly),
                properties);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }

    private CookInit() {
    }
}
