package dev.dontstarve;

import com.mojang.brigadier.context.CommandContext;
import dev.dontstarve.world.IslandState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator commands used to drive the island lifecycle during development:
 * {@code /ds island current} and {@code /ds island next}.
 */
public final class DSCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("island")
                        .then(Commands.literal("current").executes(DSCommands::currentIsland))
                        .then(Commands.literal("next").executes(DSCommands::nextIsland))));
    }

    private static int currentIsland(CommandContext<CommandSourceStack> ctx) {
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return 0;
        }
        int index = IslandState.get(overworld).islandIndex();
        BlockPos center = IslandState.islandCenter(index);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Current island " + index + " centered at " + center.toShortString()), false);
        return 1;
    }

    private static int nextIsland(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        if (overworld == null || islands == null) {
            return 0;
        }
        int index = IslandState.get(overworld).advance();
        BlockPos center = IslandState.islandCenter(index);
        IslandState.pregenArrival(islands, index);
        ctx.getSource().sendSuccess(() -> Component.literal("Island " + index
                        + " created at " + center.toShortString() + ", arrival area pre-generated")
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private DSCommands() {
    }
}
