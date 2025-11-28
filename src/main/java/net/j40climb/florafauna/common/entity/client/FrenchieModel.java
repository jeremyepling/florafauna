package net.j40climb.florafauna.common.entity.client;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class FrenchieModel extends EntityModel<FrenchieRenderState> {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 10.0F, 4.0F, Set.of("head"));


    public static final ModelLayerLocation FRENCHIE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie"), "frenchie"); // is path is folder under textures/entity?
    public static final ModelLayerLocation FRENCHIE_BABY = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie_baby"), "frenchie_baby"); // is path is folder under textures/entity?

    private final ModelPart root;
    private final ModelPart head;

    public FrenchieModel(ModelPart root) {
        super(root);

        this.root = root.getChild("root");
        this.head = this.root.getChild("head");

    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.483F, -1.6125F, -4.125F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -3.8337F));

        PartDefinition earR_behind_r1 = earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(8, 21).addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(8, 24).addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -3.8337F));

        PartDefinition earL_behind_r1 = earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition tongueBone = head.addOrReplaceChild("tongueBone", CubeListBuilder.create(), PartPose.offset(0.767F, 5.0625F, -0.475F));

        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        PartDefinition frontLegL = root.addOrReplaceChild("frontLegL", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        PartDefinition frontLegR = root.addOrReplaceChild("frontLegR", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        PartDefinition backLegL = root.addOrReplaceChild("backLegL", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        PartDefinition backLegR = root.addOrReplaceChild("backLegR", CubeListBuilder.create().texOffs(21, 31).addBox(-0.951F, 0.9441F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0054F, 3.0549F, 5.0F, 0.0F, 0.0F, 0.0436F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -1.5F, 6.75F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(FrenchieRenderState renderState) {  //FrenchieEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(renderState);
        this.applyHeadRotation(renderState.yRot, renderState.xRot);

        if (renderState.isSwimming) {
            this.animateWalk(FrenchieAnimations.ANIM_SWIM, renderState.walkAnimationPos, renderState.walkAnimationSpeed, 1F, 2.5F);
        } else {
            this.animateWalk(FrenchieAnimations.ANIM_WALK, renderState.walkAnimationPos, renderState.walkAnimationSpeed, 1.5F, 2.5F);
        }

        this.animate(renderState.idleAnimationState, FrenchieAnimations.ANIM_IDLE, renderState.ageInTicks, 1.0F);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headYaw, -325f, 45);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }
}
