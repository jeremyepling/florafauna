package net.j40climb.florafauna.common.entity.client.gecko;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GeckoModel extends EntityModel<GeckoRenderState> {

    public static final ModelLayerLocation GECKO =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "gecko"), "gecko"); // is path is folder under textures/entity?

    private final ModelPart root;
    private final ModelPart head;

    public GeckoModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.head = this.root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, -1.0F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 10).addBox(-2.0F, -1.9F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.6F, -3.0F));

        PartDefinition tongueBone = head.addOrReplaceChild("tongueBone", CubeListBuilder.create().texOffs(-2, 27).addBox(-1.0F, 0.55F, -2.8F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, -0.45F, -0.6F));

        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(-1, -1).addBox(-2.0F, -3.0F, -4.0F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition frontLegL = root.addOrReplaceChild("frontLegL", CubeListBuilder.create(), PartPose.offset(3.1986F, -0.4741F, -2.4807F));

        PartDefinition FToesL_r1 = frontLegL.addOrReplaceChild("FToesL_r1", CubeListBuilder.create().texOffs(5, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.3014F, 0.7741F, -1.4193F, 0.0F, -0.1789F, -3.1416F));

        PartDefinition FLegL_r1 = frontLegL.addOrReplaceChild("FLegL_r1", CubeListBuilder.create().texOffs(17, 20).addBox(-1.1428F, -0.5F, -0.6528F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2612F, -0.0259F, -0.2248F, 0.0573F, 0.2106F, 0.2679F));

        PartDefinition frontLegR = root.addOrReplaceChild("frontLegR", CubeListBuilder.create(), PartPose.offset(-3.275F, -0.5804F, -2.5515F));

        PartDefinition FToesR_r1 = frontLegR.addOrReplaceChild("FToesR_r1", CubeListBuilder.create().texOffs(9, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.325F, 0.8804F, -0.3485F, 3.1416F, -0.1384F, 0.0F));

        PartDefinition FLegR_r1 = frontLegR.addOrReplaceChild("FLegR_r1", CubeListBuilder.create().texOffs(17, 24).addBox(-2.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, 0.25F, -0.25F, 0.0573F, -0.2106F, -0.2679F));

        PartDefinition backLegL = root.addOrReplaceChild("backLegL", CubeListBuilder.create(), PartPose.offset(3.2612F, -0.5957F, 0.7739F));

        PartDefinition BToesL_r1 = backLegL.addOrReplaceChild("BToesL_r1", CubeListBuilder.create().texOffs(-3, 23).addBox(-1.0F, 0.2F, -1.0F, 2.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.7388F, 0.8957F, 0.5761F, 0.0F, 0.3819F, -3.1416F));

        PartDefinition BLegL_r1 = backLegL.addOrReplaceChild("BLegL_r1", CubeListBuilder.create().texOffs(17, 18).addBox(-0.9F, -0.4F, -0.45F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8612F, -0.0043F, -0.0239F, -0.0883F, -0.3958F, 0.2794F));

        PartDefinition backLegR = root.addOrReplaceChild("backLegR", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.2612F, -0.5957F, 0.7739F, 0.0F, 0.0F, 0.0436F));

        PartDefinition BToesR_r1 = backLegR.addOrReplaceChild("BToesR_r1", CubeListBuilder.create().texOffs(1, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7388F, 0.8957F, 1.6261F, 0.0F, 2.7817F, -3.098F));

        PartDefinition BLegR_r1 = backLegR.addOrReplaceChild("BLegR_r1", CubeListBuilder.create().texOffs(17, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.9612F, 0.5957F, 0.4761F, -0.0883F, 0.3958F, -0.2794F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(15, 11).addBox(-1.5F, -1.0F, 0.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 17).addBox(-1.0F, 0.0F, 3.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 2.5F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(GeckoRenderState renderState) {
        super.setupAnim(renderState);
        this.applyHeadRotation(renderState.yRot, renderState.xRot);

        this.animateWalk(GeckoAnimations.ANIM_GECKO_WALK, renderState.walkAnimationPos, renderState.walkAnimationSpeed, 1.5F, 2.5F);
        this.animate(renderState.idleAnimationState, GeckoAnimations.ANIM_GECKO_IDLE, renderState.ageInTicks, 1f);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headYaw, -325f, 45);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }
}
