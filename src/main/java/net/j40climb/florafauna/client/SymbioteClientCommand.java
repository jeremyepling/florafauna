package net.j40climb.florafauna.client;

import com.mojang.brigadier.CommandDispatcher;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-side commands for the FloraFauna mod.
 * These commands only affect client-side state and don't require server communication.
 *
 * Usage:
 *   /florafauna debug - Toggle debug overlay
 *   /florafauna debug on - Enable debug overlay
 *   /florafauna debug off - Disable debug overlay
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class SymbioteClientCommand {

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("florafauna")
                        .then(Commands.literal("debug")
                                .executes(context -> toggleDebug(context.getSource()))
                                .then(Commands.literal("on")
                                        .executes(context -> setDebug(context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(context -> setDebug(context.getSource(), false))))
        );
    }

    private static int toggleDebug(CommandSourceStack source) {
        SymbioteDebugOverlay.toggle();
        boolean newState = SymbioteDebugOverlay.isEnabled();

        source.sendSuccess(() -> Component.translatable(
                newState ? "command.florafauna.symbiote.debug_enabled" : "command.florafauna.symbiote.debug_disabled"
        ).withStyle(style -> style.withColor(newState ? 0x2ECC71 : 0xE74C3C)), false);

        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        SymbioteDebugOverlay.setEnabled(enabled);

        source.sendSuccess(() -> Component.translatable(
                enabled ? "command.florafauna.symbiote.debug_enabled" : "command.florafauna.symbiote.debug_disabled"
        ).withStyle(style -> style.withColor(enabled ? 0x2ECC71 : 0xE74C3C)), false);

        return 1;
    }
}
