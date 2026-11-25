package net.j40climb.florafauna.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.j40climb.florafauna.debug.DebugOverlay;
import net.j40climb.florafauna.noclip.ClippingEntity;
import net.j40climb.florafauna.noclip.NoClipClientState;
import net.j40climb.florafauna.noclip.NoClipPayload;
import net.j40climb.florafauna.setup.ClientSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class FabricClientCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                literal("florafauna")
                        .requires(source -> source.getPlayer().isCreative() ||
                                source.getPlayer().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(literal("debug").executes(context -> debug(context.getSource(), !DebugOverlay.isEnabled()))
                                .then(literal("on").executes(context -> debug(context.getSource(), true)))
                                .then(literal("off").executes(context -> debug(context.getSource(), false))))
                        .then(literal("noclip").executes(context -> noclip(context.getSource(), !NoClipClientState.isEnabled()))
                                .then(literal("on").executes(context -> noclip(context.getSource(), true)))
                                .then(literal("off").executes(context -> noclip(context.getSource(), false))))));
    }

    private static int debug(FabricClientCommandSource source, boolean enabled) {
        DebugOverlay.setEnabled(enabled);
        source.sendFeedback(Component.translatable(enabled ? "command.florafauna.symbiote.debug_enabled" :
                "command.florafauna.symbiote.debug_disabled").withColor(enabled ? 0x2ECC71 : 0xE74C3C));
        return 1;
    }

    private static int noclip(FabricClientCommandSource source, boolean enabled) {
        var player = source.getPlayer();
        if (!player.isCreative()) {
            source.sendError(Component.translatable("command.florafauna.noclip.creative_only"));
            return 0;
        }
        NoClipClientState.setEnabled(enabled);
        if (player instanceof ClippingEntity clipping) clipping.setClipping(enabled);
        ClientSetup.sendToServer(new NoClipPayload(enabled));
        source.sendFeedback(Component.translatable(enabled ? "command.florafauna.noclip.enabled" :
                "command.florafauna.noclip.disabled").withColor(enabled ? 0x2ECC71 : 0xE74C3C));
        return 1;
    }
}
