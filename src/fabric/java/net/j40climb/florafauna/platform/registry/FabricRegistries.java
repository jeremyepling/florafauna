package net.j40climb.florafauna.platform.registry;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Queues the shared content definitions so components exist before item factories run. */
public final class FabricRegistries {
    private FabricRegistries() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, name);
    }

    public static class Entries<T> {
        private final Registry<T> registry;
        private final List<Runnable> registrations = new ArrayList<>();
        private boolean registered;

        public Entries(Registry<T> registry) { this.registry = registry; }

        public <U extends T> Supplier<U> register(String name, Supplier<U> factory) {
            if (registered) throw new IllegalStateException("Content registered after initialization: " + name);
            final class Entry implements Supplier<U> {
                private U value;
                @Override public U get() {
                    if (value == null) throw new IllegalStateException("Content used before registration: " + name);
                    return value;
                }
                private void register() { value = Registry.register(registry, id(name), factory.get()); }
            }
            Entry entry = new Entry();
            registrations.add(entry::register);
            return entry;
        }

        public void register() {
            if (registered) return;
            registered = true;
            registrations.forEach(Runnable::run);
        }
    }

    public static final class Blocks extends Entries<Block> {
        public Blocks() { super(BuiltInRegistries.BLOCK); }
        public <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> factory) {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
            return new DeferredBlock<>(register(name, () -> factory.apply(BlockBehaviour.Properties.of().setId(key))), key);
        }
    }

    public static final class Items extends Entries<Item> {
        public Items() { super(BuiltInRegistries.ITEM); }
        public <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> factory) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
            return register(name, () -> factory.apply(new Item.Properties().setId(key)));
        }
    }

    public static final class Components extends Entries<DataComponentType<?>> {
        public Components() { super(BuiltInRegistries.DATA_COMPONENT_TYPE); }
        public <T> Supplier<DataComponentType<T>> registerComponentType(String name,
                Function<DataComponentType.Builder<T>, DataComponentType.Builder<T>> factory) {
            return register(name, () -> factory.apply(DataComponentType.<T>builder()).build());
        }
    }

    public static final class Attachments {
        private final List<Runnable> registrations = new ArrayList<>();
        public <T> Supplier<AttachmentType<T>> register(String name, Supplier<AttachmentBuilder<T>> factory) {
            final class Entry implements Supplier<AttachmentType<T>> {
                private AttachmentType<T> value;
                @Override public AttachmentType<T> get() {
                    if (value == null) throw new IllegalStateException("Attachment used before registration: " + name);
                    return value;
                }
                private void register() { value = factory.get().builder.buildAndRegister(id(name)); }
            }
            Entry entry = new Entry();
            registrations.add(entry::register);
            return entry;
        }
        public void register() { registrations.forEach(Runnable::run); registrations.clear(); }
    }

    /** Retains the same serialized value shape and tracking policy as the NeoForge definitions. */
    public static final class AttachmentBuilder<T> {
        private final AttachmentRegistry.Builder<T> builder;
        private AttachmentBuilder(Supplier<T> initializer) {
            builder = AttachmentRegistry.<T>builder().initializer(initializer);
        }
        public static <T> AttachmentBuilder<T> builder(Supplier<T> initializer) { return new AttachmentBuilder<>(initializer); }
        public AttachmentBuilder<T> serialize(MapCodec<T> codec) { builder.persistent(codec.codec()); return this; }
        public AttachmentBuilder<T> sync(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            builder.syncWith(codec, AttachmentSyncPredicate.all()); return this;
        }
        public AttachmentBuilder<T> copyOnDeath() { builder.copyOnDeath(); return this; }
        public AttachmentBuilder<T> build() { return this; }
    }
}
