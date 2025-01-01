package net.j40climb.florafauna.item.custom;

import net.j40climb.florafauna.component.DataComponentTypes;
import net.j40climb.florafauna.component.MiningModeData;
import net.j40climb.florafauna.component.MiningShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static net.j40climb.florafauna.client.ClientUtils.raycastFromPlayer;

public class HammerItem extends DiggerItem {
    public HammerItem(Tier pTier, Properties pProperties) {
        // New tags in 1.21.30 make this easier #minecraft:iron_tier_destructible for all diggers or
        // #minecraft:is_pickaxe_item_destructible for pickaxe

        // TODO Changing this to ModTags.Blocks.PAXEL_MINEABLE causes a max networking error
        super(pTier, BlockTags.MINEABLE_WITH_PICKAXE, pProperties
                .component(DataComponentTypes.MINING_MODE_DATA, new MiningModeData(MiningShape.FLAT_SQUARE, 1, 9))
        );
    }

    static int gameTicksMining = 0;
    static BlockPos destroyPos = BlockPos.ZERO;

    public static Set<BlockPos> getBlocksToBeBroken(BlockPos initalBlockPos, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        float maxDistance = 6f; // Define the maximum raycast distance
        BlockHitResult traceResult = (BlockHitResult) raycastFromPlayer(player, maxDistance);

        switch (traceResult.getType()) {
            case HitResult.Type.BLOCK:
                MiningModeData miningModeData = player.getMainHandItem().getOrDefault(DataComponentTypes.MINING_MODE_DATA, new MiningModeData(MiningShape.SINGLE, 1, 1));
                if (miningModeData.shape() == MiningShape.SINGLE) {
                    positions.add(initalBlockPos);
                }
                if (miningModeData.shape() == MiningShape.FLAT_SQUARE) {
                    positions = findSurroundingBlocksInFlatSquare(player, initalBlockPos, traceResult, miningModeData.radius());
                }
                if (miningModeData.shape() == MiningShape.SHAPELESS) {
                    BlockState blockState = player.level().getBlockState(traceResult.getBlockPos());
                    positions = findSurroundingSameBlocksShapeless(player, blockState, initalBlockPos, 64, miningModeData.radius());
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
     * from https://github.com/Direwolf20-MC/JustDireThings/blob/main/src/main/java/com/direwolf20/justdirethings/common/items/interfaces/Helpers.java
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

    public static void doBlockCrumblings(PlayerInteractEvent.LeftClickBlock event) {

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
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP ||
                event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) { //Server Only
            cancelBlockBreaks(level, blockState, blockPos, player);
        }

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) { //Client Only
            if (blockPos.equals(destroyPos)) {
                gameTicksMining++;
            } else {
                gameTicksMining = 0;
                destroyPos = blockPos;
            }
            incrementBlockCrumblingsProgress(level, blockState, blockPos, player);
        }
    }

    private static void incrementBlockCrumblingsProgress(Level level, BlockState blockState, BlockPos pPos, Player player) {
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

    private static void cancelBlockBreaks(Level level, BlockState pState, BlockPos targetBlockPos, Player player) {
        Set<BlockPos> breakBlockPositions = getBlocksToBeBroken(targetBlockPos, player);
        for (BlockPos blockPos : breakBlockPositions) {
            if (blockPos.equals(targetBlockPos)) continue; //Let the vanilla mechanics handle the block we're hitting
            player.level().destroyBlockProgress(player.getId() + generatePosHash(blockPos), blockPos, -1);
        }
    }

    private static int generatePosHash(BlockPos blockPos) {
        return (31 * 31 * blockPos.getX()) + (31 * blockPos.getY()) + blockPos.getZ(); //For now this is probably good enough, will add more randomness if needed
    }

    private static void sendDestroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress, ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
    }

    public static boolean airBurst(Level level, Player player, ItemStack itemStack) {
        int multiplier = 3;
        if (!level.isClientSide) {
            // Get the player's looking direction as a vector
            Vec3 lookDirection = player.getViewVector(1.0F);
            // Define the strength of the burst, adjust this value to change how strong the burst should be
            double addedStrength = (double) multiplier / 2;
            double burstStrength = 1.5 + addedStrength;
            // Set the player's motion based on the look direction and burst strength
            player.setDeltaMovement(lookDirection.x * burstStrength, lookDirection.y * burstStrength, lookDirection.z * burstStrength);
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityMotionPacket(player));
            //player.hurtMarked = true; //This tells the server to move the client
            player.resetFallDistance();
            // Optionally, you could add some effects or sounds here
            //damageTool(itemStack, player, Ability.AIRBURST, multiplier);
            //PacketDistributor.sendToPlayer((ServerPlayer) player, new ClientSoundPayload(SoundEvents.FIRECHARGE_USE.getLocation(), 0.5f, 0.125f));
            //level.playSound(player, player.getOnPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 0.125f);
            return true;
        }
        return true;
    }

    /**
     * Teleport drops from https://github.com/Direwolf20-MC/JustDireThings/blob/main/src/main/java/com/direwolf20/justdirethings/common/items/interfaces/Helpers.java#L352
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

    //
    // Overrides for DiggerItem
    //

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        if(!pContext.getLevel().isClientSide()) {
            Level level = pContext.getLevel();
            BlockPos blockpos = pContext.getClickedPos();
            Player player = pContext.getPlayer();
            ItemStack hammerItemStack = player.getMainHandItem();

            // Get current mode and shapeId
            MiningModeData miningMode = hammerItemStack.getOrDefault(DataComponentTypes.MINING_MODE_DATA, new MiningModeData(MiningShape.SINGLE, 1, 1));
            int shapeId = miningMode.shape().id();

            // Go to next shape
            hammerItemStack.set(DataComponentTypes.MINING_MODE_DATA, MiningModeData.getNextMode(shapeId));

            // Output the change
            MiningModeData miningModeManager2 = hammerItemStack.getOrDefault(DataComponentTypes.MINING_MODE_DATA, new MiningModeData(MiningShape.SINGLE, 1, 1));
            player.sendSystemMessage(Component.literal("New Mining Mode: " + miningModeManager2.shape().name()));

        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        if(pStack.get(DataComponentTypes.MINING_MODE_DATA.get()) != null) {
            MiningModeData miningModeData = Objects.requireNonNull(pStack.get(DataComponentTypes.MINING_MODE_DATA));
            pTooltipComponents.add(Component.literal("Mining shape:" + miningModeData.shape().name() + " radius:" + + miningModeData.radius()));
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}
