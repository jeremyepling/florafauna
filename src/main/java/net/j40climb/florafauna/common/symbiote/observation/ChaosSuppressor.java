package net.j40climb.florafauna.common.symbiote.observation;

import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Tracks rapid damage events and suppresses voice output during chaotic situations.
 * Prevents the symbiote from speaking when the player is taking heavy damage.
 */
public class ChaosSuppressor {
    /**
     * Number of damage events in the window to trigger suppression
     */
    private static final int DAMAGE_THRESHOLD = 5;

    /**
     * Time window in ticks (3 seconds = 60 ticks)
     */
    private static final int WINDOW_TICKS = 60;

    /**
     * Map of player UUID to list of damage event ticks
     */
    private static final Map<UUID, List<Long>> recentDamageEvents = new HashMap<>();

    /**
     * Record a damage event for a player.
     * Should be called whenever the player takes damage.
     *
     * @param player The player who took damage
     */
    public static void recordDamage(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long tick = player.level().getGameTime();

        recentDamageEvents.computeIfAbsent(uuid, k -> new ArrayList<>()).add(tick);

        // Prune old events to prevent memory leak
        pruneOldEvents(uuid, tick);
    }

    /**
     * Check if voice output should be suppressed due to chaos.
     *
     * @param player The player to check
     * @return true if voice should be suppressed
     */
    public static boolean isSuppressed(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long tick = player.level().getGameTime();

        pruneOldEvents(uuid, tick);

        List<Long> events = recentDamageEvents.get(uuid);
        return events != null && events.size() >= DAMAGE_THRESHOLD;
    }

    /**
     * Remove damage events older than the window.
     */
    private static void pruneOldEvents(UUID uuid, long currentTick) {
        List<Long> events = recentDamageEvents.get(uuid);
        if (events != null) {
            events.removeIf(t -> currentTick - t > WINDOW_TICKS);
            // Clean up empty lists
            if (events.isEmpty()) {
                recentDamageEvents.remove(uuid);
            }
        }
    }

    /**
     * Clear all tracking data for a player (e.g., on logout).
     *
     * @param player The player to clear
     */
    public static void clearPlayer(ServerPlayer player) {
        recentDamageEvents.remove(player.getUUID());
    }

    /**
     * Clear all tracking data (e.g., on server shutdown).
     */
    public static void clearAll() {
        recentDamageEvents.clear();
    }
}
