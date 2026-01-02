package net.j40climb.florafauna.common.symbiote.data;

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
 * Combines symbiote bond state with cocoon chamber state and restoration husk tracking.
 *
 * Symbiote fields:
 * - symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost
 *
 * Cocoon fields:
 * - symbioteBindable: whether player can bind (set by consuming symbiote_stew)
 * - cocoonSpawnPos/Dim: current cocoon spawn point
 * - previousBedSpawnPos/Dim: snapshot of bed spawn before setting cocoon spawn
 * - symbioteStewConsumedOnce, cocoonSpawnSetOnce: progression flags
 *
 * Restoration Husk fields:
 * - restorationHuskPos/Dim: location of the player's restoration husk
 * - restorationHuskActive: whether the husk is still active
 */
public record PlayerSymbioteData(
        // Symbiote bond state
        SymbioteState symbioteState,
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
        boolean cocoonSpawnSetOnce,
        // Restoration Husk tracking
        @Nullable BlockPos restorationHuskPos,
        @Nullable ResourceKey<Level> restorationHuskDim,
        boolean restorationHuskActive
) {
    /**
     * Helper record for serializing position + dimension pairs as a single optional field.
     */
    private record DimensionPos(BlockPos pos, ResourceKey<Level> dim) {
        static final Codec<DimensionPos> CODEC = RecordCodecBuilder.create(b -> b.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(DimensionPos::pos),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(DimensionPos::dim)
        ).apply(b, DimensionPos::new));
    }

    public static final Codec<PlayerSymbioteData> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    // Symbiote fields (7)
                    SymbioteState.CODEC.fieldOf("symbioteState").forGetter(PlayerSymbioteData::symbioteState),
                    Codec.LONG.fieldOf("bondTime").forGetter(PlayerSymbioteData::bondTime),
                    Codec.INT.fieldOf("tier").forGetter(PlayerSymbioteData::tier),
                    Codec.BOOL.fieldOf("dash").forGetter(PlayerSymbioteData::dash),
                    Codec.BOOL.fieldOf("featherFalling").forGetter(PlayerSymbioteData::featherFalling),
                    Codec.BOOL.fieldOf("speed").forGetter(PlayerSymbioteData::speed),
                    Codec.INT.fieldOf("jumpBoost").forGetter(PlayerSymbioteData::jumpBoost),
                    // Cocoon fields (5)
                    Codec.BOOL.fieldOf("symbioteBindable").forGetter(PlayerSymbioteData::symbioteBindable),
                    DimensionPos.CODEC.optionalFieldOf("cocoonSpawn").forGetter(d ->
                            d.cocoonSpawnPos != null && d.cocoonSpawnDim != null
                                    ? Optional.of(new DimensionPos(d.cocoonSpawnPos, d.cocoonSpawnDim))
                                    : Optional.empty()),
                    DimensionPos.CODEC.optionalFieldOf("previousBedSpawn").forGetter(d ->
                            d.previousBedSpawnPos != null && d.previousBedSpawnDim != null
                                    ? Optional.of(new DimensionPos(d.previousBedSpawnPos, d.previousBedSpawnDim))
                                    : Optional.empty()),
                    Codec.BOOL.fieldOf("symbioteStewConsumedOnce").forGetter(PlayerSymbioteData::symbioteStewConsumedOnce),
                    Codec.BOOL.fieldOf("cocoonSpawnSetOnce").forGetter(PlayerSymbioteData::cocoonSpawnSetOnce),
                    // Restoration Husk fields (2)
                    DimensionPos.CODEC.optionalFieldOf("restorationHusk").forGetter(d ->
                            d.restorationHuskPos != null && d.restorationHuskDim != null
                                    ? Optional.of(new DimensionPos(d.restorationHuskPos, d.restorationHuskDim))
                                    : Optional.empty()),
                    Codec.BOOL.fieldOf("restorationHuskActive").forGetter(PlayerSymbioteData::restorationHuskActive)
            ).apply(builder, PlayerSymbioteData::fromCodec));

    private static PlayerSymbioteData fromCodec(
            SymbioteState symbioteState, long bondTime, int tier, boolean dash, boolean featherFalling, boolean speed, int jumpBoost,
            boolean symbioteBindable,
            Optional<DimensionPos> cocoonSpawn,
            Optional<DimensionPos> previousBedSpawn,
            boolean symbioteStewConsumedOnce, boolean cocoonSpawnSetOnce,
            Optional<DimensionPos> restorationHusk,
            boolean restorationHuskActive
    ) {
        return new PlayerSymbioteData(
                symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable,
                cocoonSpawn.map(DimensionPos::pos).orElse(null),
                cocoonSpawn.map(DimensionPos::dim).orElse(null),
                previousBedSpawn.map(DimensionPos::pos).orElse(null),
                previousBedSpawn.map(DimensionPos::dim).orElse(null),
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHusk.map(DimensionPos::pos).orElse(null),
                restorationHusk.map(DimensionPos::dim).orElse(null),
                restorationHuskActive
        );
    }

    public static final StreamCodec<ByteBuf, PlayerSymbioteData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerSymbioteData decode(ByteBuf buffer) {
            // Symbiote fields
            SymbioteState symbioteState = SymbioteState.STREAM_CODEC.decode(buffer);
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

            // Restoration Husk fields
            boolean hasRestorationHusk = ByteBufCodecs.BOOL.decode(buffer);
            BlockPos restorationHuskPos = hasRestorationHusk ? BlockPos.STREAM_CODEC.decode(buffer) : null;
            ResourceKey<Level> restorationHuskDim = hasRestorationHusk ? ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer) : null;
            boolean restorationHuskActive = ByteBufCodecs.BOOL.decode(buffer);

            return new PlayerSymbioteData(
                    symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                    symbioteBindable, cocoonSpawnPos, cocoonSpawnDim,
                    previousBedSpawnPos, previousBedSpawnDim,
                    symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                    restorationHuskPos, restorationHuskDim, restorationHuskActive
            );
        }

        @Override
        public void encode(ByteBuf buffer, PlayerSymbioteData data) {
            // Symbiote fields
            SymbioteState.STREAM_CODEC.encode(buffer, data.symbioteState);
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

            // Restoration Husk fields
            boolean hasRestorationHusk = data.restorationHuskPos != null && data.restorationHuskDim != null;
            ByteBufCodecs.BOOL.encode(buffer, hasRestorationHusk);
            if (hasRestorationHusk) {
                BlockPos.STREAM_CODEC.encode(buffer, data.restorationHuskPos);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, data.restorationHuskDim);
            }
            ByteBufCodecs.BOOL.encode(buffer, data.restorationHuskActive);
        }
    };

    public static final PlayerSymbioteData DEFAULT = new PlayerSymbioteData(
            SymbioteState.UNBOUND, 0L, 1, false, false, false, 0,
            false, null, null, null, null, false, false,
            null, null, false
    );

    // Symbiote builder methods

    /**
     * Creates a new PlayerSymbioteData with the given symbiote state.
     */
    public PlayerSymbioteData withSymbioteState(SymbioteState state) {
        return new PlayerSymbioteData(state, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    /**
     * Creates a new PlayerSymbioteData with all symbiote bond fields.
     */
    public PlayerSymbioteData withBond(SymbioteState state, long bondTime, int tier,
                                        boolean dash, boolean featherFalling, boolean speed, int jumpBoost) {
        return new PlayerSymbioteData(state, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withTier(int tier) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withDash(boolean dash) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withFeatherFalling(boolean featherFalling) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withSpeed(boolean speed) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withJumpBoost(int jumpBoost) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    // Cocoon builder methods

    public PlayerSymbioteData withSymbioteBindable(boolean bindable) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                bindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withSymbioteStewConsumedOnce(boolean consumed) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                consumed, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withCocoonSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, pos, dim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withPreviousBedSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, pos, dim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    public PlayerSymbioteData withCocoonSpawnSetOnce(boolean set) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, set,
                restorationHuskPos, restorationHuskDim, restorationHuskActive);
    }

    // Restoration Husk builder methods

    /**
     * Sets the restoration husk location.
     */
    public PlayerSymbioteData withRestorationHusk(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim, boolean active) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                pos, dim, active);
    }

    /**
     * Clears the restoration husk tracking.
     */
    public PlayerSymbioteData clearRestorationHusk() {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                null, null, false);
    }

    /**
     * Creates a PlayerSymbioteData from an item's SymbioteData (for binding).
     * Preserves cocoon state and restoration husk state from existing player data.
     */
    public PlayerSymbioteData withSymbioteFromItem(SymbioteData itemData, long currentGameTime) {
        return new PlayerSymbioteData(
                SymbioteState.BONDED_ACTIVE, currentGameTime, itemData.tier(), itemData.dash(), itemData.featherFalling(), itemData.speed(), itemData.jumpBoost(),
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive
        );
    }

    /**
     * Creates a SymbioteData for storing on an item (for unbinding).
     */
    public SymbioteData toItemData() {
        return new SymbioteData(false, 0L, tier, dash, featherFalling, speed, jumpBoost);
    }

    /**
     * Resets symbiote bond state while preserving cocoon state and restoration husk state.
     */
    public PlayerSymbioteData withSymbioteReset() {
        return new PlayerSymbioteData(
                SymbioteState.UNBOUND, 0L, 1, false, false, false, 0,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive
        );
    }
}
