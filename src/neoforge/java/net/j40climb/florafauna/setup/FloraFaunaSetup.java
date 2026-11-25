package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.FloraFaunaCommands;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.common.block.mininganchor.networking.AnchorFillStatePayload;
import net.j40climb.florafauna.common.block.mobbarrier.networking.UpdateMobBarrierConfigPayload;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueLoader;
import net.j40climb.florafauna.dev.DevPlotCommands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Handles common mod setup: networking, creative tabs, event registration.
 * Defines HOW things connect. Runs after content exists.
 */
public class FloraFaunaSetup {

    // ==================== CREATIVE TABS ====================

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FloraFauna.MOD_ID);

    public static final Supplier<CreativeModeTab> FLORAFAUNA_ITEMS_TAB =
            CREATIVE_TABS.register("florafauna_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.florafauna_items_tab"))
                    .icon(() -> new ItemStack(FloraFaunaRegistry.DORMANT_SYMBIOTE.get()))
                    .displayItems((parameters, output) -> FloraFaunaCreativeItems.accept(output)).build());

    // ==================== NETWORKING ====================

    /**
     * Registers all network payloads.
     */
    public static void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::onServerReceived);
        registrar.playToServer(CocoonActionPayload.TYPE, CocoonActionPayload.STREAM_CODEC, CocoonActionPayload::onServerReceived);
        registrar.playToServer(UpdateMobBarrierConfigPayload.TYPE, UpdateMobBarrierConfigPayload.STREAM_CODEC, UpdateMobBarrierConfigPayload::onServerReceived);
        registrar.playToServer(net.j40climb.florafauna.noclip.NoClipPayload.TYPE, net.j40climb.florafauna.noclip.NoClipPayload.STREAM_CODEC, net.j40climb.florafauna.noclip.NoClipPayload::onServerReceived);

        // Server to client
        registrar.playToClient(ItemInputAnimationPayload.TYPE, ItemInputAnimationPayload.STREAM_CODEC, ItemInputAnimationPayload::onClientReceived);
        registrar.playToClient(OpenCocoonScreenPayload.TYPE, OpenCocoonScreenPayload.STREAM_CODEC);
        registrar.playToClient(AnchorFillStatePayload.TYPE, AnchorFillStatePayload.STREAM_CODEC, AnchorFillStatePayload::handleClient);
    }

    // ==================== COMMANDS ====================

    /**
     * Registers mod commands.
     */
    public static void registerCommands(RegisterCommandsEvent event) {
        FloraFaunaCommands.register(event.getDispatcher());
        DevPlotCommands.register(event.getDispatcher());
    }

    // ==================== CAPABILITIES ====================

    /**
     * Registers block entity capabilities.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Allow hoppers and automation to extract from Tier2 pods
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                FloraFaunaRegistry.TIER2_POD_BE.get(),
                (pod, direction) -> pod.getItemHandler()
        );
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initializes mod setup: registers to mod event bus and NeoForge event bus.
     */
    public static void init(IEventBus modEventBus) {
        // Register creative tabs to mod bus
        CREATIVE_TABS.register(modEventBus);

        // Register networking to mod bus
        modEventBus.addListener(FloraFaunaSetup::registerNetworking);

        // Register capabilities to mod bus
        modEventBus.addListener(FloraFaunaSetup::registerCapabilities);

        // Register to NeoForge event bus for game events
        NeoForge.EVENT_BUS.addListener(FloraFaunaSetup::registerCommands);
        NeoForge.EVENT_BUS.addListener(SymbioteDialogueLoader::registerReloadListener);
    }
}
