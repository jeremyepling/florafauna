package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Display data only. The live server-side dialog owns the selection and confirms custody. */
public record DeliveryPopupPayload(UUID dog, List<Choice> choices, Optional<Preview> preview)
        implements CustomPacketPayload {
    public record Choice(String address, Component label) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Choice> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(36), Choice::address, ComponentSerialization.STREAM_CODEC, Choice::label, Choice::new);
    }

    public record Preview(Choice destination, Identifier dimension, int seconds) {
        private static final StreamCodec<RegistryFriendlyByteBuf, Preview> CODEC = StreamCodec.composite(
                Choice.CODEC, Preview::destination, Identifier.STREAM_CODEC, Preview::dimension,
                ByteBufCodecs.VAR_INT, Preview::seconds, Preview::new);
    }

    public static final Type<DeliveryPopupPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "delivery_popup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryPopupPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DeliveryPopupPayload::dog,
            Choice.CODEC.apply(ByteBufCodecs.list()), DeliveryPopupPayload::choices,
            Preview.CODEC.apply(ByteBufCodecs::optional), DeliveryPopupPayload::preview,
            DeliveryPopupPayload::new);

    @Override
    public Type<DeliveryPopupPayload> type() {
        return TYPE;
    }
}
