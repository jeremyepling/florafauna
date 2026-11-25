package net.j40climb.florafauna.platform.transfer.item;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
public record ItemResource(ItemVariant variant) {
 public static final ItemResource EMPTY=new ItemResource(ItemVariant.blank());
 public static ItemResource of(net.minecraft.world.level.ItemLike item) { return new ItemResource(ItemVariant.of(item)); }
 public static ItemResource of(ItemStack stack) { return new ItemResource(ItemVariant.of(stack)); }
 public boolean isEmpty() { return variant.isBlank(); }
 public boolean matches(ItemStack stack) { return variant.equals(ItemVariant.of(stack)); }
 public ItemStack toStack() { return toStack(1); }
 public ItemStack toStack(int amount) { return amount<=0||isEmpty()?ItemStack.EMPTY:variant.toStack(amount); }
 public Item getItem() { return variant.getItem(); }
 public int getMaxStackSize() { return variant.toStack().getMaxStackSize(); }
}
