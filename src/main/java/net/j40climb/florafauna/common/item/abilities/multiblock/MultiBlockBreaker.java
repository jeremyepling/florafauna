package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;
import static net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns.*;

/**
 * Handles server-side multi-block breaking operations and stair placement for tunnel modes.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class MultiBlockBreaker {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();
    private static final Set<BlockPos> POTENTIAL_STAIR_BLOCKS = new HashSet<>();

    // Pending stair placements with the tick they were queued on
    // Only processed after at least one tick has passed
    private record PendingStairPlacement(long queuedAtTick, Level level, BlockPos initialBlockPos, Direction dir, BlockHitResult hit, boolean ascending) {}
    private static final Queue<PendingStairPlacement> PENDING_STAIR_PLACEMENTS = new ConcurrentLinkedQueue<>();

    private MultiBlockBreaker() {} // Utility class

    /**
     * Processes pending stair placements that were queued on a previous tick.
     * Only runs work when queue is non-empty, and ensures at least one tick has passed.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_STAIR_PLACEMENTS.isEmpty()) {
            return; // Fast exit - no work to do
        }

        long currentTick = event.getServer().getTickCount();

        // Process all items queued on previous ticks (not current tick)
        while (!PENDING_STAIR_PLACEMENTS.isEmpty() &&
               PENDING_STAIR_PLACEMENTS.peek().queuedAtTick() < currentTick) {
            PendingStairPlacement pending = PENDING_STAIR_PLACEMENTS.poll();
            placeStairs(pending.level(), pending.initialBlockPos(), pending.dir(), pending.hit(), pending.ascending());
        }
    }

    /**
     * Breaks multiple blocks based on mining mode configuration.
     * <p>
     * Block validation is centralized in {@link MultiBlockPatterns#isValidToMine(Player, BlockPos)},
     * which checks if blocks can be mined based on the ignoreToolRestrictions flag.
     * When ignoreToolRestrictions is true, any block breakable by a netherite tool is valid.
     * The getBlocksToBreak() method uses this validation, so blocks returned are pre-validated.
     * <p>
     * For tunnel modes, stair placement is scheduled for the next tick to ensure all blocks
     * are broken before stairs are placed, avoiding the need to cancel the original event.
     *
     * @param mainHandItem The item being used to break blocks
     * @param initialBlockPos The block the player targeted
     * @param serverPlayer The player breaking blocks
     * @param level The world level
     */
    public static void breakBlocks(ItemStack mainHandItem, BlockPos initialBlockPos, ServerPlayer serverPlayer, Level level) {
        MiningModeData miningModeData = mainHandItem.get(FloraFaunaRegistry.MULTI_BLOCK_MINING);
        BlockHitResult hitResult = null;

        if (miningModeData == null) {
            return;
        }

        if (HARVESTED_BLOCKS.contains(initialBlockPos)) {
            return;
        }

        // Use shared validation logic (respects ignoreToolRestrictions flag)
        if (!isValidToMine(serverPlayer, initialBlockPos)) {
            return;
        }

        Direction horizontalDirForTunnel = null;
        if (miningModeData.shape() == MiningShape.TUNNEL_UP || miningModeData.shape() == MiningShape.TUNNEL_DOWN) {
            float maxDistance = 6f;
            hitResult = (BlockHitResult) raycastFromPlayer(serverPlayer, maxDistance);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                horizontalDirForTunnel = getHorizontalMiningDirection(serverPlayer, hitResult);
            }
        }

        // Break all blocks in the mining pattern except initialBlockPos (handled by original event)
        for (BlockPos blockPos : getBlocksToBreak(initialBlockPos, serverPlayer)) {
            // Skip initialBlockPos - the original BreakEvent breaks it after we return
            if (blockPos.equals(initialBlockPos)) {
                continue;
            }

            // HARVESTED_BLOCKS prevents infinite recursion: destroyBlock() triggers another
            // BreakEvent which calls breakBlocks() again - the check at method start exits early
            HARVESTED_BLOCKS.add(blockPos);
            // POTENTIAL_STAIR_BLOCKS tracks broken blocks for ascending tunnel stair placement
            // (stairs only placed where blocks existed, not in pre-existing air gaps)
            POTENTIAL_STAIR_BLOCKS.add(blockPos);
            serverPlayer.gameMode.destroyBlock(blockPos);
            // Remove after destroy so same position can be mined in future operations
            HARVESTED_BLOCKS.remove(blockPos);
        }

        if (horizontalDirForTunnel != null) {
            boolean ascending = miningModeData.shape() == MiningShape.TUNNEL_UP;
            TunnelLogic tunnelLogic = getTunnelYLogic(hitResult, ascending);
            if (tunnelLogic.buildTunnel()) {
                // Queue stair placement for next server tick - guarantees all blocks are broken first
                PENDING_STAIR_PLACEMENTS.add(new PendingStairPlacement(
                    level.getServer().getTickCount(),
                    level, initialBlockPos, horizontalDirForTunnel, hitResult, ascending
                ));
            }
        }
        // Original BreakEvent proceeds and breaks initialBlockPos
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
