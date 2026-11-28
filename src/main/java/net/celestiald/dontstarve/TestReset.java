package net.celestiald.dontstarve;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.List;
import net.celestiald.dontstarve.craft.Builder;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.survive.DSStats;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.survive.HoundWave;
import net.celestiald.dontstarve.survive.Survival;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /ds test reset [players]}: puts the server back to a clean slate between test
 * scenarios so one server session (and one client) can run the whole suite. The run
 * moves to a fresh island (new run clock, no hound waves, no wipe countdown), test
 * switches and game rules go back to their defaults, and each given player is sent to
 * the overworld spawn as a fresh, living, empty-handed survivor outside any run.
 */
public final class TestReset {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test")
                        .then(Commands.literal("reset")
                                .executes(ctx -> reset(ctx, List.of()))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> reset(ctx, EntityArgument.getPlayers(ctx, "players")))))));
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        IslandState state = IslandState.get(overworld);
        int index = state.advance();
        state.setWipeSecondsLeft(-1);
        HoundWave.paused = false;
        server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, server);
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, server);
        server.tickRateManager().stopSprinting();
        server.tickRateManager().setFrozen(false);
        server.tickRateManager().setTickRate(20.0F);
        for (ServerPlayer player : players) {
            resetPlayer(player, overworld);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("test reset: island " + index
                + ", players " + players.size()), false);
        return 1;
    }

    private static void resetPlayer(ServerPlayer player, ServerLevel overworld) {
        GhostState.clear(player);
        player.setData(DSAttachments.RUN_ISLAND.get(), -1);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        Survival.set(player, DSStats.FULL);
        Builder.freshStart(player);
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn);
        player.teleportTo(overworld, top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 0.0F, 0.0F);
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0.0F;
    }

    private TestReset() {
    }
}
