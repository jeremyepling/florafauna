package net.j40climb.florafauna.common.entity.gecko;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class GeckoRenderState extends LivingEntityRenderState {
    public GeckoVariant variant;
    public boolean isSleeping;
    public final AnimationState idleAnimationState = new AnimationState();

}
