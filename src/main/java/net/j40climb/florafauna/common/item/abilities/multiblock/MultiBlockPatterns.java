package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiBlockPatterns {
    private static final int STAIR_STEPS = 5;
    private static final int STAIR_HEIGHT = 5;
    private static final List<ItemStack> NETHERITE_TOOLS = List.of(
            new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(Items.NETHERITE_SHOVEL), new ItemStack(Items.NETHERITE_HOE));

    private MultiBlockPatterns() {}

    /** Positions are computed from the original hit, before any player break callbacks run. */
    public record Plan(List<BlockPos> blocks, List<BlockPos> stairs, Direction stairFacing) {
        BlockState stairState() {
            return Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, stairFacing);
        }
    }

    public static Plan plan(Player player, ItemStack tool, BlockPos center, Direction face) {
        MiningModeData mode = tool.getOrDefault(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);
        Level level = player.level();
        if (!isMineable(level, player, tool, mode, center)) {
            return new Plan(List.of(), List.of(), face);
        }
        return switch (mode.shape()) {
            case SINGLE -> new Plan(List.of(center), List.of(), face);
            case FLAT_3X3, FLAT_5X5, FLAT_7X7 -> {
                List<BlockPos> blocks = new ArrayList<>();
                int radius = mode.radius();
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        BlockPos pos = switch (face.getAxis()) {
                            case X -> center.offset(0, b, a);
                            case Y -> center.offset(a, 0, b);
                            case Z -> center.offset(a, b, 0);
                        };
                        if (isMineable(level, player, tool, mode, pos)) blocks.add(pos);
                    }
                }
                yield new Plan(List.copyOf(blocks), List.of(), face);
            }
            case SHAPELESS -> new Plan(vein(player, tool, mode, center), List.of(), face);
            case TUNNEL_UP, TUNNEL_DOWN -> tunnel(player, tool, mode, center, face);
            case WALKABLE_TUNNEL, LARGE_TUNNEL, BORE -> straightTunnel(player, tool, mode, center, face);
        };
    }

    private static List<BlockPos> vein(Player player, ItemStack tool, MiningModeData mode, BlockPos center) {
        Level level = player.level();
        var ore = level.getBlockState(center).getBlock();
        var queue = new ArrayDeque<BlockPos>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> selected = new LinkedHashSet<>();
        queue.add(center);
        visited.add(center);
        selected.add(center);
        int radius = mode.radius();
        while (!queue.isEmpty() && selected.size() < mode.maxBlocksToBreak()) {
            BlockPos current = queue.remove();
            // Coordinate iteration gives equal-distance candidates a stable order on both sides.
            for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-radius, -radius, -radius),
                    current.offset(radius, radius, radius))) {
                BlockPos pos = neighbor.immutable();
                if (visited.add(pos) && level.hasChunkAt(pos) && level.getBlockState(pos).is(ore)
                        && isMineable(level, player, tool, mode, pos)) {
                    selected.add(pos);
                    if (selected.size() == mode.maxBlocksToBreak()) break;
                    queue.add(pos);
                }
            }
        }
        return List.copyOf(selected);
    }

    private static Plan straightTunnel(Player player, ItemStack tool, MiningModeData mode, BlockPos center, Direction face) {
        Direction travel = face.getOpposite();
        boolean vertical = travel.getAxis().isVertical();
        Direction heading = vertical ? player.getDirection() : travel;
        Direction across = heading.getClockWise();
        Direction up = vertical ? heading : Direction.UP;
        boolean large = mode.shape() == MiningShape.LARGE_TUNNEL;
        boolean bore = mode.shape() == MiningShape.BORE;
        int widthRadius = large ? 1 : 0;
        int height = bore ? 1 : 3;
        int depth = bore ? 7 : 5;
        // A wall hit anchors a walkable tunnel at foot level. Other cross sections center on the hit.
        int base = large || (vertical && !bore) ? -1 : 0;

        List<BlockPos> blocks = new ArrayList<>();
        for (int step = 0; step < depth; step++) {
            BlockPos slice = center.relative(travel, step);
            for (int x = -widthRadius; x <= widthRadius; x++) {
                for (int y = base; y < base + height; y++) {
                    BlockPos pos = slice.relative(across, x).relative(up, y);
                    if (isMineable(player.level(), player, tool, mode, pos)) blocks.add(pos);
                }
            }
        }
        return new Plan(List.copyOf(blocks), List.of(), face);
    }

    private static Plan tunnel(Player player, ItemStack tool, MiningModeData mode, BlockPos center, Direction face) {
        boolean ascending = mode.shape() == MiningShape.TUNNEL_UP;
        if (face == Direction.DOWN || (ascending && face == Direction.UP)) {
            return new Plan(List.of(center), List.of(), face);
        }
        Direction travel = face == Direction.UP ? player.getDirection() : face.getOpposite();
        int baseOffset = face == Direction.UP ? 0 : ascending ? -1 : -2;
        Level level = player.level();
        List<BlockPos> blocks = new ArrayList<>();
        List<BlockPos> stairs = new ArrayList<>();
        for (int step = 0; step < STAIR_STEPS; step++) {
            int floorOffset = baseOffset + (ascending ? step : -step);
            BlockPos floor = center.relative(travel, step).above(floorOffset);
            for (int height = 0; height < STAIR_HEIGHT; height++) {
                BlockPos pos = floor.above(height);
                if (isMineable(level, player, tool, mode, pos)) {
                    blocks.add(pos);
                    if (height == 0) stairs.add(pos);
                } else if (height == 0 && canInteract(level, player, pos) && level.isEmptyBlock(pos)) {
                    // An empty tread needs a stair but has no block to break.
                    stairs.add(pos);
                }
            }
        }
        return new Plan(List.copyOf(blocks), List.copyOf(stairs), ascending ? travel : travel.getOpposite());
    }

    static boolean canInteract(Level level, Player player, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.mayInteract(player, pos);
    }

    static boolean isMineable(Level level, Player player, ItemStack tool, MiningModeData mode, BlockPos pos) {
        if (!canInteract(level, player, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        if (!mode.ignoreToolRestrictions()) return tool.isCorrectToolForDrops(state);
        return !state.requiresCorrectToolForDrops() || NETHERITE_TOOLS.stream().anyMatch(stack -> stack.isCorrectToolForDrops(state));
    }
}
