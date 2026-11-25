package net.j40climb.florafauna.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.j40climb.florafauna.common.symbiote.abilities.SymbioteAbilityEvents;
import net.j40climb.florafauna.common.symbiote.observation.ObservationEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class FabricPlayerDamageMixin {
    // Player overrides actuallyHurt. Match NeoForge Pre: after armor and magic, before absorption.
    @ModifyExpressionValue(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float florafauna$modifyDamage(float amount, ServerLevel level, DamageSource source, float incoming) {
        Player player = (Player) (Object) this;
        float damage = SymbioteAbilityEvents.onLivingDamage(player, source, amount);
        ObservationEvents.onLivingDamage(player, source, damage);
        return damage;
    }
}
