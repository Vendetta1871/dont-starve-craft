package net.celestiald.dontstarve.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.celestiald.dontstarve.DSRefs;
import net.celestiald.dontstarve.craft.Builder;
import net.celestiald.dontstarve.craft.DSRecipe;
import net.celestiald.dontstarve.craft.DSRecipes;
import net.celestiald.dontstarve.network.PlacePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Structure placement after a buffered build (DST placer): a green or red box shows
 * where the structure would go; left click places it, right click cancels (the build
 * stays paid and can be placed later from the menu).
 */
public final class Placement {

    /** Ticks to wait for the server to confirm the buffered build. */
    private static final int CONFIRM_TICKS = 40;

    private static String recipe;
    private static int waitTicks;

    public static void start(String name) {
        recipe = name;
        waitTicks = CONFIRM_TICKS;
    }

    public static boolean active() {
        return recipe != null && ClientBuilder.data().isBuffered(recipe);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (recipe == null) {
            return;
        }
        if (mc.player == null || mc.level == null || !mc.level.dimension().equals(DSRefs.ISLANDS)) {
            recipe = null;
        } else if (!ClientBuilder.data().isBuffered(recipe) && --waitTicks <= 0) {
            recipe = null; // placed, or the server refused the build
        }
    }

    private static BlockPos target() {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (mc.level == null || !(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = block.getBlockPos();
        return mc.level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(block.getDirection());
    }

    private static boolean valid(BlockPos pos) {
        DSRecipe r = DSRecipes.get(recipe);
        Minecraft mc = Minecraft.getInstance();
        return r != null && pos != null && mc.player.canInteractWithBlock(pos, 1.0) && Builder.canDeploy(mc.level, pos, r);
    }

    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack()) {
            BlockPos pos = target();
            if (valid(pos)) {
                PacketDistributor.sendToServer(new PlacePayload(recipe, pos));
                waitTicks = CONFIRM_TICKS;
            }
        } else if (event.isUseItem()) {
            recipe = null;
        }
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !active()) {
            return;
        }
        BlockPos pos = target();
        if (pos == null) {
            return;
        }
        boolean ok = valid(pos);
        float r = ok ? 0.3F : 0.9F;
        float g = ok ? 0.9F : 0.2F;
        float b = 0.2F;
        Vec3 cam = event.getCamera().getPosition();
        AABB box = new AABB(pos).move(-cam.x, -cam.y, -cam.z);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        DebugRenderer.renderFilledBox(pose, buffers, box, r, g, b, 0.3F);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, r, g, b, 1.0F);
        buffers.endBatch();
    }

    /** The structure name and the controls under the crosshair. */
    public static void renderHud(GuiGraphics graphics, DeltaTracker delta) {
        if (!active()) {
            return;
        }
        DSRecipe r = DSRecipes.get(recipe);
        Minecraft mc = Minecraft.getInstance();
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        graphics.drawCenteredString(mc.font, r.product().getDescription(), x, y, 0xFFFFFF);
        graphics.drawCenteredString(mc.font, Component.translatable("dontstarve.crafting.place_controls"), x, y + 11, 0xCCCCCC);
    }

    private Placement() {
    }
}
