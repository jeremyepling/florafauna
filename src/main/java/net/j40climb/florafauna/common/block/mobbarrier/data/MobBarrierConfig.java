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
import java.util.List;

/**
 * Configuration data for MobBarrierBlock.
 * Stores lists of entity IDs and entity tags that should be blocked.
 *
 * Entity IDs: e.g., "minecraft:zombie", "florafauna:gecko"
 * Entity Tags: e.g., "#minecraft:undead", "#minecraft:raiders"
 */
public record MobBarrierConfig(
        List<String> entityIds,
        List<String> entityTags
) {

    public static final MobBarrierConfig DEFAULT = new MobBarrierConfig(List.of(), List.of());

    public MobBarrierConfig {
        entityIds = List.copyOf(entityIds);
        entityTags = List.copyOf(entityTags);
    }

    public static final Codec<MobBarrierConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("entityIds").forGetter(MobBarrierConfig::entityIds),
                    Codec.STRING.listOf().fieldOf("entityTags").forGetter(MobBarrierConfig::entityTags)
            ).apply(instance, MobBarrierConfig::new)
    );

    public static final StreamCodec<ByteBuf, MobBarrierConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            MobBarrierConfig::entityIds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            MobBarrierConfig::entityTags,
            MobBarrierConfig::new
    );

    /**
     * Checks if the given entity should be blocked by this barrier.
     * Returns true if the entity matches any configured entity ID or tag.
     */
    public boolean shouldBlockEntity(Entity entity) {
        if (entity == null) return false;

        EntityType<?> entityType = entity.getType();
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

        // Check direct entity ID matches
        for (String id : entityIds) {
            if (id.equals(entityId.toString())) {
                return true;
            }
        }

        // Check entity tag matches
        for (String tagString : entityTags) {
            String tagPath = tagString.startsWith("#") ? tagString.substring(1) : tagString;
            Identifier tagId = Identifier.tryParse(tagPath);
            if (tagId != null) {
                TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagId);
                if (entityType.is(tagKey)) {
                    return true;
                }
            }
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
        return new MobBarrierConfig(newIds, entityTags);
    }

    /**
     * Creates a new config with an entity ID removed.
     */
    public MobBarrierConfig withRemovedEntityId(String entityId) {
        List<String> newIds = new ArrayList<>(entityIds);
        newIds.remove(entityId);
        return new MobBarrierConfig(newIds, entityTags);
    }

    /**
     * Creates a new config with an additional entity tag.
     */
    public MobBarrierConfig withAddedEntityTag(String entityTag) {
        String normalizedTag = entityTag.startsWith("#") ? entityTag : "#" + entityTag;
        if (entityTags.contains(normalizedTag)) return this;
        List<String> newTags = new ArrayList<>(entityTags);
        newTags.add(normalizedTag);
        return new MobBarrierConfig(entityIds, newTags);
    }

    /**
     * Creates a new config with an entity tag removed.
     */
    public MobBarrierConfig withRemovedEntityTag(String entityTag) {
        List<String> newTags = new ArrayList<>(entityTags);
        newTags.remove(entityTag);
        return new MobBarrierConfig(entityIds, newTags);
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

    /**re
     * Validates an entity tag string.
     * Note: Tags are data-driven and may not be loaded on client.
     * This just validates the format, not existence.
     * @return true if the tag has valid format (namespace:path)
     */
    public static boolean isValidTagFormat(String tagString) {
        String tagPath = tagString.startsWith("#") ? tagString.substring(1) : tagString;
        Identifier tagId = Identifier.tryParse(tagPath);
        return tagId != null;
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
