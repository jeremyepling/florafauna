package net.j40climb.florafauna.client.renderer;

import net.j40climb.florafauna.common.attachments.FrenchieBackpackData;
import net.j40climb.florafauna.common.attachments.ModAttachmentTypes;
import net.j40climb.florafauna.common.entity.client.frenchie.FrenchieVariant;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side manager for caching backpack render state data extracted from player attachments.
 * This bridges the gap between the attachment system and the render layer,
 * since AvatarRenderState doesn't contain a direct player reference.
 */
public class BackpackRenderStateManager {

    // Use entity ID as key since AvatarRenderState has 'id' field
    private static final Map<Integer, BackpackRenderState> BACKPACK_STATES = new HashMap<>();

    /**
     * Extracts backpack data from a player's FrenchieBackpackData attachment
     * and caches it for rendering.
     */
    public static void extractFromPlayer(Player player) {
        FrenchieBackpackData data = player.getData(ModAttachmentTypes.FRENCHIE_BACKPACK_DATA);

        BackpackRenderState state = new BackpackRenderState();
        state.hasBackpack = data.hasCarriedFrenchie();

        if (state.hasBackpack && data.frenchieNBT() != null) {
            // Extract variant ID from NBT (matches how FrenchieEntity stores it)
            // getInt returns Optional<Integer> in newer versions
            int variantId = data.frenchieNBT().getInt("Variant").orElse(0);
            state.variant = FrenchieVariant.byId(variantId);
        } else {
            // Default to FAWN if no valid data
            state.variant = FrenchieVariant.FAWN;
        }

        BACKPACK_STATES.put(player.getId(), state);
    }

    /**
     * Gets the cached backpack render state for a player by entity ID.
     * Returns a default empty state if no data is cached.
     */
    public static BackpackRenderState getState(int entityId) {
        return BACKPACK_STATES.getOrDefault(entityId, new BackpackRenderState());
    }

    /**
     * Simple data holder for backpack render state.
     */
    public static class BackpackRenderState {
        public boolean hasBackpack = false;
        public FrenchieVariant variant = FrenchieVariant.FAWN;
    }
}
