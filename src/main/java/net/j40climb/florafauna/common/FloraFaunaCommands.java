package net.j40climb.florafauna.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.SymbioteBindingHelper;
import net.j40climb.florafauna.common.item.symbiote.SymbioteState;
import net.j40climb.florafauna.common.item.symbiote.dream.DreamInsightEngine;
import net.j40climb.florafauna.common.item.symbiote.dream.DreamLevel;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationArbiter;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalUpdater;
import net.j40climb.florafauna.common.item.symbiote.progress.SignalState;
import net.j40climb.florafauna.common.item.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Centralized command registration for the FloraFauna mod.
 * All commands use /florafauna as the base command.
 *
 * Usage:
 *   /florafauna symbiote check - Display current symbiote data
 *   /florafauna symbiote bond - Bond a symbiote to the player
 *   /florafauna symbiote unbond - Unbond the symbiote from the player
 *   /florafauna symbiote reset - Reset the symbiote completely
 *   /florafauna symbiote toggle <ability> - Toggle an ability
 *   /florafauna symbiote setJumpBoost <0-4> - Set jump boost level
 *   /florafauna symbiote dream - Trigger a dream insight
 *   /florafauna symbiote dream force <1-3> - Force a dream at a specific level
 *   /florafauna symbiote cooldown reset - Reset all voice cooldowns
 *   /florafauna symbiote progress - Show progress signals
 *   /florafauna symbiote progress set <concept> <state> - Set a progress state
 */
public class FloraFaunaCommands {

