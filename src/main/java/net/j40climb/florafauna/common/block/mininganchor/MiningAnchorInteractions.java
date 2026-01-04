package net.j40climb.florafauna.common.block.mininganchor;

import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Handles player interactions with Mining Anchors.
 * - Right-click: Set as active waypoint
 * - Shift-right-click: Bind/unbind to symbiote
 */
public class MiningAnchorInteractions {

    /**
     * Handles right-click on a mining anchor.
     * Sets the anchor as the player's active waypoint.
     *
     * @param player The player interacting
     * @param pos The anchor position
     * @param level The level
     * @return true if the interaction was handled
     */
    public static boolean handleSetWaypoint(ServerPlayer player, BlockPos pos, Level level) {
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Must have a bonded symbiote to use waypoints
        if (!data.symbioteState().isBonded()) {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.requires_symbiote"),
                    true
            );
            return false;
        }

        ResourceKey<Level> dim = level.dimension();

        // Check if already set as waypoint - if so, clear it
        if (data.hasActiveWaypointAnchor() &&
            pos.equals(data.activeWaypointAnchorPos()) &&
            dim.equals(data.activeWaypointAnchorDim())) {

            // Clear waypoint
            PlayerSymbioteData newData = data.clearActiveWaypointAnchor();
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.waypoint_cleared"),
                    true
            );
        } else {
            // Set as waypoint
            PlayerSymbioteData newData = data.withActiveWaypointAnchor(pos, dim);
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.waypoint_set"),
                    true
            );
        }

        return true;
    }

    /**
     * Handles shift-right-click on a mining anchor.
     * Binds or unbinds the anchor to the player's symbiote.
     *
     * @param player The player interacting
     * @param pos The anchor position
     * @param level The level
     * @return true if the interaction was handled
     */
    public static boolean handleBindToggle(ServerPlayer player, BlockPos pos, Level level) {
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Must have a bonded symbiote to bind anchors
        if (!data.symbioteState().isBonded()) {
            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.requires_symbiote"),
                    true
            );
            return false;
        }

        ResourceKey<Level> dim = level.dimension();

        // Check if already bound to this anchor - if so, unbind
        if (data.hasAnchorBound() &&
            pos.equals(data.boundAnchorPos()) &&
            dim.equals(data.boundAnchorDim())) {

            // Unbind anchor (also clears waypoint if it was this anchor)
            PlayerSymbioteData newData = data.clearBoundAnchor();
            if (pos.equals(data.activeWaypointAnchorPos()) && dim.equals(data.activeWaypointAnchorDim())) {
                newData = newData.clearActiveWaypointAnchor();
            }
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.unbound"),
                    true
            );
        } else {
            // Bind anchor (also sets as waypoint automatically)
            PlayerSymbioteData newData = data
                    .withBoundAnchor(pos, dim)
                    .withActiveWaypointAnchor(pos, dim);
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, newData);

            player.displayClientMessage(
                    Component.translatable("message.florafauna.mining_anchor.bound"),
                    true
            );
        }

        return true;
    }
}
