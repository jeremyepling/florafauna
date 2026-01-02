package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles client-side visual feedback for multi-block mining (crumbling animations).
 * Maintains state for tracking mining progress across game ticks.
 */
public final class MultiBlockVisualFeedback {
    private static BlockPos destroyPos = BlockPos.ZERO;
    private static int gameTicksMining = 0;

    private MultiBlockVisualFeedback() {} // Utility class

    /**
     * Processes left-click block events to manage visual feedback for multi-block mining.
     */
    public static void processLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);

        switch (event.getAction()) {
            case START -> {
                if (level.isClientSide()) {
                    gameTicksMining = 0;
                    destroyPos = blockPos;
                }
            }
            case STOP, ABORT -> MultiBlockBreaker.cancelDestroyProgress(blockPos, player);
            case CLIENT_HOLD -> {
                if (blockPos.equals(destroyPos)) {
                    gameTicksMining++;
                } else {
                    gameTicksMining = 0;
                    destroyPos = blockPos;
                }
                MultiBlockBreaker.updateDestroyProgress(level, blockState, blockPos, player);
            }
        }
    }

    /**
     * Gets the current number of game ticks spent mining the current block.
     */
    public static int getGameTicksMining() {
        return gameTicksMining;
    }

    /**
     * Gets the current block position being destroyed.
     */
    public static BlockPos getDestroyPos() {
        return destroyPos;
    }
}
