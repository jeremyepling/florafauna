package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Custom tags for the FloraFauna mod.
 */
public class FloraFaunaTags {

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
}
