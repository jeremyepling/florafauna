package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationArbiter;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A consumable item that bonds a symbiote to the player.
 * When consumed, it attaches symbiote data to the player and initiates the bonding process.
 *
 * <h2>Bonding Process</h2>
 * <ol>
 *   <li>Player right-clicks with symbiote item</li>
 *   <li>2-second consumption animation plays</li>
 *   <li>On completion, symbiote data is copied from item to player attachments</li>
 *   <li>Bonding observation triggers a Tier 2 breakthrough moment</li>
 *   <li>Item is consumed</li>
 * </ol>
 *
 * <h2>Data Persistence</h2>
 * The symbiote remembers its experiences. When unbonded, progress is saved to the item
 * and restored when re-bonded to any player.
 */
public class SymbioteItem extends Item {
    /**
     * Constructs a new SymbioteItem with the specified properties.
     * Sets default components for symbiote data and progress tracking.
     *
     * @param properties the item properties
     */
    public SymbioteItem(Properties properties) {
        super(properties
                .component(RegisterDataComponentTypes.SYMBIOTE_DATA, SymbioteData.DEFAULT)
                .component(RegisterDataComponentTypes.SYMBIOTE_PROGRESS, ProgressSignalTracker.DEFAULT)
        );
    }

    /**
     * Called when the player right-clicks with the item.
     * Prevents consumption if the player already has a bonded symbiote.
     *
     * @param level the level/world
     * @param player the player using the item
     * @param hand the hand holding the item
     * @return the interaction result
     */
    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Check if player already has a bonded symbiote (on both sides for immediate feedback)
        SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

        if (currentData.bonded()) {
            if (level.isClientSide()) {
                // Show message on client side for immediate feedback
                player.displayClientMessage(
                        Component.translatable("symbiote.florafauna.already_bonded")
                                .withStyle(style -> style.withColor(0xFF6B6B)),
                        true // actionBar = true for less intrusive message
                );
            }
            return InteractionResult.FAIL;
        }

        // Allow normal consumption to proceed
        return super.use(level, player, hand);
    }

    /**
     * Gets the use animation for this item.
     *
     * @param itemStack the item stack
     * @return DRINK animation for consuming the symbiote
     */
    @Override
    public @NotNull ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.DRINK;
    }

    /**
     * Gets the use duration (how long to consume).
     *
     * @param itemStack the item stack
     * @param entity the entity using the item
     * @return 40 ticks (2 seconds) consumption time
     */
    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 40; // 2 seconds (40 ticks)
    }

    /**
     * Called when the item finishes being used (consumed).
     * Bonds the symbiote to the player on the server side.
     *
     * @param itemStack the item stack being consumed
     * @param level the level/world
     * @param livingEntity the entity that consumed the item
     * @return the remaining item stack
     */
    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        // Only process on server side
        if (!level.isClientSide() && livingEntity instanceof Player player) {
            // Get current symbiote data
            SymbioteData currentData = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);

            // Check if player already has a bonded symbiote
            if (currentData.bonded()) {
                player.displayClientMessage(
                        Component.translatable("symbiote.florafauna.already_bonded")
                                .withStyle(style -> style.withColor(0xFF6B6B)),
                        false
                );
                return itemStack; // Don't consume the item
            }

            // Read symbiote state from item components
            SymbioteData itemData = itemStack.getOrDefault(
                    RegisterDataComponentTypes.SYMBIOTE_DATA,
                    SymbioteData.DEFAULT
            );
            ProgressSignalTracker progressTracker = itemStack.getOrDefault(
                    RegisterDataComponentTypes.SYMBIOTE_PROGRESS,
                    ProgressSignalTracker.DEFAULT
            );

            // Bond the symbiote - copy item state to player attachments and set bonded=true
            SymbioteData bondedData = new SymbioteData(
                    true,                        // bonded = true
                    level.getGameTime(),         // bondTime = current game time
                    itemData.tier(),             // tier from item
                    itemData.dash(),             // dash from item
                    itemData.featherFalling(),   // featherFalling from item
                    itemData.speed(),            // speed from item
                    itemData.jumpBoost()         // jumpBoost from item
            );

            player.setData(RegisterAttachmentTypes.SYMBIOTE_DATA, bondedData);
            player.setData(RegisterAttachmentTypes.SYMBIOTE_PROGRESS, progressTracker);
            player.setData(RegisterAttachmentTypes.VOICE_COOLDOWNS, VoiceCooldownState.DEFAULT);

            // Trigger bonding observation - this is a BONDING_MILESTONE (Tier 2 breakthrough)
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ObservationArbiter.observe(serverPlayer, ObservationCategory.BONDING_MILESTONE, 100, Map.of(
                    "event", "bonded"
            ));

            // Display success message
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.bonded_success")
                            .withStyle(style -> style.withColor(0x9B59B6)),
                    false
            );

            // Consume the item
            itemStack.shrink(1);
        }

        return itemStack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.florafauna.symbiote"));
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
