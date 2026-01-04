package net.j40climb.florafauna.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlock;
import net.j40climb.florafauna.common.entity.frontpack.FrontpackLayer;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        // Check if holding a mob barrier block item
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
            return;
        }

        Level level = player.level();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;

        BlockPos playerPos = player.blockPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());

        // Scan a 20-block radius cube around the player
        int radius = 20;
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-radius, -radius, -radius),
                playerPos.offset(radius, radius, radius))) {
            // Check distance (20 blocks = 400 distance squared)
            if (playerPos.distSqr(pos) <= 400) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof MobBarrierBlock) {
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

    private static void renderShape(
            PoseStack poseStack,
            VertexConsumer consumer,
            VoxelShape shape,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;

            consumer.addVertex(pose.pose(), (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, dx, dy, dz)
                    .setLineWidth(4.0f);
            consumer.addVertex(pose.pose(), (float) (x2 + x), (float) (y2 + y), (float) (z2 + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, dx, dy, dz)
                    .setLineWidth(4.0f);
        });
    }

    /**
     * Extracts frontpack data from player attachments during client tick.
     * This allows the FrenchFrontpackLayer to access the attachment data
     * via the FrontpackRenderStateManager cache.
     *
     * Note: In Minecraft 1.21+, the rendering system uses RenderState objects
     * which don't contain entity references, so we extract data here during tick.
     */
    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof Player player) {
            // Only run on client side
            if (player.level().isClientSide()) {
                // Extract and cache frontpack data for this player
                FrontpackLayer.FrontpackRenderStateManager.extractFromPlayer(player);
            }
        }
    }
}
