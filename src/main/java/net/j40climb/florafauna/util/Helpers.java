package net.j40climb.florafauna.util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


// from https://github.com/baileyholl/Ars-Nouveau/blob/main/src/main/java/com/hollingsworth/arsnouveau/common/util/HolderHelper.java
public class Helpers {

    public static <T> Holder<T> toHolder(Level level, ResourceKey<T> key){
        return level.registryAccess().registryOrThrow(key.registryKey()).getHolderOrThrow(key);
    }

    public static <T> Holder<T> toHolder(BlockEntity entity, ResourceKey<T> key){
        return entity.getLevel().registryAccess().registryOrThrow(key.registryKey()).getHolderOrThrow(key);
    }

    public static boolean checkEnchantment(ItemStack itemStack, Holder<Enchantment> enchantmentHolder, Level level) {
        return itemStack.supportsEnchantment(enchantmentHolder) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(itemStack).keySet(), enchantmentHolder);
    }
}