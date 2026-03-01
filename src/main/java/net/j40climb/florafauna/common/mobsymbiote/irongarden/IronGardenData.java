package net.j40climb.florafauna.common.mobsymbiote.irongarden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Entity attachment data for the Iron Garden system.
 * Tracks garden state, timing, phase progress, carried poppies, remembered storage location, and current activity.
 *
 * @param ironGardenState Current garden state
 * @param stateEnteredTick Game tick when current state was entered
 * @param lastCombatTick Game tick when golem last took or dealt damage
 * @param plantsThisPhase Number of poppies planted in current planting phase
 * @param harvestsThisPhase Number of poppies harvested in current harvesting phase
 * @param carriedPoppies Number of ferric poppies being carried (0-16)
 * @param gardenCenterX X coordinate of garden center
 * @param gardenCenterY Y coordinate of garden center
 * @param gardenCenterZ Z coordinate of garden center
 * @param hasGardenCenter Whether a garden center has been established
 * @param storageX X coordinate of remembered storage
 * @param storageY Y coordinate of remembered storage
 * @param storageZ Z coordinate of remembered storage
 * @param hasStorage Whether a storage location has been remembered
 * @param activity Current activity (for client-side debug display)
 */
public record IronGardenData(
        IronGardenState ironGardenState,
        long stateEnteredTick,
        long lastCombatTick,
        int plantsThisPhase,
        int harvestsThisPhase,
        int carriedPoppies,
        int gardenCenterX,
        int gardenCenterY,
        int gardenCenterZ,
        boolean hasGardenCenter,
        int storageX,
        int storageY,
        int storageZ,
        boolean hasStorage,
        IronGardenActivity activity
) {
    public static final int MAX_CARRIED_POPPIES = 16;

    public static final Codec<IronGardenData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.STRING.xmap(IronGardenState::valueOf, IronGardenState::name)
                    .fieldOf("ironGardenState").forGetter(IronGardenData::ironGardenState),
            Codec.LONG.fieldOf("stateEnteredTick").forGetter(IronGardenData::stateEnteredTick),
            Codec.LONG.fieldOf("lastCombatTick").forGetter(IronGardenData::lastCombatTick),
            Codec.INT.fieldOf("plantsThisPhase").forGetter(IronGardenData::plantsThisPhase),
            Codec.INT.fieldOf("harvestsThisPhase").forGetter(IronGardenData::harvestsThisPhase),
            Codec.INT.fieldOf("carriedPoppies").forGetter(IronGardenData::carriedPoppies),
            Codec.INT.fieldOf("gardenCenterX").forGetter(IronGardenData::gardenCenterX),
            Codec.INT.fieldOf("gardenCenterY").forGetter(IronGardenData::gardenCenterY),
            Codec.INT.fieldOf("gardenCenterZ").forGetter(IronGardenData::gardenCenterZ),
            Codec.BOOL.fieldOf("hasGardenCenter").forGetter(IronGardenData::hasGardenCenter),
            Codec.INT.optionalFieldOf("storageX", 0).forGetter(IronGardenData::storageX),
            Codec.INT.optionalFieldOf("storageY", 0).forGetter(IronGardenData::storageY),
            Codec.INT.optionalFieldOf("storageZ", 0).forGetter(IronGardenData::storageZ),
            Codec.BOOL.optionalFieldOf("hasStorage", false).forGetter(IronGardenData::hasStorage),
            Codec.STRING.xmap(IronGardenActivity::valueOf, IronGardenActivity::name)
                    .optionalFieldOf("activity", IronGardenActivity.IDLE).forGetter(IronGardenData::activity)
    ).apply(builder, IronGardenData::new));

    public static final StreamCodec<ByteBuf, IronGardenData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, data.ironGardenState().name());
                ByteBufCodecs.VAR_LONG.encode(buf, data.stateEnteredTick());
                ByteBufCodecs.VAR_LONG.encode(buf, data.lastCombatTick());
                ByteBufCodecs.VAR_INT.encode(buf, data.plantsThisPhase());
                ByteBufCodecs.VAR_INT.encode(buf, data.harvestsThisPhase());
                ByteBufCodecs.VAR_INT.encode(buf, data.carriedPoppies());
                ByteBufCodecs.VAR_INT.encode(buf, data.gardenCenterX());
                ByteBufCodecs.VAR_INT.encode(buf, data.gardenCenterY());
                ByteBufCodecs.VAR_INT.encode(buf, data.gardenCenterZ());
                ByteBufCodecs.BOOL.encode(buf, data.hasGardenCenter());
                ByteBufCodecs.VAR_INT.encode(buf, data.storageX());
                ByteBufCodecs.VAR_INT.encode(buf, data.storageY());
                ByteBufCodecs.VAR_INT.encode(buf, data.storageZ());
                ByteBufCodecs.BOOL.encode(buf, data.hasStorage());
                ByteBufCodecs.STRING_UTF8.encode(buf, data.activity().name());
            },
            buf -> new IronGardenData(
                    IronGardenState.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    IronGardenActivity.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))
            )
    );

    public static final IronGardenData DEFAULT = new IronGardenData(
            IronGardenState.UNBONDED, 0L, 0L, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, false,
            IronGardenActivity.IDLE
    );

    /**
     * Transition to a new garden state.
     *
     * @param newState The new state
     * @param tick Current game tick
     * @return New IronGardenData with updated state
     */
    public IronGardenData withState(IronGardenState newState, long tick) {
        return new IronGardenData(newState, tick, lastCombatTick, plantsThisPhase, harvestsThisPhase,
                carriedPoppies, gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Record a combat event.
     *
     * @param tick Current game tick
     * @return New IronGardenData with updated combat tick
     */
    public IronGardenData withCombat(long tick) {
        return new IronGardenData(ironGardenState, stateEnteredTick, tick, plantsThisPhase, harvestsThisPhase,
                carriedPoppies, gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Record a combat event and transition to BONDED_NOT_CALM.
     *
     * @param tick Current game tick
     * @return New IronGardenData with combat recorded and state changed
     */
    public IronGardenData withCombatBreakingCalm(long tick) {
        return new IronGardenData(IronGardenState.BONDED_NOT_CALM, tick, tick, 0, 0,
                carriedPoppies, gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, IronGardenActivity.IDLE);
    }

    /**
     * Increment the plants counter for this phase.
     *
     * @return New IronGardenData with incremented plant count
     */
    public IronGardenData incrementPlants() {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase + 1, harvestsThisPhase, carriedPoppies,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Increment the harvests counter for this phase and add to carried poppies.
     *
     * @return New IronGardenData with incremented harvest count and carried poppies
     */
    public IronGardenData incrementHarvests() {
        int newCarried = Math.min(carriedPoppies + 1, MAX_CARRIED_POPPIES);
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase + 1, newCarried,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Set the number of carried poppies.
     *
     * @param count New carried poppy count (clamped to 0-MAX_CARRIED_POPPIES)
     * @return New IronGardenData with updated carried count
     */
    public IronGardenData withCarriedPoppies(int count) {
        int clamped = Math.max(0, Math.min(count, MAX_CARRIED_POPPIES));
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, clamped,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Clear all carried poppies.
     *
     * @return New IronGardenData with zero carried poppies
     */
    public IronGardenData clearCarriedPoppies() {
        return withCarriedPoppies(0);
    }

    /**
     * Set the garden center position.
     *
     * @param pos The garden center position
     * @return New IronGardenData with updated center
     */
    public IronGardenData withGardenCenter(BlockPos pos) {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, carriedPoppies,
                pos.getX(), pos.getY(), pos.getZ(), true,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Clear the garden center.
     *
     * @return New IronGardenData without a garden center
     */
    public IronGardenData clearGardenCenter() {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, carriedPoppies,
                0, 0, 0, false,
                storageX, storageY, storageZ, hasStorage, activity);
    }

    /**
     * Get the garden center position if set.
     *
     * @return Optional containing the position, or empty if not set
     */
    public Optional<BlockPos> getGardenCenter() {
        if (hasGardenCenter) {
            return Optional.of(new BlockPos(gardenCenterX, gardenCenterY, gardenCenterZ));
        }
        return Optional.empty();
    }

    /**
     * Set the remembered storage position.
     *
     * @param pos The storage position
     * @return New IronGardenData with updated storage location
     */
    public IronGardenData withStorage(BlockPos pos) {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, carriedPoppies,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                pos.getX(), pos.getY(), pos.getZ(), true, activity);
    }

    /**
     * Clear the remembered storage location.
     *
     * @return New IronGardenData without a storage location
     */
    public IronGardenData clearStorage() {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, carriedPoppies,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                0, 0, 0, false, activity);
    }

    /**
     * Set the current activity (for debug display).
     *
     * @param newActivity The new activity
     * @return New IronGardenData with updated activity
     */
    public IronGardenData withActivity(IronGardenActivity newActivity) {
        return new IronGardenData(ironGardenState, stateEnteredTick, lastCombatTick,
                plantsThisPhase, harvestsThisPhase, carriedPoppies,
                gardenCenterX, gardenCenterY, gardenCenterZ, hasGardenCenter,
                storageX, storageY, storageZ, hasStorage, newActivity);
    }

    /**
     * Get the remembered storage position if set.
     *
     * @return Optional containing the position, or empty if not set
     */
    public Optional<BlockPos> getStorage() {
        if (hasStorage) {
            return Optional.of(new BlockPos(storageX, storageY, storageZ));
        }
        return Optional.empty();
    }

    /**
     * Calculate how many ticks the golem has been in the current state.
     *
     * @param currentTick Current game tick
     * @return Ticks in current state
     */
    public long getTicksInState(long currentTick) {
        return currentTick - stateEnteredTick;
    }

    /**
     * Calculate how many ticks since the last combat event.
     *
     * @param currentTick Current game tick
     * @return Ticks since last combat (or very large if never in combat)
     */
    public long getTicksSinceCombat(long currentTick) {
        if (lastCombatTick == 0) {
            return Long.MAX_VALUE; // Never been in combat
        }
        return currentTick - lastCombatTick;
    }

    /**
     * @return true if the golem is carrying any poppies
     */
    public boolean isCarryingPoppies() {
        return carriedPoppies > 0;
    }

    /**
     * @return true if the golem is at max carrying capacity
     */
    public boolean isCarryingFull() {
        return carriedPoppies >= MAX_CARRIED_POPPIES;
    }
}
