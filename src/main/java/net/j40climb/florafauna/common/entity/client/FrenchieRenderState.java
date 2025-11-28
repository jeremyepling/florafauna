package net.j40climb.florafauna.common.entity.client;

import net.j40climb.florafauna.common.entity.FrenchieVariant;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class FrenchieRenderState extends LivingEntityRenderState {
    public FrenchieVariant variant;
    public boolean isSleeping;
    public boolean isSitting;
    public boolean isSwimming;
    public final AnimationState idleAnimationState = new AnimationState();

}
