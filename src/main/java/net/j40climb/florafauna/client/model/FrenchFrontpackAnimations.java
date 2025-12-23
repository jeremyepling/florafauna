package net.j40climb.florafauna.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class FrenchFrontpackAnimations {
    public static final AnimationDefinition ANIM_IDLE = AnimationDefinition.Builder.withLength(0.0F).looping()
            .addAnimation("frenchie", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.2F, -0.6F, -1.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("frenchie", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(0.7F, 0.6F, 0.6F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("chest", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.4943F, -0.1307F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("chest", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 0.8F, 1.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_front_left", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(24.9055F, 2.4976F, -4.3329F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_front_left", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.6849F, 0.9439F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_front_right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(27.4737F, -1.5216F, 1.9838F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_front_right", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.7845F, 0.9352F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_back_left", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(7.4366F, 0.9762F, -7.4366F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_back_left", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.0958F, -0.0959F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_back_right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(12.3964F, -1.6189F, 7.3242F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_back_right", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.9962F, -0.0872F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();
}
