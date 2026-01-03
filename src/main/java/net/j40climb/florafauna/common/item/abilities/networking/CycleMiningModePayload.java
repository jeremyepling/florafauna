package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload sent from client to server when the cycle mining mode key is pressed.
 * Cycles the mining mode on the held item if it has the MULTI_BLOCK_MINING component.
 */
public enum CycleMiningModePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<CycleMiningModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "cycle_mining_mode"));

    public static final StreamCodec<ByteBuf, CycleMiningModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final CycleMiningModePayload data, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();

        // Check if item has multi-block mining component
        if (!stack.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) {
            return;
        }

        // Cycle to next mode
        stack.update(
                FloraFaunaRegistry.MULTI_BLOCK_MINING,
                MiningModeData.DEFAULT,
                MiningModeData::getNextMode
        );

        // Display the new mode to the player
        MiningModeData currentMode = stack.get(FloraFaunaRegistry.MULTI_BLOCK_MINING);
        if (currentMode != null) {
            player.displayClientMessage(Component.literal(currentMode.getMiningModeString()), true);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
