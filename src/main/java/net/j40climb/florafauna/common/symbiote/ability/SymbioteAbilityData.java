package net.j40climb.florafauna.common.symbiote.ability;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stores all symbiote ability progress for a player.
 * Attached to players via ModAttachmentTypes.SYMBIOTE_ABILITY_DATA.
 */
public record SymbioteAbilityData(Map<SymbioteAbility, AbilityProgress> abilities) {

    /**
     * Default empty state - no progress on any abilities.
     */
    public static final SymbioteAbilityData DEFAULT = new SymbioteAbilityData(new EnumMap<>(SymbioteAbility.class));

    /**
     * Codec for NBT persistence using unboundedMap with string keys.
     */
    public static final Codec<SymbioteAbilityData> CODEC = Codec.unboundedMap(
            SymbioteAbility.CODEC,
            AbilityProgress.CODEC
    ).xmap(
            map -> new SymbioteAbilityData(new EnumMap<>(map)),
            SymbioteAbilityData::abilities
    );

    /**
     * StreamCodec for network synchronization.
     */
    public static final StreamCodec<ByteBuf, SymbioteAbilityData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SymbioteAbilityData decode(ByteBuf buf) {
            int size = ByteBufCodecs.VAR_INT.decode(buf);
            EnumMap<SymbioteAbility, AbilityProgress> map = new EnumMap<>(SymbioteAbility.class);
            for (int i = 0; i < size; i++) {
                SymbioteAbility ability = SymbioteAbility.STREAM_CODEC.decode(buf);
                AbilityProgress progress = AbilityProgress.STREAM_CODEC.decode(buf);
                map.put(ability, progress);
            }
            return new SymbioteAbilityData(map);
        }

        @Override
        public void encode(ByteBuf buf, SymbioteAbilityData data) {
            ByteBufCodecs.VAR_INT.encode(buf, data.abilities().size());
            for (Map.Entry<SymbioteAbility, AbilityProgress> entry : data.abilities().entrySet()) {
                SymbioteAbility.STREAM_CODEC.encode(buf, entry.getKey());
                AbilityProgress.STREAM_CODEC.encode(buf, entry.getValue());
            }
        }
    };

    /**
     * Gets the progress for a specific ability.
     */
    public AbilityProgress getProgress(SymbioteAbility ability) {
        return abilities.getOrDefault(ability, AbilityProgress.DEFAULT);
    }

    /**
     * Checks if an ability is unlocked.
     */
    public boolean isUnlocked(SymbioteAbility ability) {
        return getProgress(ability).unlocked();
    }

    /**
     * Checks if an ability is currently active.
     */
    public boolean isActive(SymbioteAbility ability) {
        AbilityProgress progress = getProgress(ability);
        return progress.unlocked() && progress.active();
    }

    /**
     * Adds progress toward an ability by consuming items.
     *
     * @param ability The ability to progress
     * @param amount  Number of items consumed
     * @return New SymbioteAbilityData with updated progress
     */
    public SymbioteAbilityData addProgress(SymbioteAbility ability, int amount) {
        EnumMap<SymbioteAbility, AbilityProgress> newMap = new EnumMap<>(abilities);
        AbilityProgress current = getProgress(ability);
        AbilityProgress updated = current.addProgress(amount, ability.getRequiredCount());
        newMap.put(ability, updated);
        return new SymbioteAbilityData(newMap);
    }

    /**
     * Toggles an ability on or off.
     */
    public SymbioteAbilityData toggleAbility(SymbioteAbility ability) {
        EnumMap<SymbioteAbility, AbilityProgress> newMap = new EnumMap<>(abilities);
        AbilityProgress current = getProgress(ability);
        newMap.put(ability, current.toggleActive());
        return new SymbioteAbilityData(newMap);
    }

    /**
     * Sets whether an ability is active.
     */
    public SymbioteAbilityData setAbilityActive(SymbioteAbility ability, boolean active) {
        EnumMap<SymbioteAbility, AbilityProgress> newMap = new EnumMap<>(abilities);
        AbilityProgress current = getProgress(ability);
        newMap.put(ability, current.setActive(active));
        return new SymbioteAbilityData(newMap);
    }

    /**
     * Finds which ability (if any) uses the given item for unlocking.
     *
     * @param item The item to check
     * @return The ability that uses this item, or null if none
     */
    public static SymbioteAbility findAbilityForItem(Item item) {
        for (SymbioteAbility ability : SymbioteAbility.values()) {
            if (ability.getRequiredItem() == item) {
                return ability;
            }
        }
        return null;
    }

    /**
     * Gets the count of unlocked abilities.
     */
    public int getUnlockedCount() {
        int count = 0;
        for (AbilityProgress progress : abilities.values()) {
            if (progress.unlocked()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the count of active abilities.
     */
    public int getActiveCount() {
        int count = 0;
        for (AbilityProgress progress : abilities.values()) {
            if (progress.unlocked() && progress.active()) {
                count++;
            }
        }
        return count;
    }
}