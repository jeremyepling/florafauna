package net.j40climb.florafauna.client.model;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;

/**
 * Model for the Frenchie frontpack that renders on players.
 * This model will be positioned on the player's front.
 *
 */
public class FrenchFrontpackModel extends EntityModel<AvatarRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "french_frontpack"), "main");

    private final KeyframeAnimation idlingAnimation;
    private final AnimationState idleAnimationState = new AnimationState();

    private final ModelPart root;
    private final ModelPart carrier;
    private final ModelPart strap_left;
    private final ModelPart strap_right;
    private final ModelPart strap_waist;
    private final ModelPart frenchie;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart earR;
    private final ModelPart earL;
    private final ModelPart tonge;
    private final ModelPart chest;
    private final ModelPart tail;
    private final ModelPart leg_front_left;
    private final ModelPart leg_front_right;
    private final ModelPart leg_back_left;
    private final ModelPart leg_back_right;

    public FrenchFrontpackModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.carrier = this.root.getChild("carrier");
        this.strap_left = this.carrier.getChild("strap_left");
        this.strap_right = this.carrier.getChild("strap_right");
        this.strap_waist = this.carrier.getChild("strap_waist");
        this.frenchie = this.root.getChild("frenchie");
        this.body = this.frenchie.getChild("body");
        this.head = this.body.getChild("head");
        this.earR = this.head.getChild("earR");
        this.earL = this.head.getChild("earL");
        this.tonge = this.head.getChild("tonge");
        this.chest = this.body.getChild("chest");
        this.tail = this.chest.getChild("tail");
        this.leg_front_left = this.body.getChild("leg_front_left");
        this.leg_front_right = this.body.getChild("leg_front_right");
        this.leg_back_left = this.body.getChild("leg_back_left");
        this.leg_back_right = this.body.getChild("leg_back_right");

        // Bake the idle animation - converts the AnimationDefinition to a KeyframeAnimation
        this.idlingAnimation = FrenchFrontpackAnimations.ANIM_IDLE.bake(root);

        // Start the animation state so it's always active
        this.idleAnimationState.start(0);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition carrier = root.addOrReplaceChild("carrier", CubeListBuilder.create().texOffs(45, 0).addBox(-5.7F, 2.0F, -2.9F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.7F, -9.0F, -1.1F));

        PartDefinition strap_left = carrier.addOrReplaceChild("strap_left", CubeListBuilder.create().texOffs(49, 2).addBox(-1.0F, -3.1F, -0.2F, 1.0F, 5.1F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(47, 1).addBox(-1.0F, -3.3F, -0.2F, 1.0F, 0.2F, 4.6F, new CubeDeformation(0.0F))
                .texOffs(55, 0).addBox(-1.0F, -3.1F, 4.2F, 1.0F, 9.1F, 0.2F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition strap_right = carrier.addOrReplaceChild("strap_right", CubeListBuilder.create().texOffs(49, 2).addBox(-1.2F, -3.1F, -0.2F, 1.0F, 5.1F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(47, 1).addBox(-1.2F, -3.3F, -0.2F, 1.0F, 0.2F, 4.6F, new CubeDeformation(0.0F))
                .texOffs(55, 0).addBox(-1.2F, -3.1F, 4.2F, 1.0F, 9.1F, 0.2F, new CubeDeformation(0.0F)), PartPose.offset(-4.2F, 0.0F, 0.0F));

        PartDefinition strap_waist = carrier.addOrReplaceChild("strap_waist", CubeListBuilder.create().texOffs(47, 1).addBox(-0.2F, -1.0F, -3.3F, 0.2F, 1.0F, 4.6F, new CubeDeformation(0.0F))
                .texOffs(58, 5).addBox(0.0F, -1.0F, -3.3F, 1.1F, 1.0F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(46, 7).addBox(0.0F, -1.0F, 1.1F, 8.2F, 1.0F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(58, 5).mirror().addBox(7.1F, -1.0F, -3.3F, 1.1F, 1.0F, 0.2F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(47, 1).addBox(8.2F, -1.0F, -3.3F, 0.2F, 1.0F, 4.6F, new CubeDeformation(0.0F)), PartPose.offset(-6.8F, 7.0F, 3.1F));

        PartDefinition frenchie = root.addOrReplaceChild("frenchie", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.5F, -6.0F, 0.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition body = frenchie.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.483F, 1.3761F, -4.3865F, 1.0908F, 0.0F, 0.0F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -3.8337F));

        PartDefinition earR_behind_r1 = earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 24).mirror().addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -3.8337F));

        PartDefinition earL_behind_r1 = earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition tonge = head.addOrReplaceChild("tonge", CubeListBuilder.create(), PartPose.offset(-0.483F, 2.6125F, -5.875F));

        PartDefinition cube_r1 = tonge.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, 0.9F, 0.6545F, 0.0F, 0.0F));

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        PartDefinition tail = chest.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.75F, 5.25F));

        PartDefinition leg_front_left = body.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        PartDefinition leg_front_right = body.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        PartDefinition leg_back_left = body.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        PartDefinition leg_back_right = body.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(21, 31).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState renderState) {
        super.setupAnim(renderState);

        // Apply the idle animation (static pose that's always active)
        // Using the KeyframeAnimation.apply() method like in FrenchieModel
        this.idlingAnimation.apply(this.idleAnimationState, renderState.ageInTicks, 1.0F);
    }
}
