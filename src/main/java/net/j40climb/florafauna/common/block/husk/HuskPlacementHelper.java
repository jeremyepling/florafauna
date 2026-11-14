package net.j40climb.florafauna.common.block.husk;

import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Helper for finding valid husk placement positions on player death.
 *
 * Placement rules (from spec):
 * - Death in fluid → highest air block above column
 * - Death in air → first solid block below
 * - Void death → cocoon spawn, else world spawn
 * - Blocked → upward then radial search
 * - Valid placement: air/replaceable block with solid support below
 */
public final class HuskPlacementHelper {
    private HuskPlacementHelper() {} // Utility class

    private static final int MAX_UPWARD_SEARCH = 10;
    private static final int MAX_RADIAL_SEARCH = 5;

    public record Placement(ServerLevel level, BlockPos pos) {}

    /**
     * Find a valid placement position for a husk near the death location.
     *
     * @param player The player who died
     * @param deathPos The position where the player died
     * @param level The server level
     * @return A validated destination, or null if no safe location exists
     */
    @Nullable
    public static Placement findPlacementPosition(ServerPlayer player, BlockPos deathPos, ServerLevel level) {
        // 1. Check for void death (Y < world minimum)
        if (deathPos.getY() < level.getMinY()) {
            return getFallbackSpawn(player, level);
        }

        // 2. Check if death in fluid
        if (level.getFluidState(deathPos).isSource()) {
            BlockPos aboveFluid = findHighestAirAbove(level, deathPos);
            if (aboveFluid != null && isValidPlacement(level, aboveFluid)) {
                return new Placement(level, aboveFluid);
            }
        }

        // 3. Check if death in air (no solid below immediate position)
        if (!level.getBlockState(deathPos.below()).isSolid()) {
            BlockPos belowSolid = findFirstSolidBelow(level, deathPos);
            if (belowSolid != null && isValidPlacement(level, belowSolid)) {
                return new Placement(level, belowSolid);
            }
        }

        // 4. Try direct placement at death location
        if (isValidPlacement(level, deathPos)) {
            return new Placement(level, deathPos);
        }

        // 5. Search upward
        BlockPos upward = searchUpward(level, deathPos);
        if (upward != null) {
            return new Placement(level, upward);
        }

        // 6. Search radially
        BlockPos radial = searchRadial(level, deathPos);
        if (radial != null) {
            return new Placement(level, radial);
        }

        // 7. Fallback to cocoon spawn or world spawn
        return getFallbackSpawn(player, level);
    }

    /**
     * Check if a position is valid for husk placement.
     * Valid: air or replaceable block with solid support below.
     */
    private static boolean isValidPlacement(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());

        boolean canPlace = state.isAir() || state.canBeReplaced();
        boolean hasSolidSupport = below.isSolid();

        return canPlace && hasSolidSupport;
    }

    /**
     * Find the highest air block above a fluid column.
     */
    private static BlockPos findHighestAirAbove(Level level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();

        // Search upward from the death position
        for (int y = start.getY(); y <= level.getMaxY(); y++) {
            pos.setY(y);

            if (!level.getFluidState(pos).isSource()) {
                // Found air above fluid
                if (isValidPlacement(level, pos)) {
                    return pos.immutable();
                }
            }
        }

        return null;
    }

    /**
     * Find the first solid block below and return the position above it.
     */
    private static BlockPos findFirstSolidBelow(Level level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();

        for (int y = start.getY(); y > level.getMinY(); y--) {
            pos.setY(y);

            if (level.getBlockState(pos.below()).isSolid()) {
                if (isValidPlacement(level, pos)) {
                    return pos.immutable();
                }
            }
        }

        return null;
    }

    /**
     * Search upward from a position for a valid placement.
     */
    private static BlockPos searchUpward(Level level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();

        for (int dy = 1; dy <= MAX_UPWARD_SEARCH; dy++) {
            pos.setY(start.getY() + dy);

            if (pos.getY() > level.getMaxY()) {
                break;
            }

            if (isValidPlacement(level, pos)) {
                return pos.immutable();
            }
        }

        return null;
    }

    /**
     * Search radially around a position for valid placement.
     */
    private static BlockPos searchRadial(Level level, BlockPos center) {
        for (int radius = 1; radius <= MAX_RADIAL_SEARCH; radius++) {
            // Search in a square pattern at each radius
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Skip inner positions (already checked at smaller radius)
                    if (Math.abs(dx) < radius && Math.abs(dz) < radius) {
                        continue;
                    }

                    // Check at same Y level and nearby Y levels
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos testPos = center.offset(dx, dy, dz);

                        if (testPos.getY() < level.getMinY() ||
                                testPos.getY() > level.getMaxY()) {
                            continue;
                        }

                        if (isValidPlacement(level, testPos)) {
                            return testPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get a fallback spawn position (cocoon spawn or world spawn).
     */
    @Nullable
    private static Placement getFallbackSpawn(ServerPlayer player, ServerLevel deathLevel) {
        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (data.cocoonSpawnPos() != null && data.cocoonSpawnDim() != null) {
            ServerLevel cocoonLevel = deathLevel.getServer().getLevel(data.cocoonSpawnDim());
            if (cocoonLevel != null) {
                Placement atCocoon = findNear(cocoonLevel, data.cocoonSpawnPos());
                if (atCocoon != null) {
                    return atCocoon;
                }
            }
        }

        var spawn = deathLevel.getServer().getRespawnData();
        ServerLevel spawnLevel = deathLevel.getServer().getLevel(spawn.dimension());
        return spawnLevel == null ? null : findNear(spawnLevel, spawn.pos());
    }

    @Nullable
    private static Placement findNear(ServerLevel level, BlockPos pos) {
        if (isValidPlacement(level, pos)) {
            return new Placement(level, pos);
        }
        BlockPos nearby = searchUpward(level, pos);
        if (nearby == null) {
            nearby = searchRadial(level, pos);
        }
        return nearby == null ? null : new Placement(level, nearby);
    }
}
