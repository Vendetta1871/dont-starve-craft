package net.celestiald.dontstarve.work;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.celestiald.dontstarve.craft.DSRecipe;
import net.celestiald.dontstarve.craft.DSRecipes;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.item.DSToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * DST work: a target takes a number of tool hits (workable.lua WorkedBy) and
 * each hit uses the tool (finiteuses consumption). In Minecraft one hit is one
 * filled mining bar: the bar only fills with the right DS tool, completing it
 * counts a hit instead of breaking the block, and holding the button keeps
 * swinging at the DS rate.
 */
public final class DSWork {

    /** Mining bar progress per tick: one hit per {@link DSAction#hitTicks}, nothing without the tool. */
    public static float destroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!(state.getBlock() instanceof DSWorkable workable)) {
            return 0.0F;
        }
        DSAction action = workable.workAction(state);
        if (action == null || !DSToolItem.canDo(player.getMainHandItem(), action)) {
            return 0.0F;
        }
        return 1.0F / action.hitTicks;
    }

    /**
     * A completed mining bar: one hit. The block is never removed by Minecraft
     * (returns false); the workable removes itself when its work is done.
     * Creative players break it outright, as with any block.
     */
    public static boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isCreative()) {
            return true;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer worker) {
            workedBy(server, pos, state, worker, 1);
        }
        return false;
    }

    /** Forgets the work left when the block at {@code pos} is replaced by another block (onRemove). */
    public static void forget(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (!level.isClientSide && !newState.is(state.getBlock())) {
            setWorkLeft(level, pos, -1);
        }
    }

    /** Result of one {@link #workedBy} call, for commands and tests. */
    public enum Result { NOT_WORKABLE, WRONG_TOOL, WORKED, FINISHED }

    /**
     * workable:WorkedBy. A player must hold a tool that performs the block's
     * action; the tool is used up by its consumption per hit.
     */
    public static Result workedBy(ServerLevel level, BlockPos pos, BlockState state, @Nullable ServerPlayer worker,
                                  int numWorks) {
        if (!(state.getBlock() instanceof DSWorkable workable)) {
            return Result.NOT_WORKABLE;
        }
        DSAction action = workable.workAction(state);
        if (action == null) {
            return Result.NOT_WORKABLE;
        }
        if (worker != null) {
            ItemStack tool = worker.getMainHandItem();
            if (!DSToolItem.canDo(tool, action)) {
                return Result.WRONG_TOOL;
            }
            DSToolItem.use(tool, worker, action);
        }
        BlockPos key = workable.workPos(state, level, pos);
        int left = workLeft(level, key, workable.maxWork(state, level, key));
        // workable.lua: less than one full work remaining just finishes it.
        left = left <= 1 ? 0 : left - numWorks;
        if (left <= 0) {
            setWorkLeft(level, key, -1);
            workable.onWorkFinished(level, pos, state, worker);
            return Result.FINISHED;
        }
        setWorkLeft(level, key, left);
        workable.onWork(level, pos, state, worker, left);
        return Result.WORKED;
    }

    /** Work left at {@code pos}, or {@code fresh} when the target has not been worked yet. */
    public static int workLeft(Level level, BlockPos pos, int fresh) {
        Integer left = level.getChunkAt(pos).getData(DSAttachments.WORK_LEFT).get(key(pos));
        return left != null ? left : fresh;
    }

    /** Stores the work left; a negative value forgets the position (fresh target). */
    public static void setWorkLeft(Level level, BlockPos pos, int left) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<String, Integer> map = new HashMap<>(chunk.getData(DSAttachments.WORK_LEFT));
        if (left < 0) {
            if (map.remove(key(pos)) == null) {
                return;
            }
        } else {
            map.put(key(pos), left);
        }
        chunk.setData(DSAttachments.WORK_LEFT, Map.copyOf(map));
    }

    private static String key(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    /**
     * lootdropper:GetRecipeLoot for a hammered structure: each ingredient back
     * at ceil(amount * HAMMER_LOOT_PERCENT 0.5). Character ingredients are not
     * returned. The recipe is the one named after the block.
     */
    public static void dropRecipeLoot(ServerLevel level, BlockPos pos, Block block) {
        dropRecipeLoot(level, pos, block, false);
    }

    /**
     * lootdropper:GetRecipeLoot / GenerateLoot. A burnt structure returns only
     * BURNT_HAMMER_LOOT_PERCENT of the recipe and, 40 % of the time, a charcoal (when the
     * {@code dontstarve:charcoal} item exists).
     */
    public static void dropRecipeLoot(ServerLevel level, BlockPos pos, Block block, boolean burnt) {
        DSRecipe recipe = DSRecipes.get(BuiltInRegistries.BLOCK.getKey(block).getPath());
        if (recipe != null) {
            double percent = burnt ? BURNT_HAMMER_LOOT_PERCENT : HAMMER_LOOT_PERCENT;
            for (DSRecipe.Ing ing : recipe.ingredients()) {
                int amount = (int) Math.ceil(ing.amount() * percent);
                // One stack per ingredient: separate items of one kind merge on the ground anyway.
                if (amount > 0) {
                    Block.popResource(level, pos, new ItemStack(ing.icon(), amount));
                }
            }
        }
        if (burnt && level.random.nextDouble() < 0.4) {
            var charcoal = BuiltInRegistries.ITEM.getOptional(net.celestiald.dontstarve.DontStarveCraft.rl("charcoal"));
            charcoal.ifPresent(item -> Block.popResource(level, pos, new ItemStack(item)));
        }
    }

    /** TUNING.BURNT_HAMMER_LOOT_PERCENT. */
    public static final double BURNT_HAMMER_LOOT_PERCENT = 0.25;

    /** TUNING.HAMMER_LOOT_PERCENT. */
    public static final double HAMMER_LOOT_PERCENT = 0.5;

    private DSWork() {
    }
}
