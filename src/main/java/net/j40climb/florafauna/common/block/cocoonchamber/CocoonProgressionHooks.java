package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.common.symbiote.observation.ObservationArbiter;
import net.j40climb.florafauna.common.symbiote.observation.ObservationCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Progression hooks for the Cocoon Chamber system.
 * These methods are called at key moments to trigger dialogue, advancements, and story flags.
 * Implement narrative/progression logic here without modifying the core cocoon mechanics.
 */
public final class CocoonProgressionHooks {

    private CocoonProgressionHooks() {} // Utility class

    /**
     * Called when a player consumes symbiote_stew for the first time.
     * This prepares them for binding with the symbiote.
     *
     * @param player the server player who consumed the stew
     */
    public static void onSymbioteStewConsumed(ServerPlayer player) {
        // Trigger observation for dialogue/advancement
        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 50, Map.of(
                "event", "stew_consumed"
        ));
    }

    /**
     * Called when a player sets their spawn point to a Cocoon Chamber.
     *
     * @param player the server player who set spawn
     * @param pos the position of the Cocoon Chamber
     */
    public static void onCocoonSpawnSet(ServerPlayer player, BlockPos pos) {
        // Trigger observation for dialogue/advancement
        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 40, Map.of(
                "event", "cocoon_spawn_set"
        ));
    }

    /**
     * Called when a player successfully binds with a symbiote via the Cocoon Chamber.
     *
     * @param player the server player who bonded
     */
    public static void onSymbioteBound(ServerPlayer player) {
        // Trigger observation for dialogue/advancement
        // Note: This is a Tier 2 breakthrough moment (severity 100)
        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 100, Map.of(
                "event", "bonded"
        ));
    }

    /**
     * Called when a player unbinds from their symbiote via the Cocoon Chamber.
     *
     * @param player the server player who unbonded
     */
    public static void onSymbioteUnbound(ServerPlayer player) {
        // Trigger observation for dialogue/advancement
        ObservationArbiter.observe(player, ObservationCategory.BONDING_MILESTONE, 70, Map.of(
                "event", "unbonded"
        ));
    }
}
