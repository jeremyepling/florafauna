package net.j40climb.florafauna.noclip;

import net.j40climb.florafauna.FloraFauna;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Client-side state management for no-clip mode.
 * Tracks whether the local player has noclip enabled.
 * Resets on logout to prevent state from persisting across sessions.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class NoClipClientState {

    private static boolean enabled = false;

    /**
     * @return true if noclip is currently enabled on the client
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the noclip state.
     * @param value true to enable, false to disable
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Toggles the noclip state.
     * @return the new state after toggling
     */
    public static boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    /**
     * Resets noclip state when player logs out.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        enabled = false;
    }
}
