package net.j40climb.florafauna.common.item.custom;

import net.j40climb.florafauna.common.attachments.ModAttachmentTypes;
import net.j40climb.florafauna.common.attachments.SymbioteData;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.j40climb.florafauna.common.component.SymbioteAbilityState;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogue;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueTrigger;
import net.j40climb.florafauna.common.symbiote.tracking.SymbioteEventTracker;
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

import java.util.function.Consumer;

/**
 * A consumable item that bonds a symbiote to the player.
 * When consumed, it attaches symbiote data to the player and initiates the bonding process.
 */
public class SymbioteItem extends Item {
    /**
     * Constructs a new SymbioteItem with the specified properties.
     * Sets default components for ability state and event tracking.
     *
     * @param properties the item properties
     */
    public SymbioteItem(Properties properties) {
        super(properties
                .component(ModDataComponentTypes.SYMBIOTE_ABILITY_STATE, SymbioteAbilityState.DEFAULT)
                .component(ModDataComponentTypes.SYMBIOTE_EVENT_TRACKER, SymbioteEventTracker.DEFAULT)
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
        SymbioteData currentData = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

        if (currentData.bonded()) {
            if (level.isClientSide()) {
                // Show message on client side for immediate feedback
                player.displayClientMessage(
                        Component.literal("You already have a bonded symbiote!").withStyle(style -> style.withColor(0xFF6B6B)),
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
            SymbioteData currentData = player.getData(ModAttachmentTypes.SYMBIOTE_DATA);

            // Check if player already has a bonded symbiote
            if (currentData.bonded()) {
                player.displayClientMessage(
                        Component.literal("You already have a bonded symbiote!").withStyle(style -> style.withColor(0xFF6B6B)),
                        false
                );
                return itemStack; // Don't consume the item
            }

            // Read symbiote state from item components
            SymbioteAbilityState abilityState = itemStack.getOrDefault(
                    ModDataComponentTypes.SYMBIOTE_ABILITY_STATE,
                    SymbioteAbilityState.DEFAULT
            );
            SymbioteEventTracker eventTracker = itemStack.getOrDefault(
                    ModDataComponentTypes.SYMBIOTE_EVENT_TRACKER,
                    SymbioteEventTracker.DEFAULT
            );

            // Bond the symbiote - copy item state to player attachments
            SymbioteData newData = new SymbioteData(
                    true,                        // bonded = true
                    level.getGameTime(),         // bondTime = current game time
                    abilityState.tier(),         // tier from item
                    abilityState.dash(),         // dash from item
                    abilityState.featherFalling(), // featherFalling from item
                    abilityState.speed());       // speed from item

            player.setData(ModAttachmentTypes.SYMBIOTE_DATA, newData);
            player.setData(ModAttachmentTypes.SYMBIOTE_EVENT_TRACKER, eventTracker);

            // Trigger bonding dialogue
            SymbioteDialogue.forceTrigger((ServerPlayer) player, SymbioteDialogueTrigger.BONDED);

            // Display success message
            player.displayClientMessage(
                    Component.literal("Symbiote bonded! You feel a presence merging with your body...")
                            .withStyle(style -> style.withColor(0x9B59B6)),
                    false
            );

            // Consume the item
            itemStack.shrink(1);
        }

        return itemStack;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.florafauna.symbiote"));
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
