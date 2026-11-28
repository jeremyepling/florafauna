package net.j40climb.florafauna.mixin.symbiote;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.j40climb.florafauna.common.symbiote.abilities.SymbiotePassiveStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class LivingEntityJumpMixin {
    // Minecraft adds its Jump Boost term after applying the takeoff block's jump factor.
    @ModifyReturnValue(method = "getJumpBoostPower", at = @At("RETURN"))
    private float florafauna$useStrongerJumpBoost(float potionBonus) {
        return (Object) this instanceof Player player
                ? Math.max(potionBonus, SymbiotePassiveStats.jumpBonus(player))
                : potionBonus;
    }
}
