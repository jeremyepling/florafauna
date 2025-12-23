package net.j40climb.florafauna.common.network.payloadandhandlers;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.event.SymbioteAbilityEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload to sync jump key state from client to server.
 * Used to enable variable jump height based on how long the jump key is held.
 */
public record JumpStatePayload(boolean isJumping) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JumpStatePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "jump_state_payload"));

    public static final StreamCodec<ByteBuf, JumpStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            JumpStatePayload::isJumping,
            JumpStatePayload::new
    );

    public static void onServerReceived(final JumpStatePayload data, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        if (data.isJumping) {
            SymbioteAbilityEvents.setPlayerJumping(player, true);
        } else {
            SymbioteAbilityEvents.setPlayerJumping(player, false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
