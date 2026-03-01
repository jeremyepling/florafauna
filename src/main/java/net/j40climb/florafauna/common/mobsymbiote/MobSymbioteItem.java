package net.j40climb.florafauna.common.entity.mobsymbiote;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Item used to apply a Level 1 MobSymbiote to mobs.
 * Right-click on a mob to attach a MobSymbiote, enabling luring and transport.
 */
public class MobSymbioteItem extends Item {

    public MobSymbioteItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {

        // Only works on Mobs (not players, armor stands, etc.)
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }

        // Check if this mob is excluded (deny list - bosses, warden, etc.)
        if (MobSymbioteHelper.isMobSymbioteExcluded(mob)) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.florafauna.mob_symbiote.excluded")
                                .withStyle(style -> style.withColor(0xAA0000)),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        // Server-side only
        if (player.level() instanceof ServerLevel serverLevel) {
            long currentTick = serverLevel.getGameTime();

            // Check if already has a MobSymbiote
            if (MobSymbioteHelper.hasMobSymbiote(mob)) {
                // Already has MobSymbiote - show message
                player.displayClientMessage(
                        Component.translatable("message.florafauna.mob_symbiote.already_bonded")
                                .withStyle(style -> style.withColor(0x888888)),
                        true
                );
                return InteractionResult.FAIL;
            }

            // Apply Level 1 MobSymbiote
            MobSymbioteHelper.applyMobSymbioteLevel1(mob, currentTick);

            // Show success message
            player.displayClientMessage(
                    Component.translatable("message.florafauna.mob_symbiote.bonded")
                            .withStyle(style -> style.withColor(0x00AA00)),
                    true
            );

            // Consume item if not in creative mode
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(
                Component.translatable("tooltip.florafauna.mob_symbiote")
                        .withStyle(style -> style.withColor(0x9B59B6).withItalic(true))
        );
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
