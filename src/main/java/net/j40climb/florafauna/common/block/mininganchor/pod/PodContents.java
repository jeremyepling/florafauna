package net.j40climb.florafauna.common.block.mininganchor.pod;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Data component that stores pod inventory contents.
 * Used by HardenedPod items to preserve contents when picked up (like shulker boxes).
 */
public record PodContents(List<ItemStack> items) {

    public static final PodContents EMPTY = new PodContents(List.of());

    public static final Codec<PodContents> CODEC = ItemStack.OPTIONAL_CODEC.listOf()
            .xmap(PodContents::new, PodContents::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, PodContents> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(PodContents::new, PodContents::items);

    /**
     * Creates PodContents from a list of items, filtering out empty stacks.
     */
    public static PodContents of(NonNullList<ItemStack> stacks) {
        List<ItemStack> nonEmpty = stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        return nonEmpty.isEmpty() ? EMPTY : new PodContents(nonEmpty);
    }

    /**
     * Creates PodContents from a list of items.
     */
    public static PodContents of(List<ItemStack> stacks) {
        List<ItemStack> nonEmpty = stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        return nonEmpty.isEmpty() ? EMPTY : new PodContents(nonEmpty);
    }

    /**
     * Returns whether this has any items.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the total item count across all stacks.
     */
    public int getItemCount() {
        return items.stream().mapToInt(ItemStack::getCount).sum();
    }
}
