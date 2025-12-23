package net.j40climb.florafauna.common.entity.lizard;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;

public class LizardRenderer extends MobRenderer<LizardEntity, LizardRenderState, LizardModel> {

    public LizardRenderer(EntityRendererProvider.Context context) {
        super(context, new LizardModel(context.bakeLayer(LizardModel.LIZARD)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(LizardRenderState renderState) {
        if(renderState.isSaddled) {
            return ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/lizard/lizard_saddled.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/lizard/lizard.png");
        }
    }

    @Override
    public void submit(LizardRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if(renderState.isBaby) {
            poseStack.scale(0.5f, 0.6f, 0.5f);
        }
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public LizardRenderState createRenderState() {
        return new LizardRenderState();
    }

    @Override
    public void extractRenderState(LizardEntity entity, LizardRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.idleAnimationState.copyFrom(entity.idleAnimationState);
    }
}