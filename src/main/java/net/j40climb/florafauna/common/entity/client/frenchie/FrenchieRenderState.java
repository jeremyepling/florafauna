package net.j40climb.florafauna.common.entity.client.frenchie;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class FrenchieRenderState extends LivingEntityRenderState {
    public FrenchieVariant variant;
    public boolean isSleeping;
    public boolean isSwimming;
    public boolean isSitting;
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState standToSitAnimationState = new AnimationState();
    public final AnimationState sitToStandAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState standToSleepAnimationState = new AnimationState();
    public final AnimationState sleepToStandAnimationState = new AnimationState();

}
