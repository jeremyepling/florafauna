package net.j40climb.florafauna.common.entity.lizard;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class LizardRenderState extends LivingEntityRenderState {
    public boolean isSleeping;
    public boolean isSitting;
    public boolean isSwimming;
    public boolean isSaddled;
    public final AnimationState idleAnimationState = new AnimationState();

}
