package net.j40climb.florafauna.common.symbiote.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility for sending styled symbiote messages to players.
 * Provides consistent formatting across all symbiote dialogue.
 * Message format: 👾 Symbiote: message text (all in purple)
 */
public class SymbioteMessageSender {

    // Purple/alien color for symbiote messages
    private static final int SYMBIOTE_COLOR = 0x9B59B6;

    // Prefix symbol for symbiote messages
    private static final String SYMBIOTE_PREFIX = "👾 ";

    // Display name for the symbiote
    private static final String SYMBIOTE_NAME = "Symbiote";

    /**
     * Send a localized symbiote message to a player's chat
     *
     * @param player The player to send the message to
     * @param localizationKey The translation key for the message
     */
    public static void sendMessage(ServerPlayer player, String localizationKey) {
        MutableComponent message = Component.empty()
                .append(Component.literal(SYMBIOTE_PREFIX))
                .append(Component.literal(SYMBIOTE_NAME)
                        .withStyle(style -> style
                                .withColor(SYMBIOTE_COLOR)
                                .withBold(true)))
                .append(Component.literal(": "))
                .append(Component.translatable(localizationKey)
                        .withStyle(style -> style.withColor(SYMBIOTE_COLOR)));

        // Send to chat (false = not action bar)
        player.displayClientMessage(message, false);
    }

}
