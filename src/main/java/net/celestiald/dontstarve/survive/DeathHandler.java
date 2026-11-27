package net.celestiald.dontstarve.survive;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.grave.GraveBlockEntity;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.init.DSBlocks;
import net.celestiald.dontstarve.portal.ReturnPoints;
import net.celestiald.dontstarve.world.IslandState;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

        List<ItemStack> belongings = Belongings.strip(player);

        int sealedXp = player.totalExperience;
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.connection.send(new ClientboundSetExperiencePacket(0.0F, 0, 0));

        BlockPos gravePos = findGraveSpot(level, player);
        level.setBlockAndUpdate(gravePos, DSBlocks.GRAVE.get().defaultBlockState());
        if (player.getY() < level.getMinBuildHeight()) {
            // Fell off the island: the spirit rises back to where its grave lies.
            player.connection.teleport(gravePos.getX() + 0.5, gravePos.getY() + 1.5, gravePos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
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
        IslandState state = IslandState.get(overworld);

        for (ServerPlayer player : List.copyOf(islands.players())) {
            int run = IslandState.runOf(player);
            if (run >= 0 && run != state.islandIndex()) {
                // Their run was wiped while they were offline: the island is gone for them.
                ReturnPoints.sendHome(player);
                player.sendSystemMessage(Component.translatable("message.dontstarve.run_lost")
                        .withStyle(ChatFormatting.GOLD));
                continue;
            }
            GhostState.upkeep(player);
            if (!GhostState.isGhost(player) && player.onGround()) {
                player.setData(DSAttachments.LAST_GROUND.get(), player.blockPosition());
            }
        }

        // Only members of the current run decide its fate (admins teleported in do not).
        List<ServerPlayer> crew = islands.players().stream().filter(state::isCurrentRun).toList();
        boolean allGhosts = !crew.isEmpty() && crew.stream().allMatch(GhostState::isGhost);
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
            wipe(server, state, islands, overworld, crew);
        } else if (left <= 10 || left % 30 == 0) {
            broadcast(server, "message.dontstarve.wipe_countdown", left);
        }
    }

    /**
     * Abandons the current island and ejects its crew into the overworld.
     * Offline members are sent home on their next login (stale run check).
     */
    private static void wipe(MinecraftServer server, IslandState state, ServerLevel islands,
                             ServerLevel overworld, List<ServerPlayer> crew) {
        state.advance();
        for (ServerPlayer player : crew) {
            ReturnPoints.sendHome(player);
        }
        state.setWipeSecondsLeft(-1);
        broadcast(server, "message.dontstarve.wipe_done");
    }

    // -------------------------------------------------------------- utils

    /**
     * The grave sits on the ground under the spot of death. A death over the
     * void (fallen off the island) has no ground below, so the grave goes to
     * where the player last stood, or to the island plaza as a last resort —
     * belongings are never lost to the void.
     */
    private static BlockPos findGraveSpot(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        int top = Math.min(feet.getY(), level.getMaxBuildHeight() - 1);
        BlockPos onGround = groundBelow(level, feet.getX(), top, feet.getZ());
        if (onGround != null) {
            return onGround;
        }
        if (player.hasData(DSAttachments.LAST_GROUND.get())) {
            BlockPos last = player.getData(DSAttachments.LAST_GROUND.get());
            BlockPos atLast = groundBelow(level, last.getX(), last.getY() + 1, last.getZ());
            if (atLast != null) {
                return atLast;
            }
        }
        int run = Math.max(IslandState.runOf(player), 0);
        BlockPos plaza = IslandState.islandCenter(run);
        BlockPos atPlaza = groundBelow(level, plaza.getX(), IslandState.PLAZA_Y + 4, plaza.getZ());
        return atPlaza != null ? atPlaza : new BlockPos(plaza.getX(), IslandState.PLAZA_Y + 1, plaza.getZ());
    }

    /** First free spot resting on a solid block, scanning down from {@code fromY}; null over void. */
    private static BlockPos groundBelow(ServerLevel level, int x, int fromY, int z) {
        for (int y = fromY; y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (canHoldGrave(level, pos) && !canHoldGrave(level, pos.below())) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canHoldGrave(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static void broadcast(MinecraftServer server, String key, Object... args) {
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(key, args).withStyle(ChatFormatting.GOLD), false);
    }

    private DeathHandler() {
    }
}
