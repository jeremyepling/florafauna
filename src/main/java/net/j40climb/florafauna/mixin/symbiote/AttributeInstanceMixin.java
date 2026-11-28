package net.j40climb.florafauna.mixin.symbiote;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.j40climb.florafauna.common.symbiote.abilities.SymbiotePassiveStats;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttributeInstance.class)
abstract class AttributeInstanceMixin {
    @Unique
    private static final Identifier FLORAFAUNA_VANILLA_SPEED = Identifier.withDefaultNamespace("effect.speed");
    @Unique
    private static final Identifier FLORAFAUNA_VANILLA_JUMP = Identifier.withDefaultNamespace("effect.jump_boost");

    @Shadow
    public abstract AttributeModifier getModifier(Identifier id);

    // The bond and vanilla effect modifiers remain independent, including when synchronized.
    // Ignore the weaker modifier as soon as either one changes, and let the potion win ties.
    @WrapOperation(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D"))
    private double florafauna$useStrongerModifier(AttributeModifier modifier, Operation<Double> original) {
        double amount = original.call(modifier);
        Identifier id = modifier.id();
        if (id.equals(FLORAFAUNA_VANILLA_SPEED)) {
            return florafauna$selectAmount(amount, SymbiotePassiveStats.SPEED_MODIFIER, true);
        }
        if (id.equals(SymbiotePassiveStats.SPEED_MODIFIER)) {
            return florafauna$selectAmount(amount, FLORAFAUNA_VANILLA_SPEED, false);
        }
        if (id.equals(FLORAFAUNA_VANILLA_JUMP)) {
            return florafauna$selectAmount(amount, SymbiotePassiveStats.SAFE_FALL_MODIFIER, true);
        }
        if (id.equals(SymbiotePassiveStats.SAFE_FALL_MODIFIER)) {
            return florafauna$selectAmount(amount, FLORAFAUNA_VANILLA_JUMP, false);
        }
        return amount;
    }

    @Unique
    private double florafauna$selectAmount(double amount, Identifier alternative, boolean winsTie) {
        var other = getModifier(alternative);
        if (other == null || amount > other.amount() || (winsTie && amount == other.amount())) {
            return amount;
        }
        return 0;
    }
}
