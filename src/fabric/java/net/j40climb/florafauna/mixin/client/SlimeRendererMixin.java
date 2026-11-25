package net.j40climb.florafauna.mixin.client;

import net.j40climb.florafauna.common.block.slimegrate.SlimeSplitLayer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.world.entity.monster.Slime;
//? if >=26.3 {
/*import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The 26.3 extraction implementation moved into the shared cube-mob renderer.
//? if <26.3 {
@Mixin(SlimeRenderer.class)
//?} else {
/*@Mixin(AbstractCubeMobRenderer.class)
*///?}
abstract class SlimeRendererMixin {
    //? if <26.3 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Slime;Lnet/minecraft/client/renderer/entity/state/SlimeRenderState;F)V", at = @At("TAIL"))
    private void florafauna$extractSplit(Slime slime, SlimeRenderState state, float partialTick, CallbackInfo ci) {
        SlimeSplitLayer.extractState(slime, state);
    }
    //?} else {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob;Lnet/minecraft/client/renderer/entity/state/SlimeRenderState;F)V", at = @At("TAIL"))
    private void florafauna$extractSplit(AbstractCubeMob cube, SlimeRenderState state, float partialTick, CallbackInfo ci) {
        if (cube instanceof Slime slime) SlimeSplitLayer.extractState(slime, state);
    }
    *///?}
}
