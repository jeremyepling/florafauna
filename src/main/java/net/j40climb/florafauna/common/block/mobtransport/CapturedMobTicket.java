package net.j40climb.florafauna.common.block.mobtransport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a captured mob in the transport queue.
 * Contains all data needed to reconstruct the mob at the destination.
 * <p>
 * Full NBT preservation ensures names, equipment, variants, and modded data are retained.
 *
 * @param entityTypeId The Identifier of the entity type (e.g., "minecraft:cow")
 * @param entityNbt Full NBT data of the captured entity
 * @param capturedGameTime Game tick when the mob was captured
 * @param readyAtTick Game tick when the mob is ready for release
 * @param destinationPos The MobOutput block position where this mob will emerge (may be null)
 * @param destinationDim The dimension of the destination (may be null)
 * @param debugUUID Optional UUID for debugging/tracking
 */
public record CapturedMobTicket(
        Identifier entityTypeId,
        CompoundTag entityNbt,
        long capturedGameTime,
        long readyAtTick,
        @Nullable BlockPos destinationPos,
        @Nullable ResourceKey<Level> destinationDim,
        Optional<UUID> debugUUID
) {
    /**
     * Helper record for serializing position + dimension pairs.
     */
    private record DestinationPos(BlockPos pos, ResourceKey<Level> dim) {
        static final Codec<DestinationPos> CODEC = RecordCodecBuilder.create(b -> b.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(DestinationPos::pos),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(DestinationPos::dim)
        ).apply(b, DestinationPos::new));
    }

    public static final Codec<CapturedMobTicket> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Identifier.CODEC.fieldOf("entityType").forGetter(CapturedMobTicket::entityTypeId),
            CompoundTag.CODEC.fieldOf("entityNbt").forGetter(CapturedMobTicket::entityNbt),
            Codec.LONG.fieldOf("capturedAt").forGetter(CapturedMobTicket::capturedGameTime),
            Codec.LONG.fieldOf("readyAt").forGetter(CapturedMobTicket::readyAtTick),
            DestinationPos.CODEC.optionalFieldOf("destination").forGetter(t ->
                    t.destinationPos != null && t.destinationDim != null
                            ? Optional.of(new DestinationPos(t.destinationPos, t.destinationDim))
                            : Optional.empty()),
            Codec.STRING.optionalFieldOf("debugUUID").forGetter(t -> t.debugUUID.map(UUID::toString))
    ).apply(builder, CapturedMobTicket::fromCodec));

    private static CapturedMobTicket fromCodec(
            Identifier type, CompoundTag nbt, long captured, long ready,
            Optional<DestinationPos> dest, Optional<String> uuid) {
        BlockPos destPos = dest.map(DestinationPos::pos).orElse(null);
        ResourceKey<Level> destDim = dest.map(DestinationPos::dim).orElse(null);
        return new CapturedMobTicket(type, nbt, captured, ready, destPos, destDim,
                uuid.map(UUID::fromString));
    }

    /**
     * Creates a new ticket for a captured mob.
     *
     * @param entityType The entity type
     * @param entityNbt The serialized entity NBT
     * @param capturedAt The game tick when captured
     * @param readyAt The game tick when ready for release
     * @param destPos The destination block position (may be null)
     * @param destDim The destination dimension (may be null)
     * @return A new CapturedMobTicket
     */
    public static CapturedMobTicket create(
            EntityType<?> entityType,
            CompoundTag entityNbt,
            long capturedAt,
            long readyAt,
            @Nullable BlockPos destPos,
            @Nullable ResourceKey<Level> destDim
    ) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return new CapturedMobTicket(typeId, entityNbt, capturedAt, readyAt, destPos, destDim, Optional.empty());
    }

    /**
     * Returns true if this ticket is ready for release at the given tick.
     *
     * @param currentTick The current game tick
     * @return true if ready for release
     */
    public boolean isReady(long currentTick) {
        return currentTick >= readyAtTick;
    }

    /**
     * Creates a ticket for a mob that's ready immediately (for emergency release).
     */
    public CapturedMobTicket withImmediateRelease() {
        return new CapturedMobTicket(entityTypeId, entityNbt, capturedGameTime, 0L, destinationPos, destinationDim, debugUUID);
    }
}