    /**
     * Registers all mod commands.
     *
     * @param dispatcher the command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("florafauna")
                        .then(Commands.literal("symbiote")
                                .then(Commands.literal("check")
                                        .executes(FloraFaunaCommands::checkSymbiote))
                                .then(Commands.literal("bond")
                                        .executes(FloraFaunaCommands::bondSymbiote))
                                .then(Commands.literal("unbond")
                                        .executes(FloraFaunaCommands::unbondSymbiote))
                                .then(Commands.literal("reset")
                                        .executes(FloraFaunaCommands::resetSymbiote))
                                .then(Commands.literal("toggle")
                                        .then(Commands.argument("ability", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("dash");
                                                    builder.suggest("featherFalling");
                                                    builder.suggest("speed");
                                                    return builder.buildFuture();
                                                })
                                                .executes(FloraFaunaCommands::toggleAbility)))
                                .then(Commands.literal("setJumpBoost")
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                                                .executes(FloraFaunaCommands::setJumpBoost)))
                                .then(Commands.literal("dream")
                                        .executes(FloraFaunaCommands::triggerDream)
                                        .then(Commands.literal("force")
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                                        .executes(FloraFaunaCommands::forceDream))))
                                .then(Commands.literal("cooldown")
                                        .then(Commands.literal("reset")
                                                .executes(FloraFaunaCommands::resetCooldowns)))
                                .then(Commands.literal("progress")
                                        .executes(FloraFaunaCommands::showProgress)
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("concept", StringArgumentType.word())
                                                        .then(Commands.argument("state", StringArgumentType.word())
                                                                .suggests((context, builder) -> {
                                                                    for (SignalState state : SignalState.values()) {
                                                                        builder.suggest(state.name().toLowerCase());
                                                                    }
                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(FloraFaunaCommands::setProgressState))))))
        );
    }

    // ==================== Symbiote Commands ====================

    private static int checkSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Display the header
        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.header")
                .withStyle(style -> style.withColor(0x9B59B6).withBold(true)), false);

        // Display symbiote state
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.state",
                data.symbioteState().getSerializedName()), false);

        // Display all bond fields if bonded
        if (data.symbioteState().isBonded()) {
            source.sendSuccess(() -> formatField("command.florafauna.symbiote.abilities_active",
                    formatBoolean(data.symbioteState().areAbilitiesActive())), false);
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

        // Display cocoon state section
        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.cocoon_header")
                .withStyle(style -> style.withColor(0x9B59B6).withBold(true)), false);

        source.sendSuccess(() -> formatField("command.florafauna.symbiote.bindable",
                formatBoolean(data.symbioteBindable())), false);
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.stew_consumed",
                formatBoolean(data.symbioteStewConsumedOnce())), false);
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.cocoon_set",
                formatBoolean(data.cocoonSpawnSetOnce())), false);
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.cocoon_spawn",
                formatPosAndDim(data.cocoonSpawnPos(), data.cocoonSpawnDim())), false);
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.previous_bed",
                formatPosAndDim(data.previousBedSpawnPos(), data.previousBedSpawnDim())), false);

        // Display restoration husk section
        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.husk_header")
                .withStyle(style -> style.withColor(0x9B59B6).withBold(true)), false);

        source.sendSuccess(() -> formatField("command.florafauna.symbiote.husk_active",
                formatBoolean(data.restorationHuskActive())), false);
        source.sendSuccess(() -> formatField("command.florafauna.symbiote.husk_position",
                formatPosAndDim(data.restorationHuskPos(), data.restorationHuskDim())), false);

        return 1;
    }

    private static int bondSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (currentData.symbioteState().isBonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.already_bonded"));
            return 0;
        }

        PlayerSymbioteData newData = currentData.withBond(
                SymbioteState.BONDED_ACTIVE,
                player.level().getGameTime(),
                1,
                false, false, false, 0
        );
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);
        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, ProgressSignalTracker.DEFAULT);
        player.setData(FloraFaunaRegistry.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 100, Map.of(
                "event", "bonded"
        ));

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.bonded_success")
                .withStyle(style -> style.withColor(0x2ECC71)), false);
        return 1;
    }

    private static int unbondSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        SymbioteBindingHelper.UnbindResult result = SymbioteBindingHelper.unbindSymbiote(player);

        if (!result.success()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        if (!player.getInventory().add(result.symbioteItem())) {
            player.drop(result.symbioteItem(), false);
        }

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.unbonded_success")
                .withStyle(style -> style.withColor(0xE74C3C)), false);
        return 1;
    }

    private static int resetSymbiote(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        PlayerSymbioteData resetData = currentData.withSymbioteReset();
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, resetData);
        player.setData(FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, ProgressSignalTracker.DEFAULT);
        player.setData(FloraFaunaRegistry.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.reset_success")
                .withStyle(style -> style.withColor(0xF39C12)), false);
        return 1;
    }

    private static int toggleAbility(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        String ability = StringArgumentType.getString(context, "ability");
        PlayerSymbioteData newData;

        switch (ability.toLowerCase()) {
            case "dash":
                newData = currentData.withDash(!currentData.dash());
                break;
            case "featherfalling":
                newData = currentData.withFeatherFalling(!currentData.featherFalling());
                break;
            case "speed":
                newData = currentData.withSpeed(!currentData.speed());
                break;
            default:
                source.sendFailure(Component.translatable("command.florafauna.symbiote.invalid_ability", ability));
                return 0;
        }

        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

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

    private static int setJumpBoost(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        int level = IntegerArgumentType.getInteger(context, "level");
        PlayerSymbioteData newData = currentData.withJumpBoost(level);
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

        source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.jump_boost_set",
                level).withStyle(style -> style.withColor(0xF39C12)), false);

        return 1;
    }

    // ==================== Dream Commands ====================

    private static int triggerDream(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
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

    private static int forceDream(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        PlayerSymbioteData currentData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        if (!currentData.symbioteState().isBonded()) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.not_bonded"));
            return 0;
        }

        int levelNum = IntegerArgumentType.getInteger(context, "level");
        DreamLevel level = DreamLevel.fromOrdinal(levelNum - 1);

        boolean success = DreamInsightEngine.forceDream(player, level);

        if (success) {
            source.sendSuccess(() -> Component.translatable("command.florafauna.symbiote.dream_forced", level.name())
                    .withStyle(style -> style.withColor(0x9B59B6)), false);
        }

        return success ? 1 : 0;
    }

    // ==================== Voice Debug Commands ====================

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

    private static int showProgress(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.florafauna.symbiote.player_only"));
            return 0;
        }

        String summary = ProgressSignalUpdater.getProgressSummary(player);

        for (String line : summary.split("\n")) {
            if (!line.isBlank()) {
                source.sendSuccess(() -> Component.literal(line)
                        .withStyle(style -> style.withColor(0x9B59B6)), false);
            }
        }

        return 1;
    }

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

    private static Component formatField(String labelKey, String value) {
        return Component.translatable(labelKey).append(": " + value);
    }

    private static String formatBoolean(boolean value) {
        return Component.translatable(value ?
                "command.florafauna.symbiote.yes" :
                "command.florafauna.symbiote.no").getString();
    }

    private static String formatEnabledDisabled(boolean value) {
        return Component.translatable(value ?
                "command.florafauna.symbiote.enabled" :
                "command.florafauna.symbiote.disabled").getString();
    }

    private static String formatPosAndDim(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        if (pos == null || dim == null) {
            return Component.translatable("command.florafauna.symbiote.none").getString();
        }
        String dimName = dim.identifier().getPath();
        return String.format("%d, %d, %d (%s)", pos.getX(), pos.getY(), pos.getZ(), dimName);
    }
}
