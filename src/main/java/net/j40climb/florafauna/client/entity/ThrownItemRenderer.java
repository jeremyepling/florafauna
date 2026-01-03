package net.j40climb.florafauna.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.j40climb.florafauna.common.entity.projectile.ThrownItemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renderer for ThrownItemEntity.
 * Renders the thrown item's model oriented in the direction of travel.
 */
public class ThrownItemRenderer extends EntityRenderer<ThrownItemEntity, ThrownItemRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ThrownItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.shadowRadius = 0.15f;
        this.shadowStrength = 0.75f;
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new ThrownItemRenderState();
    }

    @Override
    public void extractRenderState(ThrownItemEntity entity, ThrownItemRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.xRot = entity.getXRot(partialTick);
        state.yRot = entity.getYRot(partialTick);
        state.isReturning = entity.isReturning();

        // Extract the item render state
        this.itemModelResolver.updateForNonLiving(state.item, entity.getThrownItem(), ItemDisplayContext.GROUND, entity);
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraState) {
        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Rotate to face direction of travel
        // First rotate around Y to match horizontal direction
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot + 90.0f));
        // Then pitch forward 90 degrees so item points in direction of travel
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot + 90.0f));

        // Scale up slightly for visibility
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Render the item
        state.item.submit(poseStack, nodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
        super.submit(state, poseStack, nodeCollector, cameraState);
    }
}
