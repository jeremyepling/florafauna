package net.j40climb.florafauna.mixin.symbiote;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerAdaptationMixin {
    @Inject(method = "awardStat(Lnet/minecraft/stats/Stat;I)V", at = @At("RETURN"))
    private void florafauna$trackSprintDistance(Stat<?> stat, int count, CallbackInfo info) {
        if (stat.getType() == Stats.CUSTOM && Stats.SPRINT_ONE_CM.equals(stat.getValue())) {
            SymbioteOnboarding.onSprintDistance((ServerPlayer) (Object) this, count);
        }
    }

    @WrapMethod(method = "hurtServer")
    private boolean florafauna$trackDamagingLanding(ServerLevel level, DamageSource source, float damage,
                                                     Operation<Boolean> original) {
        if (!source.is(DamageTypeTags.IS_FALL)) return original.call(level, source, damage);

        var player = (ServerPlayer) (Object) this;
        float before = player.getHealth();
        boolean hurt = original.call(level, source, damage);
        // Observe the outcome after armor, absorption and existing Feather Falling have run.
        if (hurt && player.isAlive() && player.getHealth() < before) {
            SymbioteOnboarding.onSurvivedDamagingLanding(player);
        }
        return hurt;
    }
}
