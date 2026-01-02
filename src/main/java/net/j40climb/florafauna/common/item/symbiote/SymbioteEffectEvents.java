package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Handles events related to symbiote mob effects.
 * Specifically handles the expiration of the Symbiote Prepared effect.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class SymbioteEffectEvents {

    /**
     * Called when any mob effect expires on an entity.
     * If the Symbiote Prepared effect expires and the player hasn't bonded yet,
     * clears the symbioteBindable state.
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance == null) {
            return;
        }

        // Check if this is our Symbiote Prepared effect
        if (!effectInstance.getEffect().equals(ModRegistry.SYMBIOTE_PREPARED)) {
            return;
        }

        // Only handle for players
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Get player symbiote data
        PlayerSymbioteData data = player.getData(ModRegistry.PLAYER_SYMBIOTE_DATA);

        // If player is already bonded, don't clear the bindable state
        if (data.symbioteState().isBonded()) {
            return;
        }

        // Clear the symbioteBindable state since the effect expired without bonding
        PlayerSymbioteData newData = data.withSymbioteBindable(false);
        player.setData(ModRegistry.PLAYER_SYMBIOTE_DATA, newData);

        // Notify the player
        player.displayClientMessage(
                Component.translatable("symbiote.florafauna.effect_expired")
                        .withStyle(style -> style.withColor(0xE74C3C)),
                true
        );
    }
}
