package net.j40climb.florafauna.common.block.mobtransport;

import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Handles symbiote dialogue triggers for the Mob Transport system.
 */
public class MobTransportDialogueEvents {

    private static final double DIALOGUE_RANGE = 16.0;

    /**
     * Called when a mob is captured by a MobInput block.
     * Notifies nearby bonded players via symbiote voice.
     *
     * @param level The server level
     * @param capturePos The position where the mob was captured
     */
    public static void onMobCaptured(ServerLevel level, BlockPos capturePos) {
        // Find nearby players who have a bonded symbiote
        AABB searchBox = new AABB(capturePos).inflate(DIALOGUE_RANGE);

        for (ServerPlayer player : level.players()) {
            if (!searchBox.contains(player.position())) {
                continue;
            }

            var data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
            if (!data.symbioteState().isBonded()) {
                continue;
            }

            // Trigger dialogue for the capture event
            SymbioteVoiceService.trySpeak(
                    player,
                    VoiceTier.TIER_1_AMBIENT,
                    ObservationCategory.MOB_TRANSPORT,
                    "symbiote.dialogue.mob_input_capture"
            );
        }
    }
}
