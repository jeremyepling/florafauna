package net.j40climb.florafauna.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna.common.attachments.ModAttachmentTypes;
import net.j40climb.florafauna.common.attachments.SymbioteData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug command for checking symbiote attachment data.
 * Usage: /symbiote check
 */
public class SymbioteCommand {
    /**
     * Registers the symbiote command.
     *
     * @param dispatcher the command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("symbiote")
                        .then(Commands.literal("check")
                                .executes(SymbioteCommand::checkSymbiote))
        );
    }

    /**
     * Executes the check command to display symbiote data.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int checkSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Check if the command was run by a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        // Get the player's symbiote data
        SymbioteData data = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        // Display the header
        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.header")
                .withStyle(style -> style.withColor(0x9B59B6).withBold(true)), false);

        // Display bonded status
        source.sendSuccess(() -> formatField(
                "command.florafauna.symbiote.bonded",
                formatBoolean(data.bonded())
        ), false);

        // Display all other fields if bonded
        if (data.bonded()) {
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.bond_time",
                    String.valueOf(data.bondTime())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.tier",
                    String.valueOf(data.tier())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.energy",
                    String.valueOf(data.energy())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.health",
                    data.health() + "/100"), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.dash",
                    formatEnabledDisabled(data.dash())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.feather_falling",
                    formatEnabledDisabled(data.featherFalling())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.speed",
                    formatEnabledDisabled(data.speed())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.ability_multiplier",
                    String.format("%.2f%%", data.getAbilityMultiplier() * 100)), false);
        }

        return 1;
    }

    /**
     * Formats a field with its translated label and value.
     *
     * @param labelKey the translation key for the field label
     * @param value the value to display
     * @return a formatted Component
     */
    private static Component formatField(String labelKey, String value) {
        return Component.translatable(labelKey).append(": " + value);
    }

    /**
     * Formats a boolean as Yes/No using translation keys.
     *
     * @param value the boolean value
     * @return the translated yes/no string
     */
    private static String formatBoolean(boolean value) {
        return Component.translatable(value ?
                "command.florafauna.symbiote.yes" :
                "command.florafauna.symbiote.no").getString();
    }

    /**
     * Formats a boolean as Enabled/Disabled using translation keys.
     *
     * @param value the boolean value
     * @return the translated enabled/disabled string
     */
    private static String formatEnabledDisabled(boolean value) {
        return Component.translatable(value ?
                "command.florafauna.symbiote.enabled" :
                "command.florafauna.symbiote.disabled").getString();
    }

}
