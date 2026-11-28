package net.celestiald.dontstarve.fire;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.celestiald.dontstarve.DSRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for the fire engine, all on the islands level (replies carry no commas, the test
 * runner reads "x, y, z" triples as island coordinates):
 * {@code /ds fire ignite|extinguish|smoulder|check <pos>}, {@code /ds fire heat <pos> <amount>},
 * {@code /ds fire light <player> <pos>} (the torch LIGHT action),
 * {@code /ds fire feed <player> <pos>} (the main-hand item as fuel, ADDFUEL) and
 * {@code /ds cookfire <player> <pos>} (the COOK action with the main-hand item) and
 * {@code /ds fire fuel <pos> set|add <ds seconds>} (fueled fires).
 */
public final class FireCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("fire")
                        .then(Commands.literal("ignite").then(pos().executes(ctx -> ignite(ctx))))
                        .then(Commands.literal("extinguish").then(pos().executes(ctx -> extinguish(ctx))))
                        .then(Commands.literal("smoulder").then(pos().executes(ctx -> smoulder(ctx))))
                        .then(Commands.literal("check").then(pos().executes(ctx -> check(ctx))))
                        .then(Commands.literal("heat").then(pos()
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> heat(ctx)))))
                        .then(Commands.literal("light")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(pos().executes(ctx -> light(ctx)))))
                        .then(Commands.literal("feed")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(pos().executes(ctx -> feed(ctx)))))
                        .then(Commands.literal("fuel").then(pos()
                                .then(Commands.literal("set").then(Commands.argument("ds", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> fuel(ctx, false))))
                                .then(Commands.literal("add").then(Commands.argument("ds", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> fuel(ctx, true)))))))
                .then(Commands.literal("cookfire")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(pos().executes(ctx -> cook(ctx))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> pos() {
        return Commands.argument("pos", BlockPosArgument.blockPos());
    }

    private static ServerLevel level(CommandContext<CommandSourceStack> ctx) {
        ServerLevel islands = ctx.getSource().getServer().getLevel(DSRefs.ISLANDS);
        return islands != null ? islands : ctx.getSource().getLevel();
    }

    private static BlockPos pos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return BlockPosArgument.getBlockPos(ctx, "pos");
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text, boolean ok) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return ok ? 1 : 0;
    }

    private static String name(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath();
    }

    private static int ignite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        boolean ok = DSFire.ignite(level, pos, null);
        return reply(ctx, (ok ? "ignited " : "cannot ignite ") + name(level, pos), ok);
    }

    private static int extinguish(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        boolean ok = DSFire.extinguish(level, pos);
        return reply(ctx, (ok ? "extinguished " : "nothing to extinguish at ") + name(level, pos), ok);
    }

    private static int smoulder(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        boolean ok = DSFire.smoulder(level, pos);
        return reply(ctx, (ok ? "smouldering " : "cannot smoulder ") + name(level, pos), ok);
    }

    private static int heat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        DSFire.addHeat(level, pos, DoubleArgumentType.getDouble(ctx, "amount"));
        return reply(ctx, String.format(Locale.ROOT, "heat %s %.1f", name(level, pos), DSFire.heat(level, pos)), true);
    }

    private static int light(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        boolean ok = DSFire.light(level, pos, player);
        return reply(ctx, (ok ? "lit " : "cannot light ") + name(level, pos), ok);
    }

    /** Right-click with the main-hand item on a fueled fire (ADDFUEL). */
    private static int feed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        var stack = player.getMainHandItem();
        if (!FueledFireBlock.feed(level, pos, player, stack)) {
            return reply(ctx, "not fuel for " + name(level, pos), false);
        }
        return reply(ctx, describe(level, pos) + " left " + stack.getCount(), true);
    }

    /** The COOK action with the main-hand item on the fire: "cooked X into Y left N" or "cannot cook X". */
    private static int cook(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        var stack = player.getMainHandItem();
        String raw = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        var product = Cooking.cook(level, pos, player, stack);
        if (product.isEmpty()) {
            return reply(ctx, "cannot cook " + raw, false);
        }
        return reply(ctx, "cooked " + raw + " into "
                + BuiltInRegistries.ITEM.getKey(product.get().getItem()).getPath() + " left " + stack.getCount(), true);
    }

    private static int fuel(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        BlockPos pos = pos(ctx);
        double ds = DoubleArgumentType.getDouble(ctx, "ds");
        if (!(level.getBlockEntity(pos) instanceof FueledFireBlockEntity fire)) {
            return reply(ctx, "not a fueled fire " + name(level, pos), false);
        }
        fire.setFuel(level, pos, add ? fire.fuel() + ds : ds);
        return reply(ctx, describe(level, pos), true);
    }

    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = level(ctx);
        return reply(ctx, describe(level, pos(ctx)), true);
    }

    /** One line: block, fire state, heat, light block, fuel. */
    static String describe(ServerLevel level, BlockPos pos) {
        BlockPos key = DSFire.burnPos(level, pos);
        if (key == null) {
            return "fire " + name(level, pos) + " not burnable";
        }
        BlockState state = level.getBlockState(key);
        FireData.Entry e = FireData.get(level).at(key);
        StringBuilder out = new StringBuilder("fire ").append(name(level, key)).append(' ');
        if (e != null && e.burning) {
            out.append("burning");
        } else if (e != null && e.smoldering()) {
            out.append("smoldering");
        } else {
            out.append("idle");
        }
        out.append(String.format(Locale.ROOT, " heat %.1f", e == null ? 0.0 : e.heat));
        out.append(" paused ").append(e != null && e.paused);
        if (e != null && e.burning) {
            long left = e.burnEnd < 0 ? -1 : e.burnEnd - level.getGameTime();
            out.append(" ticksleft ").append(left);
            out.append(" warmth ").append((int) e.warmth);
            out.append(" lightblock ").append(e.light != null && level.getBlockState(e.light).is(net.minecraft.world.level.block.Blocks.LIGHT));
        }
        if (state.getBlock() instanceof net.celestiald.dontstarve.craft.PrototyperBlock) {
            out.append(" burnt ").append(state.getValue(net.celestiald.dontstarve.craft.PrototyperBlock.BURNT));
        }
        if (level.getBlockEntity(key) instanceof FueledFireBlockEntity fire) {
            out.append(String.format(Locale.ROOT, " fuel %.1f section %d light %d queued %s", fire.fuel(), fire.section(),
                    state.getValue(FueledFireBlock.LIGHT), fire.queuedCharcoal()));
        }
        return out.toString();
    }

    private FireCommands() {
    }
}
