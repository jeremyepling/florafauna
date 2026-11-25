package net.j40climb.florafauna.platform.transfer.item;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A vanilla slot backed by the same mutable stack that the resource handler owns. */
public final class ResourceHandlerSlot extends Slot {
    @FunctionalInterface public interface Setter { void set(int slot, ItemResource resource, int amount); }
    private final ItemStacksResourceHandler handler;
    private final int index;

    public ResourceHandlerSlot(ItemStacksResourceHandler handler, Setter setter, int index, int x, int y) {
        super(new Container() {
            public int getContainerSize() { return handler.size(); }
            public boolean isEmpty() {
                for (int i = 0; i < handler.size(); i++) if (!handler.getStackInSlot(i).isEmpty()) return false;
                return true;
            }
            public ItemStack getItem(int slot) { return handler.getStackInSlot(slot); }
            public void setItem(int slot, ItemStack stack) { setter.set(slot, ItemResource.of(stack), stack.getCount()); }
            public ItemStack removeItem(int slot, int count) {
                ItemStack removed = getItem(slot).split(count);
                if (!removed.isEmpty()) handler.contentsChanged(slot);
                return removed;
            }
            public ItemStack removeItemNoUpdate(int slot) {
                ItemStack previous = getItem(slot);
                setItem(slot, ItemStack.EMPTY);
                return previous;
            }
            public void setChanged() { handler.contentsChanged(index); }
            public boolean stillValid(Player player) { return true; }
            public void clearContent() {
                for (int i = 0; i < handler.size(); i++) setItem(i, ItemStack.EMPTY);
            }
        }, index, x, y);
        this.handler = handler;
        this.index = index;
    }

    @Override public boolean mayPlace(ItemStack stack) { return handler.isValid(index, ItemResource.of(stack)); }
    @Override public int getMaxStackSize(ItemStack stack) {
        return (int) handler.getCapacityAsLong(index, ItemResource.of(stack));
    }
}
