package net.j40climb.florafauna.platform.network;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
public final class PacketDistributor {
 public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player,payload); }
 public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos pos, CustomPacketPayload payload) {
  for (ServerPlayer player:PlayerLookup.tracking(level,pos)) ServerPlayNetworking.send(player,payload);
 }
}
