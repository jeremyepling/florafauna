package net.j40climb.florafauna.common.block.cocoonchamber.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlock;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonProgressionHooks;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.SymbioteBindingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network payload for Cocoon Chamber actions.
 * Sent from client screen to server when player clicks an action button.
 */
public record CocoonActionPayload(CocoonAction action, BlockPos chamberPos) implements CustomPacketPayload {

    public enum CocoonAction {
        SET_SPAWN,
        CLEAR_SPAWN,
        BIND,
        UNBIND
    }

    public static final Type<CocoonActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "cocoon_action")
    );

    public static final StreamCodec<ByteBuf, CocoonActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> CocoonAction.values()[i], CocoonAction::ordinal),
            CocoonActionPayload::action,
            BlockPos.STREAM_CODEC,
            CocoonActionPayload::chamberPos,
            CocoonActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void onServerReceived(final CocoonActionPayload data, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        // Validate: block exists and is CocoonChamber
        if (!(player.level().getBlockState(data.chamberPos).getBlock() instanceof CocoonChamberBlock)) {
            return;
        }

        // Validate: player in range
        if (player.distanceToSqr(data.chamberPos.getX() + 0.5, data.chamberPos.getY() + 0.5, data.chamberPos.getZ() + 0.5) > 64.0) {
            return;
        }

        switch (data.action) {
            case SET_SPAWN -> handleSetSpawn(player, data.chamberPos);
            case CLEAR_SPAWN -> handleClearSpawn(player);
            case BIND -> handleBind(player, data.chamberPos);
            case UNBIND -> handleUnbind(player);
        }
    }

    private static void handleSetSpawn(ServerPlayer player, BlockPos chamberPos) {
        PlayerSymbioteData data = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);
        ResourceKey<Level> currentDim = player.level().dimension();

        // Snapshot current bed spawn (before we change it)
        ServerPlayer.RespawnConfig currentConfig = player.getRespawnConfig();
        BlockPos currentRespawnPos = null;
        ResourceKey<Level> currentRespawnDim = null;
        if (currentConfig != null && currentConfig.respawnData() != null) {
            currentRespawnPos = currentConfig.respawnData().pos();
            currentRespawnDim = currentConfig.respawnData().dimension();
        }

        // Only snapshot if there's actually a bed spawn set (and it's not already a cocoon spawn)
        PlayerSymbioteData updatedData;
        if (currentRespawnPos != null && data.cocoonSpawnPos() == null) {
            updatedData = data.withPreviousBedSpawn(currentRespawnPos, currentRespawnDim);
        } else {
            updatedData = data;
        }

        // Set cocoon spawn
        updatedData = updatedData
                .withCocoonSpawn(chamberPos, currentDim)
                .withCocoonSpawnSetOnce(true);

        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, updatedData);

        // Set vanilla respawn to cocoon position (forced=true for explicit spawn point)
        LevelData.RespawnData respawnData = LevelData.RespawnData.of(currentDim, chamberPos, 0f, 0f);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawnData, true), true);

        // Trigger progression hook
        CocoonProgressionHooks.onCocoonSpawnSet(player, chamberPos);

        // Send feedback
        player.displayClientMessage(
                Component.translatable("symbiote.florafauna.spawn_set")
                        .withStyle(style -> style.withColor(0x9B59B6)),
                false
        );
    }

    private static void handleClearSpawn(ServerPlayer player) {
        PlayerSymbioteData data = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        if (data.cocoonSpawnPos() == null) {
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.no_cocoon_spawn"),
                    true
            );
            return;
        }

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

        // Clear cocoon spawn data
        PlayerSymbioteData updatedData = data.withCocoonSpawn(null, null);
        player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, updatedData);

        player.displayClientMessage(
                Component.translatable("symbiote.florafauna.spawn_cleared")
                        .withStyle(style -> style.withColor(0x9B59B6)),
                false
        );
    }

    private static void handleBind(ServerPlayer player, BlockPos chamberPos) {
        PlayerSymbioteData data = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA);

        // Check bindable state
        if (!data.symbioteBindable()) {
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.not_bindable")
                            .withStyle(style -> style.withColor(0xFF6B6B)),
                    false
            );
            return;
        }

        // Find dormant symbiote in inventory
        ItemStack symbioteItem = SymbioteBindingHelper.findDormantSymbioteInInventory(player);
        if (symbioteItem.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.no_dormant_symbiote")
                            .withStyle(style -> style.withColor(0xFF6B6B)),
                    false
            );
            return;
        }

        // Perform binding
        SymbioteBindingHelper.BindResult result = SymbioteBindingHelper.bindSymbiote(player, symbioteItem);

        if (result.success()) {
            // Clear bindable state - re-fetch data since binding may have modified it
            PlayerSymbioteData updatedData = player.getData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA)
                    .withSymbioteBindable(false);
            player.setData(RegisterAttachmentTypes.PLAYER_SYMBIOTE_DATA, updatedData);

            // Trigger progression hook
            CocoonProgressionHooks.onSymbioteBound(player);
        }

        // Send feedback
        player.displayClientMessage(
                Component.translatable(result.messageKey())
                        .withStyle(style -> style.withColor(result.success() ? 0x9B59B6 : 0xFF6B6B)),
                false
        );
    }

    private static void handleUnbind(ServerPlayer player) {
        SymbioteBindingHelper.UnbindResult result = SymbioteBindingHelper.unbindSymbiote(player);

        if (result.success()) {
            // Add the dormant symbiote item to player inventory
            if (!player.getInventory().add(result.symbioteItem())) {
                // If inventory is full, drop the item
                player.drop(result.symbioteItem(), false);
            }

            // Trigger progression hook
            CocoonProgressionHooks.onSymbioteUnbound(player);
        }

        // Send feedback
        player.displayClientMessage(
                Component.translatable(result.messageKey())
                        .withStyle(style -> style.withColor(result.success() ? 0x9B59B6 : 0xFF6B6B)),
                false
        );
    }
}
