package net.j40climb.florafauna.common.item.symbiote;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.item.symbiote.dream.DreamInsightEngine;
import net.j40climb.florafauna.common.item.symbiote.dream.DreamLevel;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationArbiter;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalUpdater;
import net.j40climb.florafauna.common.item.symbiote.progress.SignalState;
import net.j40climb.florafauna.common.item.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Commands for managing symbiote attachment data.
 * Usage:
 *   /symbiote check - Display current symbiote data
 *   /symbiote bond - Bond a symbiote to the player
 *   /symbiote unbond - Unbond the symbiote from the player (returns item with memory intact)
 *   /symbiote reset - Reset the symbiote completely (for testing, no item created)
 *   /symbiote toggle <ability> - Toggle an ability (dash, featherFalling, speed)
 *   /symbiote setJumpBoost <0-4> - Set jump boost level (0=off, 1-4=Jump Boost I-IV)
 *   /symbiote dream - Trigger a dream insight
 *   /symbiote dream force <1-3> - Force a dream at a specific level
 *   /symbiote cooldown reset - Reset all voice cooldowns
 *   /symbiote progress - Show progress signals
 *   /symbiote progress set <concept> <state> - Set a progress state
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
                        .then(Commands.literal("setJumpBoost")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                                        .executes(SymbioteCommand::setJumpBoost)))
                        // Dream commands
                        .then(Commands.literal("dream")
                                .executes(SymbioteCommand::triggerDream)
                                .then(Commands.literal("force")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                                .executes(SymbioteCommand::forceDream))))
                        // Voice system debug commands
                        .then(Commands.literal("cooldown")
                                .then(Commands.literal("reset")
                                        .executes(SymbioteCommand::resetCooldowns)))
                        .then(Commands.literal("progress")
                                .executes(SymbioteCommand::showProgress)
                                .then(Commands.literal("set")
                                        .then(Commands.argument("concept", StringArgumentType.word())
                                                .then(Commands.argument("state", StringArgumentType.word())
                                                        .suggests((context, builder) -> {
                                                            for (SignalState state : SignalState.values()) {
                                                                builder.suggest(state.name().toLowerCase());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(SymbioteCommand::setProgressState)))))
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
        PlayerSymbioteData data = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

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
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.jump_boost",
                    String.valueOf(data.jumpBoost())), false);
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

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.already_bonded"));
            return 0;
        }

        // Bond the symbiote
        PlayerSymbioteData newData = currentData.withBond(
                true,
                player.level().getGameTime(),
                1,
                false, false, false, 0
        );
        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, newData);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, ProgressSignalTracker.DEFAULT);
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

        // Trigger bonding observation - BONDING_MILESTONE is Tier 2 breakthrough
        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 100, Map.of(
                "event", "bonded"
        ));

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.bonded_success")
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    /**
     * Executes the unbond command to remove the symbiote from the player.
     * Creates an item with the symbiote's memory/progress preserved.
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

        // Use the binding helper for unbinding
        SymbioteBindingHelper.UnbindResult result = SymbioteBindingHelper.unbindSymbiote(player);

        if (!result.success()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        // Give item to player
        if (!player.getInventory().add(result.symbioteItem())) {
            // If inventory is full, drop at player's feet
            player.drop(result.symbioteItem(), false);
        }

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

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        // Full reset - clear symbiote bond while preserving cocoon state
        PlayerSymbioteData resetData = currentData.withSymbioteReset();
        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, resetData);
        player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, ProgressSignalTracker.DEFAULT);
        player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

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

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        String ability = StringArgumentType.getString(context, "ability");
        PlayerSymbioteData newData;

        switch (ability.toLowerCase()) {
            case "dash":
                newData = currentData.withBond(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        !currentData.dash(),
                        currentData.featherFalling(),
                        currentData.speed(),
                        currentData.jumpBoost()
                );
                break;
            case "featherfalling":
                newData = currentData.withBond(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        !currentData.featherFalling(),
                        currentData.speed(),
                        currentData.jumpBoost()
                );
                break;
            case "speed":
                newData = currentData.withBond(
                        currentData.bonded(),
                        currentData.bondTime(),
                        currentData.tier(),
                        currentData.dash(),
                        currentData.featherFalling(),
                        !currentData.speed(),
                        currentData.jumpBoost()
                );
                break;
            default:
                source.sendFailure(Component.translatable("command.florafauna.symbiote.invalid_ability", ability));
                return 0;
        }

        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, newData);

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
     * Executes the setJumpBoost command to set the jump boost level.
     *
     * @param context the command context
     * @return 1 if successful
     */
    private static int setJumpBoost(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        int level = IntegerArgumentType.getInteger(context, "level");

        // Create new data with updated jump boost
        PlayerSymbioteData newData = currentData.withBond(
                currentData.bonded(),
                currentData.bondTime(),
                currentData.tier(),
                currentData.dash(),
                currentData.featherFalling(),
                currentData.speed(),
                level
        );

        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, newData);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.jump_boost_set",
                level).withStyle(style -> style.withColor(0xF39C12)), false);

        return 1;
    }

    // ==================== Dream Commands ====================

    /**
     * Trigger a dream for the player.
     */
    private static int triggerDream(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        boolean success = DreamInsightEngine.processDream(player);

        if (success) {
            source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.dream_triggered")
                    .withStyle(style -> style.withColor(0x9B59B6)), false);
        }

        return success ? 1 : 0;
    }

    /**
     * Force a dream at a specific level.
     */
    private static int forceDream(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (!currentData.bonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        int levelNum = IntegerArgumentType.getInteger(context, "level");
        DreamLevel level = DreamLevel.fromOrdinal(levelNum - 1); // Convert 1-3 to 0-2

        boolean success = DreamInsightEngine.forceDream(player, level);

        if (success) {
            source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.dream_forced", level.name())
                    .withStyle(style -> style.withColor(0x9B59B6)), false);
        }

        return success ? 1 : 0;
    }

    // ==================== Voice Debug Commands ====================

    /**
     * Reset all voice cooldowns.
     */
    private static int resetCooldowns(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteVoiceService.resetCooldowns(player);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.cooldowns_reset")
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    /**
     * Show progress signals.
     */
    private static int showProgress(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        String summary = ProgressSignalUpdater.getProgressSummary(player);

        // Split by newlines and send each line
        for (String line : summary.split("\n")) {
            if (!line.isBlank()) {
                source.sendSuccess(() -> Component.literal(line)
                        .withStyle(style -> style.withColor(0x9B59B6)), false);
            }
        }

        return 1;
    }

    /**
     * Set a progress signal state.
     */
    private static int setProgressState(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        String concept = StringArgumentType.getString(context, "concept");
        String stateStr = StringArgumentType.getString(context, "state");

        SignalState state;
        try {
            state = SignalState.valueOf(stateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.invalid_state", stateStr));
            return 0;
        }

        ProgressSignalUpdater.forceState(player, concept, state);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.progress_set", concept, state.name())
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    // ==================== Formatting Helpers ====================

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
