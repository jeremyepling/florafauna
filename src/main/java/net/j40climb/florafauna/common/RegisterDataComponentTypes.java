package net.j40climb.florafauna.common;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.symbiote.SymbioteData;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// All the data components for the mod
public class RegisterDataComponentTypes {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FloraFauna.MOD_ID);

    // Tool ability data components (composable - can be added to any item)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModeData>> MULTI_BLOCK_MINING = DATA_COMPONENT_TYPES.registerComponentType("multi_block_mining", builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolConfig>> TOOL_CONFIG = DATA_COMPONENT_TYPES.registerComponentType("tool_config", builder -> builder.persistent(ToolConfig.CODEC).networkSynchronized(ToolConfig.STREAM_CODEC));

    // Marker components for abilities (Unit type - presence = has ability)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LIGHTNING_ABILITY = DATA_COMPONENT_TYPES.registerComponentType("lightning_ability", builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> TELEPORT_SURFACE_ABILITY = DATA_COMPONENT_TYPES.registerComponentType("teleport_surface_ability", builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    // Symbiote item data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SymbioteData>> SYMBIOTE_DATA = DATA_COMPONENT_TYPES.registerComponentType("symbiote_data", builder -> builder.persistent(SymbioteData.CODEC).networkSynchronized(SymbioteData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProgressSignalTracker>> SYMBIOTE_PROGRESS = DATA_COMPONENT_TYPES.registerComponentType("symbiote_progress", builder -> builder.persistent(ProgressSignalTracker.CODEC).networkSynchronized(ProgressSignalTracker.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}