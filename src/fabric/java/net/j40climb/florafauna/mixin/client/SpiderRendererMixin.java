package net.j40climb.florafauna.mixin.client;

import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpiderRenderer.class)
abstract class SpiderRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/spider/Spider;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void florafauna$extractSilkPose(Spider spider, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (AttachmentAccess.has(spider, FloraFaunaRegistry.SILK_HANGING) &&
                AttachmentAccess.get(spider, FloraFaunaRegistry.SILK_HANGING)) state.isUpsideDown = true;
    }
}
