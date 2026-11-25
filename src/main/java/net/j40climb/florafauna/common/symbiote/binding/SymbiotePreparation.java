package net.j40climb.florafauna.common.symbiote.binding;

import net.j40climb.florafauna.common.block.cocoonchamber.CocoonProgressionHooks;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Records permanent readiness after pollen is consumed. */
public final class SymbiotePreparation {
    private SymbiotePreparation() {}

    public static boolean prepare(ServerPlayer player) {
        PlayerSymbioteData data = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (data.symbioteBindable()) {
            player.sendOverlayMessage(Component.translatable("symbiote.florafauna.already_prepared"));
            return false;
        }
        if (data.symbioteState().isBonded()) {
            player.sendOverlayMessage(Component.translatable("symbiote.florafauna.already_bonded"));
            return false;
        }
        net.j40climb.florafauna.platform.AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA,
                data.withSymbioteBindable(true).withPollenConsumedOnce(true));
        CocoonProgressionHooks.onSymbiotePrepared(player);
        SymbioteOnboarding.onPrepared(player);
        return true;
    }
}
