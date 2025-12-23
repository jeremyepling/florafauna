package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.energyhammer.MiningModeData;
import net.j40climb.florafauna.common.item.energyhammer.MiningSpeed;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.tracking.SymbioteEventTracker;
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

    // Symbiote item data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SymbioteData>> SYMBIOTE_DATA = DATA_COMPONENT_TYPES.registerComponentType("symbiote_data", builder -> builder.persistent(SymbioteData.CODEC).networkSynchronized(SymbioteData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SymbioteEventTracker>> SYMBIOTE_EVENT_TRACKER = DATA_COMPONENT_TYPES.registerComponentType("symbiote_event_tracker", builder -> builder.persistent(SymbioteEventTracker.CODEC).networkSynchronized(SymbioteEventTracker.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}