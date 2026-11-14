package net.j40climb.florafauna.common.block.mobtransport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.j40climb.florafauna.FloraFauna;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are in MobInput linking mode.
 * <p>
 * When a player shift+right-clicks a MobInput, they enter linking mode.
 * The next right-click on a MobOutput completes the pairing.
 * <p>
 * Uses player UUID as key for cross-dimension support.
 * ConcurrentHashMap for thread safety during server tick processing.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class MobInputLinkingState {

    private MobInputLinkingState() {
        // Utility class
    }

    // ConcurrentHashMap allows safe concurrent access from multiple threads
    private static final Map<UUID, GlobalPos> linkingPlayers = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearLinking(event.getEntity());
    }

    /**
     * Sets the MobInput position a player is linking from.
     *
     * @param player The player entering linking mode
     * @param inputPos The MobInput block position
     */
    public static void setLinkingFrom(Player player, BlockPos inputPos) {
        linkingPlayers.put(player.getUUID(), GlobalPos.of(player.level().dimension(), inputPos.immutable()));
    }

    /**
     * Gets the MobInput position a player is linking from.
     *
     * @param player The player to check
     * @return The MobInput position, or null if not in linking mode
     */
    @Nullable
    public static BlockPos getLinkingFrom(Player player) {
        GlobalPos origin = linkingPlayers.get(player.getUUID());
        if (origin == null) return null;
        if (origin.dimension().equals(player.level().dimension())) return origin.pos();
        clearLinking(player);
        return null;
    }

    /**
     * Clears the linking state for a player.
     *
     * @param player The player to clear
     */
    public static void clearLinking(Player player) {
        linkingPlayers.remove(player.getUUID());
    }

    /**
     * Checks if a player is in linking mode.
     *
     * @param player The player to check
     * @return true if in linking mode
     */
    public static boolean isLinking(Player player) {
        return linkingPlayers.containsKey(player.getUUID());
    }

    /**
     * Clears all linking states.
     * Called on server shutdown or world unload.
     */
    public static void clearAll() {
        linkingPlayers.clear();
    }
}
