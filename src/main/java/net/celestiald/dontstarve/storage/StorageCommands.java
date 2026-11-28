package net.celestiald.dontstarve.storage;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.item.Perishable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test commands for the food structures, on the islands level (replies carry no commas):
 * <ul>
 *   <li>{@code /ds dryer <pos> put <player>} puts one item of the main hand on the rack
 *       ("rack took X slot N" / "rack cannot take X"), {@code check} ("rack used N of 3 burnt B"),
 *       {@code slot <n>} ("rack slot N X drying|done T fresh P%" / "rack slot N empty") and
 *       {@code harvest <player>} ("harvested X" / "nothing to harvest").</li>
 *   <li>{@code /ds icebox <pos> put <player>} stores the main-hand stack ("icebox took X xN" /
 *       "icebox rejects X"), {@code check} ("icebox used N of 9"), {@code slot <n>}
 *       ("icebox slot N X xC fresh P%" / "icebox slot N empty") and {@code take <player> <slot>}
 *       ("took X xN" / "nothing to take").</li>
 * </ul>
 */
public final class StorageCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("dryer").then(pos()
                        .then(Commands.literal("put").then(player().executes(StorageCommands::dryerPut)))
                        .then(Commands.literal("check").executes(StorageCommands::dryerCheck))
                        .then(Commands.literal("slot").then(Commands.argument("n", IntegerArgumentType.integer(0, 2))
                                .executes(StorageCommands::dryerSlot)))
                        .then(Commands.literal("harvest").then(player().executes(StorageCommands::dryerHarvest)))))
                .then(Commands.literal("icebox").then(pos()
                        .then(Commands.literal("put").then(player().executes(StorageCommands::iceboxPut)))
                        .then(Commands.literal("check").executes(StorageCommands::iceboxCheck))
                        .then(Commands.literal("slot").then(Commands.argument("n", IntegerArgumentType.integer(0, 8))
                                .executes(StorageCommands::iceboxSlot)))
                        .then(Commands.literal("take").then(player()
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 8))
                                        .executes(StorageCommands::iceboxTake)))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> pos() {
        return Commands.argument("pos", BlockPosArgument.blockPos());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> player() {
        return Commands.argument("player", EntityArgument.player());
    }

    private static ServerLevel level(CommandContext<CommandSourceStack> ctx) {
        ServerLevel islands = ctx.getSource().getServer().getLevel(DSRefs.ISLANDS);
        return islands != null ? islands : ctx.getSource().getLevel();
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text, boolean ok) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return ok ? 1 : 0;
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static String fresh(ItemStack stack) {
        return String.format(Locale.ROOT, "fresh %.1f%%", Perishable.percent(stack) * 100.0F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ---- Drying Rack ----

    private static DryingRackBlockEntity rack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return level(ctx).getBlockEntity(BlockPosArgument.getBlockPos(ctx, "pos")) instanceof DryingRackBlockEntity r ? r : null;
    }

    private static int dryerPut(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DryingRackBlockEntity rack = rack(ctx);
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack stack = player.getMainHandItem();
        int slot = rack == null ? -1 : rack.put(stack);
        if (slot < 0) {
            return reply(ctx, "rack cannot take " + id(stack), false);
        }
        String name = id(stack);
        stack.shrink(1);
        return reply(ctx, "rack took " + name + " slot " + slot, true);
    }

    private static int dryerCheck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DryingRackBlockEntity rack = rack(ctx);
        if (rack == null) {
            return reply(ctx, "not a rack", false);
        }
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        return reply(ctx, "rack used " + rack.used() + " of " + DryingRackBlockEntity.SLOTS + " burnt "
                + level(ctx).getBlockState(pos).getValue(DryingRackBlock.BURNT), true);
    }

    private static int dryerSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DryingRackBlockEntity rack = rack(ctx);
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (rack == null) {
            return reply(ctx, "not a rack", false);
        }
        ItemStack stack = rack.get(n);
        if (stack.isEmpty()) {
            return reply(ctx, "rack slot " + n + " empty", true);
        }
        float left = rack.dryLeft(n);
        String state = left >= 0 ? String.format(Locale.ROOT, "drying %.1f", left) : "done";
        return reply(ctx, "rack slot " + n + " " + id(stack) + " " + state + " " + fresh(stack), true);
    }

    private static int dryerHarvest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DryingRackBlockEntity rack = rack(ctx);
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack taken = rack == null ? ItemStack.EMPTY : rack.take();
        if (taken.isEmpty()) {
            return reply(ctx, "nothing to harvest", false);
        }
        String name = id(taken);
        give(player, taken);
        return reply(ctx, "harvested " + name, true);
    }

    // ---- Ice Box ----

    private static IceboxBlockEntity box(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return level(ctx).getBlockEntity(BlockPosArgument.getBlockPos(ctx, "pos")) instanceof IceboxBlockEntity b ? b : null;
    }

    private static int iceboxPut(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        IceboxBlockEntity box = box(ctx);
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack stack = player.getMainHandItem();
        if (box == null || !IceboxBlockEntity.accepts(stack)) {
            return reply(ctx, "icebox rejects " + id(stack), false);
        }
        int before = stack.getCount();
        ItemStack rest = HopperBlockEntity.addItem(null, box, stack.copy(), null);
        if (rest.getCount() == before) {
            return reply(ctx, "icebox rejects " + id(stack), false);
        }
        String name = id(stack);
        stack.setCount(rest.getCount());
        return reply(ctx, "icebox took " + name + " x" + (before - rest.getCount()), true);
    }

    private static int iceboxCheck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        IceboxBlockEntity box = box(ctx);
        if (box == null) {
            return reply(ctx, "not an icebox", false);
        }
        int used = 0;
        for (int i = 0; i < IceboxBlockEntity.SIZE; i++) {
            used += box.getItem(i).isEmpty() ? 0 : 1;
        }
        return reply(ctx, "icebox used " + used + " of " + IceboxBlockEntity.SIZE, true);
    }

    private static int iceboxSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        IceboxBlockEntity box = box(ctx);
        int n = IntegerArgumentType.getInteger(ctx, "n");
        if (box == null) {
            return reply(ctx, "not an icebox", false);
        }
        ItemStack stack = box.getItem(n);
        if (stack.isEmpty()) {
            return reply(ctx, "icebox slot " + n + " empty", true);
        }
        return reply(ctx, "icebox slot " + n + " " + id(stack) + " x" + stack.getCount() + " " + fresh(stack), true);
    }

    private static int iceboxTake(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        IceboxBlockEntity box = box(ctx);
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int n = IntegerArgumentType.getInteger(ctx, "n");
        ItemStack stack = box == null ? ItemStack.EMPTY : box.removeItemNoUpdate(n);
        if (stack.isEmpty()) {
            return reply(ctx, "nothing to take", false);
        }
        String line = "took " + id(stack) + " x" + stack.getCount();
        give(player, stack);
        box.setChanged();
        return reply(ctx, line, true);
    }

    private StorageCommands() {
    }
}
