package net.j40climb.florafauna.common.block.cocoonchamber.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-to-client payload to open the Cocoon Chamber screen.
 * <p>
 * This follows the vanilla pattern for button-only block GUIs:
 * Server sends packet -> Client opens screen directly (no Menu system).
 * <p>
 * The Menu system is only needed for inventory slot synchronization.
 * Since CocoonChamber has no slots, we use direct screen opening.
 *
 * @see CocoonActionPayload for client-to-server action packets
 */
public record OpenCocoonScreenPayload(BlockPos chamberPos) implements CustomPacketPayload {

    public static final Type<OpenCocoonScreenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "open_cocoon_screen")
    );

    public static final StreamCodec<ByteBuf, OpenCocoonScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpenCocoonScreenPayload::chamberPos,
            OpenCocoonScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Called on CLIENT when server sends this packet.
     * Opens the CocoonChamberScreen directly.
     */
    public static void onClientReceived(final OpenCocoonScreenPayload data, final IPayloadContext context) {
        // Must run on main thread for screen opening
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new CocoonChamberScreen(data.chamberPos));
        });
    }
}
