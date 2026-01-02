package net.j40climb.florafauna.common.block.cocoonchamber;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles respawn validation for Cocoon Chamber spawns.
 * If the cocoon spawn point is invalid (block destroyed), restores previous bed spawn.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class CocoonRespawnHandler {

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Only check if player has cocoon spawn set
        if (data.cocoonSpawnPos() == null || data.cocoonSpawnDim() == null) {
            return;
        }

        // Validate cocoon spawn is still valid
        if (!isCocoonSpawnValid(player, data.cocoonSpawnPos(), data.cocoonSpawnDim())) {
            // Cocoon spawn is invalid - restore previous bed spawn
            restorePreviousBedSpawn(player, data);
        }
    }

    /**
     * Checks if the cocoon spawn point is still valid (block exists and is a CocoonChamber).
     */
    private static boolean isCocoonSpawnValid(ServerPlayer player, BlockPos pos, ResourceKey<Level> dim) {
        // Get the server level for the cocoon spawn dimension
        ServerLevel targetLevel = player.level().getServer().getLevel(dim);
        if (targetLevel == null) {
            return false;
        }

        // Check if the chunk is loaded - if not, assume it's valid (don't force-load)
        if (!targetLevel.isLoaded(pos)) {
            return true; // Assume valid if not loaded
        }

        // Check if the block at the position is a CocoonChamberBlock
        return targetLevel.getBlockState(pos).getBlock() instanceof CocoonChamberBlock;
    }

    /**
     * Restores the previous bed spawn point and clears the cocoon spawn data.
     */
    private static void restorePreviousBedSpawn(ServerPlayer player, PlayerSymbioteData data) {
        // Restore previous bed spawn if available
        if (data.previousBedSpawnPos() != null && data.previousBedSpawnDim() != null) {
            LevelData.RespawnData respawnData = LevelData.RespawnData.of(
                    data.previousBedSpawnDim(),
                    data.previousBedSpawnPos(),
                    0f, 0f
            );
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawnData, true), true);
        } else {
            // Clear respawn entirely (will use world spawn)
            player.setRespawnPosition(null, true);
        }

        // Clear cocoon spawn data but preserve previous bed spawn for future use
        PlayerSymbioteData updatedData = data.withCocoonSpawn(null, null);
        player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, updatedData);

        // Notify player
        player.displayClientMessage(
                Component.translatable("symbiote.florafauna.cocoon_destroyed")
                        .withStyle(style -> style.withColor(0xFF6B6B)),
                false
        );
    }
}
