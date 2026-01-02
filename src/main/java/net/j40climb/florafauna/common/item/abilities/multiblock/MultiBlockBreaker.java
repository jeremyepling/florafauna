package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.Set;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;
import static net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns.*;

/**
 * Handles server-side multi-block breaking operations and stair placement for tunnel modes.
 */
public final class MultiBlockBreaker {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();
    private static final Set<BlockPos> POTENTIAL_STAIR_BLOCKS = new HashSet<>();

    private MultiBlockBreaker() {} // Utility class

    /**
     * Breaks multiple blocks based on mining mode configuration.
     *
     * @param mainHandItem The item being used to break blocks
     * @param initialBlockPos The block the player targeted
     * @param serverPlayer The player breaking blocks
     * @param level The world level
     * @return true if the original block break event should be cancelled (for stair placement)
     */
    public static boolean breakBlocks(ItemStack mainHandItem, BlockPos initialBlockPos, ServerPlayer serverPlayer, Level level) {
        MiningModeData miningModeData = mainHandItem.get(ModRegistry.MULTI_BLOCK_MINING);
        BlockHitResult hitResult = null;

        if (miningModeData == null) {
            return false;
        }

        if (HARVESTED_BLOCKS.contains(initialBlockPos)) {
            return false;
        }

        if (!mainHandItem.isCorrectToolForDrops(level.getBlockState(initialBlockPos))) {
            return false;
        }

        Direction horizontalDirForTunnel = null;
        if (miningModeData.shape() == MiningShape.TUNNEL_UP || miningModeData.shape() == MiningShape.TUNNEL_DOWN) {
            float maxDistance = 6f;
            hitResult = (BlockHitResult) raycastFromPlayer(serverPlayer, maxDistance);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                horizontalDirForTunnel = getHorizontalMiningDirection(serverPlayer, hitResult);
            }
        }

        for (BlockPos blockPos : getBlocksToBreak(initialBlockPos, serverPlayer)) {
            if (blockPos == initialBlockPos || !mainHandItem.isCorrectToolForDrops(level.getBlockState(blockPos))) {
                continue;
            }

            HARVESTED_BLOCKS.add(blockPos);
            POTENTIAL_STAIR_BLOCKS.add(blockPos);
            serverPlayer.gameMode.destroyBlock(blockPos);
            HARVESTED_BLOCKS.remove(blockPos);
        }

        if (horizontalDirForTunnel != null) {
            boolean ascending = miningModeData.shape() == MiningShape.TUNNEL_UP;
            TunnelLogic tunnelLogic = getTunnelYLogic(hitResult, ascending);
            if (tunnelLogic.buildTunnel()) {
                placeStairs(level, initialBlockPos, horizontalDirForTunnel, hitResult, ascending);
                return true;
            }
        }

        return false;
    }

    /**
     * Places stairs for tunnel mining patterns.
     */
    public static void placeStairs(Level level, BlockPos initialBlockPos, Direction horizontalDir, BlockHitResult hitResult, boolean ascending) {
        for (int step = 0; step < STAIR_STEPS; step++) {
            int yFlip = ascending ? step : -step;
            int yOffset = getTunnelYLogic(hitResult, ascending).yOffset();
            int floorY = initialBlockPos.getY() + yOffset + yFlip;

            BlockPos stepBase = initialBlockPos.relative(horizontalDir, step);
            BlockPos stairPos = new BlockPos(stepBase.getX(), floorY, stepBase.getZ());

            boolean shouldPlace = (level.getBlockState(stairPos).isAir() && !ascending) ||
                    (level.getBlockState(stairPos).isAir() && ascending && POTENTIAL_STAIR_BLOCKS.contains(stairPos));

            if (shouldPlace) {
                Direction stairFacing = ascending ? horizontalDir : horizontalDir.getOpposite();

                BlockState stairState = Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, stairFacing)
                        .setValue(StairBlock.HALF, Half.BOTTOM);
                level.setBlock(stairPos, stairState, 3);
            }
        }
    }

    /**
     * Sends block destruction progress to the client for visual feedback.
     */
    public static void sendDestroyProgress(int breakerId, BlockPos pos, int progress, ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundBlockDestructionPacket(breakerId, pos, progress));
    }

    /**
     * Updates the destruction progress visual for all blocks in the mining pattern.
     */
    public static void updateDestroyProgress(Level level, BlockState blockState, BlockPos initialBlockPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBreak(initialBlockPos, player);
        int i = MultiBlockVisualFeedback.getGameTicksMining();
        float f = blockState.getDestroyProgress(player, player.level(), initialBlockPos) * (float) (i + 1);
        int j = (int) (f * 10.0F);

        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(initialBlockPos)) continue;
            if (level.isClientSide()) {
                level.destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, j);
            } else {
                sendDestroyProgress(player.getId() + generatePosHash(blockPos), blockPos, -1, (ServerPlayer) player);
            }
        }
    }

    /**
     * Cancels the destruction progress visual for all blocks in the mining pattern.
     */
    public static void cancelDestroyProgress(BlockPos pos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBreak(pos, player);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pos)) continue;
            player.level().destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1);
        }
    }
}
