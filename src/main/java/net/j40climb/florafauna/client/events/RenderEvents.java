package net.j40climb.florafauna.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.CustomBlockOutlineRenderer;
import net.j40climb.florafauna.common.block.custom.CopperGolemBarrierBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    public static void extractBlockOutlineRenderStateEvent(ExtractBlockOutlineRenderStateEvent event) {
        event.addCustomRenderer(new CustomBlockOutlineRenderer());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        // Check if holding a copper block
        ItemStack heldItem = player.getMainHandItem();
        if (!isCopperBlock(heldItem)) {
            return;
        }

        Level level = player.level();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;

        BlockPos playerPos = player.blockPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        // Scan a 20-block radius cube around the player
        int radius = 20;
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-radius, -radius, -radius),
                playerPos.offset(radius, radius, radius))) {
            // Check distance (20 blocks = 400 distance squared)
            if (playerPos.distSqr(pos) <= 400) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CopperGolemBarrierBlock) {
                    // Render orange outline for this barrier block
                    poseStack.pushPose();
                    renderShape(
                            poseStack,
                            vertexConsumer,
                            Shapes.block(),
                            (double) pos.getX() - cameraPos.x(),
                            (double) pos.getY() - cameraPos.y(),
                            (double) pos.getZ() - cameraPos.z(),
                            1.0F,  // Red
                            0.65F, // Green (for orange)
                            0.0F,  // Blue
                            0.4F   // Alpha
                    );
                    poseStack.popPose();
                }
            }
        }
    }

    /**
     * Checks if the given ItemStack is any copper block (waxed or unwaxed)
     */
    private static boolean isCopperBlock(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COPPER_BLOCK
                || item == Items.EXPOSED_COPPER
                || item == Items.WEATHERED_COPPER
                || item == Items.OXIDIZED_COPPER
                || item == Items.WAXED_COPPER_BLOCK
                || item == Items.WAXED_EXPOSED_COPPER
                || item == Items.WAXED_WEATHERED_COPPER
                || item == Items.WAXED_OXIDIZED_COPPER
                || item == Items.CUT_COPPER
                || item == Items.EXPOSED_CUT_COPPER
                || item == Items.WEATHERED_CUT_COPPER
                || item == Items.OXIDIZED_CUT_COPPER
                || item == Items.WAXED_CUT_COPPER
                || item == Items.WAXED_EXPOSED_CUT_COPPER
                || item == Items.WAXED_WEATHERED_CUT_COPPER
                || item == Items.WAXED_OXIDIZED_CUT_COPPER
                || item == Items.CHISELED_COPPER
                || item == Items.EXPOSED_CHISELED_COPPER
                || item == Items.WEATHERED_CHISELED_COPPER
                || item == Items.OXIDIZED_CHISELED_COPPER
                || item == Items.WAXED_CHISELED_COPPER
                || item == Items.WAXED_EXPOSED_CHISELED_COPPER
                || item == Items.WAXED_WEATHERED_CHISELED_COPPER
                || item == Items.WAXED_OXIDIZED_CHISELED_COPPER;
    }

    private static void renderShape(
            PoseStack pPoseStack,
            VertexConsumer pConsumer,
            VoxelShape pShape,
            double pX,
            double pY,
            double pZ,
            float pRed,
            float pGreen,
            float pBlue,
            float pAlpha
    ) {
        PoseStack.Pose pose = pPoseStack.last();
        pShape.forAllEdges(
                (p_234280_, p_234281_, p_234282_, p_234283_, p_234284_, p_234285_) -> {
                    float f = (float) (p_234283_ - p_234280_);
                    float f1 = (float) (p_234284_ - p_234281_);
                    float f2 = (float) (p_234285_ - p_234282_);
                    float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
                    f /= f3;
                    f1 /= f3;
                    f2 /= f3;
                    pConsumer.addVertex(pose.pose(), (float) (p_234280_ + pX), (float) (p_234281_ + pY), (float) (p_234282_ + pZ))
                            .setColor(pRed, pGreen, pBlue, pAlpha)
                            .setNormal(pose, f, f1, f2);
                    pConsumer.addVertex(pose.pose(), (float) (p_234283_ + pX), (float) (p_234284_ + pY), (float) (p_234285_ + pZ))
                            .setColor(pRed, pGreen, pBlue, pAlpha)
                            .setNormal(pose, f, f1, f2);
                }
        );
    }
}
