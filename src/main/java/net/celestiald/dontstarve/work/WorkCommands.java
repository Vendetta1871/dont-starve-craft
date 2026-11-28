package net.celestiald.dontstarve.work;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
 * {@code /ds work <player> <pos> [hits]}: the player works the block with the
 * tool in hand (tests and debugging); {@code swing} instead completes one mining
 * bar through Minecraft's break path, as a held mouse button does. Replies carry no
 * commas (the test runner reads "x, y, z" triples as island coordinates).
 */
public final class WorkCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("work")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> work(ctx, 1))
                                        .then(Commands.literal("swing").executes(WorkCommands::swing))
                                        .then(Commands.argument("hits", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> work(ctx, IntegerArgumentType.getInteger(ctx, "hits"))))))));
    }

    private static int work(CommandContext<CommandSourceStack> ctx, int hits) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        ServerLevel level = player.serverLevel();
        DSWork.Result result = DSWork.Result.NOT_WORKABLE;
        String block = "";
        int done = 0;
        for (int i = 0; i < hits; i++) {
            BlockState state = level.getBlockState(pos);
            block = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            result = DSWork.workedBy(level, pos, state, player, 1);
            if (result != DSWork.Result.WORKED) {
                if (result == DSWork.Result.FINISHED) {
                    done++;
                }
                break;
            }
            done++;
        }
        String reply = switch (result) {
            case NOT_WORKABLE -> "not workable " + block;
            case WRONG_TOOL -> "wrong tool for " + block;
            case WORKED -> "worked " + block + " hits " + done + " left "
                    + DSWork.workLeft(level, ((DSWorkable) level.getBlockState(pos).getBlock())
                    .workPos(level.getBlockState(pos), level, pos), -1);
            case FINISHED -> "finished " + block + " hits " + done;
        };
        ctx.getSource().sendSuccess(() -> Component.literal(reply), false);
        return result == DSWork.Result.WORKED || result == DSWork.Result.FINISHED ? 1 : 0;
    }

    /** One completed mining bar through Minecraft's own break path (ServerPlayerGameMode.destroyBlock). */
    private static int swing(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        ServerLevel level = player.serverLevel();
        player.gameMode.destroyBlock(pos);
        BlockState state = level.getBlockState(pos);
        String reply = state.getBlock() instanceof DSWorkable workable
                ? "swing " + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " left "
                        + DSWork.workLeft(level, workable.workPos(state, level, pos), workable.maxWork(state, level, pos))
                : "swing gone " + BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ctx.getSource().sendSuccess(() -> Component.literal(reply), false);
        return 1;
    }

    private WorkCommands() {
    }
}
