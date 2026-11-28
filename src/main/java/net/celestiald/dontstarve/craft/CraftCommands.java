package net.celestiald.dontstarve.craft;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.TreeMap;
import net.celestiald.dontstarve.network.OpenCraftingPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Crafting test commands under {@code /ds}:
 * {@code builder <player> [reset]} knowledge and tech report,
 * {@code recipes <player>} the menu state of every visible recipe,
 * {@code craft <player> <recipe>} the menu's build button,
 * {@code place <player> <recipe> <pos>} placing a buffered structure,
 * {@code grid <player>} what the vanilla 2x2 grid makes from an oak log there,
 * {@code ui <player> [filter] [recipe]} opens the menu on the client (screenshots),
 * {@code check dsrecipe <name>} whether a recipe is in the DS book.
 */
public final class CraftCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("builder")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CraftCommands::builder)
                                .then(Commands.literal("reset").executes(ctx -> {
                                    Builder.freshStart(EntityArgument.getPlayer(ctx, "player"));
                                    return builder(ctx);
                                }))))
                .then(Commands.literal("recipes")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CraftCommands::recipes)))
                .then(Commands.literal("craft")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("recipe", StringArgumentType.word())
                                        .executes(ctx -> {
                                            Builder.makeFromMenu(EntityArgument.getPlayer(ctx, "player"),
                                                    StringArgumentType.getString(ctx, "recipe"));
                                            return builder(ctx);
                                        }))))
                .then(Commands.literal("place")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("recipe", StringArgumentType.word())
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(CraftCommands::place)))))
                .then(Commands.literal("grid")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CraftCommands::grid)))
                .then(Commands.literal("ui")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> ui(ctx, "", ""))
                                .then(Commands.argument("filter", StringArgumentType.word())
                                        .executes(ctx -> ui(ctx, StringArgumentType.getString(ctx, "filter"), ""))
                                        .then(Commands.argument("recipe", StringArgumentType.word())
                                                .executes(ctx -> ui(ctx, StringArgumentType.getString(ctx, "filter"),
                                                        StringArgumentType.getString(ctx, "recipe")))))))
                .then(Commands.literal("check")
                        .then(Commands.literal("dsrecipe")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(CraftCommands::checkRecipe)))));
    }

    private static int builder(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BuilderData data = Builder.data(player);
        TechTrees.Access access = TechTrees.at(player.level(), player.position());
        String proto = access.prototyper() == null ? "none"
                : BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(access.prototyper()).getBlock()).getPath();
        String line = String.format(Locale.ROOT, "%s known %s buffered %s tech %s at %s",
                player.getGameProfile().getName(), data.recipes(), data.buffered(),
                new TreeMap<>(access.trees()), proto);
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int recipes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BuilderData data = Builder.data(player);
        TechTrees.Access access = TechTrees.at(player.level(), player.position());
        StringJoiner out = new StringJoiner(" ", "recipes ", "");
        for (DSRecipe recipe : DSRecipes.all()) {
            Builder.State state = Builder.state(player, data, access, recipe);
            if (state.visible()) {
                out.add(recipe.name() + "=" + state.name().toLowerCase(Locale.ROOT));
            }
        }
        String line = out.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /** The position is printed without commas: the test runner reads "x, y, z" triples as coordinates. */
    private static int place(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String recipe = StringArgumentType.getString(ctx, "recipe");
        var pos = BlockPosArgument.getBlockPos(ctx, "pos");
        boolean ok = Builder.placeAt(player, recipe, pos);
        ctx.getSource().sendSuccess(() -> Component.literal((ok ? "placed " : "cannot place ") + recipe + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
        return ok ? 1 : 0;
    }

    /** Runs the inventory's 2x2 grid on one oak log and reports the result slot. */
    private static int grid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        var menu = player.inventoryMenu;
        menu.getCraftSlots().setItem(0, new ItemStack(Items.OAK_LOG));
        menu.slotsChanged(menu.getCraftSlots());
        ItemStack result = menu.getSlot(0).getItem().copy();
        menu.getCraftSlots().clearContent();
        menu.slotsChanged(menu.getCraftSlots());
        String line = "grid result " + (result.isEmpty() ? "none" : BuiltInRegistries.ITEM.getKey(result.getItem()) + " x" + result.getCount());
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int ui(CommandContext<CommandSourceStack> ctx, String filter, String recipe) throws CommandSyntaxException {
        PacketDistributor.sendToPlayer(EntityArgument.getPlayer(ctx, "player"), new OpenCraftingPayload(filter, recipe));
        ctx.getSource().sendSuccess(() -> Component.literal("ui sent"), false);
        return 1;
    }

    private static int checkRecipe(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        DSRecipe recipe = DSRecipes.get(name);
        String line = recipe == null ? "MISSING dsrecipe " + name
                : "OK dsrecipe " + name + " tech " + recipe.level() + (recipe.placer() ? " placer" : "")
                        + " filters " + recipe.filters();
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return recipe == null ? 0 : 1;
    }

    private CraftCommands() {
    }
}
