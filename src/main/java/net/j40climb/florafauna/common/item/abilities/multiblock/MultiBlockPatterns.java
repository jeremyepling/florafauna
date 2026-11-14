package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class MultiBlockPatterns {
    private static final int STAIR_STEPS = 5;
    private static final int STAIR_HEIGHT = 5;
    private static final List<ItemStack> NETHERITE_TOOLS = List.of(
            new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_AXE),
            new ItemStack(Items.NETHERITE_SHOVEL), new ItemStack(Items.NETHERITE_HOE));

    private MultiBlockPatterns() {}

    /** Positions are computed from the original hit, before any player break callbacks run. */
    public record Plan(List<BlockPos> blocks, List<BlockPos> stairs, Direction stairFacing) {}

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
        };
    }

    private static List<BlockPos> vein(Player player, ItemStack tool, MiningModeData mode, BlockPos center) {
        Level level = player.level();
        boolean exact = tool.getOrDefault(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT).exactVein();
        Predicate<BlockState> matches = veinMatcher(level.getBlockState(center), exact);
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
                if (visited.add(pos) && level.hasChunkAt(pos) && matches.test(level.getBlockState(pos))
                        && isMineable(level, player, tool, mode, pos)) {
                    selected.add(pos);
                    if (selected.size() == mode.maxBlocksToBreak()) break;
                    queue.add(pos);
                }
            }
        }
        return List.copyOf(selected);
    }

    private static Predicate<BlockState> veinMatcher(BlockState origin, boolean exact) {
        Block block = origin.getBlock();
        if (exact || !isStoneOrDeepslateOre(origin)) return state -> state.is(block);

        List<TagKey<Block>> materials = origin.typeHolder().tags()
                .filter(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("ores/"))
                .toList();
        return state -> state.is(block) || isStoneOrDeepslateOre(state) && materials.stream().anyMatch(state::is);
    }

    private static boolean isStoneOrDeepslateOre(BlockState state) {
        return state.is(Tags.Blocks.ORES_IN_GROUND_STONE) || state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE);
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
