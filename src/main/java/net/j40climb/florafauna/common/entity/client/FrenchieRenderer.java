package net.j40climb.florafauna.common.entity.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.FrenchieVariant;
import net.j40climb.florafauna.common.entity.custom.FrenchieEntity;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FrenchieRenderer extends MobRenderer<FrenchieEntity, FrenchieModel<FrenchieEntity>> {
    private static final Map<FrenchieVariant, ResourceLocation> LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(FrenchieVariant.class), map -> {
                map.put(FrenchieVariant.FAWN,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/frenchie/frenchie.png"));
                map.put(FrenchieVariant.BRINDLE,
                        ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/frenchie/frenchie_brindle.png"));
            });


    public FrenchieRenderer(EntityRendererProvider.Context context) {
        super(context, new FrenchieModel<>(context.bakeLayer(FrenchieModel.FRENCHIE)), 0.5f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FrenchieEntity entity) {
        //return ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "textures/entity/gecko/gecko.png");
        return LOCATION_BY_VARIANT.get(entity.getVariant());
    }

    @Override
    public void render(FrenchieEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.45f, 0.45f, 0.45f);
        } else {
            poseStack.scale(1f, 1f, 1f);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void setupRotations(FrenchieEntity entity, PoseStack poseStack,
                                  float bob, float yBodyRot, float partialTick, float scale) {
        // This is needed so the entity isn't pointing straight up when asleep
        if (entity.getPose() != Pose.SLEEPING) {
            super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(yBodyRot));
        }
    }

    @Override
    public @NotNull Vec3 getRenderOffset(FrenchieEntity entity, float partialTick) {
        // This is needed to put the shadow under the entity instead of beside it
        return entity.getPose() == Pose.SLEEPING ?
                new Vec3(0.0D, 0.0D, -0.6D) :
                super.getRenderOffset(entity, partialTick);
    }
}
