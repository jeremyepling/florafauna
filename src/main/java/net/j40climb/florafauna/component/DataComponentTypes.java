package net.j40climb.florafauna.component;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

// All the data components for the mod
public class DataComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FloraFauna.MOD_ID);

    // Data components to register
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> COORDINATES = register("coordinates", builder -> builder.persistent(BlockPos.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FoundBlockData>> FOUND_BLOCK = register("found_block", builder -> builder.persistent(FoundBlockData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModeData>> MINING_MODE_DATA = register("mining_mode", builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC));

    // Handles registration for the above
    private static <T>DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}