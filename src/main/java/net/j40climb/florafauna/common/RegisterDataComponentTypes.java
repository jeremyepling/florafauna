package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.hammer.data.MiningModeData;
import net.j40climb.florafauna.common.item.hammer.data.MiningSpeed;
import net.j40climb.florafauna.common.item.hammer.menu.HammerConfig;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// All the data components for the mod
public class RegisterDataComponentTypes {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FloraFauna.MOD_ID);

    //// Energy hammer data components Data components to register
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningSpeed>> MINING_SPEED = DATA_COMPONENT_TYPES.registerComponentType("mining_speed", builder -> builder.persistent(MiningSpeed.CODEC).networkSynchronized(MiningSpeed.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModeData>> MINING_MODE_DATA = DATA_COMPONENT_TYPES.registerComponentType("mining_mode", builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HammerConfig>> HAMMER_CONFIG = DATA_COMPONENT_TYPES.registerComponentType("hammer_config", builder -> builder.persistent(HammerConfig.CODEC).networkSynchronized(HammerConfig.STREAM_CODEC));

    // Symbiote item data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SymbioteData>> SYMBIOTE_DATA = DATA_COMPONENT_TYPES.registerComponentType("symbiote_data", builder -> builder.persistent(SymbioteData.CODEC).networkSynchronized(SymbioteData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProgressSignalTracker>> SYMBIOTE_PROGRESS = DATA_COMPONENT_TYPES.registerComponentType("symbiote_progress", builder -> builder.persistent(ProgressSignalTracker.CODEC).networkSynchronized(ProgressSignalTracker.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}