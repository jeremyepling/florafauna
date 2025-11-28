/*
package net.j40climb.florafauna.common.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.custom.LizardEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class LizardModel<T extends LizardEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LIZARD =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "lizard"), "lizard");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hair;
    private final ModelPart neck;
    private final ModelPart torso;
    private final ModelPart frontLegL;
    private final ModelPart frontLegR;
    private final ModelPart backLegL;
    private final ModelPart backLegR;
    private final ModelPart tail;

    public LizardModel(ModelPart root) {
        this.root = root.getChild("root");
        this.head = this.root.getChild("head");
        this.hair = this.head.getChild("hair");
        this.neck = this.root.getChild("neck");
        this.torso = this.root.getChild("torso");
        this.frontLegL = this.root.getChild("frontLegL");
        this.frontLegR = this.root.getChild("frontLegR");
        this.backLegL = this.root.getChild("backLegL");
        this.backLegR = this.root.getChild("backLegR");
        this.tail = this.root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, -1.0F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -1.6F, -3.0F));

        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 69).addBox(-8.0F, -5.8377F, -0.0413F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -7.0623F, -16.9587F, 0.2618F, 0.0F, 0.0F));

        PartDefinition tongueBone = head.addOrReplaceChild("tongueBone", CubeListBuilder.create().texOffs(70, 64).addBox(-1.0F, 0.55F, -16.8F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, -0.45F, -0.6F));

        PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create(), PartPose.offset(-3.0F, -14.0F, -10.0F));

        PartDefinition hair_r1 = hair.addOrReplaceChild("hair_r1", CubeListBuilder.create().texOffs(18, 85).addBox(1.0F, -4.0F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition hair_r2 = hair.addOrReplaceChild("hair_r2", CubeListBuilder.create().texOffs(14, 85).addBox(1.0F, -4.0F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, -1.0F, -0.9554F, 0.0231F, 0.0143F));

        PartDefinition hair_r3 = hair.addOrReplaceChild("hair_r3", CubeListBuilder.create().texOffs(10, 85).addBox(1.0F, -6.0F, -1.0F, 0.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.0F, -1.0F, -0.3491F, 0.0F, 0.0F));

        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offsetAndRotation(3.0F, -1.6623F, -17.9587F, -0.5847F, 0.0F, 0.0F));

        PartDefinition neck_r1 = neck.addOrReplaceChild("neck_r1", CubeListBuilder.create().texOffs(40, 69).addBox(-7.0F, -5.8377F, -1.0413F, 8.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, -2.0F, -0.1309F, 0.0F, 0.0F));

        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -12.5F, -9.0F, 10.0F, 10.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition frontLegL = root.addOrReplaceChild("frontLegL", CubeListBuilder.create(), PartPose.offset(3.1986F, -0.4741F, -2.4807F));

        PartDefinition frontToesL_r1 = frontLegL.addOrReplaceChild("frontToesL_r1", CubeListBuilder.create().texOffs(76, 81).addBox(-5.0F, -1.8F, -1.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.2014F, 0.7741F, -9.9193F, 0.0F, 0.9675F, 0.0F));

        PartDefinition frontLegL_r1 = frontLegL.addOrReplaceChild("frontLegL_r1", CubeListBuilder.create().texOffs(70, 56).addBox(-8.9374F, -3.0F, -0.2945F, 11.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.7388F, -1.0259F, -6.2248F, -0.0873F, 0.1974F, 0.4887F));

        PartDefinition frontLegR = root.addOrReplaceChild("frontLegR", CubeListBuilder.create(), PartPose.offset(-3.275F, -0.5804F, -2.5515F));

        PartDefinition frontToesR_r1 = frontLegR.addOrReplaceChild("frontToesR_r1", CubeListBuilder.create().texOffs(76, 73).addBox(-5.0F, -1.8F, -1.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.325F, 0.8804F, -5.8485F, 0.0F, -0.9524F, 0.0F));

        PartDefinition frontLegR_r1 = frontLegR.addOrReplaceChild("frontLegR_r1", CubeListBuilder.create().texOffs(72, 14).addBox(-3.125F, -3.1696F, -0.1985F, 11.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.6F, -0.75F, -6.25F, -0.0873F, -0.1974F, -0.4887F));

        PartDefinition backLegL = root.addOrReplaceChild("backLegL", CubeListBuilder.create(), PartPose.offset(3.2612F, -0.5957F, 5.7739F));

        PartDefinition backToesL_r1 = backLegL.addOrReplaceChild("backToesL_r1", CubeListBuilder.create().texOffs(76, 64).addBox(-6.4F, -1.8F, -0.6F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.1388F, 0.8957F, 2.8261F, 0.0F, 0.4875F, 0.0F));

        PartDefinition backLegL_r1 = backLegL.addOrReplaceChild("backLegL_r1", CubeListBuilder.create().texOffs(70, 46).addBox(-13.4F, -3.9F, -0.75F, 15.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.1388F, -1.0043F, 5.9761F, 0.0873F, -0.3958F, 0.4887F));

        PartDefinition backLegR = root.addOrReplaceChild("backLegR", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.2612F, -0.5957F, 0.7739F, 0.0F, 0.0F, 0.0436F));

        PartDefinition backToesR_r1 = backLegR.addOrReplaceChild("backToesR_r1", CubeListBuilder.create().texOffs(72, 22).addBox(-4.4F, -1.8F, 2.4F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.3388F, 0.8957F, 5.8261F, 0.0F, -0.4724F, 0.0F));

        PartDefinition backLegR_r1 = backLegR.addOrReplaceChild("backLegR_r1", CubeListBuilder.create().texOffs(70, 36).addBox(-17.7F, -6.5F, -2.25F, 15.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.9612F, -5.4043F, 5.4761F, 0.0873F, 0.3958F, -0.4887F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(72, 31).addBox(0.0F, -15.0F, 15.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 85).addBox(-3.0F, -20.0F, 18.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(82, 31).addBox(0.0F, -22.0F, 21.5F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(72, 0).addBox(-3.0F, -26.5F, 28.5F, 6.0F, 3.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 2.5F));

        PartDefinition tail_r1 = tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 36).addBox(-4.0F, -6.5F, -3.0F, 8.0F, 6.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 17.5F, 0.8727F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(LizardEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch);

        this.animateWalk(LizardAnimations.ANIM_GECKO_WALK, limbSwing, limbSwingAmount, 6f, 2.5f);
        this.animate(entity.idleAnimationState, LizardAnimations.ANIM_GECKO_IDLE, ageInTicks, 1f);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headYaw, -325f, 45);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
*/