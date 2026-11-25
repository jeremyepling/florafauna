package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierVisionBlockingHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
abstract class FabricMobTargetMixin {
 @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
 private void florafauna$blockVision(LivingEntity target,CallbackInfo ci){
  Mob mob=(Mob)(Object)this;
  if(target!=null&&!mob.level().isClientSide()&&MobBarrierVisionBlockingHandler.isVisionBlockedByBarrier(mob,target))ci.cancel();
 }
}
