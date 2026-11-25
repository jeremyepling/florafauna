package net.j40climb.florafauna.common.block.mobbarrier;

import com.mojang.blaze3d.vertex.PoseStack;
//? if <26.3 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
//?}
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.ArrayList;
import java.util.List;

/** Extract loaded barriers on the game thread, then draw outlines only while their item is held. */
public final class FabricMobBarrierOutlines {
    private static final int RADIUS = 20;
    private static final RenderStateDataKey<List<BlockPos>> BARRIERS =
            RenderStateDataKey.create(() -> "florafauna mob barrier outlines");

    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            var player = Minecraft.getInstance().player;
            List<BlockPos> barriers = new ArrayList<>();
            if (player != null && player.getMainHandItem().is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
                BlockPos center = player.blockPosition();
                for (int cx = (center.getX() - RADIUS) >> 4; cx <= (center.getX() + RADIUS) >> 4; cx++) {
                    for (int cz = (center.getZ() - RADIUS) >> 4; cz <= (center.getZ() + RADIUS) >> 4; cz++) {
                        var chunk = context.level().getChunkSource().getChunkNow(cx, cz);
                        if (chunk == null) continue;
                        for (var entity : chunk.getBlockEntities().values()) {
                            BlockPos pos = entity.getBlockPos();
                            if (entity instanceof MobBarrierBlockEntity && center.distSqr(pos) <= RADIUS * RADIUS) {
                                barriers.add(pos.immutable());
                            }
                        }
                    }
                }
            }
            context.levelState().setData(BARRIERS, List.copyOf(barriers));
        });
        //? if <26.3 {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            var barriers = context.levelState().getData(BARRIERS);
            if (barriers == null || barriers.isEmpty()) return;
            var camera = context.levelState().cameraRenderState.pos;
            var consumer = context.bufferSource().getBuffer(RenderTypes.lines());
            for (BlockPos pos : barriers) {
                renderShape(context.poseStack(), consumer, Shapes.block(), pos.getX() - camera.x,
                        pos.getY() - camera.y, pos.getZ() - camera.z, 1F, 0.65F, 0F, 0.4F);
            }
        });
        //?} else {
        /*LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            var barriers = context.levelState().getData(BARRIERS);
            if (barriers == null || barriers.isEmpty()) return;
            var camera = context.levelState().cameraRenderState.pos;
            PoseStack pose = context.poseStack();
            for (BlockPos pos : barriers) {
                pose.pushPose();
                pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                context.submitNodeCollector().submitShapeOutline(pose, Shapes.block(),
                        RenderTypes.linesTranslucentNoDepthWrite(), 0x66FFA600, 4.0F, true);
                pose.popPose();
            }
        });
        *///?}
    }

    //? if <26.3 {
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
    //?}
}
