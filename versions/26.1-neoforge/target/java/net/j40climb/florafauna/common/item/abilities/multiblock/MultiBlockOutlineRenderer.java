package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.List;

/** Draws the additional outlines for a multi-block mining tool. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class MultiBlockOutlineRenderer {

    @SubscribeEvent
    public static void extractBlockOutlines(ExtractBlockOutlineRenderStateEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
            return;
        }

        BlockPos target = event.getBlockPos();
        List<VoxelShape> outlines = MultiBlockPatterns.plan(player, player.getMainHandItem(), target, event.getHitResult().getDirection()).blocks().stream()
                .filter(pos -> !pos.equals(target))
                .map(pos -> event.getLevel().getBlockState(pos)
                        .getShape(event.getLevel(), pos, event.getCollisionContext())
                        .move(pos.getX(), pos.getY(), pos.getZ()))
                .filter(shape -> !shape.isEmpty())
                .toList();
        if (outlines.isEmpty()) {
            return;
        }

        event.addCustomRenderer((state, buffers, poseStack, translucentPass, levelState) -> {
            if (translucentPass == state.isTranslucent()) {
                Vec3 camera = levelState.cameraRenderState.pos;
                var vertices = buffers.getBuffer(RenderTypes.lines());
                for (VoxelShape outline : outlines) {
                    ShapeRenderer.renderShape(poseStack, vertices, outline, -camera.x, -camera.y, -camera.z, 0x66000000, 2.0F);
                }
            }
            return false; // Vanilla draws the targeted block.
        });
    }
}
