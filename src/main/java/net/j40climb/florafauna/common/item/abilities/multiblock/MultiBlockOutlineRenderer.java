package net.j40climb.florafauna.common.item.abilities.multiblock;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.List;
import java.util.Optional;

/** Draws the additional outlines for a multi-block mining tool. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class MultiBlockOutlineRenderer {
    // Planned treads can be inside the rock being excavated or across open air.
    private static final RenderPipeline PLACEMENT_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "pipeline/hammer_placement"))
            .withDepthStencilState(Optional.empty())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();
    private static final RenderType PLACEMENT_LINES = RenderType.create("florafauna_hammer_placement",
            RenderSetup.builder(PLACEMENT_PIPELINE).createRenderSetup());
    private static final int PLACEMENT_COLOR = 0xD92DE7B1;

    @SubscribeEvent
    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PLACEMENT_PIPELINE);
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
                        .move(pos.getX() - target.getX(), pos.getY() - target.getY(), pos.getZ() - target.getZ()))
                .filter(shape -> !shape.isEmpty())
                .toList();
        List<VoxelShape> placements = plan.stairs().stream()
                .map(pos -> plan.stairState().getShape(event.getLevel(), pos)
                        .move(pos.getX() - target.getX(), pos.getY() - target.getY(), pos.getZ() - target.getZ()))
                .toList();
        if (outlines.isEmpty() && placements.isEmpty()) {
            return;
        }

        event.addCustomRenderer((state, collector, poseStack, levelState) -> {
            Vec3 camera = levelState.cameraRenderState.pos;
            poseStack.pushPose();
            poseStack.translate(target.getX() - camera.x, target.getY() - camera.y, target.getZ() - camera.z);
            for (VoxelShape outline : outlines) {
                collector.submitShapeOutline(poseStack, outline, RenderTypes.linesTranslucentNoDepthWrite(),
                        0x66000000, 2.0F, state.isTranslucent());
            }
            for (VoxelShape stair : placements) {
                collector.submitShapeOutline(poseStack, stair, PLACEMENT_LINES, PLACEMENT_COLOR, 2.5F, state.isTranslucent());
            }
            poseStack.popPose();
            return false; // Vanilla draws the targeted block.
        });
    }
}
