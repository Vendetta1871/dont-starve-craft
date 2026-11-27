package net.celestiald.dontstarve.portal;

import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.init.DSAttachments;
import net.celestiald.dontstarve.init.DSItems;
import net.celestiald.dontstarve.world.IslandState;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Codex Umbra (DST "waxwelljournal", Maxwell's book) — our way into the
 * Don't Starve world. Reading it outside the islands burns the book, drops
 * everything the player carries on the ground where they stand ("you arrive
 * with nothing") and sends them to the current island, joining its run.
 * Every player gets one copy on first join; more are crafted from a book and
 * a wither rose.
 */
public class CodexUmbraItem extends Item {

    public CodexUmbraItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (level.dimension().equals(DSRefs.ISLANDS)) {
            return InteractionResultHolder.fail(book);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(book);
        }
        if (player instanceof ServerPlayer serverPlayer && travel(serverPlayer, book)) {
            return InteractionResultHolder.consume(book);
        }
        return InteractionResultHolder.fail(book);
    }

    /**
     * Burns one book, drops the player's belongings where they stand and
     * sends them to the current island. Also the test hook of /ds codex.
     */
    public static boolean travel(ServerPlayer player, ItemStack book) {
        ServerLevel from = player.serverLevel();
        ServerLevel islands = player.server.getLevel(DSRefs.ISLANDS);
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (islands == null || overworld == null || from.dimension().equals(DSRefs.ISLANDS)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            book.shrink(1);
        }
        player.getInventory().dropAll();
        player.setData(DSAttachments.RETURN_POINT.get(), GlobalPos.of(from.dimension(), player.blockPosition()));

        int index = IslandState.get(overworld).islandIndex();
        player.setData(DSAttachments.RUN_ISLAND.get(), index);
        net.celestiald.dontstarve.survive.Survival.freshStart(player);
        from.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.changeDimension(new DimensionTransition(islands, IslandState.arrivalPos(index), Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND));
        return true;
    }

    /** Hands every player their first Codex Umbra when they join for the first time. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !player.getData(DSAttachments.CODEX_GIVEN.get())) {
            player.setData(DSAttachments.CODEX_GIVEN.get(), true);
            player.getInventory().placeItemBackInInventory(new ItemStack(DSItems.WAXWELLJOURNAL.get()));
        }
    }
}
