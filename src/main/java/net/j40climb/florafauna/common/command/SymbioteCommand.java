package net.j40climb.florafauna.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna.common.attachments.ModAttachmentTypes;
import net.j40climb.florafauna.common.attachments.SymbioteData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for managing symbiote attachment data.
 * Usage:
 *   /symbiote check - Display current symbiote data
 *   /symbiote bond - Bond a symbiote to the player
 *   /symbiote unbond - Unbond the symbiote from the player
 *   /symbiote toggle <ability> - Toggle an ability (dash, featherFalling, speed)
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
                        .then(Commands.literal("bond")
                                .executes(SymbioteCommand::bondSymbiote))
                        .then(Commands.literal("unbond")
                                .executes(SymbioteCommand::unbondSymbiote))
                        .then(Commands.literal("toggle")
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("dash");
                                            builder.suggest("featherFalling");
                                            builder.suggest("speed");
                                            return builder.buildFuture();
                                        })
                                        .executes(SymbioteCommand::toggleAbility)))
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
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.bonded",
                formatBoolean(data.bonded())), false);

        // Display all other fields if bonded
        if (data.bonded()) {
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.bond_time",
                    String.valueOf(data.bondTime())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.tier",
                    String.valueOf(data.tier())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.dash",
                    formatEnabledDisabled(data.dash())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.feather_falling",
                    formatEnabledDisabled(data.featherFalling())), false);
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.speed",
                    formatEnabledDisabled(data.speed())), false);
        }

        return 1;
    }

    /**
     * Executes the bond command to bond a symbiote to the player.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int bondSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteData currentData = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        if (currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.already_bonded"));
            return 0;
        }

        // Bond the symbiote
        SymbioteData newData = new SymbioteData(
                true,
                player.level().getGameTime(),
                1,
                false, false, false
        );
        player.setData(ModAttachmentTypes.SYMBIOTE_DATA, newData);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.bonded_success")
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    /**
     * Executes the unbond command to remove the symbiote from the player.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int unbondSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteData currentData = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        // Unbond the symbiote (reset to default)
        player.setData(ModAttachmentTypes.SYMBIOTE_DATA, SymbioteData.DEFAULT);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.unbonded_success")
                .withStyle(style -> style.withColor(0xE74C3C)), false);
        return 1;
    }

    /**
     * Executes the toggle command to toggle an ability.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int toggleAbility(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteData currentData = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        String ability = StringArgumentType.getString(context, "ability");
        SymbioteData newData;

        switch (ability.toLowerCase()) {
            case "dash":
                newData = new SymbioteData(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        !currentData.dash(),
                        currentData.featherFalling(),
                        currentData.speed()
                );
                break;
            case "featherfalling":
                newData = new SymbioteData(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        !currentData.featherFalling(),
                        currentData.speed()
                );
                break;
            case "speed":
                newData = new SymbioteData(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        currentData.featherFalling(),
                        !currentData.speed()
                );
                break;
            default:
                source.sendFailure(Component.translatable("command.florafauna.symbiote.invalid_ability", ability));
                return 0;
        }

        player.setData(ModAttachmentTypes.SYMBIOTE_DATA, newData);

        boolean newState = switch (ability.toLowerCase()) {
            case "dash" -> newData.dash();
            case "featherfalling" -> newData.featherFalling();
            case "speed" -> newData.speed();
            default -> false;
        };

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.toggle_success",
                ability,
                Component.translatable(newState ?
                        "command.florafauna.symbiote.enabled" :
                        "command.florafauna.symbiote.disabled")
        ).withStyle(style -> style.withColor(0xF39C12)), false);

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
