package net.j40climb.florafauna.client;

import net.j40climb.florafauna.component.MiningModeData;
import net.j40climb.florafauna.component.MiningShape;
import net.j40climb.florafauna.component.ModDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.stream.Collectors;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;

public class BlockBreakUtils {
    public static BlockPos destroyPos = BlockPos.ZERO;
    public static int gameTicksMining = 0;

    public static void doExtraCrumblings(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) { //Client and Server
            if (level.isClientSide) {
                gameTicksMining = 0;
                destroyPos = blockPos;
            }
        }
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) { //Server Only
            cancelBreaks(blockPos, player);
        }

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) { //Client Only
            if (blockPos.equals(destroyPos)) {
                gameTicksMining++;
            } else {
                gameTicksMining = 0;
                destroyPos = blockPos;
            }
            incrementDestroyProgress(level, blockState, blockPos, player);
        }
    }

    static void incrementDestroyProgress(Level level, BlockState blockState, BlockPos pPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBroken(pPos, player);
        int i = gameTicksMining;
        float f = blockState.getDestroyProgress(player, player.level(), pPos) * (float) (i + 1);
        int j = (int) (f * 10.0F);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pPos)) continue; //Let the vanilla mechanics handle the block we're hitting
            if (level.isClientSide)
                level.destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, j);
            else
                sendDestroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1, (ServerPlayer) player);
        }
    }

    static void cancelBreaks(BlockPos pPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBroken(pPos, player);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(pPos)) continue; //Let the vanilla mechanics handle the block we're hitting
            player.level().destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1);
        }
    }

    public static Set<BlockPos> getBlocksToBeBroken(BlockPos initalBlockPos, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        float maxDistance = 6f; // Define the maximum raycast distance
        BlockHitResult traceResult = (BlockHitResult) raycastFromPlayer(player, maxDistance);

        switch (traceResult.getType()) {
            case HitResult.Type.BLOCK:
                MiningModeData miningModeData = player.getMainHandItem().getOrDefault(ModDataComponentTypes.MINING_MODE_DATA, new MiningModeData());
                switch (miningModeData.shape()) {
                    case MiningShape.SINGLE -> positions.add(initalBlockPos);
                    case MiningShape.FLAT_3X3, MiningShape.FLAT_5X5, MiningShape.FLAT_7X7 -> positions = findSurroundingBlocksInFlatSquare(player, initalBlockPos, traceResult, miningModeData.radius());
                    case MiningShape.SHAPELESS -> {
                        BlockState blockState = player.level().getBlockState(traceResult.getBlockPos());
                        positions = findSurroundingSameBlocksShapeless(player, blockState, initalBlockPos, miningModeData.maxBlocksToBreak(), miningModeData.radius());
                    }
                }
            case HitResult.Type.ENTITY:
                // Handle entity hit
                return positions;
            case HitResult.Type.MISS:
                // Handle miss
                return positions;
        }
        return positions;
    }

    private static Set<BlockPos> findSurroundingBlocksInFlatSquare(Player player, BlockPos initalBlockPos, BlockHitResult traceResult, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks

        for(int x = -radius; x <= radius; x++) {
            for(int y = -radius; y <= radius; y++) {
                BlockState blockState = player.level().getBlockState(traceResult.getBlockPos());
                BlockPos blockPosToCheck = null;

                if (player.getMainHandItem().isCorrectToolForDrops(blockState)) { // Is the target block mineable
                    if (traceResult.getDirection() == Direction.DOWN || traceResult.getDirection() == Direction.UP) {
                        // Get the position of the block around the target
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

    /**
     * from <a href="https://github.com/Direwolf20-MC/JustDireThings/blob/main/src/main/java/com/direwolf20/justdirethings/common/items/interfaces/Helpers.java">...</a>
     */

    private static Set<BlockPos> findSurroundingSameBlocksShapeless(Player player, BlockState initialBlockState, BlockPos initialBlockPos, int maxBreak, int radius) {
        Set<BlockPos> foundBlocks = new HashSet<>(); //The matching Blocks
        Queue<BlockPos> blocksToScan = new LinkedList<>(); //A list of blocks to scan around found blocks
        Set<BlockPos> scannedBlocks = new HashSet<>(); //A list of blocks we already checked
        Level level = player.level();

        foundBlocks.add(initialBlockPos); //Obviously the block we broke is included in the return!
        blocksToScan.add(initialBlockPos); //Start scanning around the block we broke

        while (!blocksToScan.isEmpty()) {
            BlockPos posToCheck = blocksToScan.poll(); //Get the next blockPos to scan around

            if (!scannedBlocks.add(posToCheck))
                continue; //Don't check blockPos we've checked before

            Set<BlockPos> matchingBlocks = BlockPos.betweenClosedStream(posToCheck.offset(-radius, -radius, -radius), posToCheck.offset(radius, radius, radius))
                    .filter(blockPos -> level.getBlockState(blockPos).is(initialBlockState.getBlock()))
                    .map(BlockPos::immutable)
                    .collect(Collectors.toSet());

            for (BlockPos toAdd : matchingBlocks) { //Ensure we don't go beyond maxBreak
                if (foundBlocks.size() < maxBreak) {
                    if (isValidToMine(player, toAdd)) {
                        foundBlocks.add(toAdd); //Add all the blocks we found to our set of found blocks
                    }
                    if (!scannedBlocks.contains(toAdd))
                        if (isValidToMine(player, toAdd)) {
                            blocksToScan.add(toAdd); //Add all the blocks we found to be checked as well
                        }
                } else
                    return foundBlocks;
            }
        }
        return foundBlocks;
    }

    private static boolean isValidToMine(Player player, BlockPos blockPosToAdd) {
        BlockState blockStateToAdd = player.level().getBlockState(blockPosToAdd);
        // Are the surrounding blocks mineable
        return player.getMainHandItem().isCorrectToolForDrops(blockStateToAdd) && !blockStateToAdd.isAir();
    }

    private static int generatePosHash(BlockPos blockPos) {
        return (31 * 31 * blockPos.getX()) + (31 * blockPos.getY()) + blockPos.getZ(); //For now this is probably good enough, will add more randomness if needed
    }

    private static void sendDestroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress, ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
    }

    /**
     * Teleport drops from <a href="https://github.com/Direwolf20-MC/JustDireThings/blob/main/src/main/java/com/direwolf20/justdirethings/common/items/interfaces/Helpers.java#L352">...</a>
     * TODO need to implement this
     */
    public static ItemStack teleportDrop(ItemStack itemStack, IItemHandler handler) {
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, itemStack, false);
        return leftover;
    }

    public static void teleportDrops(List<ItemStack> drops, IItemHandler handler) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack leftover = teleportDrop(drop, handler);
            if (!leftover.isEmpty()) {
                leftovers.add(leftover);
            }
        }
        // Clear the original drops list and add all leftovers to it
        drops.clear();
        drops.addAll(leftovers);
    }
}
