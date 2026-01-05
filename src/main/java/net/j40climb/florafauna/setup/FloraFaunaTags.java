package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * Central location for all FloraFauna tags.
 */
public final class FloraFaunaTags {

    private FloraFaunaTags() {
        // Utility class
    }

    /**
     * Block tags for FloraFauna.
     */
    public static class Blocks {
        /**
         * Blocks that can be replaced when spawning storage pods.
         * Includes air by default; this tag covers other replaceable blocks like grass, flowers, etc.
         */
        public static final TagKey<Block> POD_REPLACEABLE = create("pod_replaceable");

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name));
        }
    }

    public static final class EntityTypes {
        /**
         * Mobs that are EXCLUDED from receiving any MobSymbiote.
         * This is a deny list - mobs on this list cannot be bonded or transported.
         * Includes bosses and special entities (Wither, Ender Dragon, Warden, Elder Guardian).
         */
        public static final TagKey<EntityType<?>> MOB_SYMBIOTE_EXCLUDED = create("mob_symbiote_excluded");

        /**
         * Mobs that are eligible for Level 2 MobSymbiote upgrade.
         * This is an allowlist - only mobs on this list can receive enhanced symbiote behaviors.
         */
        public static final TagKey<EntityType<?>> MOB_SYMBIOTE_LEVEL2_ELIGIBLE = create("mob_symbiote_level2_eligible");

        private static TagKey<EntityType<?>> create(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name));
        }
    }
}
