package net.j40climb.florafauna.client.renderer;

import net.j40climb.florafauna.common.attachments.FrenchFrontpackData;
import net.j40climb.florafauna.common.attachments.ModAttachmentTypes;
import net.j40climb.florafauna.common.entity.client.frenchie.FrenchieVariant;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side manager for caching frontpack render state data extracted from player attachments.
 * This bridges the gap between the attachment system and the render layer,
 * since AvatarRenderState doesn't contain a direct player reference.
 */
public class FrontpackRenderStateManager {

    // Use entity ID as key since AvatarRenderState has 'id' field
    private static final Map<Integer, FrontpackRenderState> FRONTPACK_STATES = new HashMap<>();

    /**
     * Extracts frontpack data from a player's FrenchFrontpackData attachment
     * and caches it for rendering.
     */
    public static void extractFromPlayer(Player player) {
        FrenchFrontpackData data = player.getData(ModAttachmentTypes.FRENCH_FRONTPACK_DATA);

        FrontpackRenderState state = new FrontpackRenderState();
        state.hasFrontpack = data.hasCarriedFrenchie();

        if (state.hasFrontpack && data.frenchieNBT() != null) {
            // Extract variant ID from NBT (matches how FrenchieEntity stores it)
            // getInt returns Optional<Integer> in newer versions
            int variantId = data.frenchieNBT().getInt("Variant").orElse(0);
            state.variant = FrenchieVariant.byId(variantId);
        } else {
            // Default to FAWN if no valid data
            state.variant = FrenchieVariant.FAWN;
        }

        FRONTPACK_STATES.put(player.getId(), state);
    }

    /**
     * Gets the cached frontpack render state for a player by entity ID.
     * Returns a default empty state if no data is cached.
     */
    public static FrontpackRenderState getState(int entityId) {
        return FRONTPACK_STATES.getOrDefault(entityId, new FrontpackRenderState());
    }

    /**
     * Simple data holder for frontpack render state.
     */
    public static class FrontpackRenderState {
        public boolean hasFrontpack = false;
        public FrenchieVariant variant = FrenchieVariant.FAWN;
    }
}
