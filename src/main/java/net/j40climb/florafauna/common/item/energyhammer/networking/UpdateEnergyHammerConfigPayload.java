package net.j40climb.florafauna.common.item.energyhammer.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.j40climb.florafauna.common.item.energyhammer.EnergyHammerConfig;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network packet to update the Energy Hammer configuration.
 * Sent from client to server when the player changes config settings.
 */
public record UpdateEnergyHammerConfigPayload(EnergyHammerConfig config) implements CustomPacketPayload {

    public static final Type<UpdateEnergyHammerConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "update_energy_hammer_config")
    );

    public static final StreamCodec<ByteBuf, UpdateEnergyHammerConfigPayload> STREAM_CODEC = StreamCodec.composite(
            EnergyHammerConfig.STREAM_CODEC,
            UpdateEnergyHammerConfigPayload::config,
            UpdateEnergyHammerConfigPayload::new
    );

    public static void onServerReceived(final UpdateEnergyHammerConfigPayload data, final IPayloadContext context) {
        Player player = context.player();
        ItemStack heldItem = player.getMainHandItem();

        // Verify the player is holding an energy hammer
        if (heldItem.is(RegisterItems.ENERGY_HAMMER.get())) {
            // Update the config on the held item
            heldItem.set(RegisterDataComponentTypes.ENERGY_HAMMER_CONFIG, data.config);

            // Apply enchantments based on config
            applyEnchantments(heldItem, data.config, player);
        }
    }

    private static void applyEnchantments(ItemStack itemStack, EnergyHammerConfig config, Player player) {
        Holder<Enchantment> silkTouchHolder = player.level().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH);
        Holder<Enchantment> fortuneHolder = player.level().registryAccess().holderOrThrow(Enchantments.FORTUNE);

        // Remove both enchantments first
        EnchantmentHelper.updateEnchantments(itemStack, enchantments -> {
            enchantments.removeIf(holder -> holder.is(Enchantments.SILK_TOUCH) || holder.is(Enchantments.FORTUNE));
        });

        // Apply the appropriate enchantment based on config
        if (config.fortune()) {
            itemStack.enchant(fortuneHolder, 3);
        } else if (config.silkTouch()) {
            itemStack.enchant(silkTouchHolder, 1);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
