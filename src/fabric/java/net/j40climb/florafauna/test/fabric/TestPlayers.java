package net.j40climb.florafauna.test.fabric;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** A non-ticking server player, equivalent to the NeoForge FakePlayer used by shared unit fixtures. */
public final class TestPlayers {
    private TestPlayers() {}
    public static ServerPlayer create(ServerLevel level) {
        var profile = new GameProfile(UUID.randomUUID(), "test-player");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(Packet<?> packet) {}
            @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener) {}
        };
        return player;
    }
}
