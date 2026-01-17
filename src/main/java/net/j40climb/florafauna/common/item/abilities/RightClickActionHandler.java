package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler that intercepts right-click interactions and executes abilities
 * based on data components on the held item.
 *
 * Priority order:
 * 1. Multi-tool modifications (strip/path/till) if MULTI_TOOL_ABILITY component present
 * 2. RIGHT_CLICK_ACTION ability if component present
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class RightClickActionHandler {

    /**
     * Tracks the game tick when each player last performed a tool modification.
     * Used to prevent RightClickItem from also triggering an ability in the same tick
     * when RightClickBlock already performed a tool modification.
     */
    private static final Map<UUID, Long> recentToolModifications = new ConcurrentHashMap<>();

    /**
     * Handle right-click on a block.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (handleRightClick(event.getEntity(), event.getHand(), event.getPos(), event.getHitVec())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Handle right-click in air (with item in hand).
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (handleRightClick(event.getEntity(), event.getHand(), null, null)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Common handler for right-click interactions.
     * First tries multi-tool modifications, then falls back to RIGHT_CLICK_ACTION ability.
     *
     * @param player the player performing the interaction
     * @param hand the hand used for the interaction
     * @param clickedPos the position of the clicked block, or null if right-clicking in air
     * @param hitResult the block hit result, or null if right-clicking in air
     * @return true if an action was executed and the event should be canceled
     */
    private static boolean handleRightClick(Player player, InteractionHand hand, @Nullable BlockPos clickedPos, @Nullable BlockHitResult hitResult) {
        // Server-side only
        if (player.level().isClientSide()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        long currentTick = player.level().getGameTime();
        ItemStack stack = player.getItemInHand(hand);

        // Priority 1: Try multi-tool modifications if targeting a block
        if (clickedPos != null && hitResult != null) {
            MultiToolAbilityData multiTool = stack.get(FloraFaunaRegistry.MULTI_TOOL_ABILITY);
            if (multiTool != null && multiTool.hasAnyEnabled()) {
                if (tryToolModification(serverPlayer, hand, clickedPos, hitResult, multiTool)) {
                    // Track that we did a tool modification this tick
                    // This prevents RightClickItem from also executing the ability
                    recentToolModifications.put(player.getUUID(), currentTick);
                    return true;
                }
            }
        }

        // Check if we already did a tool modification this tick
        // (Handles case where RightClickItem fires after RightClickBlock did a tool modification)
        Long lastToolTick = recentToolModifications.get(player.getUUID());
        if (lastToolTick != null && lastToolTick == currentTick) {
            return false; // Don't run ability - tool modification already happened this tick
        }

        // Priority 2: Execute RIGHT_CLICK_ACTION ability
        RightClickAction action = stack.get(FloraFaunaRegistry.RIGHT_CLICK_ACTION);
        if (action == null || action.equals(RightClickAction.NONE)) {
            return false;
        }

        String abilityName = action.abilityId().getPath();
        switch (abilityName) {
            case "throwable_ability" -> ThrowItemPayload.throwItem(serverPlayer, hand);
            case "multi_block_mining" -> CycleMiningModePayload.cycleMiningMode(serverPlayer);
            case "lightning_ability" -> {
                if (clickedPos != null) {
                    SpawnLightningPayload.spawnLightningBolt(player.level(), clickedPos);
                }
            }
            case "teleport_surface_ability" -> TeleportToSurfacePayload.teleportToSurface(serverPlayer);
            default -> {
                return false; // Unknown ability, don't cancel the event
            }
        }
        return true; // Ability executed, cancel the event
    }

    /**
     * Attempts to perform a tool modification (strip/path/till) on blocks based on mining pattern.
     * Uses the same pattern calculation as multi-block mining.
     *
     * @param player the player performing the action
     * @param hand the hand holding the tool
     * @param pos the position of the target block
     * @param hitResult the block hit result
     * @param multiTool the multi-tool configuration
     * @return true if any modification was performed
     */
    private static boolean tryToolModification(ServerPlayer player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult, MultiToolAbilityData multiTool) {
        // Get blocks based on mining pattern (reuses existing pattern logic)
        Set<BlockPos> blocksToModify = MultiBlockPatterns.getTargetBlocks(pos, player);

        // Try to modify each block in the pattern
        boolean anyModified = false;
        for (BlockPos targetPos : blocksToModify) {
            if (tryModifySingleBlock(player, hand, targetPos, hitResult, multiTool)) {
                anyModified = true;
            }
        }
        return anyModified;
    }

    /**
     * Attempts to perform a tool modification on a single block.
     *
     * @param player the player performing the action
     * @param hand the hand holding the tool
     * @param pos the position of the target block
     * @param originalHit the original block hit result (used for direction)
     * @param multiTool the multi-tool configuration
     * @return true if the block was modified
     */
    private static boolean tryModifySingleBlock(ServerPlayer player, InteractionHand hand, BlockPos pos, BlockHitResult originalHit, MultiToolAbilityData multiTool) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        // Create synthetic hit result for this block position
        BlockHitResult syntheticHit = new BlockHitResult(
                Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        UseOnContext context = new UseOnContext(player, hand, syntheticHit);

        // Try each enabled tool action in order of priority
        if (multiTool.strip()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.flatten()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.till()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        return false;
    }
}
