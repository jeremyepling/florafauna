package net.j40climb.florafauna.common.block.mobbarrier.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration data for MobBarrierBlock.
 * Stores lists of entity IDs and entity tags that should be blocked.
 *
 * Entity IDs: e.g., "minecraft:zombie", "florafauna:gecko"
 * Entity Tags: e.g., "#minecraft:undead", "#minecraft:raiders"
 */
public final class MobBarrierConfig {
    public static final MobBarrierConfig DEFAULT = new MobBarrierConfig(List.of(), List.of(), false);

    // Keep the original spelling and order for saved presets and the editor.
    private final List<String> entityIds;
    private final List<String> entityTags;
    private final boolean blockVision;
    private final Set<Identifier> blockedIds;
    private final Set<TagKey<EntityType<?>>> blockedTags;

    public MobBarrierConfig(List<String> entityIds, List<String> entityTags, boolean blockVision) {
        this.entityIds = List.copyOf(entityIds);
        this.entityTags = List.copyOf(entityTags);
        this.blockVision = blockVision;
        this.blockedIds = new HashSet<>();
        this.blockedTags = new HashSet<>();
        for (String entry : this.entityIds) {
            Identifier id = Identifier.tryParse(entry);
            if (id != null) blockedIds.add(id);
        }
        for (String entry : this.entityTags) {
            Identifier id = parseTag(entry);
            if (id != null) blockedTags.add(TagKey.create(Registries.ENTITY_TYPE, id));
        }
    }

    public List<String> entityIds() {
        return entityIds;
    }

    public List<String> entityTags() {
        return entityTags;
    }

    public boolean blockVision() {
        return blockVision;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MobBarrierConfig config && blockVision == config.blockVision
                && entityIds.equals(config.entityIds) && entityTags.equals(config.entityTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityIds, entityTags, blockVision);
    }

    public static final Codec<MobBarrierConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("entityIds").forGetter(MobBarrierConfig::entityIds),
                    Codec.STRING.listOf().fieldOf("entityTags").forGetter(MobBarrierConfig::entityTags),
                    Codec.BOOL.optionalFieldOf("blockVision", false).forGetter(MobBarrierConfig::blockVision)
            ).apply(instance, MobBarrierConfig::new)
    );

    public static final StreamCodec<ByteBuf, MobBarrierConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            MobBarrierConfig::entityIds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            MobBarrierConfig::entityTags,
            ByteBufCodecs.BOOL,
            MobBarrierConfig::blockVision,
            MobBarrierConfig::new
    );

    /**
     * Checks if the given entity should be blocked by this barrier.
     * Returns true if the entity matches any configured entity ID or tag.
     */
    public boolean shouldBlockEntity(Entity entity) {
        if (entity == null) return false;

        if (blockedIds.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) return true;
        // Resolve membership against the live holder so datapack reloads are visible.
        for (TagKey<EntityType<?>> tag : blockedTags) {
            if (entity.is(tag)) return true;
        }
        return false;
    }

    /**
     * Creates a new config with an additional entity ID.
     */
    public MobBarrierConfig withAddedEntityId(String entityId) {
        if (entityIds.contains(entityId)) return this;
        List<String> newIds = new ArrayList<>(entityIds);
        newIds.add(entityId);
        return new MobBarrierConfig(newIds, entityTags, blockVision);
    }

    /**
     * Creates a new config with an entity ID removed.
     */
    public MobBarrierConfig withRemovedEntityId(String entityId) {
        List<String> newIds = new ArrayList<>(entityIds);
        newIds.remove(entityId);
        return new MobBarrierConfig(newIds, entityTags, blockVision);
    }

    /**
     * Creates a new config with an additional entity tag.
     */
    public MobBarrierConfig withAddedEntityTag(String entityTag) {
        String normalizedTag = entityTag.startsWith("#") ? entityTag : "#" + entityTag;
        if (entityTags.contains(normalizedTag)) return this;
        List<String> newTags = new ArrayList<>(entityTags);
        newTags.add(normalizedTag);
        return new MobBarrierConfig(entityIds, newTags, blockVision);
    }

    /**
     * Creates a new config with an entity tag removed.
     */
    public MobBarrierConfig withRemovedEntityTag(String entityTag) {
        List<String> newTags = new ArrayList<>(entityTags);
        newTags.remove(entityTag);
        return new MobBarrierConfig(entityIds, newTags, blockVision);
    }

    /**
     * Creates a new config with blockVision toggled.
     */
    public MobBarrierConfig withBlockVision(boolean blockVision) {
        return new MobBarrierConfig(entityIds, entityTags, blockVision);
    }

    /**
     * Returns true if this config has no entries (blocks nothing).
     */
    public boolean isEmpty() {
        return entityIds.isEmpty() && entityTags.isEmpty();
    }

    /**
     * Returns total count of all entries (IDs + tags).
     */
    public int totalEntries() {
        return entityIds.size() + entityTags.size();
    }

    /**
     * Validates an entity ID string.
     * @return true if the entity ID exists in the registry
     */
    public static boolean isValidEntityId(String entityId) {
        Identifier id = Identifier.tryParse(entityId);
        if (id == null) return false;
        return BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

    /**
     * Validates an entity tag string.
     * Note: Tags are data-driven and may not be loaded on client.
     * This just validates the format, not existence.
     * @return true if the tag has valid format (namespace:path)
     */
    public static boolean isValidTagFormat(String tagString) {
        return parseTag(tagString) != null;
    }

    private static Identifier parseTag(String entry) {
        Identifier id = Identifier.tryParse(entry.startsWith("#") ? entry.substring(1) : entry);
        return id != null && !id.getPath().isEmpty() ? id : null;
    }

    /**
     * Common entity type tags for suggestions.
     * These are vanilla Minecraft tags that exist in the game.
     */
    public static final List<String> COMMON_ENTITY_TAGS = List.of(
            // Passive/Friendly mobs
            "#minecraft:followable_friendly_mobs",  // 25 passive mobs (cow, pig, sheep, chicken, etc.)
            "#minecraft:beehive_inhabitors",
            // Hostile mob categories
            "#minecraft:undead",
            "#minecraft:arthropod",
            "#minecraft:raiders",
            "#minecraft:skeletons",
            "#minecraft:zombies",
            "#minecraft:illager",
            // Special abilities/immunities
            "#minecraft:fall_damage_immune",
            "#minecraft:freeze_immune_entity_types",
            "#minecraft:dismounts_underwater",
            "#minecraft:can_breathe_under_water",
            "#minecraft:not_scary_for_pufferfish",
            "#minecraft:axolotl_always_hostiles",
            "#minecraft:axolotl_hunt_targets",
            "#minecraft:frog_food",
            "#minecraft:powder_snow_walkable_mobs",
            "#minecraft:deflects_projectiles"
    );
}
