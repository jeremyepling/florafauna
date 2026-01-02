package net.j40climb.florafauna.common.symbiote.binding;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A custom mob effect that indicates the player is prepared to bond with a symbiote.
 * Applied when drinking Symbiote Stew, lasts 10 minutes.
 *
 * While this effect is active:
 * - The player can interact with a Cocoon Chamber to bond with a symbiote
 * - Purple particles appear around the player
 *
 * When the effect expires:
 * - The player's symbioteBindable state is cleared if they haven't bonded yet
 */
public class SymbiotePreparedEffect extends MobEffect {

    // Purple color to match symbiote theme
    private static final int EFFECT_COLOR = 0x9B59B6;

    public SymbiotePreparedEffect() {
        super(MobEffectCategory.BENEFICIAL, EFFECT_COLOR);
    }
}
