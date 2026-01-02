package net.j40climb.florafauna.common.item.abilities.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;

import java.util.Set;

/**
 * Client-side renderer for multi-block mining outlines.
 * Renders additional block outlines for all blocks that will be affected by the mining pattern.
 */
public class MultiBlockOutlineRenderer implements CustomBlockOutlineRenderer {

    @Override
    public boolean render(BlockOutlineRenderState renderState, MultiBufferSource.BufferSource buffer, PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        // Only render outlines if the held item has multi-block mining capability
        if (!player.getMainHandItem().has(RegisterDataComponentTypes.MULTI_BLOCK_MINING)) {
            return false;
        }

        Level level = player.level();
        BlockPos targetPos = renderState.pos();

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();

        Set<BlockPos> breakBlockPositions = MultiBlockPatterns.getBlocksToBreak(targetPos, player);

        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(targetPos)) {
                continue; // Skip the main target - vanilla handles that
            }
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderTypes.lines());
            renderBlockOutline(poseStack, vertexConsumer, player, camX, camY, camZ, level, blockPos, level.getBlockState(blockPos));
        }

        return false; // Allow vanilla to render the main target block outline
    }

    private static void renderBlockOutline(
            PoseStack poseStack,
            VertexConsumer consumer,
            Entity entity,
            double camX,
            double camY,
            double camZ,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        renderShape(
                poseStack,
                consumer,
                state.getShape(level, pos, CollisionContext.of(entity)),
                (double) pos.getX() - camX,
                (double) pos.getY() - camY,
                (double) pos.getZ() - camZ,
                0.0F,
                0.0F,
                0.0F,
                0.4F
        );
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
                    .setLineWidth(2.0f);
            consumer.addVertex(pose.pose(), (float) (x2 + x), (float) (y2 + y), (float) (z2 + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, dx, dy, dz)
                    .setLineWidth(2.0f);
        });
    }
}
