package net.celestiald.dontstarve.cook;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.celestiald.dontstarve.DontStarveCraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Crock Pot test commands (replies carry no commas, the test runner reads "x, y, z" triples as island coordinates):
 * <ul>
 *   <li>{@code /ds cook calc <ingredients...>} the resolver on DST prefab names or item ids (any count, also items
 *       that do not exist yet): {@code cook calc <product> priority <p> cooktime <n> candidates <a>/<b>
 *       tags <tag>=<v>/...}</li>
 *   <li>{@code /ds cookpot <pos> put <player>} one main-hand item into the first free slot:
 *       {@code cookpot put <prefab> slot <n>} or {@code cookpot cannot put <item>}</li>
 *   <li>{@code /ds cookpot <pos> cook} the Cook button: {@code cookpot cooking <product> ticks <n>} or
 *       {@code cookpot cannot cook <reason>}</li>
 *   <li>{@code /ds cookpot <pos> check}:
 *       {@code cookpot state <empty|cooking|done> burnt <b> items <n> product <p> cookticks <n> spoilticks <n> spoilage <f>}</li>
 *   <li>{@code /ds cookpot <pos> advance <ds seconds>} stewer LongUpdate, replies like {@code check}</li>
 *   <li>{@code /ds cook belly <player>}: {@code cook belly <degrees> <ds seconds left>} or {@code cook belly none}</li>
 *   <li>{@code /ds cookpot <pos> harvest <player>}: {@code cookpot harvested <prefab> x<count> fresh <percent>} or
 *       {@code cookpot nothing to harvest}</li>
 * </ul>
 */
public final class CookCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("cook")
                        .then(Commands.literal("calc")
                                .then(Commands.argument("ingredients", StringArgumentType.greedyString())
                                        .executes(CookCommands::calc))))
                .then(Commands.literal("cook")
                        .then(Commands.literal("belly")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CookCommands::belly))))
                .then(Commands.literal("cookpot")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("put")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(CookCommands::put)))
                                .then(Commands.literal("cook").executes(CookCommands::cook))
                                .then(Commands.literal("check").executes(CookCommands::check))
                                .then(Commands.literal("advance")
                                        .then(Commands.argument("ds", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0))
                                                .executes(CookCommands::advance)))
                                .then(Commands.literal("harvest")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(CookCommands::harvest))))));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text, boolean ok) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return ok ? 1 : 0;
    }

    private static ServerLevel level(CommandContext<CommandSourceStack> ctx) {
        ServerLevel islands = ctx.getSource().getServer().getLevel(DSRefs.ISLANDS);
        return islands != null ? islands : ctx.getSource().getLevel();
    }

    /** An argument as a DST prefab name: {@code dontstarve:berries} and {@code minecraft:gold_nugget} included. */
    private static String prefabArg(String arg) {
        if (arg.contains(":")) {
            ResourceLocation id = ResourceLocation.parse(arg);
            Item item = BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
            String prefab = item == null ? null : Cooking.prefab(item);
            return prefab != null ? prefab : id.getPath();
        }
        return arg;
    }

    private static int calc(CommandContext<CommandSourceStack> ctx) {
        List<String> prefabs = new ArrayList<>();
        for (String word : StringArgumentType.getString(ctx, "ingredients").trim().split("\\s+")) {
            prefabs.add(prefabArg(word));
        }
        Cooking.IngredientData data = Cooking.ingredientData(prefabs);
        List<CookRecipe> candidates = Cooking.candidates(data);
        CookRecipe chosen = Cooking.calculate(prefabs, ctx.getSource().getLevel().random);
        String tags = data.tags().asMap().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + trim(e.getValue())).collect(Collectors.joining("/"));
        return reply(ctx, String.format(Locale.ROOT, "cook calc %s priority %s cooktime %s candidates %s tags %s",
                chosen == null ? "none" : chosen.name(), chosen == null ? "-" : trim(chosen.priority()),
                chosen == null ? "-" : trim(chosen.cookTime()),
                candidates.stream().map(CookRecipe::name).collect(Collectors.joining("/")),
                tags.isEmpty() ? "none" : tags), chosen != null);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : String.format(Locale.ROOT, "%s", v);
    }

    private static CookpotBlockEntity pot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        return level(ctx).getBlockEntity(pos) instanceof CookpotBlockEntity be ? be : null;
    }

    private static int put(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CookpotBlockEntity be = pot(ctx);
        if (be == null) {
            return reply(ctx, "cookpot none here", false);
        }
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack hand = player.getMainHandItem();
        String name = BuiltInRegistries.ITEM.getKey(hand.getItem()).getPath();
        if (hand.isEmpty() || !Cooking.isIngredient(hand)) {
            return reply(ctx, "cookpot cannot put " + (hand.isEmpty() ? "nothing" : name), false);
        }
        for (int i = 0; i < CookpotBlockEntity.SIZE; i++) {
            if (be.getItem(i).isEmpty() && be.canPlaceItem(i, hand)) {
                be.setItem(i, hand.copyWithCount(1));
                hand.shrink(1);
                return reply(ctx, "cookpot put " + name + " slot " + i, true);
            }
        }
        return reply(ctx, "cookpot cannot put " + name + " full or busy", false);
    }

    private static int cook(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CookpotBlockEntity be = pot(ctx);
        if (be == null) {
            return reply(ctx, "cookpot none here", false);
        }
        String why = be.startCooking(null);
        if (why != null) {
            return reply(ctx, "cookpot cannot cook " + why, false);
        }
        return reply(ctx, "cookpot cooking " + be.product() + " ticks " + be.cookTicks(), true);
    }

    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CookpotBlockEntity be = pot(ctx);
        if (be == null) {
            return reply(ctx, "cookpot none here", false);
        }
        var state = be.getBlockState();
        return reply(ctx, String.format(Locale.ROOT,
                "cookpot state %s burnt %s items %d product %s cookticks %d spoilticks %d spoilage %.3f",
                be.phase().getSerializedName(), state.getValue(CookpotBlock.BURNT), be.itemCount(),
                be.product() == null ? "none" : be.product(), be.cookTicks(), be.spoilTicks(), be.productSpoilage()), true);
    }

    /** stewer:LongUpdate by a number of DS seconds. */
    private static int advance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CookpotBlockEntity be = pot(ctx);
        if (be == null) {
            return reply(ctx, "cookpot none here", false);
        }
        be.longUpdate(DSScale.ticks(com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "ds")));
        return check(ctx);
    }

    /** The belly temperature of the last warm or cold food: {@code cook belly <degrees> <ds seconds left>} or {@code cook belly none}. */
    private static int belly(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String report = net.celestiald.dontstarve.survive.Survival.bellyReport(EntityArgument.getPlayer(ctx, "player"));
        return reply(ctx, "cook belly " + (report == null ? "none" : report), true);
    }

    private static int harvest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CookpotBlockEntity be = pot(ctx);
        if (be == null) {
            return reply(ctx, "cookpot none here", false);
        }
        ItemStack dish = be.harvest(EntityArgument.getPlayer(ctx, "player"));
        if (dish == null) {
            return reply(ctx, "cookpot nothing to harvest", false);
        }
        return reply(ctx, String.format(Locale.ROOT, "cookpot harvested %s x%d fresh %.1f",
                BuiltInRegistries.ITEM.getKey(dish.getItem()).getPath(), dish.getCount(),
                net.celestiald.dontstarve.item.Perishable.percent(dish) * 100.0F), true);
    }

    private CookCommands() {
    }
}
