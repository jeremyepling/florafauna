package net.j40climb.florafauna.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.BlockBreakUtils;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import java.util.Set;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class RenderEvents {

    @SubscribeEvent
    static void renderBlockHighlight(RenderHighlightEvent.Block evt) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.player == null) {
            return;
        }

        if (player.getMainHandItem().get(ModDataComponentTypes.MINING_MODE_DATA) != null) {
            Level level = player.level();
            BlockPos targetPos = evt.getTarget().getBlockPos();

            Set<BlockPos> breakBlockPositions = BlockBreakUtils.getBlocksToBeBrokenWithMiningMode(targetPos, player);
            Vec3 vec3 = evt.getCamera().getPosition();
            double d0 = vec3.x();
            double d1 = vec3.y();
            double d2 = vec3.z();
            for (BlockPos blockPos : breakBlockPositions) {
                if (blockPos.equals(targetPos)) {
                    continue; //Let the original event draw this one!
                }
                VertexConsumer vertexConsumer = evt.getMultiBufferSource().getBuffer(RenderType.lines());
                renderHitOutline(evt.getPoseStack(), vertexConsumer, player, d0, d1, d2, level, blockPos, level.getBlockState(blockPos));
                }
        }
    }

    private static void renderHitOutline(
            PoseStack pPoseStack,
            VertexConsumer pConsumer,
            Entity pEntity,
            double pCamX,
            double pCamY,
            double pCamZ,
            Level level,
            BlockPos pPos,
            BlockState pState
    ) {
        renderShape(
                pPoseStack,
                pConsumer,
                pState.getShape(level, pPos, CollisionContext.of(pEntity)),
                (double) pPos.getX() - pCamX,
                (double) pPos.getY() - pCamY,
                (double) pPos.getZ() - pCamZ,
                0.0F,
                0.0F,
                0.0F,
                0.4F
        );
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
