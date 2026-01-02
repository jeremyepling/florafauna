package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.*;
import java.util.stream.Collectors;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;

public class MiningModeBlockInteractions {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();
    private static final Set<BlockPos> POTENTIAL_STAIR_BLOCKS = new HashSet<>();
    public static BlockPos destroyPos = BlockPos.ZERO;
    public static int gameTicksMining = 0;
    private static final int STAIR_STEPS = 5;
    private static final int STAIR_HEIGHT = 5;

    public static void doExtraCrumblings(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            if (level.isClientSide()) {
                gameTicksMining = 0;
                destroyPos = blockPos;
            }
        }
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            cancelBreaks(blockPos, player);
        }

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            if (blockPos.equals(destroyPos)) {
                gameTicksMining++;
            } else {
                gameTicksMining = 0;
                destroyPos = blockPos;
            }
            incrementDestroyProgress(level, blockState, blockPos, player);
        }
    }

    static void incrementDestroyProgress(Level level, BlockState blockState, BlockPos initialBlockPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBrokenWithMiningMode(initialBlockPos, player);
        int i = gameTicksMining;
        float f = blockState.getDestroyProgress(player, player.level(), initialBlockPos) * (float) (i + 1);
        int j = (int) (f * 10.0F);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(initialBlockPos)) continue;
            if (level.isClientSide())
                level.destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, j);
            else
                sendDestroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1, (ServerPlayer) player);
        }
    }

    static void cancelBreaks(BlockPos pPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBrokenWithMiningMode(pPos, player);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pPos)) continue;
            player.level().destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1);
        }
    }

    public static Set<BlockPos> getBlocksToBeBrokenWithMiningMode(BlockPos initalBlockPos, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        float maxDistance = 6f;
        BlockHitResult traceResult = (BlockHitResult) raycastFromPlayer(player, maxDistance);

        switch (traceResult.getType()) {
            case HitResult.Type.BLOCK:
                MiningModeData miningModeData = player.getMainHandItem().getOrDefault(RegisterDataComponentTypes.MINING_MODE_DATA, MiningModeData.DEFAULT);
                switch (miningModeData.shape()) {
                    case MiningShape.SINGLE -> positions.add(initalBlockPos);
                    case MiningShape.FLAT_3X3, MiningShape.FLAT_5X5, MiningShape.FLAT_7X7 -> positions = findSurroundingBlocksInFlatSquare(player, initalBlockPos, traceResult, miningModeData.radius());
                    case MiningShape.SHAPELESS -> {
                        BlockState blockState = player.level().getBlockState(traceResult.getBlockPos());
                        positions = findSurroundingSameBlocksShapeless(player, blockState, initalBlockPos, miningModeData.maxBlocksToBreak(), miningModeData.radius());
                    }
                    case MiningShape.TUNNEL_UP -> positions = findTunnelPatternBlocks(player, initalBlockPos, traceResult, true);
                    case MiningShape.TUNNEL_DOWN -> positions = findTunnelPatternBlocks(player, initalBlockPos, traceResult, false);
                }
            case HitResult.Type.ENTITY:
                return positions;
            case HitResult.Type.MISS:
                return positions;
        }
        return positions;
    }

    private static Set<BlockPos> findSurroundingBlocksInFlatSquare(Player player, BlockPos initalBlockPos, BlockHitResult traceResult, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>();

        for(int x = -radius; x <= radius; x++) {
            for(int y = -radius; y <= radius; y++) {
                BlockPos blockPosToCheck = null;

                if (isValidToMine(player, initalBlockPos)) {
                    if (traceResult.getDirection() == Direction.DOWN || traceResult.getDirection() == Direction.UP) {
                        blockPosToCheck = new BlockPos(initalBlockPos.getX() + x, initalBlockPos.getY(), initalBlockPos.getZ() + y);
                        if (isValidToMine(player, blockPosToCheck)) {
                            foundBlocks.add(blockPosToCheck);
                        }
                    }
                    if (traceResult.getDirection() == Direction.NORTH || traceResult.getDirection() == Direction.SOUTH) {
                        blockPosToCheck = new BlockPos(initalBlockPos.getX() + x, initalBlockPos.getY() + y, initalBlockPos.getZ());
                        if (isValidToMine(player, blockPosToCheck)) {
                            foundBlocks.add(blockPosToCheck);
                        }
                    }
                    if (traceResult.getDirection() == Direction.EAST || traceResult.getDirection() == Direction.WEST) {
                        blockPosToCheck = new BlockPos(initalBlockPos.getX(), initalBlockPos.getY() + y, initalBlockPos.getZ() + x);
                        if (isValidToMine(player, blockPosToCheck)) {
                            foundBlocks.add(blockPosToCheck);
                        }
                    }
                }
            }
        }
        return foundBlocks;
    }

    private static Set<BlockPos> findSurroundingSameBlocksShapeless(Player player, BlockState initialBlockState, BlockPos initialBlockPos, int maxBreak, int radius) {
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

            Set<BlockPos> matchingBlocks = BlockPos.betweenClosedStream(posToCheck.offset(-radius, -radius, -radius), posToCheck.offset(radius, radius, radius))
                    .filter(blockPos -> level.getBlockState(blockPos).is(initialBlockState.getBlock()))
                    .map(BlockPos::immutable)
                    .collect(Collectors.toSet());

            for (BlockPos toAdd : matchingBlocks) {
                if (foundBlocks.size() < maxBreak) {
                    if (isValidToMine(player, toAdd)) {
                        foundBlocks.add(toAdd);
                    }
                    if (!scannedBlocks.contains(toAdd))
                        if (isValidToMine(player, toAdd)) {
                            blocksToScan.add(toAdd);
                        }
                } else
                    return foundBlocks;
            }
        }
        return foundBlocks;
    }

    public record TunnelLogic(boolean buildTunnel, int yOffset) {};

    public static TunnelLogic getTunnelYLogic(BlockHitResult hitResult, boolean ascending) {
        boolean hitFromTop = hitResult.getDirection() == Direction.UP;
        boolean hitFromBottom = hitResult.getDirection() == Direction.DOWN;
        boolean hitFromSide = !hitFromTop && !hitFromBottom;

        if (ascending) {
            if (hitFromSide) return new TunnelLogic(true, -1);
            if (hitFromTop || hitFromBottom) return new TunnelLogic(false, 0);
        }
        else {
            if (hitFromSide) return new TunnelLogic(true, -2);
            if (hitFromTop) return new TunnelLogic(true, 0);
            if (hitFromBottom) return new TunnelLogic(false, 0);
        }
        return new TunnelLogic(false, 0);
    }

    private static Set<BlockPos> findTunnelPatternBlocks(Player player, BlockPos initialBlockPos, BlockHitResult hitResult, boolean ascending) {
        Set<BlockPos> foundBlocks = new HashSet<>();
        Direction horizontalDir = getHorizontalMiningDirection(player, hitResult);

        if (!getTunnelYLogic(hitResult, ascending).buildTunnel) return Collections.singleton(initialBlockPos);

        for (int step = 0; step < STAIR_STEPS; step++) {
            int yFlip = ascending ? step : -step;
            int yOffset = getTunnelYLogic(hitResult, ascending).yOffset;
            int floorY = initialBlockPos.getY() + yOffset + yFlip;

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

    private static boolean isValidToMine(Player player, BlockPos blockPosToCheck) {
        BlockState blockStateToCheck = player.level().getBlockState(blockPosToCheck);
        return player.getMainHandItem().isCorrectToolForDrops(blockStateToCheck) && !blockStateToCheck.isAir();
    }

    private static int generatePosHash(BlockPos blockPos) {
        return (31 * 31 * blockPos.getX()) + (31 * blockPos.getY()) + blockPos.getZ();
    }

    private static void sendDestroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress, ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
    }

    private static Direction getHorizontalMiningDirection(Player player, BlockHitResult hitResult) {
        Direction hitFace = hitResult.getDirection();
        if (hitFace.getAxis().isHorizontal()) {
            return hitFace.getOpposite();
        }
        return player.getDirection();
    }

    public static boolean breakWithMiningMode(ItemStack mainHandItem, BlockPos initialBlockPos, ServerPlayer serverPlayer, Level level) {
        MiningModeData miningModeData = mainHandItem.get(RegisterDataComponentTypes.MINING_MODE_DATA);
        BlockHitResult hitResult = null;

        if(miningModeData != null ) {
            if (HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return false;
            }

            if (mainHandItem.isCorrectToolForDrops(level.getBlockState(initialBlockPos))) {
                Direction horizontalDirForTunnel = null;
                if (miningModeData.shape() == MiningShape.TUNNEL_UP || miningModeData.shape() == MiningShape.TUNNEL_DOWN) {
                    float maxDistance = 6f;
                    hitResult = (BlockHitResult) raycastFromPlayer(serverPlayer, maxDistance);
                    if (hitResult.getType() == HitResult.Type.BLOCK) {
                        horizontalDirForTunnel = getHorizontalMiningDirection(serverPlayer, hitResult);
                    }
                }

                for (BlockPos blockPos : getBlocksToBeBrokenWithMiningMode(initialBlockPos, serverPlayer)) {
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
                    if (getTunnelYLogic(hitResult, ascending).buildTunnel) {
                        placeStairsForPattern(level, initialBlockPos, horizontalDirForTunnel, hitResult, ascending);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void placeStairsForPattern(Level level, BlockPos initialBlockPos, Direction horizontalDir, BlockHitResult hitResult, boolean ascending) {

        for (int step = 0; step < STAIR_STEPS; step++) {

            int yFlip = ascending ? step : -step;
            int yOffset = getTunnelYLogic(hitResult, ascending).yOffset;
            int floorY = initialBlockPos.getY() + yOffset + yFlip;

            BlockPos stepBase = initialBlockPos.relative(horizontalDir, step);
            BlockPos stairPos = new BlockPos(stepBase.getX(), floorY, stepBase.getZ());

            if ((level.getBlockState(stairPos).isAir() && !ascending) || (level.getBlockState(stairPos).isAir() && ascending && POTENTIAL_STAIR_BLOCKS.contains(stairPos))) {
                Direction stairFacing = ascending ? horizontalDir : horizontalDir.getOpposite();

                BlockState stairState = Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, stairFacing)
                        .setValue(StairBlock.HALF, Half.BOTTOM);
                level.setBlock(stairPos, stairState, 3);
            }
        }
    }

}
