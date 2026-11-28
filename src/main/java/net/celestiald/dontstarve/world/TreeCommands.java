package net.celestiald.dontstarve.world;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.Map;
import java.util.TreeMap;
import net.celestiald.dontstarve.fire.DSBurnable;
import net.celestiald.dontstarve.work.DSWork;
import net.celestiald.dontstarve.work.DSWorkable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test-only {@code /ds tree ...} commands for the evergreen lifecycle (tests/scenarios/trees.txt).
 * Replies carry no commas (the test runner reads "x, y, z" triples as island coordinates).
 * <ul>
 *   <li>{@code place <pos> short|normal|tall|old [lumpy]}: a whole tree with its base at pos.</li>
 *   <li>{@code stump <pos> [lumpy]}, {@code sapling <pos> [lumpy]}: those blocks (setblock would
 *       overwrite the scenario's coordinates).</li>
 *   <li>{@code grow <pos>}: one growable step now (what the stage timer does).</li>
 *   <li>{@code burn <pos>}: the block's burnt result (what the fire system does when the burn time is up).</li>
 *   <li>{@code info <pos>}: block, stage and chops left of the tree at pos.</li>
 *   <li>{@code sweep <pos>}: counts and deletes the item entities within 6 blocks of pos.</li>
 *   <li>{@code plant <player> <pos>}: the player uses the held item on top of the block at pos.</li>
 * </ul>
 */
public final class TreeCommands {

    public static void register(RegisterCommandsEvent event) {
        var placePos = pos(null);
        for (int i = 0; i < 4; i++) {
            placePos.then(stage(i));
        }
        var place = Commands.literal("place").then(placePos);
        var stump = Commands.literal("stump").then(pos(null)
                .executes(ctx -> stump(ctx, false))
                .then(Commands.literal("lumpy").executes(ctx -> stump(ctx, true))));
        var sapling = Commands.literal("sapling").then(pos(null)
                .executes(ctx -> sapling(ctx, false))
                .then(Commands.literal("lumpy").executes(ctx -> sapling(ctx, true))));
        event.getDispatcher().register(Commands.literal("ds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("tree")
                        .then(place)
                        .then(stump)
                        .then(sapling)
                        .then(Commands.literal("grow").then(pos(null).executes(TreeCommands::grow)))
                        .then(Commands.literal("burn").then(pos(null).executes(TreeCommands::burn)))
                        .then(Commands.literal("info").then(pos(null).executes(TreeCommands::info)))
                        .then(Commands.literal("sweep").then(pos(null).executes(TreeCommands::sweep)))
                        .then(Commands.literal("plant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(pos(null).executes(TreeCommands::plant))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> pos(
            ArgumentBuilder<CommandSourceStack, ?> child) {
        var arg = Commands.argument("pos", BlockPosArgument.blockPos());
        if (child != null) {
            arg.then(child);
        }
        return arg;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> stage(int i) {
        return Commands.literal(EvergreenTree.STAGE_NAMES[i])
                .executes(ctx -> place(ctx, i, false))
                .then(Commands.literal("lumpy").executes(ctx -> place(ctx, i, true)));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static String id(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    private static int place(CommandContext<CommandSourceStack> ctx, int stage, boolean sparse)
            throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        boolean placed = EvergreenTree.place(level, pos, stage, sparse);
        return reply(ctx, (placed ? "placed " : "blocked ") + id(EvergreenTree.trunk(sparse).defaultBlockState())
                + " " + EvergreenTree.STAGE_NAMES[stage]);
    }

    private static int stump(CommandContext<CommandSourceStack> ctx, boolean sparse) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockState state = EvergreenTree.stump(sparse).defaultBlockState();
        ctx.getSource().getLevel().setBlock(pos, state, Block.UPDATE_ALL);
        return reply(ctx, "placed " + id(state));
    }

    private static int sapling(CommandContext<CommandSourceStack> ctx, boolean sparse) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockState state = (sparse ? net.celestiald.dontstarve.init.DSBlocks.LUMPY_SAPLING
                : net.celestiald.dontstarve.init.DSBlocks.PINECONE_SAPLING).get().defaultBlockState();
        ctx.getSource().getLevel().setBlock(pos, state, Block.UPDATE_ALL);
        return reply(ctx, "placed " + id(state));
    }

    private static int grow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos base = EvergreenTree.baseOf(level, BlockPosArgument.getBlockPos(ctx, "pos"));
        BlockState state = level.getBlockState(base);
        if (!(state.getBlock() instanceof EvergreenBlock)) {
            return reply(ctx, "no living tree");
        }
        EvergreenTree.grow(level, base, state);
        return reply(ctx, "grown " + EvergreenTree.STAGE_NAMES[level.getBlockState(base).getValue(EvergreenBlock.STAGE)]);
    }

    private static int burn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DSBurnable burnable) || burnable.burnTime(state) <= 0) {
            return reply(ctx, "cannot burn " + id(state));
        }
        burnable.onBurnt(level, pos, state);
        return reply(ctx, "burnt " + id(state));
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EvergreenTree.Trunk)) {
            return reply(ctx, "no tree " + id(state));
        }
        BlockPos base = EvergreenTree.baseOf(level, pos);
        BlockState baseState = level.getBlockState(base);
        int stage = baseState.getValue(EvergreenBlock.STAGE);
        int left = DSWork.workLeft(level, base, ((DSWorkable) baseState.getBlock()).maxWork(baseState, level, base));
        return reply(ctx, "tree " + id(baseState) + " stage " + EvergreenTree.STAGE_NAMES[stage] + " left " + left
                + " tick " + level.getBlockTicks().hasScheduledTick(base, baseState.getBlock()));
    }

    private static int sweep(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        Map<String, Integer> totals = new TreeMap<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6))) {
            ItemStack stack = item.getItem();
            totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            item.discard();
        }
        StringBuilder text = new StringBuilder("items");
        totals.forEach((k, v) -> text.append(' ').append(k).append('=').append(v));
        if (totals.isEmpty()) {
            text.append(" none");
        }
        return reply(ctx, text.toString());
    }

    private static int plant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        InteractionResult result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        return reply(ctx, result.consumesAction() ? "planted " + id(level.getBlockState(pos.above())) : "not planted");
    }

    private TreeCommands() {
    }
}
