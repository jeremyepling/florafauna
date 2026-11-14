package net.j40climb.florafauna.common.mobsymbiote.silk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** The air gap between a pair of posts. Always oriented east or south. */
record SilkSpan(BlockPos first, Direction along, int length) {
    static final int MAX_GAP = 3;

    static SilkSpan emptyGap(LevelReader level, BlockPos post, Direction along) {
        for (int offset = 1; offset <= MAX_GAP + 1; offset++) {
            BlockPos pos = post.relative(along, offset);
            if (!level.hasChunkAt(pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FENCES)) {
                return offset > 1 && !level.getBlockState(pos.above()).is(BlockTags.FENCES)
                        ? new SilkSpan(post.relative(along), along, offset - 1) : null;
            }
            if (!state.isAir()) return null;
        }
        return null;
    }

    static SilkSpan existing(LevelReader level, BlockPos pos, Direction along) {
        BlockPos start = pos;
        for (int i = 0; i < MAX_GAP && SilkLineBlock.matches(level, start.relative(along.getOpposite()), along); i++) {
            start = start.relative(along.getOpposite());
        }
        if (!level.getBlockState(start.relative(along.getOpposite())).is(BlockTags.FENCES)) return null;
        for (int i = 1; i <= MAX_GAP; i++) {
            BlockPos next = start.relative(along, i);
            if (level.getBlockState(next).is(BlockTags.FENCES)) return new SilkSpan(start, along, i);
            if (!SilkLineBlock.matches(level, next, along)) return null;
        }
        return null;
    }

    BlockPos piece(int i) {
        return first.relative(along, i);
    }

    BlockPos coveredPost(LevelReader level) {
        for (BlockPos post : new BlockPos[]{first.relative(along.getOpposite()), piece(length)}) {
            // A low roof over either post. Leave the spinning space itself clear.
            for (int h = 1; h <= 2; h++) {
                BlockPos roof = post.above(h);
                BlockState state = level.getBlockState(roof);
                if (!state.is(BlockTags.FENCES) && state.isFaceSturdy(level, roof, Direction.DOWN)) return post;
            }
        }
        return null;
    }
}
