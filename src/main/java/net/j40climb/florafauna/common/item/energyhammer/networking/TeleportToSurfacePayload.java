package net.j40climb.florafauna.common.item.energyhammer.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// How to send packet without data from https://discord.com/channels/313125603924639766/1301510569269919784/1320686104705372191
//      > don't know if you had a response to this, but the right way to handle packets with no payload is with StreamCodec.unit()
//      >  e.g. https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/common/network/PacketLeftClickEmpty.java
//      > (1.21.1 code but that hasn't changed in 1.21.4)
//      >  you don't have to use an enum there, but it's convenient for a singleton class like this, and it's important that the instance you give to StreamCodec.unit() is the same instance that you actually send over the network

public enum TeleportToSurfacePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<TeleportToSurfacePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "teleport_to_surface_payload"));
    public static final StreamCodec<ByteBuf, TeleportToSurfacePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final TeleportToSurfacePayload data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        Player player = context.player();
        teleportToSurface(player);
    }

    public static void teleportToSurface(Player player) {
        Level level = player.level();
        if(level.isClientSide()) return;

        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos scanPos = playerPos.mutable();

        // Scan upward for the surface. Start by looking at the block above the player's head (i.e., +2)
        for (int y = playerPos.getY() + 2; y < level.getHeight(); y++) {
            scanPos.setY(y);
            if(!level.getBlockState(scanPos).isAir() &&
                    level.getBlockState(scanPos.above()).isAir() &&
                    level.getBlockState(scanPos.above(2)).isAir()) {
                // Safe position found

                // Calculate the center of the block and one up to account for feet
                double centerX = scanPos.getX() + 0.5;
                double centerY = scanPos.getY() + 1.5;
                double centerZ = scanPos.getZ() + 0.5;

                player.teleportTo(centerX, centerY, centerZ);
                player.displayClientMessage(Component.literal("Teleported to surface!"), true);
                return;
            }
        }

        // No surface found
        player.displayClientMessage(Component.literal("No safe surface above!"), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}