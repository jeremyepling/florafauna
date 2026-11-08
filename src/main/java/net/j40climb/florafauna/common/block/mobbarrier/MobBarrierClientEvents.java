package net.j40climb.florafauna.common.block.mobbarrier;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

/** Outlines nearby invisible barriers while the player holds a mob barrier item. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class MobBarrierClientEvents {
    private static final int RADIUS = 20;
    private static final int OUTLINE_COLOR = 0x66FFA600;
    private static final ContextKey<List<BlockPos>> BARRIERS = new ContextKey<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "mob_barrier_outlines"));

    @SubscribeEvent
    public static void extractBarriers(ExtractLevelRenderStateEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().is(FloraFaunaRegistry.MOB_BARRIER.asItem())) {
            return;
        }

        BlockPos center = player.blockPosition();
        List<BlockPos> barriers = new ArrayList<>();
        ChunkPos first = ChunkPos.containing(center.offset(-RADIUS, 0, -RADIUS));
        ChunkPos last = ChunkPos.containing(center.offset(RADIUS, 0, RADIUS));
        ChunkPos.rangeClosed(first, last).forEach(pos -> {
            LevelChunk chunk = event.getLevel().getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk != null) {
                chunk.getBlockEntities().forEach((blockPos, entity) -> {
                    if (entity instanceof MobBarrierBlockEntity && center.distSqr(blockPos) <= RADIUS * RADIUS) {
                        barriers.add(blockPos);
                    }
                });
            }
        });
        event.getRenderState().setRenderData(BARRIERS, List.copyOf(barriers));
    }

    @SubscribeEvent
    public static void submitBarriers(SubmitCustomGeometryEvent event) {
        List<BlockPos> barriers = event.getLevelRenderState().getRenderData(BARRIERS);
        if (barriers == null) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (BlockPos pos : barriers) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            event.getSubmitNodeCollector().submitShapeOutline(poseStack, Shapes.block(),
                    RenderTypes.linesTranslucentNoDepthWrite(), OUTLINE_COLOR, 4.0F, true);
            poseStack.popPose();
        }
    }
}
