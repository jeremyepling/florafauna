package net.j40climb.florafauna.common.block.husk;

import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles player interaction with Symbiotic Husk blocks.
 *
 * Interaction rules:
 * - Only the owner can interact
 * - Transfer items to player inventory
 * - Partial transfer allowed (remaining items stay in husk)
 * - Restoration husks also restore symbiote abilities
 * - Block transitions to BROKEN when empty
 */
public final class HuskInteractionHandler {
    private HuskInteractionHandler() {} // Utility class

    /**
     * Handle right-click interaction with a husk.
     *
     * @param player The player interacting
     * @param husk The husk block entity
     * @param state The current block state
     * @param level The level
     * @param pos The block position
     * @return InteractionResult indicating success or failure
     */
    public static InteractionResult handleInteraction(
            ServerPlayer player,
            HuskBlockEntity husk,
            BlockState state,
            Level level,
            BlockPos pos
    ) {
        // Owner-only check
        if (!husk.isOwner(player)) {
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.husk_not_owner"),
                    false
            );
            return InteractionResult.FAIL;
        }

        HuskType huskType = state.getValue(HuskBlock.HUSK_TYPE);

        // Broken husks have no interaction
        if (huskType == HuskType.BROKEN) {
            return InteractionResult.PASS;
        }

        // Transfer items to player
        husk.transferItemsToPlayer(player);

        // If this is a restoration husk, restore symbiote abilities
        if (huskType == HuskType.RESTORATION) {
            restoreSymbiote(player);
        }

        // The block state transition to BROKEN is handled by HuskBlockEntity.checkAndTransitionToBroken()
        // which is called when contents change

        return InteractionResult.SUCCESS;
    }

    /**
     * Restore the player's symbiote to BONDED_ACTIVE state.
     * Called when interacting with a RESTORATION husk.
     */
    private static void restoreSymbiote(ServerPlayer player) {
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Only restore if currently weakened
        if (data.symbioteState() != SymbioteState.BONDED_WEAKENED) {
            return;
        }

        // Restore to BONDED_ACTIVE and clear restoration husk tracking
        PlayerSymbioteData restored = data
                .withSymbioteState(SymbioteState.BONDED_ACTIVE)
                .clearRestorationHusk();

        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, restored);

        // Display restoration message
        player.displayClientMessage(
                Component.translatable("symbiote.florafauna.abilities_restored")
                        .withStyle(style -> style.withColor(0x9B59B6).withItalic(true)),
                false
        );
    }
}
