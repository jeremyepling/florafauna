package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** The face of a block in the planned cut that borders source or flowing lava. */
public record LavaExposure(BlockPos pos, Direction face) {
    private static final Direction[] DIRECTIONS = Direction.values();

    public static List<LavaExposure> find(Level level, MultiBlockPatterns.Plan plan) {
        List<LavaExposure> faces = new ArrayList<>();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (BlockPos pos : plan.blocks()) {
            for (Direction direction : DIRECTIONS) {
                neighbor.setWithOffset(pos, direction);
                if (level.isInWorldBounds(neighbor) && level.hasChunkAt(neighbor)
                        && level.getFluidState(neighbor).is(FluidTags.LAVA)) {
                    faces.add(new LavaExposure(pos, direction));
                }
            }
        }
        return faces;
    }
}
