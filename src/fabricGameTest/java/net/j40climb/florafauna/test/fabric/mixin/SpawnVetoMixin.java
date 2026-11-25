package net.j40climb.florafauna.test.fabric.mixin;

import net.j40climb.florafauna.test.fabric.FabricSpawnVeto;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class SpawnVetoMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void florafauna$rejectTestPassenger(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (FabricSpawnVeto.blocked((ServerLevel) (Object) this, entity)) callback.setReturnValue(false);
    }
}
