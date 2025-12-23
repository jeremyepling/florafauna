package net.j40climb.florafauna.common.item.symbiote;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogue;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueTrigger;
import net.j40climb.florafauna.common.item.symbiote.tracking.SymbioteEventTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Commands for managing symbiote attachment data.
 * Usage:
 *   /symbiote check - Display current symbiote data
 *   /symbiote bond - Bond a symbiote to the player
 *   /symbiote unbond - Unbond the symbiote from the player (returns item with memory intact)
 *   /symbiote reset - Reset the symbiote completely (for testing, no item created)
 *   /symbiote toggle <ability> - Toggle an ability (dash, featherFalling, speed)
 *   /symbiote setJumpHeight <0-4> - Set jump height ability (0=off, 4=max 4 blocks)
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
                        .then(Commands.literal("reset")
                                .executes(SymbioteCommand::resetSymbiote))
                        .then(Commands.literal("toggle")
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("dash");
                                            builder.suggest("featherFalling");
                                            builder.suggest("speed");
                                            return builder.buildFuture();
                                        })
                                        .executes(SymbioteCommand::toggleAbility)))
                        .then(Commands.literal("setJumpHeight")
                                .then(Commands.argument("height", IntegerArgumentType.integer(0, 4))
                                        .executes(SymbioteCommand::setJumpHeight)))
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
        SymbioteData data = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

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
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.jump_height",
                    String.valueOf(data.jumpHeight())), false);
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

        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

        if (currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.already_bonded"));
            return 0;
        }

        // Bond the symbiote
        SymbioteData newData = new SymbioteData(
                true,
                player.level().getGameTime(),
                1,
                false, false, false, 0
        );
        player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, newData);

        // Trigger bonding dialogue
        SymbioteDialogue.forceTrigger(player, SymbioteDialogueTrigger.BONDED);

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

        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        // Trigger unbonding dialogue
        SymbioteDialogue.forceTrigger(player, SymbioteDialogueTrigger.UNBONDED);

        // Read current player state
        SymbioteEventTracker eventTracker = player.getData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER);

        // Create symbiote item with current player state
        ItemStack symbioteItem = new ItemStack(RegisterItems.SYMBIOTE.get());

        // Copy player state to item components (set bonded=false since it's an item now)
        SymbioteData unbondedData = new SymbioteData(
                false,                      // bonded = false (it's an item now)
                0L,                         // bondTime = 0 (not bonded)
                currentData.tier(),         // preserve tier
                currentData.dash(),         // preserve abilities
                currentData.featherFalling(),
                currentData.speed(),
                currentData.jumpHeight()
        );
        symbioteItem.set(RegisterDataComponentTypes.SYMBIOTE_DATA, unbondedData);

        // Copy event tracker to item (memory persists)
        symbioteItem.set(RegisterDataComponentTypes.SYMBIOTE_EVENT_TRACKER, eventTracker);

        // Give item to player
        if (!player.getInventory().add(symbioteItem)) {
            // If inventory is full, drop at player's feet
            player.drop(symbioteItem, false);
        }

        // Unbond the symbiote (reset player attachments to default)
        player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, SymbioteData.DEFAULT);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER, SymbioteEventTracker.DEFAULT);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.unbonded_success")
                .withStyle(style -> style.withColor(0xE74C3C)), false);
        return 1;
    }

    /**
     * Executes the reset command to fully reset the symbiote (for testing).
     * This does a complete reset of all symbiote data without creating an item.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int resetSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        // Full reset - clear both attachments without creating an item
        player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, SymbioteData.DEFAULT);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_EVENT_TRACKER, SymbioteEventTracker.DEFAULT);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.reset_success")
                .withStyle(style -> style.withColor(0xF39C12)), false);
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

        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

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
                        currentData.speed(),
                        currentData.jumpHeight()
                );
                break;
            case "featherfalling":
                newData = new SymbioteData(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        !currentData.featherFalling(),
                        currentData.speed(),
                        currentData.jumpHeight()
                );
                break;
            case "speed":
                newData = new SymbioteData(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        currentData.featherFalling(),
                        !currentData.speed(),
                        currentData.jumpHeight()
                );
                break;
            default:
                source.sendFailure(Component.translatable("command.florafauna.symbiote.invalid_ability", ability));
                return 0;
        }

        player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, newData);

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
     * Executes the setJumpHeight command to set the jump height value.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int setJumpHeight(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        int height = IntegerArgumentType.getInteger(context, "height");

        // Create new data with updated jump height
        SymbioteData newData = new SymbioteData(
                currentData.bonded(),
                currentData.bondTime(),
                currentData.tier(),
                currentData.dash(),
                currentData.featherFalling(),
                currentData.speed(),
                height
        );

        player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, newData);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.jump_height_set",
                height).withStyle(style -> style.withColor(0xF39C12)), false);

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
