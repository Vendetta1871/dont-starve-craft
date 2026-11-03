package dev.dontstarve.survive;

import dev.dontstarve.DSRefs;
import dev.dontstarve.grave.GraveBlockEntity;
import dev.dontstarve.init.DSBlocks;
import dev.dontstarve.world.IslandState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Death rules inside the islands dimension, ported from DST (docs/dont-starve/07):
 * dying does not respawn you — your belongings sink into a permanent grave and
 * you linger as a ghost. Ghosts are invisible, invulnerable, can fly, and cannot
 * touch anything in the world.
 *
 * On top of DST we add the roguelite pressure rule (our own invention, not DS):
 * once every online player in the islands is a ghost, a countdown runs; when it
 * expires the run is wiped — the island is abandoned, a fresh one activates, and
 * the ghosts are spat back out into the overworld.
 */
public final class DeathHandler {

    /** Seconds before an all-ghost island is abandoned (our rule, not DS). */
    public static final int WIPE_COUNTDOWN_SECONDS = 120;

    // ---------------------------------------------------------------- death

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(DSRefs.ISLANDS)) {
            return;
        }
        if (GhostState.isGhost(player)) {
            return; // ghosts cannot die twice
        }
        event.setCanceled(true);

        List<ItemStack> belongings = new ArrayList<>();
        Inventory inventory = player.getInventory();
        stealAll(inventory.items, belongings);
        stealAll(inventory.armor, belongings);
        stealAll(inventory.offhand, belongings);
        player.inventoryMenu.broadcastChanges();

        int sealedXp = player.totalExperience;
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));

        BlockPos gravePos = findGraveSpot(level, player);
        level.setBlockAndUpdate(gravePos, DSBlocks.GRAVE.get().defaultBlockState());
        if (level.getBlockEntity(gravePos) instanceof GraveBlockEntity grave) {
            grave.setContents(belongings);
            grave.setGraveXp(sealedXp);
        }
        level.playSound(null, gravePos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0F, 0.7F);

        GhostState.becomeGhost(player);
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("death.dontstarve.ghost", player.getDisplayName())
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
    }

    // ------------------------------------------------------- ghost limits

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && GhostState.isGhost(player)) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !GhostState.isGhost(player)) {
            return;
        }
        // Graves and Meat Effigies are the things a ghost may interact with
        // (effigy = the DST resurrection path; graves are read-only for the dead).
        BlockState target = event.getLevel().getBlockState(event.getPos());
        if (target.is(DSBlocks.GRAVE.get()) || target.is(DSBlocks.RESURRECTION_STATUE.get())) {
            return;
        }
        event.setCanceled(true);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (GhostState.isGhost(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (GhostState.isGhost(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (GhostState.isGhost(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onPickupItem(ItemEntityPickupEvent.Pre event) {
        if (GhostState.isGhost(event.getPlayer())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /** Mobs never chase spirits: any attempt to target a ghost is dropped. */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && GhostState.isGhost(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /**
     * Ghosts normally cannot die, so reaching the respawn screen means an admin
     * intervention (/kill); the spirit is released along with its perks.
     */
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GhostState.isGhost(player)) {
            GhostState.clear(player);
        }
    }

    // ------------------------------------------- upkeep + all-ghost wipe

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel islands = server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null) {
            return;
        }

        List<ServerPlayer> inIslands = List.copyOf(islands.players());
        for (ServerPlayer player : inIslands) {
            GhostState.upkeep(player);
        }

        IslandState state = IslandState.get(overworld);
        boolean allGhosts = !inIslands.isEmpty() && inIslands.stream().allMatch(GhostState::isGhost);
        if (!allGhosts) {
            if (state.wipeSecondsLeft() >= 0) {
                state.setWipeSecondsLeft(-1);
                broadcast(server, "message.dontstarve.wipe_cancelled");
            }
            return;
        }

        int left = state.wipeSecondsLeft();
        if (left < 0) {
            state.setWipeSecondsLeft(WIPE_COUNTDOWN_SECONDS);
            broadcast(server, "message.dontstarve.wipe_start", WIPE_COUNTDOWN_SECONDS);
            return;
        }
        left--;
        state.setWipeSecondsLeft(left);
        if (left <= 0) {
            wipe(server, state, islands, overworld);
        } else if (left <= 10 || left % 30 == 0) {
            broadcast(server, "message.dontstarve.wipe_countdown", left);
        }
    }

    /** Abandons the current island and ejects every ghost into the overworld. */
    private static void wipe(MinecraftServer server, IslandState state,
                             ServerLevel islands, ServerLevel overworld) {
        int index = state.advance();
        IslandState.pregenArrival(islands, index);

        BlockPos spawn = overworld.getLevelData().getSpawnPos();
        Vec3 exit = Vec3.atBottomCenterOf(
                overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn));
        for (ServerPlayer player : List.copyOf(islands.players())) {
            GhostState.clear(player);
            player.changeDimension(new DimensionTransition(overworld, exit, Vec3.ZERO,
                    player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
        }
        state.setWipeSecondsLeft(-1);
        broadcast(server, "message.dontstarve.wipe_done");
    }

    // -------------------------------------------------------------- utils

    private static BlockPos findGraveSpot(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (canHoldGrave(level, feet)) {
            return feet;
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos above = feet.above(dy);
            if (canHoldGrave(level, above)) {
                return above;
            }
        }
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos below = feet.below(dy);
            if (!canHoldGrave(level, below) && canHoldGrave(level, below.above())) {
                return below.above();
            }
        }
        return feet;
    }

    private static boolean canHoldGrave(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static void stealAll(List<ItemStack> from, List<ItemStack> into) {
        for (int i = 0; i < from.size(); i++) {
            ItemStack stack = from.get(i);
            if (!stack.isEmpty()) {
                into.add(stack.copy());
                from.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void broadcast(MinecraftServer server, String key, Object... args) {
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(key, args).withStyle(ChatFormatting.GOLD), false);
    }

    private DeathHandler() {
    }
}
