package net.celestiald.dontstarve.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.clock.RunClock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for the forest flora (replies carry no commas, see WorkCommands):
 * <ul>
 *   <li>{@code /ds pick <player> <pos>}: the player picks the plant by hand (the right-click path);</li>
 *   <li>{@code /ds fertilize <pos>}: pickable:Fertilize on an islands plant (stands in for fertilizer items);</li>
 *   <li>{@code /ds regrow status}: timers the regrowth manager waits on;
 *       {@code /ds regrow advance <ds seconds>}: one manager update of that many DS seconds.</li>
 * </ul>
 */
public final class FloraCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("pick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(FloraCommands::pick))))
                .then(Commands.literal("fertilize")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(FloraCommands::fertilize)))
                .then(Commands.literal("regrow")
                        .then(Commands.literal("status").executes(FloraCommands::status))
                        .then(Commands.literal("advance")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                        .executes(FloraCommands::advance)))));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text, int result) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return result;
    }

    private static int pick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (state.getBlock() instanceof PickableBlock plant && plant.pick(level, pos, state, player)) {
            return reply(ctx, "picked " + id, 1);
        }
        return reply(ctx, "not pickable " + id, 0);
    }

    private static int fertilize(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getServer().getLevel(DSRefs.ISLANDS);
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockState state = level.getBlockState(pos);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (state.getBlock() instanceof PickableBlock plant && plant.fertilize(level, pos, state)) {
            return reply(ctx, "fertilized " + id, 1);
        }
        return reply(ctx, "not barren " + id, 0);
    }

    private static String summary(FloraRegrowth regrowth) {
        return "regrow carrot_planted " + regrowth.pending("carrot_planted")
                + " flower " + regrowth.pending("flower") + " reeds " + regrowth.pending("reeds");
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel islands = ctx.getSource().getServer().getLevel(DSRefs.ISLANDS);
        return reply(ctx, summary(FloraRegrowth.get(islands)), 1);
    }

    private static int advance(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        FloraRegrowth regrowth = FloraRegrowth.get(islands);
        regrowth.update(islands, RunClock.state(server),
                IslandState.get(server.getLevel(Level.OVERWORLD)).islandIndex(), seconds);
        return reply(ctx, "advanced " + seconds + " " + summary(regrowth), 1);
    }

    private FloraCommands() {
    }
}
