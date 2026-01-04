package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.common.block.mininganchor.networking.AnchorFillStatePayload;
import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService;
import net.j40climb.florafauna.common.symbiote.voice.VoiceTier;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles symbiote dialogue triggers and fill state sync for Mining Anchors.
 */
public class MiningAnchorDialogueEvents {

    /**
     * Called when an anchor's fill state changes.
     * Notifies all players that have this anchor as their waypoint.
     *
     * @param level The server level
     * @param anchorPos The anchor position
     * @param oldState The previous fill state
     * @param newState The new fill state
     */
    public static void onFillStateChanged(
            ServerLevel level,
            BlockPos anchorPos,
            AnchorFillState oldState,
            AnchorFillState newState
    ) {
        // Find all players who have this anchor as their active waypoint
        for (ServerPlayer player : level.players()) {
            var data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

            // Check if this is the player's active waypoint anchor
            if (!data.hasActiveWaypointAnchor()) {
                continue;
            }
            if (!anchorPos.equals(data.activeWaypointAnchorPos())) {
                continue;
            }
            if (!level.dimension().equals(data.activeWaypointAnchorDim())) {
                continue;
            }

            // Send fill state update to client
            PacketDistributor.sendToPlayer(player, new AnchorFillStatePayload(anchorPos, newState));

            // Trigger dialogue based on state transition
            triggerDialogue(player, oldState, newState);
        }
    }

    /**
     * Triggers symbiote dialogue for fill state transitions.
     *
     * @param player The player to send dialogue to
     * @param oldState The previous fill state
     * @param newState The new fill state
     */
    private static void triggerDialogue(ServerPlayer player, AnchorFillState oldState, AnchorFillState newState) {
        // Only trigger if transitioning to a more concerning state
        if (oldState == AnchorFillState.NORMAL && newState == AnchorFillState.WARNING) {
            // Transitioning to 75% full - warn the player
            SymbioteVoiceService.trySpeak(
                    player,
                    VoiceTier.TIER_1_AMBIENT,
                    ObservationCategory.MINING_ANCHOR,
                    "symbiote.dialogue.anchor_warn_75"
            );
        } else if (newState == AnchorFillState.FULL && oldState != AnchorFillState.FULL) {
            // Just reached 100% full - alert the player
            SymbioteVoiceService.trySpeak(
                    player,
                    VoiceTier.TIER_1_AMBIENT,
                    ObservationCategory.MINING_ANCHOR,
                    "symbiote.dialogue.anchor_full_100"
            );
        }
    }
}
