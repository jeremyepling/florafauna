package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;
import java.util.stream.Collectors;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;

/**
 * Pure logic for determining which blocks should be affected by multi-block mining patterns.
 * This class is stateless and contains no side effects - it only calculates block positions.
 */
public final class MultiBlockPatterns {
    public static final int STAIR_STEPS = 5;
    public static final int STAIR_HEIGHT = 5;

    // Cached netherite tool stacks for bypass checks
    private static final ItemStack NETHERITE_PICKAXE = new ItemStack(Items.NETHERITE_PICKAXE);
    private static final ItemStack NETHERITE_AXE = new ItemStack(Items.NETHERITE_AXE);
    private static final ItemStack NETHERITE_SHOVEL = new ItemStack(Items.NETHERITE_SHOVEL);
    private static final ItemStack NETHERITE_HOE = new ItemStack(Items.NETHERITE_HOE);

    private MultiBlockPatterns() {} // Utility class

    /**
     * Checks if any netherite tool can break the given block state.
     * Used by both pattern generation and block breaking to bypass tool restrictions.
     */
    public static boolean canNetheriteToolBreak(BlockState blockState) {
        return NETHERITE_PICKAXE.isCorrectToolForDrops(blockState) ||
               NETHERITE_AXE.isCorrectToolForDrops(blockState) ||
               NETHERITE_SHOVEL.isCorrectToolForDrops(blockState) ||
               NETHERITE_HOE.isCorrectToolForDrops(blockState);
    }

    /**
     * Gets all blocks that should be broken based on the player's mining mode.
     *
     * @param initialBlockPos The block the player is targeting
     * @param player The player doing the mining
     * @return Set of block positions to break
     */
    public static Set<BlockPos> getBlocksToBreak(BlockPos initialBlockPos, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        float maxDistance = 6f;
        BlockHitResult traceResult = (BlockHitResult) raycastFromPlayer(player, maxDistance);

        if (traceResult.getType() != HitResult.Type.BLOCK) {
            return positions;
        }

        MiningModeData miningModeData = player.getMainHandItem()
                .getOrDefault(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);

        return switch (miningModeData.shape()) {
            case SINGLE -> Collections.singleton(initialBlockPos);
            case FLAT_3X3, FLAT_5X5, FLAT_7X7 -> findFlatSquareBlocks(player, initialBlockPos, traceResult, miningModeData.radius());
            case SHAPELESS -> {
                BlockState blockState = player.level().getBlockState(traceResult.getBlockPos());
                yield findShapelessBlocks(player, blockState, initialBlockPos, miningModeData.maxBlocksToBreak(), miningModeData.radius());
            }
            case TUNNEL_UP -> findTunnelBlocks(player, initialBlockPos, traceResult, true);
            case TUNNEL_DOWN -> findTunnelBlocks(player, initialBlockPos, traceResult, false);
        };
    }

