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
            source.sendFailure(Component.literal("This command can only be run by a player!"));
            return 0;
        }

        // Get the player's symbiote data
        SymbioteData data = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        // Display the data
        source.sendSuccess(() -> Component.literal("=== Symbiote Data ===")
                .withStyle(style -> style.withColor(0x9B59B6).withBold(true)), false);

        source.sendSuccess(() -> Component.literal("Bonded: " + (data.bonded() ? "Yes" : "No"))
                .withStyle(style -> style.withColor(data.bonded() ? 0x2ECC71 : 0xE74C3C)), false);

        if (data.bonded()) {
            source.sendSuccess(() -> Component.literal("Bond Time: " + data.bondTime()), false);
            source.sendSuccess(() -> Component.literal("Tier: " + data.tier()), false);
            source.sendSuccess(() -> Component.literal("Energy: " + data.energy()), false);
            source.sendSuccess(() -> Component.literal("Health: " + data.health() + "/100")
                    .withStyle(style -> style.withColor(getHealthColor(data.health()))), false);
            source.sendSuccess(() -> Component.literal("Ability Multiplier: " +
                    String.format("%.2f%%", data.getAbilityMultiplier() * 100)), false);
        }

        return 1;
    }

    /**
     * Gets a color for the health value (red to green gradient).
     *
     * @param health the health value (0-100)
     * @return the color as an RGB integer
     */
    private static int getHealthColor(int health) {
        if (health >= 75) return 0x2ECC71; // Green
        if (health >= 50) return 0xF39C12; // Orange
        if (health >= 25) return 0xE67E22; // Dark orange
        return 0xE74C3C; // Red
    }
}
