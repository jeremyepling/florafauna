package net.j40climb.florafauna.common.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;


// from https://github.com/baileyholl/Ars-Nouveau/blob/main/src/main/java/com/hollingsworth/arsnouveau/common/util/HolderHelper.java
public class Helpers {

    public static boolean checkEnchantment(ItemStack itemStack, Holder<Enchantment> enchantmentHolder) {
        return itemStack.supportsEnchantment(enchantmentHolder) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantmentsForCrafting(itemStack).keySet(), enchantmentHolder);
    }
}