package net.j40climb.florafauna.common.network.payloadandhandlers;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.util.Helpers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// How to send packet without data from https://discord.com/channels/313125603924639766/1301510569269919784/1320686104705372191
//      > don't know if you had a response to this, but the right way to handle packets with no payload is with StreamCodec.unit()
//      >  e.g. https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/common/network/PacketLeftClickEmpty.java
//      > (1.21.1 code but that hasn't changed in 1.21.4)
//      >  you don't have to use an enum there, but it's convenient for a singleton class like this, and it's important that the instance you give to StreamCodec.unit() is the same instance that you actually send over the network

public enum ToggleFortureAndSilkTouchPayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ToggleFortureAndSilkTouchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "toggle_fortune_and_silk_touch_payload"));
    public static final StreamCodec<ByteBuf, ToggleFortureAndSilkTouchPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final ToggleFortureAndSilkTouchPayload data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        Player player = context.player();
        toggleFortuneAndSilkTouch(player.getMainHandItem(), player.level(), player);
    }

    public static void toggleFortuneAndSilkTouch(ItemStack itemStack, Level level, Player player) {
        if (!level.isClientSide()) {

            Holder<Enchantment> silktouchHolder = level.registryAccess().holderOrThrow(Enchantments.SILK_TOUCH);

            Holder<Enchantment> fortuneHolder = level.registryAccess().holderOrThrow(Enchantments.FORTUNE);


            if (itemStack.getEnchantmentLevel(level.registryAccess().holderOrThrow(Enchantments.SILK_TOUCH)) > 0) {
                EnchantmentHelper.updateEnchantments(itemStack, itemEnchantment -> itemEnchantment.removeIf(enchantmentHolder -> enchantmentHolder.is(Enchantments.SILK_TOUCH)));
                if (Helpers.checkEnchantment(itemStack, fortuneHolder)) {
                    itemStack.enchant(fortuneHolder, 3);
                    player.displayClientMessage(Component.literal("Switched to Fortune 3"), true);
                }
            } else {
                EnchantmentHelper.updateEnchantments(itemStack, itemEnchantment -> itemEnchantment.removeIf(enchantmentHolder -> enchantmentHolder.is(Enchantments.FORTUNE)));
                if (Helpers.checkEnchantment(itemStack, silktouchHolder)) {
                    itemStack.enchant(silktouchHolder, 1);
                    player.displayClientMessage(Component.literal("Switched to Silk Touch"), true);
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}