package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PrecisionMiningPayload(boolean held) implements CustomPacketPayload {
    public static final Type<PrecisionMiningPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "precision_mining"));
    public static final StreamCodec<ByteBuf, PrecisionMiningPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(PrecisionMiningPayload::new, PrecisionMiningPayload::held);

    public static void onServerReceived(PrecisionMiningPayload payload, IPayloadContext context) {
        context.player().setData(FloraFaunaRegistry.PRECISION_MINING, payload.held());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
