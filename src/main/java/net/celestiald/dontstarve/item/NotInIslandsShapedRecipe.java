package net.celestiald.dontstarve.item;

import com.mojang.serialization.MapCodec;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * A shaped recipe that does not work inside the islands dimension. Used to
 * override vanilla recipes that Don't Starve replaces with its own, e.g. the
 * stick + coal torch: on the islands torches come only from the DS recipe.
 */
public class NotInIslandsShapedRecipe extends ShapedRecipe {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, DontStarveCraft.MODID);

    public static final Supplier<RecipeSerializer<NotInIslandsShapedRecipe>> SERIALIZER =
            SERIALIZERS.register("not_in_islands_shaped", () -> new RecipeSerializer<>() {
                private final MapCodec<NotInIslandsShapedRecipe> codec =
                        ShapedRecipe.Serializer.CODEC.xmap(NotInIslandsShapedRecipe::of, r -> r);
                private final StreamCodec<RegistryFriendlyByteBuf, NotInIslandsShapedRecipe> streamCodec =
                        ShapedRecipe.Serializer.STREAM_CODEC.map(NotInIslandsShapedRecipe::of, r -> r);

                @Override
                public MapCodec<NotInIslandsShapedRecipe> codec() {
                    return codec;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, NotInIslandsShapedRecipe> streamCodec() {
                    return streamCodec;
                }
            });

    public NotInIslandsShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                                    ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    private static NotInIslandsShapedRecipe of(ShapedRecipe recipe) {
        return new NotInIslandsShapedRecipe(recipe.getGroup(), recipe.category(), recipe.pattern,
                recipe.getResultItem(null), recipe.showNotification());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !level.dimension().equals(DSRefs.ISLANDS) && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
}
