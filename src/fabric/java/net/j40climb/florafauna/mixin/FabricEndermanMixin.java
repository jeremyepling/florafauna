package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnderMan.class)
abstract class FabricEndermanMixin {
 @Inject(method="teleport(DDD)Z",at=@At("HEAD"),cancellable=true)
 private void florafauna$stayBonded(double x,double y,double z,CallbackInfoReturnable<Boolean> cir){if(MobSymbioteHelper.hasMobSymbiote((net.minecraft.world.entity.Entity)(Object)this))cir.setReturnValue(false);}
}
