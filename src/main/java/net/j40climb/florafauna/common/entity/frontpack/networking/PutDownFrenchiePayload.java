package net.j40climb.florafauna.common.entity.frontpack.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.entity.RegisterEntities;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frontpack.FrontpackData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network payload for putting down a carried Frenchie.
 * Client sends this when player shift-right-clicks air while carrying a Frenchie.
 * Server handles by spawning the Frenchie from NBT and clearing the attachment.
 */
public enum PutDownFrenchiePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<PutDownFrenchiePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "put_down_frenchie"));

    public static final StreamCodec<ByteBuf, PutDownFrenchiePayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final PutDownFrenchiePayload data, final IPayloadContext context) {
        Player player = context.player();

        // Check if player is carrying a Frenchie
        FrontpackData frontpackData = player.getData(RegisterAttachmentTypes.FRENCH_FRONTPACK_DATA);
        if (!frontpackData.hasCarriedFrenchie()) return;

        // Spawn Frenchie in front of player
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0));

        FrenchieEntity frenchie = RegisterEntities.FRENCHIE.get().create(player.level(), EntitySpawnReason.TRIGGERED);
        if (frenchie == null) return;

        // Restore data from NBT using ValueInput
        ValueInput input = TagValueInput.create(
            ProblemReporter.DISCARDING,
            player.level().registryAccess(),
            frontpackData.frenchieNBT()
        );
        frenchie.load(input);
        frenchie.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0f);

        player.level().addFreshEntity(frenchie);

        // Clear attachment
        player.setData(RegisterAttachmentTypes.FRENCH_FRONTPACK_DATA, FrontpackData.DEFAULT);

        // Play sound
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.0f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
