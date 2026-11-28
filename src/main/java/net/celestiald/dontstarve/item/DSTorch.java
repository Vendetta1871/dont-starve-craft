package net.celestiald.dontstarve.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.DSScale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * DS torch rules for the vanilla torch inside the islands (DST prefabs/torch.lua):
 * it is a hand-held light that burns while held — TORCH_FUEL 75 s, scaled to
 * Minecraft's day length — and is gone when burnt out; it cannot be placed;
 * hitting sets the target on fire (TORCH_ATTACK_IGNITE_PERCENT = 1). Its
 * durability bar is the remaining fuel. Outside the islands it stays vanilla.
 */
public final class DSTorch {

    /** TORCH_FUEL 75 DS seconds of burn time, in Minecraft seconds (durability units). */
    public static final int BURN_SECONDS = DSScale.seconds(75);
    /** DS ignites the target for the burn of a small fire; vanilla fire aspect I equivalent. */
    private static final int IGNITE_SECONDS = 4;
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13);

    /** The light block following each torch bearer. */
    private static final Map<UUID, BlockPos> LIGHTS = new HashMap<>();

    public static boolean isTorch(ItemStack stack) {
        return stack.is(Items.TORCH);
    }

    private static boolean inIslands(Player player) {
        return player.level().dimension().equals(DSRefs.ISLANDS);
    }

    /** A torch fresh from the crafting menu: full fuel, DST TORCH_DAMAGE 17 (2.27 attack, 1 of it the fist). */
    public static ItemStack crafted() {
        ItemStack stack = new ItemStack(Items.TORCH);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.MAX_DAMAGE, BURN_SECONDS);
        stack.set(DataComponents.DAMAGE, 0);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                net.celestiald.dontstarve.DontStarveCraft.rl("torch_damage"), DSScale.stat(17) - 1.0,
                                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build());
        return stack;
    }

    /** Gives a plain vanilla torch the DS fuel gauge (one torch per stack, like DS). */
    public static void makeBurnable(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(DataComponents.MAX_DAMAGE)) {
            return;
        }
        if (stack.getCount() > 1) {
            ItemStack rest = stack.split(stack.getCount() - 1);
            player.getInventory().placeItemBackInInventory(rest);
        }
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.MAX_DAMAGE, BURN_SECONDS);
        stack.set(DataComponents.DAMAGE, 0);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        boolean burnSecond = server.getTickCount() % 20 == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            InteractionHand hand = torchHand(player);
            if (hand == null || !inIslands(player) || !player.isAlive()) {
                clearLight(player);
                continue;
            }
            if (burnSecond) {
                burn(player, hand);
                if (torchHand(player) == null) {
                    clearLight(player);
                    continue;
                }
            }
            moveLight(player);
        }
    }

    private static InteractionHand torchHand(Player player) {
        if (isTorch(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        return isTorch(player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
    }

    private static void burn(ServerPlayer player, InteractionHand hand) {
        makeBurnable(player, hand);
        ItemStack torch = player.getItemInHand(hand);
        int burnt = torch.getDamageValue() + 1;
        if (burnt >= torch.getMaxDamage()) {
            torch.shrink(1);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        } else {
            torch.setDamageValue(burnt);
        }
    }

    private static void moveLight(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos want = BlockPos.containing(player.getEyePosition());
        BlockPos have = LIGHTS.get(player.getUUID());
        if (want.equals(have) && level.getBlockState(want).is(Blocks.LIGHT)) {
            return;
        }
        clearLight(player);
        if (level.getBlockState(want).isAir()) {
            level.setBlock(want, LIGHT, net.minecraft.world.level.block.Block.UPDATE_ALL);
            LIGHTS.put(player.getUUID(), want);
        }
    }

    private static void clearLight(ServerPlayer player) {
        BlockPos old = LIGHTS.remove(player.getUUID());
        if (old != null) {
            ServerLevel level = player.server.getLevel(DSRefs.ISLANDS);
            if (level != null && level.getBlockState(old).is(Blocks.LIGHT)) {
                level.removeBlock(old, false);
            }
        }
    }

    /** DS torches cannot be placed: on the islands the torch only lights the hand. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (inIslands(event.getEntity()) && isTorch(event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (inIslands(player) && isTorch(player.getMainHandItem()) && !player.level().isClientSide) {
            event.getTarget().igniteForSeconds(IGNITE_SECONDS);
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLight(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearLight(player);
        }
    }

    private DSTorch() {
    }
}
