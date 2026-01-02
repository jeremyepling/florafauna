package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
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
 * Network packet to update tool configuration.
 * Sent from client to server when the player changes config settings.
 * Works with any item that has the TOOL_CONFIG data component.
 */
public record UpdateToolConfigPayload(ToolConfig config) implements CustomPacketPayload {

    public static final Type<UpdateToolConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "update_tool_config")
    );

    public static final StreamCodec<ByteBuf, UpdateToolConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ToolConfig.STREAM_CODEC,
            UpdateToolConfigPayload::config,
            UpdateToolConfigPayload::new
    );

    public static void onServerReceived(final UpdateToolConfigPayload data, final IPayloadContext context) {
        Player player = context.player();
        ItemStack heldItem = player.getMainHandItem();

        // Check if the held item has the TOOL_CONFIG component (component-based check)
        if (heldItem.has(FloraFaunaRegistry.TOOL_CONFIG)) {
            // Update the config on the held item
            heldItem.set(FloraFaunaRegistry.TOOL_CONFIG, data.config);

            // Apply enchantments based on config
            applyEnchantments(heldItem, data.config, player);
        }
    }

    private static void applyEnchantments(ItemStack itemStack, ToolConfig config, Player player) {
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
