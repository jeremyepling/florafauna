package net.j40climb.florafauna.common.item.symbiote.voice;

import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.observation.ChaosSuppressor;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centralized service for symbiote voice output.
 * Handles cooldown enforcement, chaos suppression, and message styling.
 *
 * All symbiote messages should go through this service to ensure
 * consistent behavior and rate limiting.
 */
public class SymbioteVoiceService {
    /**
     * Subtle purple-gray color for voice messages (per spec: no bright colors)
     */
    private static final int VOICE_COLOR = 0xB39DDB;

    /**
     * Attempt to speak a line, enforcing all cooldown rules.
     *
     * @param player The player to send the message to
     * @param tier The tier of voice output
     * @param category The observation category
     * @param lineKey The translation key for the message
     * @return true if the message was sent, false if blocked by cooldowns
     */
    public static boolean trySpeak(
            ServerPlayer player,
            VoiceTier tier,
            ObservationCategory category,
            String lineKey
    ) {
        // Must be bonded to hear the symbiote
        SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return false;
        }

        // Check chaos suppression (only for Tier 1)
        if (tier == VoiceTier.TIER_1_AMBIENT && ChaosSuppressor.isSuppressed(player)) {
            return false;
        }

        // Get current cooldown state
        VoiceCooldownState cooldowns = player.getData(RegisterAttachmentTypes.VOICE_COOLDOWNS);
        long currentTick = player.level().getGameTime();

        // Check tier and category cooldowns
        if (!cooldowns.canSpeak(tier, category, currentTick)) {
            return false;
        }

        // Send the message with subtle styling
        sendStyledMessage(player, lineKey);

        // Update cooldown state
        VoiceCooldownState newState = cooldowns.afterSpeaking(tier, category, currentTick);
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, newState);

        return true;
    }

    /**
     * Force speak a line, bypassing all cooldowns.
     * Use sparingly for critical moments like initial bonding.
     *
     * @param player The player to send the message to
     * @param lineKey The translation key for the message
     */
    public static void forceSpeak(ServerPlayer player, String lineKey) {
        // Must be bonded to hear the symbiote
        SymbioteData symbioteData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
        if (!symbioteData.bonded()) {
            return;
        }

        sendStyledMessage(player, lineKey);
    }

    /**
     * Force speak without checking bond status.
     * Used for bonding/unbonding messages.
     *
     * @param player The player to send the message to
     * @param lineKey The translation key for the message
     */
    public static void forceSpeakUnbonded(ServerPlayer player, String lineKey) {
        sendStyledMessage(player, lineKey);
    }

    /**
     * Send a styled message to the player.
     * Per spec: No prefix, no icon, subtle italic text.
     *
     * @param player The player to send to
     * @param lineKey The translation key
     */
    private static void sendStyledMessage(ServerPlayer player, String lineKey) {
        MutableComponent message = Component.translatable(lineKey)
                .withStyle(style -> style
                        .withColor(VOICE_COLOR)
                        .withItalic(true));

        // Send to chat (false = not action bar)
        player.displayClientMessage(message, false);
    }

    /**
     * Send a dream message with level-specific styling.
     *
     * @param player The player to send to
     * @param lineKey The translation key
     * @param color The color for this dream level
     */
    public static void sendDreamMessage(ServerPlayer player, String lineKey, int color) {
        MutableComponent message = Component.translatable(lineKey)
                .withStyle(style -> style
                        .withColor(color)
                        .withItalic(true));

        player.displayClientMessage(message, false);
    }

    /**
     * Reset all cooldowns for a player (for debugging).
     *
     * @param player The player to reset
     */
    public static void resetCooldowns(ServerPlayer player) {
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);
    }
}
