package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public record ChargedThrowPayload(int action) implements CustomPacketPayload {
    public static final int PRESS = 0, RELEASE = 1, CANCEL = 2;
    public static final int HOLD_TICKS = 20;
    public static final Type<ChargedThrowPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "charged_throw"));
    public static final StreamCodec<ByteBuf, ChargedThrowPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ChargedThrowPayload::action, ChargedThrowPayload::new);

    public record Hold(ItemStack hammer, ResourceKey<Level> dimension, long start) {}

    public static void handle(ChargedThrowPayload request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) key(player, request.action);
    }

    public static void key(ServerPlayer player, int action) {
        ItemStack stack = player.getMainHandItem();
        var hold = player.getExistingDataOrNull(FloraFaunaRegistry.THROW_HOLD);
        long now = player.level().getGameTime();
        if (action == PRESS) {
            if (hold == null && stack.is(FloraFaunaRegistry.HAMMER.get()) && stack.has(FloraFaunaRegistry.THROWABLE_ABILITY)
                    && player.isAlive() && !player.isSpectator()) {
                player.setData(FloraFaunaRegistry.THROW_HOLD, new Hold(stack, player.level().dimension(), now));
            }
        } else {
            player.removeData(FloraFaunaRegistry.THROW_HOLD);
            if (action == RELEASE && hold != null && hold.hammer == stack && hold.dimension == player.level().dimension() && player.isAlive() && !player.isSpectator()) {
                ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND, now - hold.start >= HOLD_TICKS);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var hold = player.getExistingDataOrNull(FloraFaunaRegistry.THROW_HOLD);
        if (hold == null) return;
        if (hold.hammer != player.getMainHandItem() || hold.dimension != player.level().dimension() || !player.isAlive() || player.isSpectator()) {
            player.removeData(FloraFaunaRegistry.THROW_HOLD);
        } else if (player.level().getGameTime() - hold.start == HOLD_TICKS) {
            player.sendOverlayMessage(Component.translatable("ability.florafauna.ore_strike.ready"));
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}