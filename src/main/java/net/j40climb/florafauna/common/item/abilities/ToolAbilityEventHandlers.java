package net.j40climb.florafauna.common.item.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockBreaker;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockOutlineRenderer;
import net.j40climb.florafauna.common.item.abilities.multiblock.MultiBlockVisualFeedback;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Event handlers for tool abilities.
 * These work with any item that has the appropriate DataComponents.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class ToolAbilityEventHandlers {
    @SubscribeEvent
    public static void extractBlockOutlineRenderStateEvent(ExtractBlockOutlineRenderStateEvent event) {
        event.addCustomRenderer(new MultiBlockOutlineRenderer());
    }

    @SubscribeEvent
    public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        GameType type = player.getAbilities().instabuild ? GameType.CREATIVE : GameType.SURVIVAL;
        ItemStack mainHandItem = player.getMainHandItem();
        BlockPos initialBlockPos = event.getPos();

        // It's on the server and the action isn't restricted, like Spectator mode
        if (player instanceof ServerPlayer serverPlayer &&
                !serverPlayer.blockActionRestricted(level, initialBlockPos, type)) {
            boolean cancelEvent = MultiBlockBreaker.breakBlocks(mainHandItem, initialBlockPos, serverPlayer, level);
            if (cancelEvent) {
                // This is needed to not delete the stairs that were placed
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void LeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemStack = event.getItemStack();

        // Component-based check - works with any item that has MULTI_BLOCK_MINING
        if (itemStack.has(ModRegistry.MULTI_BLOCK_MINING)) {
            MultiBlockVisualFeedback.processLeftClickBlock(event);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack itemStack = event.getEntity().getMainHandItem();

        // Component-based check - works with any item that has TOOL_CONFIG
        if (itemStack.has(ModRegistry.TOOL_CONFIG)) {
            ToolConfig config = itemStack.get(ModRegistry.TOOL_CONFIG);
            float newSpeed = switch (config.miningSpeed()) {
                case MiningSpeed.STANDARD -> event.getOriginalSpeed();
                case MiningSpeed.EFFICIENCY -> 35.0F;
                case MiningSpeed.INSTABREAK -> 100.0F;
            };
            event.setNewSpeed(newSpeed);
        }
    }
}
