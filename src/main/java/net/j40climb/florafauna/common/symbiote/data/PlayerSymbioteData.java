package net.j40climb.florafauna.common.symbiote.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.common.block.mininganchor.AnchorFillState;
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
 *
 * Mining Anchor fields:
 * - boundAnchorPos/Dim: the anchor bound to the player's symbiote
 * - activeWaypointAnchorPos/Dim: the anchor shown on locator bar (can differ from bound)
 * - lastAnnouncedFillState: for dialogue cooldown tracking
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
        boolean restorationHuskActive,
        // Mining Anchor tracking
        @Nullable BlockPos boundAnchorPos,
        @Nullable ResourceKey<Level> boundAnchorDim,
        @Nullable BlockPos activeWaypointAnchorPos,
        @Nullable ResourceKey<Level> activeWaypointAnchorDim,
        AnchorFillState lastAnnouncedFillState
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

    /**
     * Helper record for serializing anchor bind state as a single optional field.
     * Groups bound anchor, waypoint anchor, and fill state to reduce codec field count.
     */
    private record AnchorBindState(
            @Nullable BlockPos boundPos, @Nullable ResourceKey<Level> boundDim,
            @Nullable BlockPos waypointPos, @Nullable ResourceKey<Level> waypointDim,
            AnchorFillState lastAnnouncedFillState
    ) {
        static final AnchorBindState EMPTY = new AnchorBindState(null, null, null, null, AnchorFillState.NORMAL);

        static final Codec<AnchorBindState> CODEC = RecordCodecBuilder.create(b -> b.group(
                DimensionPos.CODEC.optionalFieldOf("bound").forGetter(s ->
                        s.boundPos != null && s.boundDim != null
                                ? Optional.of(new DimensionPos(s.boundPos, s.boundDim))
                                : Optional.empty()),
                DimensionPos.CODEC.optionalFieldOf("waypoint").forGetter(s ->
                        s.waypointPos != null && s.waypointDim != null
                                ? Optional.of(new DimensionPos(s.waypointPos, s.waypointDim))
                                : Optional.empty()),
                AnchorFillState.CODEC.optionalFieldOf("lastFillState", AnchorFillState.NORMAL).forGetter(AnchorBindState::lastAnnouncedFillState)
        ).apply(b, AnchorBindState::fromCodec));

        private static AnchorBindState fromCodec(Optional<DimensionPos> bound, Optional<DimensionPos> waypoint, AnchorFillState fillState) {
            return new AnchorBindState(
                    bound.map(DimensionPos::pos).orElse(null),
                    bound.map(DimensionPos::dim).orElse(null),
                    waypoint.map(DimensionPos::pos).orElse(null),
                    waypoint.map(DimensionPos::dim).orElse(null),
                    fillState
            );
        }
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
                    Codec.BOOL.fieldOf("restorationHuskActive").forGetter(PlayerSymbioteData::restorationHuskActive),
                    // Mining Anchor fields (1 nested record)
                    AnchorBindState.CODEC.optionalFieldOf("anchorBind", AnchorBindState.EMPTY).forGetter(d ->
                            new AnchorBindState(d.boundAnchorPos, d.boundAnchorDim,
                                    d.activeWaypointAnchorPos, d.activeWaypointAnchorDim,
                                    d.lastAnnouncedFillState))
            ).apply(builder, PlayerSymbioteData::fromCodec));

    private static PlayerSymbioteData fromCodec(
            SymbioteState symbioteState, long bondTime, int tier, boolean dash, boolean featherFalling, boolean speed, int jumpBoost,
            boolean symbioteBindable,
            Optional<DimensionPos> cocoonSpawn,
            Optional<DimensionPos> previousBedSpawn,
            boolean symbioteStewConsumedOnce, boolean cocoonSpawnSetOnce,
            Optional<DimensionPos> restorationHusk,
            boolean restorationHuskActive,
            AnchorBindState anchorBind
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
                restorationHuskActive,
                anchorBind.boundPos, anchorBind.boundDim,
                anchorBind.waypointPos, anchorBind.waypointDim,
                anchorBind.lastAnnouncedFillState
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

            // Mining Anchor fields
            boolean hasBoundAnchor = ByteBufCodecs.BOOL.decode(buffer);
            BlockPos boundAnchorPos = hasBoundAnchor ? BlockPos.STREAM_CODEC.decode(buffer) : null;
            ResourceKey<Level> boundAnchorDim = hasBoundAnchor ? ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer) : null;

            boolean hasActiveWaypoint = ByteBufCodecs.BOOL.decode(buffer);
            BlockPos activeWaypointAnchorPos = hasActiveWaypoint ? BlockPos.STREAM_CODEC.decode(buffer) : null;
            ResourceKey<Level> activeWaypointAnchorDim = hasActiveWaypoint ? ResourceKey.streamCodec(Registries.DIMENSION).decode(buffer) : null;

            AnchorFillState lastAnnouncedFillState = AnchorFillState.STREAM_CODEC.decode(buffer);

            return new PlayerSymbioteData(
                    symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                    symbioteBindable, cocoonSpawnPos, cocoonSpawnDim,
                    previousBedSpawnPos, previousBedSpawnDim,
                    symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                    restorationHuskPos, restorationHuskDim, restorationHuskActive,
                    boundAnchorPos, boundAnchorDim,
                    activeWaypointAnchorPos, activeWaypointAnchorDim,
                    lastAnnouncedFillState
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

            // Mining Anchor fields
            boolean hasBoundAnchor = data.boundAnchorPos != null && data.boundAnchorDim != null;
            ByteBufCodecs.BOOL.encode(buffer, hasBoundAnchor);
            if (hasBoundAnchor) {
                BlockPos.STREAM_CODEC.encode(buffer, data.boundAnchorPos);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, data.boundAnchorDim);
            }

            boolean hasActiveWaypoint = data.activeWaypointAnchorPos != null && data.activeWaypointAnchorDim != null;
            ByteBufCodecs.BOOL.encode(buffer, hasActiveWaypoint);
            if (hasActiveWaypoint) {
                BlockPos.STREAM_CODEC.encode(buffer, data.activeWaypointAnchorPos);
                ResourceKey.streamCodec(Registries.DIMENSION).encode(buffer, data.activeWaypointAnchorDim);
            }

            AnchorFillState.STREAM_CODEC.encode(buffer, data.lastAnnouncedFillState);
        }
    };

    public static final PlayerSymbioteData DEFAULT = new PlayerSymbioteData(
            SymbioteState.UNBOUND, 0L, 1, false, false, false, 0,
            false, null, null, null, null, false, false,
            null, null, false,
            null, null, null, null, AnchorFillState.NORMAL
    );

    // Symbiote builder methods

    /**
     * Creates a new PlayerSymbioteData with the given symbiote state.
     */
    public PlayerSymbioteData withSymbioteState(SymbioteState state) {
        return new PlayerSymbioteData(state, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    /**
     * Creates a new PlayerSymbioteData with all symbiote bond fields.
     */
    public PlayerSymbioteData withBond(SymbioteState state, long bondTime, int tier,
                                        boolean dash, boolean featherFalling, boolean speed, int jumpBoost) {
        return new PlayerSymbioteData(state, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withTier(int tier) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withDash(boolean dash) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withFeatherFalling(boolean featherFalling) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withSpeed(boolean speed) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withJumpBoost(int jumpBoost) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    // Cocoon builder methods

    public PlayerSymbioteData withSymbioteBindable(boolean bindable) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                bindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withSymbioteStewConsumedOnce(boolean consumed) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                consumed, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withCocoonSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, pos, dim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withPreviousBedSpawn(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, pos, dim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    public PlayerSymbioteData withCocoonSpawnSetOnce(boolean set) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, set,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    // Restoration Husk builder methods

    /**
     * Sets the restoration husk location.
     */
    public PlayerSymbioteData withRestorationHusk(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim, boolean active) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                pos, dim, active,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    /**
     * Clears the restoration husk tracking.
     */
    public PlayerSymbioteData clearRestorationHusk() {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                null, null, false,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    /**
     * Creates a PlayerSymbioteData from an item's SymbioteData (for binding).
     * Preserves cocoon state, restoration husk state, and anchor state from existing player data.
     */
    public PlayerSymbioteData withSymbioteFromItem(SymbioteData itemData, long currentGameTime) {
        return new PlayerSymbioteData(
                SymbioteState.BONDED_ACTIVE, currentGameTime, itemData.tier(), itemData.dash(), itemData.featherFalling(), itemData.speed(), itemData.jumpBoost(),
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState
        );
    }

    /**
     * Creates a SymbioteData for storing on an item (for unbinding).
     */
    public SymbioteData toItemData() {
        return new SymbioteData(false, 0L, tier, dash, featherFalling, speed, jumpBoost);
    }

    /**
     * Resets symbiote bond state while preserving cocoon state, restoration husk state, and anchor state.
     */
    public PlayerSymbioteData withSymbioteReset() {
        return new PlayerSymbioteData(
                SymbioteState.UNBOUND, 0L, 1, false, false, false, 0,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState
        );
    }

    // Mining Anchor builder methods

    /**
     * Returns whether the player has an anchor bound to their symbiote.
     */
    public boolean hasAnchorBound() {
        return boundAnchorPos != null && boundAnchorDim != null;
    }

    /**
     * Returns whether the player has an active waypoint anchor.
     */
    public boolean hasActiveWaypointAnchor() {
        return activeWaypointAnchorPos != null && activeWaypointAnchorDim != null;
    }

    /**
     * Sets the bound anchor location.
     */
    public PlayerSymbioteData withBoundAnchor(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                pos, dim, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    /**
     * Clears the bound anchor.
     */
    public PlayerSymbioteData clearBoundAnchor() {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                null, null, activeWaypointAnchorPos, activeWaypointAnchorDim, lastAnnouncedFillState);
    }

    /**
     * Sets the active waypoint anchor location.
     */
    public PlayerSymbioteData withActiveWaypointAnchor(@Nullable BlockPos pos, @Nullable ResourceKey<Level> dim) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, pos, dim, lastAnnouncedFillState);
    }

    /**
     * Clears the active waypoint anchor.
     */
    public PlayerSymbioteData clearActiveWaypointAnchor() {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, null, null, lastAnnouncedFillState);
    }

    /**
     * Sets the last announced fill state (for dialogue cooldown tracking).
     */
    public PlayerSymbioteData withLastAnnouncedFillState(AnchorFillState fillState) {
        return new PlayerSymbioteData(symbioteState, bondTime, tier, dash, featherFalling, speed, jumpBoost,
                symbioteBindable, cocoonSpawnPos, cocoonSpawnDim, previousBedSpawnPos, previousBedSpawnDim,
                symbioteStewConsumedOnce, cocoonSpawnSetOnce,
                restorationHuskPos, restorationHuskDim, restorationHuskActive,
                boundAnchorPos, boundAnchorDim, activeWaypointAnchorPos, activeWaypointAnchorDim, fillState);
    }
}
