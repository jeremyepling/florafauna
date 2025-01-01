package net.j40climb.florafauna.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.entity.custom.GeckoEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GeckoModel<T extends GeckoEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation GECKO =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "gecko"), "main");

    private final ModelPart gecko;
    private final ModelPart head;
    private final ModelPart TongueBone;
    private final ModelPart Torso;
    private final ModelPart FrontLegL;
    private final ModelPart FrontLegR;
    private final ModelPart BackLegL;
    private final ModelPart BackLegR;
    private final ModelPart Tail;

    public GeckoModel(ModelPart root) {
        this.gecko = root.getChild("gecko");
        this.head = this.gecko.getChild("Head");
        this.TongueBone = this.head.getChild("TongueBone");
        this.Torso = this.gecko.getChild("Torso");
        this.FrontLegL = this.gecko.getChild("FrontLegL");
        this.FrontLegR = this.gecko.getChild("FrontLegR");
        this.BackLegL = this.gecko.getChild("BackLegL");
        this.BackLegR = this.gecko.getChild("BackLegR");
        this.Tail = this.gecko.getChild("Tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition gecko = partdefinition.addOrReplaceChild("gecko", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, -1.0F));

        PartDefinition Head = gecko.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, -2.0623F, -3.9587F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.6F, -3.0F));

        PartDefinition TongueBone = Head.addOrReplaceChild("TongueBone", CubeListBuilder.create().texOffs(20, 15).addBox(-1.0F, 0.55F, -2.8F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, -0.45F, -0.6F));

        PartDefinition Torso = gecko.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(-1, -1).addBox(-2.0F, -3.0F, -4.0F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition FrontLegL = gecko.addOrReplaceChild("FrontLegL", CubeListBuilder.create(), PartPose.offset(3.1986F, -0.4741F, -2.4807F));

        PartDefinition FToesL_r1 = FrontLegL.addOrReplaceChild("FToesL_r1", CubeListBuilder.create().texOffs(20, 2).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.3014F, 0.7741F, -1.0193F, 0.0F, -0.1789F, -3.1416F));

        PartDefinition FLegL_r1 = FrontLegL.addOrReplaceChild("FLegL_r1", CubeListBuilder.create().texOffs(20, 4).addBox(-1.1428F, -0.4441F, -0.6528F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2612F, -0.0259F, -0.2248F, 0.0573F, 0.2106F, 0.2679F));

        PartDefinition FrontLegR = gecko.addOrReplaceChild("FrontLegR", CubeListBuilder.create(), PartPose.offset(-3.275F, -0.5804F, -2.5515F));

        PartDefinition FToesR_r1 = FrontLegR.addOrReplaceChild("FToesR_r1", CubeListBuilder.create().texOffs(12, 19).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.325F, 0.8804F, -0.8485F, 0.0F, -0.2106F, 0.0F));

        PartDefinition FLegR_r1 = FrontLegR.addOrReplaceChild("FLegR_r1", CubeListBuilder.create().texOffs(0, 20).addBox(-2.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, 0.25F, -0.25F, 0.0573F, -0.2106F, -0.2679F));

        PartDefinition BackLegL = gecko.addOrReplaceChild("BackLegL", CubeListBuilder.create(), PartPose.offset(3.2612F, -0.5957F, 0.7739F));

        PartDefinition BToesL_r1 = BackLegL.addOrReplaceChild("BToesL_r1", CubeListBuilder.create().texOffs(12, 15).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4888F, 0.8957F, 1.0261F, 0.0F, 0.3819F, -3.1416F));

        PartDefinition BLegL_r1 = BackLegL.addOrReplaceChild("BLegL_r1", CubeListBuilder.create().texOffs(20, 0).addBox(-0.832F, -0.4122F, -0.4744F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.8612F, -0.0043F, -0.0239F, -0.0883F, -0.3958F, 0.2794F));

        PartDefinition BackLegR = gecko.addOrReplaceChild("BackLegR", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.2612F, -0.5957F, 0.7739F, 0.0F, 0.0F, 0.0436F));

        PartDefinition BToesR_r1 = BackLegR.addOrReplaceChild("BToesR_r1", CubeListBuilder.create().texOffs(12, 17).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7388F, 0.8957F, 1.2261F, 0.0F, 1.1236F, -3.1416F));

        PartDefinition BLegR_r1 = BackLegR.addOrReplaceChild("BLegR_r1", CubeListBuilder.create().texOffs(20, 6).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.9612F, 0.5957F, 0.4761F, -0.0883F, 0.3958F, -0.2794F));

        PartDefinition Tail = gecko.addOrReplaceChild("Tail", CubeListBuilder.create().texOffs(14, 9).addBox(-1.5F, -1.0F, -0.5F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 15).addBox(-1.0F, -0.1F, 3.0F, 2.0F, 1.0F, 3.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 2.5F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(GeckoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch);

        this.animateWalk(GeckoAnimations.ANIM_GECKO_WALK, limbSwing, limbSwingAmount, 6f, 2.5f);
        this.animate(entity.idleAnimationState, GeckoAnimations.ANIM_GECKO_IDLE, ageInTicks, 1f);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headYaw, -325f, 45);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        gecko.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return gecko;
    }
}
