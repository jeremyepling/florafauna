package net.j40climb.florafauna.common.item.symbiote.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// How to send packet without data from https://discord.com/channels/313125603924639766/1301510569269919784/1320686104705372191
//      > don't know if you had a response to this, but the right way to handle packets with no payload is with StreamCodec.unit()
//      >  e.g. https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/common/network/PacketLeftClickEmpty.java
//      > (1.21.1 code but that hasn't changed in 1.21.4)
//      >  you don't have to use an enum there, but it's convenient for a singleton class like this, and it's important that the instance you give to StreamCodec.unit() is the same instance that you actually send over the network

public enum DashPayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<DashPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "dash_payload"));
    public static final StreamCodec<ByteBuf, DashPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final DashPayload data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        Player player = context.player();
        dash(player);
    }

    public static void dash(Player player) {
        int multiplier = 3;
        // Get the player's looking direction as a vector
            Vec3 lookDirection = player.getViewVector(1.0F);
        // Define the strength of the burst, adjust this value to change how strong the burst should be
        double addedStrength = (double) multiplier / 2;
        double burstStrength = 1.5 + addedStrength;
        // Set the player's motion based on the look direction and burst strength
        player.setDeltaMovement(lookDirection.x * burstStrength, lookDirection.y * burstStrength, lookDirection.z * burstStrength);
        ((ServerPlayer) player).connection.send(new ClientboundSetEntityMotionPacket(player));
        player.resetFallDistance();
        // Optionally, you could add some effects or sounds here
        //PacketDistributor.sendToPlayer((ServerPlayer) player, new ClientSoundPayload(SoundEvents.FIRECHARGE_USE.getLocation(), 0.5f, 0.125f));
        //level.playSound(player, player.getOnPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 0.125f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}