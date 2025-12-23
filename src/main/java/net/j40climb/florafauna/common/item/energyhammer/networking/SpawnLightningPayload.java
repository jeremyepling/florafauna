package net.j40climb.florafauna.common.item.energyhammer.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnLightningPayload(BlockPos targetPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpawnLightningPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "spawn_lightning_payload"));
    public static final StreamCodec<ByteBuf, SpawnLightningPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SpawnLightningPayload::targetPos,
            SpawnLightningPayload::new
    );

    public static void onServerReceived(final SpawnLightningPayload data, final IPayloadContext context) {
        // Do something with the data, on the main thread
        Level level = context.player().level();
        spawnLightningBolt(level, data.targetPos());
    }

    public static void spawnLightningBolt(Level level, BlockPos targetPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return; // Only proceed on the server side
        }

        // Create the lightning bolt
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightningBolt != null) {
            lightningBolt.setPos(Vec3.atBottomCenterOf(targetPos)); // TODO not sure if this is correct. had to update for 1.21.5
            // Summon the lightning bolt
            serverLevel.addFreshEntity(lightningBolt);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}