package net.j40climb.florafauna.common.entity.mobsymbiote;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Entity attachment data for mob symbiote levels.
 * Level 1+ mobs are eligible for MobInput capture and luring.
 * <p>
 * Levels:
 * <ul>
 *     <li>0 = No MobSymbiote attached</li>
 *     <li>1 = Basic transport (lure + capture enabled)</li>
 *     <li>2 = Enhanced (future features)</li>
 * </ul>
 *
 * @param mobSymbioteLevel The current MobSymbiote level (0=none, 1=transport, 2=enhanced)
 * @param levelUpgradedAtTick Game tick when last level change occurred
 * @param recentlyReleasedUntil Tick until which this mob has capture immunity (0 = no immunity)
 */
public record MobSymbioteData(
        int mobSymbioteLevel,
        long levelUpgradedAtTick,
        long recentlyReleasedUntil
) {
    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_TRANSPORT = 1;
    public static final int LEVEL_ENHANCED = 2;

    public static final Codec<MobSymbioteData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.INT.fieldOf("mobSymbioteLevel").forGetter(MobSymbioteData::mobSymbioteLevel),
            Codec.LONG.fieldOf("levelUpgradedAtTick").forGetter(MobSymbioteData::levelUpgradedAtTick),
            Codec.LONG.fieldOf("recentlyReleasedUntil").forGetter(MobSymbioteData::recentlyReleasedUntil)
    ).apply(builder, MobSymbioteData::new));

    public static final StreamCodec<ByteBuf, MobSymbioteData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobSymbioteData::mobSymbioteLevel,
            ByteBufCodecs.VAR_LONG, MobSymbioteData::levelUpgradedAtTick,
            ByteBufCodecs.VAR_LONG, MobSymbioteData::recentlyReleasedUntil,
            MobSymbioteData::new
    );

    public static final MobSymbioteData DEFAULT = new MobSymbioteData(LEVEL_NONE, 0L, 0L);

    /**
     * @return true if this mob has a MobSymbiote (level >= 1)
     */
    public boolean hasMobSymbiote() {
        return mobSymbioteLevel >= LEVEL_TRANSPORT;
    }

    /**
     * @return true if this mob can be upgraded to a higher MobSymbiote level
     */
    public boolean canUpgradeMobSymbiote() {
        return mobSymbioteLevel < LEVEL_ENHANCED;
    }

    /**
     * Creates a new MobSymbioteData with the specified level.
     *
     * @param level The new MobSymbiote level
     * @param tick The current game tick
     * @return New MobSymbioteData with updated level
     */
    public MobSymbioteData withMobSymbioteLevel(int level, long tick) {
        return new MobSymbioteData(level, tick, recentlyReleasedUntil);
    }

    /**
     * Creates a new MobSymbioteData with capture immunity until the specified tick.
     *
     * @param untilTick The game tick until which this mob has capture immunity
     * @return New MobSymbioteData with updated immunity
     */
    public MobSymbioteData withRecentlyReleased(long untilTick) {
        return new MobSymbioteData(mobSymbioteLevel, levelUpgradedAtTick, untilTick);
    }

    /**
     * Checks if this mob has release immunity at the given tick.
     *
     * @param currentTick The current game tick
     * @return true if the mob is immune to capture
     */
    public boolean hasReleaseImmunity(long currentTick) {
        return recentlyReleasedUntil > currentTick;
    }
}
