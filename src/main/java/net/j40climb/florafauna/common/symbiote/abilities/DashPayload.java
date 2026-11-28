package net.j40climb.florafauna.common.symbiote.abilities;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.neoforged.neoforge.network.handling.IPayloadContext;
//?} else {
/*import net.j40climb.florafauna.platform.network.IPayloadContext;
*///?}

import java.util.Map;
import java.util.WeakHashMap;

public enum DashPayload implements CustomPacketPayload {
    INSTANCE;

    // A transient grant belongs to this player entity and this dash only.
    private static final Map<ServerPlayer, Integer> DASH_START_TICKS = new WeakHashMap<>();

    public static final Type<DashPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "dash_payload"));
    public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(DashPayload data, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            dash(player);
        }
    }

    public static void dash(ServerPlayer player) {
        PlayerSymbioteData symbiote = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbiote.symbioteState().areAbilitiesActive() || !symbiote.dash()) {
            return;
        }
        player.setDeltaMovement(player.getViewVector(1.0F).scale(3.0));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.resetFallDistance();
        DASH_START_TICKS.put(player, player.tickCount);
    }

    public static boolean consumeLanding(ServerPlayer player) {
        return DASH_START_TICKS.remove(player) != null;
    }

    public static void expireLanding(ServerPlayer player) {
        Integer start = DASH_START_TICKS.get(player);
        if (start == null) return;
        // Allow a grounded launch to leave the ledge before its first movement update.
        if ((player.onGround() && player.tickCount - start > 2) || player.isInLiquid()
                || player.onClimbable() || player.isPassenger() || player.getAbilities().flying) {
            DASH_START_TICKS.remove(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}