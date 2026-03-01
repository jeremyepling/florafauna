package net.j40climb.florafauna.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Utility for scanning blocks in an area around a position.
 * Provides reusable methods for counting and finding blocks.
 */
public final class AreaScanner {

    private AreaScanner() {
        // Utility class
    }

    /**
     * Counts blocks matching a tag within a cubic area.
     *
     * @param level   The level to scan
     * @param center  The center position
     * @param radius  The radius (creates a cube of 2*radius+1 on each side)
     * @param tag     The block tag to match
     * @return The count of matching blocks
     */
    public static int countBlocksWithTag(Level level, BlockPos center, int radius, TagKey<Block> tag) {
        return countBlocks(level, center, radius, state -> state.is(tag));
    }

    /**
     * Counts blocks matching a predicate within a cubic area.
     *
     * @param level     The level to scan
     * @param center    The center position
     * @param radius    The radius (creates a cube of 2*radius+1 on each side)
     * @param predicate The predicate to match blocks
     * @return The count of matching blocks
     */
    public static int countBlocks(Level level, BlockPos center, int radius, Predicate<BlockState> predicate) {
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (predicate.test(state)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Counts blocks matching a tag within a cubic area with custom Y range.
     *
     * @param level   The level to scan
     * @param center  The center position
     * @param radiusX The horizontal radius
     * @param radiusY The vertical radius (above and below center)
     * @param tag     The block tag to match
     * @return The count of matching blocks
     */
    public static int countBlocksWithTag(Level level, BlockPos center, int radiusX, int radiusY, TagKey<Block> tag) {
        return countBlocks(level, center, radiusX, radiusY, state -> state.is(tag));
    }

    /**
     * Counts blocks matching a predicate within a cubic area with custom Y range.
     *
     * @param level     The level to scan
     * @param center    The center position
     * @param radiusX   The horizontal radius
     * @param radiusY   The vertical radius (above and below center)
     * @param predicate The predicate to match blocks
     * @return The count of matching blocks
     */
    public static int countBlocks(Level level, BlockPos center, int radiusX, int radiusY, Predicate<BlockState> predicate) {
        int count = 0;

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dy = -radiusY; dy <= radiusY; dy++) {
                for (int dz = -radiusX; dz <= radiusX; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (predicate.test(state)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Finds all block positions matching a tag within a cubic area.
     *
     * @param level  The level to scan
     * @param center The center position
     * @param radius The radius (creates a cube of 2*radius+1 on each side)
     * @param tag    The block tag to match
     * @return List of matching block positions
     */
    public static List<BlockPos> findBlocksWithTag(Level level, BlockPos center, int radius, TagKey<Block> tag) {
        return findBlocks(level, center, radius, (state, pos) -> state.is(tag));
    }

    /**
     * Finds all block positions matching a predicate within a cubic area.
     *
     * @param level     The level to scan
     * @param center    The center position
     * @param radius    The radius (creates a cube of 2*radius+1 on each side)
     * @param predicate The predicate to match blocks (receives state and position)
     * @return List of matching block positions
     */
    public static List<BlockPos> findBlocks(Level level, BlockPos center, int radius,
                                            BiPredicate<BlockState, BlockPos> predicate) {
        List<BlockPos> results = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (predicate.test(state, pos)) {
                        results.add(pos.immutable());
                    }
                }
            }
        }

        return results;
    }

    /**
     * Finds the nearest block matching a tag within a cubic area.
     *
     * @param level  The level to scan
     * @param center The center position
     * @param radius The radius (creates a cube of 2*radius+1 on each side)
     * @param tag    The block tag to match
     * @return The nearest matching block position, or null if none found
     */
    public static BlockPos findNearestBlockWithTag(Level level, BlockPos center, int radius, TagKey<Block> tag) {
        return findNearestBlock(level, center, radius, (state, pos) -> state.is(tag));
    }

    /**
     * Finds the nearest block matching a predicate within a cubic area.
     *
     * @param level     The level to scan
     * @param center    The center position
     * @param radius    The radius (creates a cube of 2*radius+1 on each side)
     * @param predicate The predicate to match blocks (receives state and position)
     * @return The nearest matching block position, or null if none found
     */
    public static BlockPos findNearestBlock(Level level, BlockPos center, int radius,
                                            BiPredicate<BlockState, BlockPos> predicate) {
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (predicate.test(state, pos)) {
                        double distSq = center.distSqr(pos);
                        if (distSq < nearestDistSq) {
                            nearest = pos.immutable();
                            nearestDistSq = distSq;
                        }
                    }
                }
            }
        }

        return nearest;
    }
}
