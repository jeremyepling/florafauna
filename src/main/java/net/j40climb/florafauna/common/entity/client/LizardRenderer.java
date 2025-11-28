/*
package net.j40climb.florafauna.common.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.custom.LizardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class LizardRenderer extends MobRenderer<LizardEntity, LizardModel<LizardEntity>> {
    public LizardRenderer(EntityRendererProvider.Context context) {
        super(context, new LizardModel<>(context.bakeLayer(LizardModel.LIZARD)), 0.6f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LizardEntity entity) {
        if(entity.isSaddled()) {
            return ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/lizard/lizard_saddled.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/lizard/lizard.png");
        }
    }

    @Override
    public void render(LizardEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.5f, 0.6f, 0.5f);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}*/