package net.j40climb.florafauna.mixin.client;

import net.j40climb.florafauna.common.block.slimegrate.SlimeSplitLayer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeRenderer.class)
abstract class SlimeRendererMixin {
    //? if <26.3 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Slime;Lnet/minecraft/client/renderer/entity/state/SlimeRenderState;F)V", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/cubemob/Slime;Lnet/minecraft/client/renderer/entity/state/SlimeRenderState;F)V", at = @At("TAIL"))
    *///?}
    private void florafauna$extractSplit(Slime slime, SlimeRenderState state, float partialTick, CallbackInfo ci) {
        SlimeSplitLayer.extractState(slime, state);
    }
}
