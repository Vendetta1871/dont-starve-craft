package net.celestiald.dontstarve.craft;

import java.util.List;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.network.BuilderPayload;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.survive.Survival;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Wilson's builder (DST components/builder.lua): recipe knowledge, prototyping at a
 * prototyper, ingredient checks and costs, buffered structure builds and placement.
 * The state queries are shared with the client's crafting menu; the actions run on
 * the server only.
 */
public final class Builder {

    /** TUNING.SANITY_MED: Builder:UnlockRecipe rewards learning a recipe. */
    public static final float UNLOCK_SANITY = 15.0F;

    /** The crafting menu's build_state for a recipe (craftingmenu_hud.lua RebuildRecipes). */
    public enum State {
        HIDE, HINT, NO_INGREDIENTS, HAS_INGREDIENTS, PROTOTYPE, BUFFERED;

        public boolean visible() {
            return this != HIDE;
        }
    }

    public static BuilderData data(Player player) {
        return player.getData(DSAttachments.BUILDER);
    }

    // ---- shared queries ----

    /** KnowsRecipe without tech bonuses: learned, or needs no tech at all. */
    public static boolean knows(BuilderData data, DSRecipe recipe) {
        return data.knows(recipe.name()) || recipe.noTech();
    }

    public static State state(Player player, BuilderData data, TechTrees.Access access, DSRecipe recipe) {
        boolean knows = knows(data, recipe);
        if (!knows && !TechTrees.shouldHint(recipe.level(), access)) {
            return State.HIDE;
        }
        if (data.isBuffered(recipe.name())) {
            return State.BUFFERED;
        }
        boolean canProto = TechTrees.canPrototype(recipe.level(), access);
        if (knows) {
            return hasIngredients(player, recipe) ? State.HAS_INGREDIENTS : State.NO_INGREDIENTS;
        }
        if (canProto) {
            if (recipe.nounlock()) {
                return hasIngredients(player, recipe) ? State.HAS_INGREDIENTS : State.NO_INGREDIENTS;
            }
            return State.PROTOTYPE;
        }
        return recipe.nounlock() ? State.HIDE : State.HINT;
    }

    /** True when the build/prototype button works (meta.can_build). */
    public static boolean canBuild(Player player, State state, DSRecipe recipe) {
        return state == State.BUFFERED
                || (state == State.HAS_INGREDIENTS || state == State.PROTOTYPE) && hasIngredients(player, recipe);
    }

    public static boolean hasIngredients(Player player, DSRecipe recipe) {
        for (DSRecipe.Ing ing : recipe.ingredients()) {
            if (count(player.getInventory(), ing) < ing.amount()) {
                return false;
            }
        }
        for (DSRecipe.CharacterIng c : recipe.characterIngredients()) {
            if (!hasCharacterIngredient(player, c)) {
                return false;
            }
        }
        return true;
    }

    /** HasCharacterIngredient: health must stay above the cost ("don't die from crafting"). */
    public static boolean hasCharacterIngredient(Player player, DSRecipe.CharacterIng c) {
        return switch (c.type()) {
            case HEALTH -> Math.ceil(player.getHealth() / DSScale.STAT) > c.amount();
            case SANITY -> Math.ceil(Survival.stats(player).sanity()) >= c.amount();
        };
    }

