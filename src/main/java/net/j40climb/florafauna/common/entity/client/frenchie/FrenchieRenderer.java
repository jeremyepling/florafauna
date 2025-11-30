package net.j40climb.florafauna.common.entity.client.frenchie;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.Util;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class FrenchieRenderer extends MobRenderer<FrenchieEntity, FrenchieRenderState, FrenchieModel> {
    private static final Map<FrenchieVariant, ResourceLocation> TEXTURE_LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(FrenchieVariant.class), map -> {
                map.put(FrenchieVariant.FAWN,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/frenchie/frenchie.png"));
                map.put(FrenchieVariant.BRINDLE,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/frenchie/frenchie_brindle.png"));
            });

    public FrenchieRenderer(EntityRendererProvider.Context context) {
        super(context, new FrenchieModel(context.bakeLayer(FrenchieModel.FRENCHIE)), 0.4f); //last parameter is the size of the shadow
    }

    @Override
    public ResourceLocation getTextureLocation(FrenchieRenderState renderState) {
        return TEXTURE_LOCATION_BY_VARIANT.get(renderState.variant);
    }

    @Override
    public void submit(FrenchieRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if(renderState.isBaby) {
            poseStack.scale(0.45f, 0.45f, 0.45f);
        } else {
            poseStack.scale(1f, 1f, 1f);
        }
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public FrenchieRenderState createRenderState() {
        return new FrenchieRenderState();
    }

    @Override
    public void extractRenderState(FrenchieEntity entity, FrenchieRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.idleAnimationState.copyFrom(entity.idleAnimationState);
        reusedState.variant = entity.getVariant();
    }
}