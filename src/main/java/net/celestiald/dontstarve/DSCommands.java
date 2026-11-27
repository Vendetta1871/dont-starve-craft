package net.celestiald.dontstarve;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.survive.GhostState;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
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
 * {@code /ds codex <player>} (reads a Codex Umbra for them), and
 * {@code /ds ghost <player> on|off|check} for the ghost/wipe flow,
 * {@code /ds run <player> join|check} for run membership and
 * {@code /ds hound now <wave>} to release a hound wave on demand and
 * {@code /ds shot <player> <name> [hud]} to make that client save a screenshot and
 * {@code /ds stats <player> [set hunger|sanity|temperature <v>]}, {@code /ds eat <player>} (main hand) and
 * {@code /ds perish <player> [percent]} (main hand freshness) for DS stats and spoilage,
 * {@code /ds clock [set <day> day|dusk|night [ticks]]} to read or jump the run clock,
 * {@code /ds speed <mob> <x> <z> <ticks>} to measure a mob's real walking speed (logged).
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
                .then(Commands.literal("codex")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DSCommands::codex)))
                .then(Commands.literal("ghost")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("on").executes(ctx -> ghost(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> ghost(ctx, false)))
                                .then(Commands.literal("check").executes(DSCommands::ghostCheck))))
                .then(Commands.literal("run")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("join").executes(DSCommands::runJoin))
                                .then(Commands.literal("check").executes(DSCommands::runCheck))))
                .then(Commands.literal("hound")
                        .then(Commands.literal("pause")
                                .then(Commands.literal("on").executes(ctx -> houndPause(ctx, true)))
                                .then(Commands.literal("off").executes(ctx -> houndPause(ctx, false))))
                        .then(Commands.literal("now")
                                .then(Commands.argument("wave", IntegerArgumentType.integer(1, 10))
                                        .executes(DSCommands::houndNow))))
                .then(Commands.literal("stats")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DSCommands::statsReport)
                                .then(Commands.literal("set")
                                        .then(Commands.literal("hunger").then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0))
                                                .executes(ctx -> statsSet(ctx, true))))
                                        .then(Commands.literal("sanity").then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0))
                                                .executes(ctx -> statsSet(ctx, false))))
                                        .then(Commands.literal("temperature").then(Commands.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-20, 90))
                                                .executes(DSCommands::temperatureSet))))))
                .then(Commands.literal("eat")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DSCommands::eat)))
                .then(Commands.literal("perish")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> perish(ctx, -1.0F))
                                .then(Commands.argument("percent", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 100))
                                        .executes(ctx -> perish(ctx, com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "percent"))))))
                .then(Commands.literal("clock")
                        .executes(DSCommands::clockReport)
                        .then(Commands.literal("set")
                                .then(Commands.argument("day", IntegerArgumentType.integer(1))
                                        .then(Commands.literal("day").executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.DAY, 0))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0)).executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.DAY, IntegerArgumentType.getInteger(ctx, "ticks")))))
                                        .then(Commands.literal("dusk").executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.DUSK, 0))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0)).executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.DUSK, IntegerArgumentType.getInteger(ctx, "ticks")))))
                                        .then(Commands.literal("night").executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.NIGHT, 0))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0)).executes(ctx -> clockSet(ctx, net.celestiald.dontstarve.clock.DSClock.Phase.NIGHT, IntegerArgumentType.getInteger(ctx, "ticks"))))))))
                .then(Commands.literal("speed")
                        .then(Commands.argument("mob", EntityArgument.entity())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                                        .executes(DSCommands::speed))))))
                .then(Commands.literal("shot")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> shot(ctx, false))
                                        .then(Commands.literal("hud").executes(ctx -> shot(ctx, true))))))
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

    private static int statsReport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        var stats = net.celestiald.dontstarve.survive.Survival.stats(player);
        String line = String.format(java.util.Locale.ROOT, "%s health %.2f hunger %.2f sanity %.2f %s temperature %.2f light %.2f",
                player.getGameProfile().getName(), player.getHealth() / net.celestiald.dontstarve.DSScale.STAT,
                stats.hunger(), stats.sanity(), stats.sane() ? "sane" : "insane", stats.temperature(),
                player.level().dimension().equals(DSRefs.ISLANDS) ? net.celestiald.dontstarve.survive.DSLight.value(player) : -1.0F);
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int statsSet(CommandContext<CommandSourceStack> ctx, boolean hunger) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        float value = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
        var stats = net.celestiald.dontstarve.survive.Survival.stats(player);
        net.celestiald.dontstarve.survive.Survival.set(player, hunger ? stats.withHunger(value) : stats.withSanity(value));
        return statsReport(ctx);
    }

    private static int temperatureSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        float value = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
        net.celestiald.dontstarve.survive.Survival.set(player, net.celestiald.dontstarve.survive.Survival.stats(player).withTemperature(value));
        return statsReport(ctx);
    }

    private static int eat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof net.celestiald.dontstarve.item.DSFoodItem)) {
            ctx.getSource().sendFailure(Component.literal("Not holding a DS food"));
            return 0;
        }
        net.celestiald.dontstarve.survive.Survival.eat(player, stack);
        stack.shrink(1);
        return statsReport(ctx);
    }

    private static int perish(CommandContext<CommandSourceStack> ctx, float percent) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ItemStack stack = player.getMainHandItem();
        if (!net.celestiald.dontstarve.item.Perishable.isPerishable(stack)) {
            ctx.getSource().sendFailure(Component.literal("Not holding a perishable food"));
            return 0;
        }
        if (percent >= 0.0F) {
            net.celestiald.dontstarve.item.Perishable.setPercent(stack, percent / 100.0F);
        }
        String line = String.format(java.util.Locale.ROOT, "%s x%d %s %.1f%%", stack.getHoverName().getString(),
                stack.getCount(), net.celestiald.dontstarve.item.Perishable.freshness(stack).name().toLowerCase(java.util.Locale.ROOT),
                net.celestiald.dontstarve.item.Perishable.percent(stack) * 100.0F);
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int houndPause(CommandContext<CommandSourceStack> ctx, boolean paused) {
        net.celestiald.dontstarve.survive.HoundWave.paused = paused;
        ctx.getSource().sendSuccess(() -> Component.literal("Hound waves " + (paused ? "paused" : "resumed")), false);
        return 1;
    }

    private static int clockReport(CommandContext<CommandSourceStack> ctx) {
        var state = net.celestiald.dontstarve.clock.RunClock.state(ctx.getSource().getServer());
        var day = state.day();
        String line = String.format(java.util.Locale.ROOT,
                "Day %d %s %d/%d %s %d/%d left segs %d/%d/%d moon %s%s ambient %.2f temperature %.1f run %d",
                day.cycles() + 1, state.phase().name().toLowerCase(java.util.Locale.ROOT),
                state.phaseTicks(), state.phaseLength(), day.season().name().toLowerCase(java.util.Locale.ROOT),
                day.remainingDays(), day.totalDays(), day.segs().day(), day.segs().dusk(), day.segs().night(),
                state.moon().name().toLowerCase(java.util.Locale.ROOT), state.waxing() ? " waxing" : " waning",
                net.celestiald.dontstarve.clock.DSClock.ambientLevel(state),
                net.celestiald.dontstarve.clock.WorldTemperature.at(state, IslandState.get(ctx.getSource().getServer().getLevel(Level.OVERWORLD)).islandIndex()),
                state.runTicks());
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int clockSet(CommandContext<CommandSourceStack> ctx, net.celestiald.dontstarve.clock.DSClock.Phase phase, int offset) {
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return 0;
        }
        int day = IntegerArgumentType.getInteger(ctx, "day");
        IslandState.get(overworld).setRunTicks(net.celestiald.dontstarve.clock.DSClock.startOf(day - 1, phase) + offset);
        return clockReport(ctx);
    }

    private static int speed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!(EntityArgument.getEntity(ctx, "mob") instanceof net.minecraft.world.entity.Mob mob)) {
            ctx.getSource().sendFailure(Component.literal("Not a mob"));
            return 0;
        }
        SpeedProbe.start(mob, IntegerArgumentType.getInteger(ctx, "x") + 0.5,
                IntegerArgumentType.getInteger(ctx, "z") + 0.5, IntegerArgumentType.getInteger(ctx, "ticks"));
        ctx.getSource().sendSuccess(() -> Component.literal("Speed probe started"), false);
        return 1;
    }

    private static int shot(CommandContext<CommandSourceStack> ctx, boolean hud) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String name = StringArgumentType.getString(ctx, "name");
        if (!name.matches("[A-Za-z0-9_-]+")) {
            ctx.getSource().sendFailure(Component.literal("Shot name must be [A-Za-z0-9_-]+"));
            return 0;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new net.celestiald.dontstarve.network.ShotPayload(name, hud));
        ctx.getSource().sendSuccess(() -> Component.literal("Shot " + name + " requested from "
                + player.getGameProfile().getName()), false);
        return 1;
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
        ctx.getSource().sendSuccess(() -> Component.literal("Island " + index
                        + " created at " + center.toShortString())
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
        BlockPos center = IslandState.islandCenter(index);
        ctx.getSource().sendSuccess(() -> Component.literal("Current island " + index
                + " centered at " + center.toShortString()), true);
        return 1;
    }

    /** Reads a Codex Umbra for the player, exactly as right-clicking the book does. */
    private static int codex(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        boolean sent = net.celestiald.dontstarve.portal.CodexUmbraItem.travel(player,
                new ItemStack(net.celestiald.dontstarve.init.DSItems.WAXWELLJOURNAL.get()));
        if (!sent) {
            ctx.getSource().sendFailure(Component.literal("Codex refused: already in the islands"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " read the Codex Umbra"), true);
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

    /** Makes the player a member of the current run, as walking through the portal would. */
    private static int runJoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return 0;
        }
        int index = IslandState.get(overworld).islandIndex();
        player.setData(net.celestiald.dontstarve.init.DSAttachments.RUN_ISLAND.get(), index);
        ctx.getSource().sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " joined run " + index), true);
        return 1;
    }

    private static int runCheck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ctx.getSource().sendSuccess(() -> Component.literal("run=" + IslandState.runOf(player)
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
