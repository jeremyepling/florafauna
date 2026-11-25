package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.gecko.GeckoRenderer;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardRenderer;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderers.class)
abstract class EntityRenderersMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void florafauna$registerRenderers(WritableRegistry<EntityRendererProvider<?>> registry, CallbackInfo ci) {
        Registry.register(registry, FloraFaunaRegistry.GECKO_KEY.identifier(), (EntityRendererProvider<GeckoEntity>) GeckoRenderer::new);
        Registry.register(registry, FloraFaunaRegistry.LIZARD_KEY.identifier(), (EntityRendererProvider<LizardEntity>) LizardRenderer::new);
    }
}
