package net.j40climb.florafauna.common.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class EffectSwordItem extends SwordItem {
    private final Holder<MobEffect> effect;
    public EffectSwordItem(Tier pTier, Properties pProperties, Holder<MobEffect> effect) {
        super(pTier, pProperties);
        this.effect = effect;
    }
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if(entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(effect, 400), player);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }
}