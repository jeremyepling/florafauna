package net.j40climb.florafauna.common.item.abilities.multiblock;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Draws the additional outlines for a multi-block mining tool. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class MultiBlockOutlineRenderer {
    // Show planned placements and newly exposed lava even inside the unbroken wall.
    private static final RenderPipeline PREVIEW_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "pipeline/hammer_preview"))
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    private static final RenderType PREVIEW_LINES = RenderType.create("florafauna_hammer_preview",
            RenderSetup.builder(PREVIEW_PIPELINE).createRenderSetup());
    private static final int PLACEMENT_COLOR = 0xD92DE7B1;
    private static final int LAVA_COLOR = 0xFFFF780D;
    private static final Map<Direction, VoxelShape> FACE_OUTLINES = Shapes.rotateAll(Shapes.box(0, 0, 0, 1, 1, 0.005));

    @SubscribeEvent
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PREVIEW_PIPELINE);
    }

    @SubscribeEvent
    public static void extractBlockOutlines(ExtractBlockOutlineRenderStateEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
            return;
        }

        BlockPos target = event.getBlockPos();
        var plan = MultiBlockPatterns.plan(player, player.getMainHandItem(), target, event.getHitResult().getDirection());
        List<VoxelShape> outlines = plan.blocks().stream()
                .filter(pos -> !pos.equals(target))
                .map(pos -> event.getLevel().getBlockState(pos)
                        .getShape(event.getLevel(), pos, event.getCollisionContext())
                        .move(pos.getX(), pos.getY(), pos.getZ()))
                .filter(shape -> !shape.isEmpty())
                .toList();
        List<VoxelShape> placements = plan.stairs().stream()
                .map(pos -> plan.stairState().getShape(event.getLevel(), pos)
                        .move(pos.getX(), pos.getY(), pos.getZ()))
                .toList();
        List<LavaExposure> lava = LavaExposure.find(event.getLevel(), plan);
        if (outlines.isEmpty() && placements.isEmpty() && lava.isEmpty()) {
            return;
        }

        event.addCustomRenderer((state, buffers, poseStack, translucentPass, levelState) -> {
            if (translucentPass == state.isTranslucent()) {
                Vec3 camera = levelState.cameraRenderState.pos;
                var vertices = buffers.getBuffer(RenderTypes.lines());
                for (VoxelShape outline : outlines) {
                    ShapeRenderer.renderShape(poseStack, vertices, outline, -camera.x, -camera.y, -camera.z, 0x66000000, 2.0F);
                }
                if (!placements.isEmpty() || !lava.isEmpty()) {
                    var previewVertices = buffers.getBuffer(PREVIEW_LINES);
                    for (VoxelShape stair : placements) {
                        ShapeRenderer.renderShape(poseStack, previewVertices, stair, -camera.x, -camera.y, -camera.z, PLACEMENT_COLOR, 2.5F);
                    }
                    for (LavaExposure exposure : lava) {
                        BlockPos pos = exposure.pos();
                        ShapeRenderer.renderShape(poseStack, previewVertices, FACE_OUTLINES.get(exposure.face()),
                                pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z, LAVA_COLOR, 3.0F);
                    }
                }
            }
            return false; // Vanilla draws the targeted block.
        });
    }
}
