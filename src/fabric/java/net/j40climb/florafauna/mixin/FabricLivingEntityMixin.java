package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.symbiote.abilities.SymbioteAbilityEvents;
import net.j40climb.florafauna.common.symbiote.observation.ObservationEvents;
import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
abstract class FabricLivingEntityMixin {
 @Inject(method="jumpFromGround",at=@At("TAIL"))
 private void florafauna$observeJump(CallbackInfo ci){SymbioteOnboarding.onJump((LivingEntity)(Object)this);}
}