    public static int count(Inventory inventory, DSRecipe.Ing ing) {
        int n = 0;
        for (List<ItemStack> list : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : list) {
                if (ing.match().test(stack)) {
                    n += stack.getCount();
                }
            }
        }
        return n;
    }

    /** The cell a structure goes into and whether it may go there (Map:CanDeployRecipeAtPoint). */
    public static boolean canDeploy(Level level, BlockPos pos, DSRecipe recipe) {
        if (!level.dimension().equals(DSRefs.ISLANDS) || !(recipe.product() instanceof BlockItem item)) {
            return false;
        }
        BlockState here = level.getBlockState(pos);
        if (!here.canBeReplaced() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        if (!level.isUnobstructed(item.getBlock().defaultBlockState(), pos, CollisionContext.empty())) {
            return false;
        }
        // min_spacing: nothing solid within that radius at the structure's level.
        int r = (int) Math.ceil(recipe.minSpacing());
        double max = recipe.minSpacing() * recipe.minSpacing();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, 0, -r), pos.offset(r, 0, r))) {
            if (p.equals(pos) || p.distSqr(pos) >= max) {
                continue;
            }
            if (!level.getBlockState(p).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    // ---- server actions ----

    private static boolean canAct(ServerPlayer player) {
        return player.level().dimension().equals(DSRefs.ISLANDS) && player.isAlive()
                && !GhostState.isGhost(player) && !player.isSpectator();
    }

    /** MakeRecipeFromMenu, or BufferBuild for a structure. */
    public static void makeFromMenu(ServerPlayer player, String name) {
        DSRecipe recipe = DSRecipes.get(name);
        if (recipe == null || !canAct(player)) {
            return;
        }
        if (recipe.placer()) {
            bufferBuild(player, recipe);
            return;
        }
        TechTrees.Access access = TechTrees.at(player.level(), player.position());
        if (!hasIngredients(player, recipe)) {
            // Craft one of each missing intermediate the builder can make (_TryMakeIngredientRecipe).
            for (DSRecipe.Ing ing : recipe.ingredients()) {
                DSRecipe sub = DSRecipes.get(ing.prefab());
                if (sub != null && !sub.placer() && count(player.getInventory(), ing) < ing.amount()
                        && hasIngredients(player, sub)) {
                    if (learnOrUse(player, sub, access)) {
                        make(player, sub);
                    }
                }
            }
            return;
        }
        if (learnOrUse(player, recipe, access)) {
            make(player, recipe);
        }
    }

    /**
     * The knowledge half of a build: a known recipe just builds (and is remembered),
     * an unknown one is prototyped at the prototyper in range and learned.
     * False when the builder can neither.
     */
    private static boolean learnOrUse(ServerPlayer player, DSRecipe recipe, TechTrees.Access access) {
        BuilderData data = data(player);
        if (knows(data, recipe)) {
            if (!recipe.nounlock()) {
                set(player, data.withRecipe(recipe.name()));
            }
            return true;
        }
        if (!TechTrees.canPrototype(recipe.level(), access)) {
            return false;
        }
        if (access.prototyper() != null
                && player.level().getBlockState(access.prototyper()).getBlock() instanceof PrototyperBlock proto) {
            proto.activate(player.level(), access.prototyper());
        }
        if (!recipe.nounlock()) {
            // UnlockRecipe: learning is rewarding.
            Survival.set(player, Survival.stats(player).withSanity(Survival.stats(player).sanity() + UNLOCK_SANITY));
            set(player, data(player).withRecipe(recipe.name()));
        }
        return true;
    }

    /** BufferBuild: pay for a structure now, place it later. */
    private static void bufferBuild(ServerPlayer player, DSRecipe recipe) {
        if (data(player).isBuffered(recipe.name()) || !hasIngredients(player, recipe)) {
            return;
        }
        if (!learnOrUse(player, recipe, TechTrees.at(player.level(), player.position()))) {
            return;
        }
        removeIngredients(player, recipe);
        set(player, data(player).withBuffered(recipe.name(), true));
    }

    /** MakeRecipeAtPoint: place a buffered structure. */
    public static boolean placeAt(ServerPlayer player, String name, BlockPos pos) {
        DSRecipe recipe = DSRecipes.get(name);
        if (recipe == null || !recipe.placer() || !canAct(player) || !data(player).isBuffered(name)
                || !player.canInteractWithBlock(pos, 1.0) || !canDeploy(player.level(), pos, recipe)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Block block = ((BlockItem) recipe.product()).getBlock();
        BlockState state = block.defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        block.setPlacedBy(level, pos, state, player, recipe.result());
        level.playSound(null, pos, state.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        set(player, data(player).withBuffered(name, false));
        return true;
    }

    /** DoBuild for an item: consume the ingredients and hand over (or equip) the product. */
    private static void make(ServerPlayer player, DSRecipe recipe) {
        removeIngredients(player, recipe);
        ItemStack product = recipe.result();
        if (product.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot() != EquipmentSlot.MAINHAND
                && player.getItemBySlot(equipable.getEquipmentSlot()).isEmpty()) {
            player.setItemSlot(equipable.getEquipmentSlot(), product);
        } else if (product.getMaxStackSize() == 1 && player.getMainHandItem().isEmpty()) {
            // Hand equippables (tools, weapons, the torch) go straight into the hand.
            player.setItemSlot(EquipmentSlot.MAINHAND, product);
        } else if (!player.getInventory().add(product)) {
            player.drop(product, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void removeIngredients(ServerPlayer player, DSRecipe recipe) {
        Inventory inventory = player.getInventory();
        for (DSRecipe.Ing ing : recipe.ingredients()) {
            int left = ing.amount();
            for (List<ItemStack> list : List.of(inventory.items, inventory.offhand)) {
                for (ItemStack stack : list) {
                    if (left > 0 && ing.match().test(stack)) {
                        int take = Math.min(left, stack.getCount());
                        stack.shrink(take);
                        left -= take;
                    }
                }
            }
        }
        for (DSRecipe.CharacterIng c : recipe.characterIngredients()) {
            switch (c.type()) {
                case HEALTH -> {
                    player.setHealth(player.getHealth() - DSScale.stat(c.amount()));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
                case SANITY -> Survival.set(player, Survival.stats(player).withSanity(Survival.stats(player).sanity() - c.amount()));
            }
        }
        inventory.setChanged();
    }

    // ---- knowledge storage ----

    public static void set(ServerPlayer player, BuilderData data) {
        player.setData(DSAttachments.BUILDER, data);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        BuilderData data = data(player);
        PacketDistributor.sendToPlayer(player, new BuilderPayload(data.recipes(), data.buffered()));
    }

    /** A new run is a new character: nothing learned, nothing buffered. */
    public static void freshStart(ServerPlayer player) {
        set(player, BuilderData.EMPTY);
    }

    private Builder() {
    }
}
