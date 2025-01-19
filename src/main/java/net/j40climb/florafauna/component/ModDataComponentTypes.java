package net.j40climb.florafauna.component;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// All the data components for the mod
public class ModDataComponentTypes {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FloraFauna.MOD_ID);

    // Data components to register
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> COORDINATES = DATA_COMPONENT_TYPES.registerComponentType("coordinates", builder -> builder.persistent(BlockPos.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FoundBlockData>> FOUND_BLOCK = DATA_COMPONENT_TYPES.registerComponentType("found_block", builder -> builder.persistent(FoundBlockData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningSpeed>> MINING_SPEED = DATA_COMPONENT_TYPES.registerComponentType("mining_speed", builder -> builder.persistent(MiningSpeed.CODEC).networkSynchronized(MiningSpeed.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModeData>> MINING_MODE_DATA = DATA_COMPONENT_TYPES.registerComponentType("mining_mode", builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}