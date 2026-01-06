package net.j40climb.florafauna.common.block.iteminput.shared;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility for scanning and finding containers with item storage capability.
 * Extracted from StorageAnchorBlockEntity to enable reuse and unit testing.
 */
public class ContainerScanner {

    /**
     * Scans for blocks with IItemHandler capability within a cubic radius.
     *
     * @param level The level to scan in
     * @param center The center position to scan around
     * @param radius The radius to scan (cubic, not spherical)
     * @param exclude Positions to exclude from results (e.g., self, already linked)
     * @return List of positions with item storage capability
     */
    public static List<BlockPos> scanForContainers(Level level, BlockPos center, int radius, Set<BlockPos> exclude) {
        return scanForContainers(level, center, radius, pos -> !exclude.contains(pos));
    }

    /**
     * Scans for blocks with IItemHandler capability within a cubic radius.
     *
     * @param level The level to scan in
     * @param center The center position to scan around
     * @param radius The radius to scan (cubic, not spherical)
     * @param filter Additional filter predicate for positions (return true to include)
     * @return List of positions with item storage capability
     */
    public static List<BlockPos> scanForContainers(Level level, BlockPos center, int radius, Predicate<BlockPos> filter) {
        List<BlockPos> containers = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    // Skip center position
                    if (pos.equals(center)) {
                        continue;
                    }

                    // Apply custom filter
                    if (!filter.test(pos)) {
                        continue;
                    }

                    // Check if block has item storage capability
                    if (hasItemCapability(level, pos)) {
                        containers.add(pos.immutable());
                    }
                }
            }
        }

        return containers;
    }

    /**
     * Checks if a block has item storage capability.
     *
     * @param level The level containing the block
     * @param pos The position to check
     * @return true if the block has IItemHandler capability
     */
    public static boolean hasItemCapability(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, null) != null;
    }
}
