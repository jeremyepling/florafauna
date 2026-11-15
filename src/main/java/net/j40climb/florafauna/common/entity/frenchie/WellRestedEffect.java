package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class WellRestedEffect extends MobEffect {
    public WellRestedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x90cde3);
        addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "well_rested"),
                0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
