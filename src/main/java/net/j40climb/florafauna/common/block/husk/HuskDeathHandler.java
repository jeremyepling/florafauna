package net.j40climb.florafauna.common.block.husk;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Event handler for player death when bonded to a symbiote.
 *
 * On death:
 * - Creates a RESTORATION husk if BONDED_ACTIVE
 * - Creates a CONTAINER husk if BONDED_WEAKENED
 * - Stores player inventory in the husk
 * - Clears player inventory (prevents normal drops)
 * - Updates player state to BONDED_WEAKENED
 * - Tracks restoration husk location if applicable
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class HuskDeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Only process if player has a bonded symbiote
        if (!data.symbioteState().isBonded()) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos deathPos = player.blockPosition();

        // Determine husk type based on current state
        HuskType huskType = (data.symbioteState() == SymbioteState.BONDED_ACTIVE)
                ? HuskType.RESTORATION
                : HuskType.CONTAINER;

        // Find valid placement position
        BlockPos huskPos = HuskPlacementHelper.findPlacementPosition(player, deathPos, level);

        // Place the husk block
        BlockState huskState = FloraFaunaRegistry.HUSK.get()
                .defaultBlockState()
                .setValue(HuskBlock.HUSK_TYPE, huskType);
        level.setBlock(huskPos, huskState, 3);

        // Initialize block entity with owner and inventory
        if (level.getBlockEntity(huskPos) instanceof HuskBlockEntity huskEntity) {
            huskEntity.setOwner(player.getUUID());
            huskEntity.populateFromPlayerInventory(player);
        }

        // Clear player inventory to prevent normal drops
        player.getInventory().clearContent();

        // Update player symbiote state
        PlayerSymbioteData updatedData = data.withSymbioteState(SymbioteState.BONDED_WEAKENED);

        // If this was a RESTORATION husk, track its location
        if (huskType == HuskType.RESTORATION) {
            updatedData = updatedData.withRestorationHusk(huskPos, level.dimension(), true);
        }

        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, updatedData);
    }
}
