package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.setup.FabricGameplayEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
abstract class FabricEntityTickMixin {
 @Inject(method="tickNonPassenger",at=@At("RETURN"))
 private void florafauna$afterTick(Entity entity,CallbackInfo ci){FabricGameplayEvents.afterTick(entity);}
 @Inject(method="tickPassenger",at=@At("RETURN"))
 private void florafauna$afterPassengerTick(Entity vehicle,Entity passenger,CallbackInfo ci){FabricGameplayEvents.afterTick(passenger);}
}
