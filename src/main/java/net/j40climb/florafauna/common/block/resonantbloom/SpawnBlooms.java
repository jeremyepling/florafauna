package net.j40climb.florafauna.common.block.resonantbloom;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Places the starter flowers while the Overworld's initial spawn is being set. */
public final class SpawnBlooms {
    private static final int COUNT = 5;
    private static final int RADIUS = 100;
    private static final double SECTOR_ANGLE = Math.TAU / COUNT;

    private SpawnBlooms() {}

    public static List<BlockPos> place(ServerLevel level, BlockPos spawn) {
        var random = RandomSource.create(level.getSeed() ^ spawn.asLong());
        double rotation = random.nextDouble() * Math.TAU;
        List<List<Column>> sectors = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            sectors.add(new ArrayList<>());
        }
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared == 0 || distanceSquared > RADIUS * RADIUS) {
                    continue;
                }
                double bearing = Math.atan2(dz, dx) - rotation;
                double angle = bearing - Math.floor(bearing / Math.TAU) * Math.TAU;
                int sector = Math.min((int) (angle / SECTOR_ANGLE), COUNT - 1);
                sectors.get(sector).add(new Column(dx, dz));
            }
        }

        var bloom = FloraFaunaRegistry.RESONANT_BLOOM.get().defaultBlockState();
        List<BlockPos> positions = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            double angle = rotation + (i + 0.4 + random.nextDouble() * 0.2) * SECTOR_ANGLE;
            int distance = 24 + 15 * i + random.nextInt(10);
            double targetX = Math.cos(angle) * distance;
            double targetZ = Math.sin(angle) * distance;
            List<Column> columns = sectors.get(i);
            columns.sort(Comparator.comparingDouble(column -> column.distanceSquared(targetX, targetZ)));
            BlockPos position = placeInSector(level, spawn, bloom, columns);
            if (position != null) {
                positions.add(position);
            }
        }
        if (positions.size() < COUNT) {
            LogUtils.getLogger().warn("Only found space for {} of {} starter Resonant Blooms near {}", positions.size(), COUNT, spawn);
        }
        return List.copyOf(positions);
    }

    private static BlockPos placeInSector(ServerLevel level, BlockPos spawn, BlockState bloom, List<Column> columns) {
        BlockPos fallback = null;
        BlockPos floatingFallback = null;
        for (Column column : columns) {
            BlockPos probe = spawn.offset(column.dx(), 0, column.dz());
            if (!level.getWorldBorder().isWithinBounds(probe)) {
                continue;
            }
            int top = level.getChunkAt(probe).getHeight(Heightmap.Types.WORLD_SURFACE, probe.getX() & 15, probe.getZ() & 15) + 1;
            BlockPos surface = probe.atY(top);
            if (level.isOutsideBuildHeight(surface)) {
                continue;
            }
            BlockState ground = level.getBlockState(surface.below());
            if (ground.is(bloom.getBlock()) && ground.canSurvive(level, surface.below())) {
                return surface.below();
            }
            if (level.isEmptyBlock(surface) && bloom.canSurvive(level, surface)
                    && level.setBlock(surface, bloom, Block.UPDATE_CLIENTS)) {
                return surface;
            }

            // An empty column uses the height of spawn, rather than the bottom of the void.
            boolean emptyColumn = top == level.getMinY();
            BlockPos support = emptyColumn
                    ? surface.atY(Math.clamp(spawn.getY() - 1, level.getMinY(), level.getMaxY() - 1))
                    : surface;
            if (!level.isOutsideBuildHeight(support.above()) && level.isEmptyBlock(support) && level.isEmptyBlock(support.above())) {
                if (fallback == null && !emptyColumn && ground.getFluidState().isEmpty() && !ground.is(BlockTags.LEAVES)) {
                    fallback = support;
                } else if (floatingFallback == null) {
                    floatingFallback = support;
                }
            }
        }

        // Sand, water, and void presets may have no soil in a sector. Add one block
        // of soil in empty space so the flower can survive without replacing terrain.
        BlockPos support = fallback == null ? floatingFallback : fallback;
        if (support == null || !level.setBlock(support, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS)) {
            return null;
        }
        BlockPos flower = support.above();
        if (level.setBlock(flower, bloom, Block.UPDATE_CLIENTS)) {
            return flower;
        }
        level.removeBlock(support, false);
        return null;
    }

    private record Column(int dx, int dz) {
        double distanceSquared(double targetX, double targetZ) {
            double x = dx - targetX;
            double z = dz - targetZ;
            return x * x + z * z;
        }
    }
}
