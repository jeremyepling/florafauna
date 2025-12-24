package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class FrenchieModel extends EntityModel<FrenchieRenderState> {

    public static final ModelLayerLocation FRENCHIE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie"), "frenchie"); // is path is folder under textures/entity?

    private final KeyframeAnimation walkingAnimation;
    private final KeyframeAnimation swimmingAnimation;
    private final KeyframeAnimation sleepingAnimation;
    private final KeyframeAnimation idlingAnimation;
    private final KeyframeAnimation standToSitAnimation;
    private final KeyframeAnimation sitToStandAnimation;
    private final KeyframeAnimation sitPoseAnimation;
    private final KeyframeAnimation standToSleepAnimation;
    private final KeyframeAnimation sleepToStandAnimation;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;

    public FrenchieModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");

        this.walkingAnimation = FrenchieAnimations.ANIM_WALK.bake(root);
        this.swimmingAnimation = FrenchieAnimations.ANIM_SWIM.bake(root);
        this.sleepingAnimation = FrenchieAnimations.ANIM_SLEEP.bake(root);
        this.idlingAnimation = FrenchieAnimations.ANIM_IDLE.bake(root);
        this.standToSitAnimation = FrenchieAnimations.ANIM_STAND_TO_SIT.bake(root);
        this.sitToStandAnimation = FrenchieAnimations.ANIM_SIT_TO_STAND.bake(root);
        this.sitPoseAnimation = FrenchieAnimations.ANIM_SIT.bake(root);
        this.standToSleepAnimation = FrenchieAnimations.ANIM_STAND_TO_SLEEP.bake(root);
        this.sleepToStandAnimation = FrenchieAnimations.ANIM_SLEEP_TO_STAND.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.483F, -1.6125F, -4.125F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -3.8337F));

        earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 24).mirror().addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -3.8337F));

        earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        head.addOrReplaceChild("tongueBone", CubeListBuilder.create(), PartPose.offset(0.767F, 5.0625F, -0.475F));

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        chest.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.75F, 5.25F));

        body.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        body.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(21, 31).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(FrenchieRenderState renderState) {
        super.setupAnim(renderState);

        // Don't rotate head/body when sleeping or sitting
        if (!renderState.isSleeping && !renderState.isSitting) {
            this.applyHeadRotation(renderState.yRot, renderState.xRot);
        }

        // Apply walk animation (swimming uses its own animation state below)
        if (!renderState.isSwimming) {
            this.walkingAnimation.applyWalk(renderState.walkAnimationPos, renderState.walkAnimationSpeed, 2.5F, 2.5F);
        }

        // Apply swim animation when in water
        if (renderState.isSwimming) {
            this.swimmingAnimation.apply(renderState.swimAnimationState, renderState.ageInTicks, 1.0F);
        }

        // Apply sleeping transitions and pose
        this.standToSleepAnimation.apply(renderState.standToSleepAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepToStandAnimation.apply(renderState.sleepToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepingAnimation.apply(renderState.sleepAnimationState, renderState.ageInTicks, 1.0F);

        // Apply sitting transitions and pose
        this.standToSitAnimation.apply(renderState.standToSitAnimationState, renderState.ageInTicks, 1.0F);
        this.sitToStandAnimation.apply(renderState.sitToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sitPoseAnimation.apply(renderState.sitPoseAnimationState, renderState.ageInTicks, 1.0F);

        // Apply idle animation
        this.idlingAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks, 1.0F);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headYaw, -325f, 45);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }
}
