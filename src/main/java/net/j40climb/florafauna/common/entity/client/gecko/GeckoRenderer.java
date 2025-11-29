package net.j40climb.florafauna.common.entity.client.gecko;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class GeckoRenderer extends MobRenderer<GeckoEntity, GeckoRenderState, GeckoModel> {
    private static final Map<GeckoVariant, ResourceLocation> TEXTURE_LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(GeckoVariant.class), map -> {
                map.put(GeckoVariant.BLUE,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/gecko/gecko.png"));
                map.put(GeckoVariant.GREEN,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/gecko/gecko_green.png"));
                map.put(GeckoVariant.PINK,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/gecko/gecko_pink.png"));
                map.put(GeckoVariant.BROWN,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/gecko/gecko_brown.png"));
            });


    public GeckoRenderer(EntityRendererProvider.Context context) {
        super(context, new GeckoModel(context.bakeLayer(GeckoModel.GECKO)), 0.25f);
    }

    @Override
    public ResourceLocation getTextureLocation(GeckoRenderState renderState) {
       return TEXTURE_LOCATION_BY_VARIANT.get(renderState.variant);
    }

    @Override
    public void render(GeckoRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if(renderState.isBaby) {
            poseStack.scale(0.45f, 0.45f, 0.45f);
        } else {
            poseStack.scale(1f, 1f, 1f);
        }
        super.render(renderState, poseStack, bufferSource, packedLight);
    }

    @Override
    public GeckoRenderState createRenderState() {
        return new GeckoRenderState();
    }

    @Override
    public void extractRenderState(GeckoEntity entity, GeckoRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.idleAnimationState.copyFrom(entity.idleAnimationState);
        reusedState.variant = entity.getVariant();
    }
}
