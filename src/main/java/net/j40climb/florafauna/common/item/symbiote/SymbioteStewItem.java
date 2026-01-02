package net.j40climb.florafauna.common.item.symbiote;

import net.j40climb.florafauna.common.block.cocoonchamber.CocoonProgressionHooks;
import net.j40climb.florafauna.setup.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
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
 * A consumable item that prepares the player for symbiote binding.
 * When consumed, applies the Symbiote Prepared effect for 10 minutes
 * and sets the player's symbioteBindable state to true,
 * allowing them to bind with a symbiote via the Cocoon Chamber.
 *
 * When the effect expires, symbioteBindable is cleared if the player
 * hasn't bonded yet.
 */
public class SymbioteStewItem extends Item {

    // 10 minutes = 12000 ticks (20 ticks per second * 60 seconds * 10 minutes)
    public static final int EFFECT_DURATION_TICKS = 12000;

    public SymbioteStewItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 40; // 2 seconds
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof Player player) {
            PlayerSymbioteData data = player.getData(ModRegistry.PLAYER_SYMBIOTE_DATA);

            // Check if already bindable (optional: could allow re-consuming)
            if (data.symbioteBindable()) {
                player.displayClientMessage(
                        Component.translatable("symbiote.florafauna.already_prepared")
                                .withStyle(style -> style.withColor(0x9B59B6)),
                        true
                );
                return itemStack; // Don't consume if already prepared
            }

            // Set symbioteBindable = true and mark progression flag
            PlayerSymbioteData newData = data
                    .withSymbioteBindable(true)
                    .withSymbioteStewConsumedOnce(true);
            player.setData(ModRegistry.PLAYER_SYMBIOTE_DATA, newData);

            // Apply the Symbiote Prepared effect (10 minutes)
            // showParticles = true, showIcon = true
            player.addEffect(new MobEffectInstance(
                    ModRegistry.SYMBIOTE_PREPARED,
                    EFFECT_DURATION_TICKS,
                    0,  // amplifier
                    false,  // ambient (false = visible particles)
                    true,   // visible (show particles)
                    true    // showIcon (show in HUD)
            ));

            // Trigger progression hook
            CocoonProgressionHooks.onSymbioteStewConsumed((ServerPlayer) player);

            // Display feedback
            player.displayClientMessage(
                    Component.translatable("symbiote.florafauna.stew_consumed")
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
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.florafauna.symbiote_stew"));
        super.appendHoverText(itemStack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
