package net.j40climb.florafauna.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.projectile.ThrownItemEntity;
import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload sent from client to server when the throw key is pressed.
 * Spawns a ThrownItemEntity from the held item if it has the THROWABLE_ABILITY component.
 */
public record ThrowItemPayload(boolean mainHand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ThrowItemPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "throw_item"));

    public static final StreamCodec<ByteBuf, ThrowItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ThrowItemPayload::mainHand,
            ThrowItemPayload::new
    );

    public static void onServerReceived(final ThrowItemPayload data, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        InteractionHand hand = data.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);

        // Check if item has throwable ability
        if (!stack.has(FloraFaunaRegistry.THROWABLE_ABILITY)) {
            return;
        }

        ThrowableAbilityData abilityData = stack.get(FloraFaunaRegistry.THROWABLE_ABILITY);
        if (abilityData == null) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // Check if player already has a thrown item of this type in the world
        boolean alreadyThrown = level.getEntities(
                FloraFaunaRegistry.THROWN_ITEM.get(),
                entity -> entity.getOwner() == player && ItemStack.isSameItem(entity.getThrownItem(), stack)
        ).stream().findAny().isPresent();

        if (alreadyThrown) {
            return; // Can't throw another until the previous one returns
        }

        // Create the thrown entity
        ThrownItemEntity entity = new ThrownItemEntity(level, player, stack, abilityData);

        // Launch in direction player is looking
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.5f, 1.0f);

        // Add entity to world
        level.addFreshEntity(entity);

        // Play throw sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Remove item from hand (will return via entity or pickup)
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
