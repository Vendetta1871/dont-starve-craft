package net.celestiald.dontstarve;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.portal.PortalStorage;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.world.IslandState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator commands used to drive the island lifecycle during development and
 * in the headless test scenarios: {@code /ds island current|next|goto <n>},
 * the vault helpers {@code /ds vault put|take <pos> [item]} that stand in for
 * a player walking into the portal or right-clicking the frame, and
 * {@code /ds ghost <player> on|off|check} for the ghost/wipe flow and
 * {@code /ds hound now <wave>} to release a hound wave on demand.
 */
public final class DSCommands {

    public static void register(RegisterCommandsEvent event) {
        var buildCtx = event.getBuildContext();
        event.getDispatcher().register(Commands.literal("ds")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("island")
                        .then(Commands.literal("current").executes(DSCommands::currentIsland))
                        .then(Commands.literal("next").executes(DSCommands::nextIsland))
                        .then(Commands.literal("goto")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(DSCommands::gotoIsland))))
                .then(Commands.literal("vault")
                        .then(Commands.literal("put")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("item", ItemArgument.item(buildCtx))
                                                .executes(DSCommands::vaultPut))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(DSCommands::vaultTake))))
                .then(Commands.literal("ghost")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("on").executes(ctx -> ghost(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> ghost(ctx, false)))
                                .then(Commands.literal("check").executes(DSCommands::ghostCheck))))
                .then(Commands.literal("hound")
                        .then(Commands.literal("now")
                                .then(Commands.argument("wave", IntegerArgumentType.integer(1, 10))
                                        .executes(DSCommands::houndNow))))
                .then(Commands.literal("check")
                        .then(Commands.literal("item")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(DSCommands::checkItem)))
                        .then(Commands.literal("block")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(DSCommands::checkBlock)))
                        .then(Commands.literal("recipe")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(DSCommands::checkRecipe)))
                        .then(Commands.literal("tag")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(DSCommands::checkTag)))));
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

    private static int gotoIsland(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        if (overworld == null || islands == null) {
            return 0;
        }
        int index = IntegerArgumentType.getInteger(ctx, "index");
        IslandState.get(overworld).setIndex(index);
        IslandState.pregenArrival(islands, index);
        BlockPos center = IslandState.islandCenter(index);
        ctx.getSource().sendSuccess(() -> Component.literal("Current island " + index
                + " centered at " + center.toShortString()), true);
        return 1;
    }

    private static int vaultPut(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return 0;
        }
        BlockPos anchor = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
        PortalStorage.get(overworld).put(anchor, stack);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Stored " + stack.getCount() + " " + stack.getDisplayName().getString()
                        + " in vault " + anchor.toShortString()), true);
        return 1;
    }

    private static int vaultTake(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return 0;
        }
        BlockPos anchor = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        List<ItemStack> items = PortalStorage.get(overworld).take(anchor);
        for (ItemStack stack : items) {
            net.minecraft.world.level.block.Block.popResource(overworld, anchor.above(), stack);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Ejected " + items.size() + " stacks from vault " + anchor.toShortString()), true);
        return 1;
    }

    private static int ghost(CommandContext<CommandSourceStack> ctx, boolean on)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (on) {
            GhostState.becomeGhost(player);
        } else {
            GhostState.clear(player);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + (on ? " is now a ghost" : " is alive again")), true);
        return 1;
    }

    private static int ghostCheck(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ctx.getSource().sendSuccess(() -> Component.literal("ghost=" + GhostState.isGhost(player)
                + " dimension=" + player.level().dimension().location()), false);
        return 1;
    }

    private static int checkItem(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        var entry = ctx.getSource().registryAccess().registryOrThrow(Registries.ITEM).getHolder(id);
        return report(ctx, entry.isPresent(), "item " + id);
    }

    private static int checkBlock(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        var entry = ctx.getSource().registryAccess().registryOrThrow(Registries.BLOCK).getHolder(id);
        return report(ctx, entry.isPresent(), "block " + id);
    }

    private static int checkRecipe(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        boolean present = ctx.getSource().getServer().getRecipeManager()
                .byKey(id).isPresent();
        return report(ctx, present, "recipe " + id);
    }

    private static int checkTag(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        var tagKey = net.minecraft.tags.TagKey.create(Registries.BLOCK, id);
        var tag = ctx.getSource().registryAccess().registryOrThrow(Registries.BLOCK).getTag(tagKey);
        return report(ctx, tag.isPresent(), "tag " + id + " ("
                + tag.map(t -> String.valueOf(t.size())).orElse("0") + " blocks)");
    }

    private static int houndNow(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return 0;
        }
        int wave = IntegerArgumentType.getInteger(ctx, "wave");
        int spawned = net.celestiald.dontstarve.survive.HoundWave.forceWave(
                islands, IslandState.get(overworld), wave);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Hound wave " + wave + " released: " + spawned + " hound(s)"), true);
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> ctx, boolean present, String what) {
        if (present) {
            ctx.getSource().sendSuccess(() -> Component.literal("OK " + what), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("MISSING " + what));
        return 0;
    }

    private DSCommands() {
    }
}
