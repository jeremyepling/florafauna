package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
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

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        recentToolModifications.remove(event.getEntity().getUUID());
    }

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
        long currentTick = player.level().getGameTime();
        ItemStack stack = player.getItemInHand(hand);
        HammerAction selected = stack.getOrDefault(FloraFaunaRegistry.HAMMER_ACTION, HammerAction.AUTO);
        if (selected == HammerAction.THROW) {
            if (!stack.has(FloraFaunaRegistry.THROWABLE_ABILITY)) return false;
            if (player instanceof ServerPlayer serverPlayer) ThrowItemPayload.throwItem(serverPlayer, hand);
            return true;
        }

        // Priority 1: Try multi-tool modifications if targeting a block
        if (clickedPos != null && hitResult != null) {
            MultiToolAbilityData multiTool = stack.get(FloraFaunaRegistry.MULTI_TOOL_ABILITY);
            if (multiTool != null) {
                var actions = selected.toolActions(multiTool);
                if (actions.hasAnyEnabled() && tryToolModification(player, hand, clickedPos, hitResult, actions, selected != HammerAction.AUTO)) {
                    // Track that we did a tool modification this tick
                    // This prevents RightClickItem from also executing the ability
                    if (player instanceof ServerPlayer) {
                        recentToolModifications.put(player.getUUID(), currentTick);
                    }
                    return true;
                }
            }
        }

        if (selected != HammerAction.AUTO || !(player instanceof ServerPlayer serverPlayer)) return false;

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

        if (!action.abilityId().getNamespace().equals(FloraFauna.MOD_ID)) return false;
        String abilityName = action.abilityId().getPath();
        switch (abilityName) {
            case "throwable_ability" -> {
                if (!stack.has(FloraFaunaRegistry.THROWABLE_ABILITY)) return false;
                ThrowItemPayload.throwItem(serverPlayer, hand);
            }
            case "multi_block_mining" -> {
                if (!stack.has(FloraFaunaRegistry.MULTI_BLOCK_MINING)) return false;
                CycleMiningModePayload.cycleMiningMode(serverPlayer, stack);
            }
            case "lightning_ability" -> {
                if (clickedPos == null || !stack.has(FloraFaunaRegistry.LIGHTNING_ABILITY)) return false;
                SpawnLightningPayload.spawnLightningBolt(player.level(), clickedPos);
            }
            case "teleport_surface_ability" -> {
                if (!stack.has(FloraFaunaRegistry.TELEPORT_SURFACE_ABILITY)) return false;
                TeleportToSurfacePayload.teleportToSurface(serverPlayer);
            }
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
    private static boolean tryToolModification(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult, MultiToolAbilityData multiTool, boolean requireTarget) {
        // Get blocks based on mining pattern (reuses existing pattern logic)
        var blocksToModify = MultiBlockPatterns.plan(player, player.getItemInHand(hand), pos, hitResult.getDirection()).blocks();

        // Try to modify each block in the pattern
        boolean anyModified = false;
        if (requireTarget) {
            // Explicit actions leave normal block use alone when the target cannot transform.
            if (!blocksToModify.contains(pos) || !tryModifySingleBlock(player, hand, pos, hitResult, multiTool)) return false;
            anyModified = true;
        }
        for (BlockPos targetPos : blocksToModify) {
            if (requireTarget && targetPos.equals(pos)) continue;
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
    private static boolean tryModifySingleBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult originalHit, MultiToolAbilityData multiTool) {
        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        UseOnContext context = new UseOnContext(player, hand, hit);

        // The vanilla axe transformer also scrapes copper. This tool's strip setting is for logs and bamboo.
        if (multiTool.strip() && (state.is(BlockTags.LOGS) || state.is(BlockTags.BAMBOO_BLOCKS))
                && transformBlock(context, BlockTransformers.AXE)) {
            return true;
        }
        if (multiTool.flatten() && transformBlock(context, BlockTransformers.SHOVEL)) {
            return true;
        }
        return multiTool.till() && transformBlock(context, BlockTransformers.HOE);
    }

    private static boolean transformBlock(UseOnContext context, ResourceKey<BlockTransformer> key) {
        Holder<BlockTransformer> transformer = context.getLevel().registryAccess().getOrThrow(key);
        return transformer.value().transformBlock(context, transformer).consumesAction();
    }
}