    /**
     * Finds blocks in a flat square pattern perpendicular to the hit face.
     */
    public static Set<BlockPos> findFlatSquareBlocks(Player player, BlockPos initialBlockPos, BlockHitResult traceResult, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                BlockPos blockPosToCheck = null;

                if (isValidToMine(player, initialBlockPos)) {
                    if (traceResult.getDirection() == Direction.DOWN || traceResult.getDirection() == Direction.UP) {
                        blockPosToCheck = new BlockPos(initialBlockPos.getX() + x, initialBlockPos.getY(), initialBlockPos.getZ() + y);
                    }
                    if (traceResult.getDirection() == Direction.NORTH || traceResult.getDirection() == Direction.SOUTH) {
                        blockPosToCheck = new BlockPos(initialBlockPos.getX() + x, initialBlockPos.getY() + y, initialBlockPos.getZ());
                    }
                    if (traceResult.getDirection() == Direction.EAST || traceResult.getDirection() == Direction.WEST) {
                        blockPosToCheck = new BlockPos(initialBlockPos.getX(), initialBlockPos.getY() + y, initialBlockPos.getZ() + x);
                    }

                    if (blockPosToCheck != null && isValidToMine(player, blockPosToCheck)) {
                        foundBlocks.add(blockPosToCheck);
                    }
                }
            }
        }
        return foundBlocks;
    }

    /**
     * Finds connected blocks of the same type using flood-fill algorithm.
     */
    public static Set<BlockPos> findShapelessBlocks(Player player, BlockState initialBlockState, BlockPos initialBlockPos, int maxBreak, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>();
        Queue<BlockPos> blocksToScan = new LinkedList<>();
        Set<BlockPos> scannedBlocks = new HashSet<>();
        Level level = player.level();

        foundBlocks.add(initialBlockPos);
        blocksToScan.add(initialBlockPos);

        while (!blocksToScan.isEmpty()) {
            BlockPos posToCheck = blocksToScan.poll();

            if (!scannedBlocks.add(posToCheck))
                continue;

            Set<BlockPos> matchingBlocks = BlockPos.betweenClosedStream(
                            posToCheck.offset(-radius, -radius, -radius),
                            posToCheck.offset(radius, radius, radius))
                    .filter(blockPos -> level.getBlockState(blockPos).is(initialBlockState.getBlock()))
                    .map(BlockPos::immutable)
                    .collect(Collectors.toSet());

            for (BlockPos toAdd : matchingBlocks) {
                if (foundBlocks.size() < maxBreak) {
                    if (isValidToMine(player, toAdd)) {
                        foundBlocks.add(toAdd);
                    }
                    if (!scannedBlocks.contains(toAdd) && isValidToMine(player, toAdd)) {
                        blocksToScan.add(toAdd);
                    }
                } else {
                    return foundBlocks;
                }
            }
        }
        return foundBlocks;
    }

    /**
     * Finds blocks in a stair/tunnel pattern going up or down.
     */
    public static Set<BlockPos> findTunnelBlocks(Player player, BlockPos initialBlockPos, BlockHitResult hitResult, boolean ascending) {
        Set<BlockPos> foundBlocks = new HashSet<>();
        Direction horizontalDir = getHorizontalMiningDirection(player, hitResult);

        TunnelLogic tunnelLogic = getTunnelYLogic(hitResult, ascending);
        if (!tunnelLogic.buildTunnel()) {
            return Collections.singleton(initialBlockPos);
        }

        for (int step = 0; step < STAIR_STEPS; step++) {
            int yFlip = ascending ? step : -step;
            int floorY = initialBlockPos.getY() + tunnelLogic.yOffset() + yFlip;

            BlockPos stepBase = initialBlockPos.relative(horizontalDir, step);
            stepBase = new BlockPos(stepBase.getX(), floorY, stepBase.getZ());

            for (int h = 0; h < STAIR_HEIGHT; h++) {
                BlockPos blockToBreak = stepBase.above(h);
                if (isValidToMine(player, blockToBreak)) {
                    foundBlocks.add(blockToBreak);
                }
            }
        }

        return foundBlocks;
    }

    /**
     * Record containing tunnel construction logic based on hit direction.
     */
    public record TunnelLogic(boolean buildTunnel, int yOffset) {}

    /**
     * Determines tunnel building logic based on where the player hit the block.
     */
    public static TunnelLogic getTunnelYLogic(BlockHitResult hitResult, boolean ascending) {
        boolean hitFromTop = hitResult.getDirection() == Direction.UP;
        boolean hitFromBottom = hitResult.getDirection() == Direction.DOWN;
        boolean hitFromSide = !hitFromTop && !hitFromBottom;

        if (ascending) {
            if (hitFromSide) return new TunnelLogic(true, -1);
            if (hitFromTop || hitFromBottom) return new TunnelLogic(false, 0);
        } else {
            if (hitFromSide) return new TunnelLogic(true, -2);
            if (hitFromTop) return new TunnelLogic(true, 0);
            if (hitFromBottom) return new TunnelLogic(false, 0);
        }
        return new TunnelLogic(false, 0);
    }

    /**
     * Determines the horizontal direction for tunnel mining.
     */
    public static Direction getHorizontalMiningDirection(Player player, BlockHitResult hitResult) {
        Direction hitFace = hitResult.getDirection();
        if (hitFace.getAxis().isHorizontal()) {
            return hitFace.getOpposite();
        }
        return player.getDirection();
    }

    /**
     * Checks if a block position is valid for mining with the player's current tool.
     * Respects the ignoreToolRestrictions flag from MiningModeData.
     */
    public static boolean isValidToMine(Player player, BlockPos blockPos) {
        BlockState blockState = player.level().getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        MiningModeData miningModeData = mainHandItem.get(FloraFaunaRegistry.MULTI_BLOCK_MINING);

        // If no mining mode data or ignoreToolRestrictions is true, check if netherite can break it
        if (miningModeData != null && miningModeData.ignoreToolRestrictions()) {
            return canNetheriteToolBreak(blockState);
        }

        return mainHandItem.isCorrectToolForDrops(blockState);
    }

    /**
     * Generates a unique hash for a block position (used for destroy progress tracking).
     */
    public static int generatePosHash(BlockPos blockPos) {
        return (31 * 31 * blockPos.getX()) + (31 * blockPos.getY()) + blockPos.getZ();
    }
}
