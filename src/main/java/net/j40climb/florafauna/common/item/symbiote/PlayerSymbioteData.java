package net.j40climb.florafauna.common.item.symbiote;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Player attachment data for the symbiote system.
 * Combines symbiote bond state with cocoon chamber state.
 *
 * Symbiote fields:
 * - bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost
 *
 * Cocoon fields:
 * - symbioteBindable: whether player can bind (set by consuming symbiote_stew)
 * - cocoonSpawnPos/Dim: current cocoon spawn point
 * - previousBedSpawnPos/Dim: snapshot of bed spawn before setting cocoon spawn
 * - symbioteStewConsumedOnce, cocoonSpawnSetOnce: progression flags
 */
public record PlayerSymbioteData(
        // Symbiote bond state
        boolean bonded,
        long bondTime,
        int tier,
        boolean dash,
        boolean featherFalling,
        boolean speed,
        int jumpBoost,
        // Cocoon state
        boolean symbioteBindable,
        @Nullable BlockPos cocoonSpawnPos,
        @Nullable ResourceKey<Level> cocoonSpawnDim,
        @Nullable BlockPos previousBedSpawnPos,
        @Nullable ResourceKey<Level> previousBedSpawnDim,
        boolean symbioteStewConsumedOnce,
        boolean cocoonSpawnSetOnce
) {
    public static final Codec<PlayerSymbioteData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    // Symbiote fields
                    Codec.BOOL.fieldOf("bonded").forGetter(PlayerSymbioteData::bonded),
                    Codec.LONG.fieldOf("bondTime").forGetter(PlayerSymbioteData::bondTime),
                    Codec.INT.fieldOf("tier").forGetter(PlayerSymbioteData::tier),
                    Codec.BOOL.fieldOf("dash").forGetter(PlayerSymbioteData::dash),
                    Codec.BOOL.fieldOf("featherFalling").forGetter(PlayerSymbioteData::featherFalling),
                    Codec.BOOL.fieldOf("speed").forGetter(PlayerSymbioteData::speed),
                    Codec.INT.fieldOf("jumpBoost").forGetter(PlayerSymbioteData::jumpBoost),
                    // Cocoon fields
                    Codec.BOOL.fieldOf("symbioteBindable").forGetter(PlayerSymbioteData::symbioteBindable),
                    BlockPos.CODEC.optionalFieldOf("cocoonSpawnPos").forGetter(d -> Optional.ofNullable(d.cocoonSpawnPos)),
                    ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("cocoonSpawnDim").forGetter(d -> Optional.ofNullable(d.cocoonSpawnDim)),
                    BlockPos.CODEC.optionalFieldOf("previousBedSpawnPos").forGetter(d -> Optional.ofNullable(d.previousBedSpawnPos)),
                    ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("previousBedSpawnDim").forGetter(d -> Optional.ofNullable(d.previousBedSpawnDim)),
                    Codec.BOOL.fieldOf("symbioteStewConsumedOnce").forGetter(PlayerSymbioteData::symbioteStewConsumedOnce),
                    Codec.BOOL.fieldOf("cocoonSpawnSetOnce").forGetter(PlayerSymbioteData::cocoonSpawnSetOnce)
            ).apply(builder, PlayerSymbioteData::fromOptionals));

    private static PlayerSymbioteData fromOptionals(
            boolean bonded, long bondTime, int tier, boolean dash, boolean featherFalling, boolean speed, int jumpBoost,
            boolean symbioteBindable,
            Optional<BlockPos> cocoonSpawnPos, Optional<ResourceKey<Level>> cocoonSpawnDim,
            Optional<BlockPos> previousBedSpawnPos, Optional<ResourceKey<Level>> previousBedSpawnDim,
            boolean symbioteStewConsumedOnce, boolean cocoonSpawnSetOnce
    ) {
        return new PlayerSymbioteData(
                bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable,
                cocoonSpawnPos.orElse(null), cocoonSpawnDim.orElse(null),
                previousBedSpawnPos.orElse(null), previousBedSpawnDim.orElse(null),
                symbioteStewConsumedOnce, cocoonSpawnSetOnce
        );
    }

    public static final StreamCodec<ByteBuf, PlayerSymbioteData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerSymbioteData decode(ByteBuf buffer) {
            // Symbiote fields
            boolean bonded = ByteBufCodecs.BOOL.decode(buffer);
            long bondTime = ByteBufCodecs.VAR_LONG.decode(buffer);
            int tier = ByteBufCodecs.INT.decode(buffer);
            boolean dash = ByteBufCodecs.BOOL.decode(buffer);
            boolean featherFalling = ByteBufCodecs.BOOL.decode(buffer);
            boolean speed = ByteBufCodecs.BOOL.decode(buffer);
            int jumpBoost = ByteBufCodecs.INT.decode(buffer);

            // Cocoon fields
            boolean symbioteBindable = ByteBufCodecs.BOOL.decode(buffer);

            boolean hasCocoonSpawn = ByteBufCodecs.BOOL.decode(buffer);
            BlockPos cocoonSpawnPos = hasCocoonSpawn ? BlockPos.STREAM_CODEC.decode(buffer) : null;
            ResourceKey<Level> cocoonSpawnDim = hasCocoonSpawn ? ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer) : null;

            boolean hasPreviousBed = ByteBufCodecs.BOOL.decode(buffer);
            BlockPos previousBedSpawnPos = hasPreviousBed ? BlockPos.STREAM_CODEC.decode(buffer) : null;
            ResourceKey<Level> previousBedSpawnDim = hasPreviousBed ? ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer) : null;

            boolean symbioteStewConsumedOnce = ByteBufCodecs.BOOL.decode(buffer);
            boolean cocoonSpawnSetOnce = ByteBufCodecs.BOOL.decode(buffer);

            return new PlayerSymbioteData(
                    bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                    symbioteBindable, cocoonSpawnPos, cocoonSpawnDim,
                    previousBedSpawnPos, previousBedSpawnDim,
                    symbioteStewConsumedOnce, cocoonSpawnSetOnce
            );
        }

        @Override
        public void encode(ByteBuf buffer, PlayerSymbioteData data) {
            // Symbiote fields
            ByteBufCodecs.BOOL.encode(buffer, data.bonded);
            ByteBufCodecs.VAR_LONG.encode(buffer, data.bondTime);
            ByteBufCodecs.INT.encode(buffer, data.tier);
            ByteBufCodecs.BOOL.encode(buffer, data.dash);
            ByteBufCodecs.BOOL.encode(buffer, data.featherFalling);
            ByteBufCodecs.BOOL.encode(buffer, data.speed);
            ByteBufCodecs.INT.encode(buffer, data.jumpBoost);

            // Cocoon fields
            ByteBufCodecs.BOOL.encode(buffer, data.symbioteBindable);

            boolean hasCocoonSpawn = data.cocoonSpawnPos != null && data.cocoonSpawnDim != null;
            ByteBufCodecs.BOOL.encode(buffer, hasCocoonSpawn);
            if (hasCocoonSpawn) {
                BlockPos.STREAM_CODEC.encode(buffer, data.cocoonSpawnPos);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, data.cocoonSpawnDim);
            }

            boolean hasPreviousBed = data.previousBedSpawnPos != null && data.previousBedSpawnDim != null;
            ByteBufCodecs.BOOL.encode(buffer, hasPreviousBed);
            if (hasPreviousBed) {
                BlockPos.STREAM_CODEC.encode(buffer, data.previousBedSpawnPos);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, data.previousBedSpawnDim);
            }

            ByteBufCodecs.BOOL.encode(buffer, data.symbioteStewConsumedOnce);
            ByteBufCodecs.BOOL.encode(buffer, data.cocoonSpawnSetOnce);
        }
    };

    public static final PlayerSymbioteData DEFAULT = new PlayerSymbioteData(
            false, 0L, 1, false, false, false, 0,
            false, null, null, null, null, false, false
    );

    // Symbiote builder methods

    /**
     * Creates a new PlayerSymbioteData with all symbiote bond fields.
     */
    public PlayerSymbioteData withBond(boolean bonded, long bondTime, int tier,
                                        boolean dash, boolean featherFalling, boolean speed, int jumpBoost) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withBonded(boolean bonded, long bondTime) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withTier(int tier) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withDash(boolean dash) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withFeatherFalling(boolean featherFalling) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withSpeed(boolean speed) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withJumpBoost(int jumpBoost) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    // Cocoon builder methods

    public PlayerSymbioteData withSymbioteBindable(boolean bindable) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                bindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withSymbioteStewConsumedOnce(boolean consumed) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                consumed, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withCocoonSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, pos, dim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withPreviousBedSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, pos, dim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce);
    }

    public PlayerSymbioteData withCocoonSpawnSetOnce(boolean set) {
        return new PlayerSymbioteData(bonded, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, set);
    }

    /**
     * Creates a PlayerSymbioteData from an item's SymbioteData (for binding).
     * Preserves cocoon state from existing player data.
     */
    public PlayerSymbioteData withSymbioteFromItem(SymbioteData itemData, long currentGameTime) {
        return new PlayerSymbioteData(
                true, currentGameTime, itemData.tier(), itemData.dash(), itemData.featherFalling(), itemData.speed(), itemData.jumpBoost(),
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce
        );
    }

    /**
     * Creates a SymbioteData for storing on an item (for unbinding).
     */
    public SymbioteData toItemData() {
        return new SymbioteData(false, 0L, tier, dash, featherFalling, speed, jumpBoost);
    }

    /**
     * Resets symbiote bond state while preserving cocoon state.
     */
    public PlayerSymbioteData withSymbioteReset() {
        return new PlayerSymbioteData(
                false, 0L, 1, false, false, false, 0,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce
        );
    }
}
