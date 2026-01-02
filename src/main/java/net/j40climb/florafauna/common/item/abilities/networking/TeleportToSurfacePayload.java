package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public enum TeleportToSurfacePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<TeleportToSurfacePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "teleport_to_surface_payload"));
    public static final StreamCodec<ByteBuf, TeleportToSurfacePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final TeleportToSurfacePayload data, final IPayloadContext context) {
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
                player.displayClientMessage(Component.translatable("ability.florafauna.teleport_surface.success"), true);
                return;
            }
        }

        // No surface found
        player.displayClientMessage(Component.translatable("ability.florafauna.teleport_surface.no_surface"), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
