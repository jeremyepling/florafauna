package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectHammerActionPayload(int slot, HammerAction action) implements CustomPacketPayload {
    public static final Type<SelectHammerActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "select_hammer_action"));
    public static final StreamCodec<ByteBuf, SelectHammerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectHammerActionPayload::slot,
            HammerAction.STREAM_CODEC, SelectHammerActionPayload::action, SelectHammerActionPayload::new);

    public static void onServerReceived(SelectHammerActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        var stack = player.getMainHandItem();
        if (player.getInventory().getSelectedSlot() == payload.slot && stack.has(FloraFaunaRegistry.HAMMER_ACTION)) {
            stack.set(FloraFaunaRegistry.HAMMER_ACTION, payload.action);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
