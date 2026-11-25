package net.j40climb.florafauna.platform.registry;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/** A block reference kept lazy until the content registration phase. */
public final class DeferredBlock<T extends Block> implements Supplier<T> {
    private final Supplier<T> value;
    private final ResourceKey<Block> key;

    DeferredBlock(Supplier<T> value, ResourceKey<Block> key) {
        this.value = value;
        this.key = key;
    }

    @Override public T get() { return value.get(); }
    public ResourceKey<Block> key() { return key; }
    public net.minecraft.world.item.Item asItem() { return get().asItem(); }
}
