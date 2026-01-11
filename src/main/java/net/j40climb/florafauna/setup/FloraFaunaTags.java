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

        /**
         * Reflective blocks that endermen will stare at and see their reflection.
         * When an enderman sees their reflection within stare distance, they become scared.
         * Includes polished stones, glass, ice, etc.
         */
        public static final TagKey<Block> REFLECTIVE_BLOCKS = create("reflective_blocks");

        /**
         * Cold blocks that contribute to blaze fear.
         * When enough cold blocks are present in a blaze's vicinity (along with snow golems),
         * the blaze becomes scared. Includes snow, ice, powder snow, etc.
         */
        public static final TagKey<Block> COLD_BLOCKS = create("cold_blocks");

        /**
         * Blocks that fear line-of-sight can pass through.
         * Includes glass and glass panes so mobs can see fear sources through windows.
         */
        public static final TagKey<Block> FEAR_LOS_TRANSPARENT = create("fear_los_transparent");

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

        /**
         * Mobs that can experience fear and participate in the fear ecosystem.
         * Currently includes creepers. Will expand to endermen, blazes, etc.
         * Requires MobSymbiote Level 1+ to actually experience fear.
         */
        public static final TagKey<EntityType<?>> FEARFUL_MOBS = create("fearful_mobs");

        private static TagKey<EntityType<?>> create(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name));
        }
    }
}
