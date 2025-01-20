package net.j40climb.florafauna.network.payloadandhandlers;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.component.MiningSpeed;
import net.j40climb.florafauna.component.ModDataComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// How to send packet without data from https://discord.com/channels/313125603924639766/1301510569269919784/1320686104705372191
//      > don't know if you had a response to this, but the right way to handle packets with no payload is with StreamCodec.unit()
//      >  e.g. https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/common/network/PacketLeftClickEmpty.java
//      > (1.21.1 code but that hasn't changed in 1.21.4)
//      >  you don't have to use an enum there, but it's convenient for a singleton class like this, and it's important that the instance you give to StreamCodec.unit() is the same instance that you actually send over the network

public record SetMiningSpeedPayload(MiningSpeed miningSpeed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetMiningSpeedPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "set_mining_speed_payload"));
    public static final StreamCodec<ByteBuf, SetMiningSpeedPayload> STREAM_CODEC = StreamCodec.composite(
            MiningSpeed.STREAM_CODEC,
            SetMiningSpeedPayload::miningSpeed,
            SetMiningSpeedPayload::new
    );

    public static void onServerReceived(final SetMiningSpeedPayload data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        Player player = context.player();
        setMiningSpeed(player.getMainHandItem(), data.miningSpeed, player.level(), player);
    }

    public static void setMiningSpeed(ItemStack itemStack, MiningSpeed miningSpeed, Level level, Player player) {
        if (!level.isClientSide()) {
            itemStack.set(ModDataComponentTypes.MINING_SPEED, miningSpeed);
            player.displayClientMessage(Component.literal("Switched to " + miningSpeed.name()), true);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
