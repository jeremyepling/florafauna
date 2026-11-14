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
        applyToHeldItem(context.player(), data.config);
    }

    public static void applyToHeldItem(Player player, ToolConfig config) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.has(FloraFaunaRegistry.TOOL_CONFIG)) return;
        stack.set(FloraFaunaRegistry.TOOL_CONFIG, config);
        var registries = player.level().registryAccess();
        Holder<Enchantment> fortune = registries.holderOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> silk = registries.holderOrThrow(Enchantments.SILK_TOUCH);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> {
            enchantments.set(fortune, config.fortune() ? 3 : 0);
            enchantments.set(silk, config.silkTouch() ? 1 : 0);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
