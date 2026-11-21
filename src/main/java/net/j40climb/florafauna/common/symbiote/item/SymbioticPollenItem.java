package net.j40climb.florafauna.common.symbiote.item;

import net.j40climb.florafauna.common.symbiote.binding.SymbiotePreparation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Edible pollen that makes the carried bloom spore ready to bond. */
public class SymbioticPollenItem extends Item {
    public SymbioticPollenItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            if (!SymbiotePreparation.prepare(player)) {
                return stack;
            }
            player.sendSystemMessage(Component.translatable("symbiote.onboarding.pollen_consumed")
                    .withStyle(style -> style.withColor(0x9B59B6)));
            stack.shrink(1);
        }
        return stack;
    }
}
