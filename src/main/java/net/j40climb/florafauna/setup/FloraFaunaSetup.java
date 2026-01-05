package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.FloraFaunaCommands;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.common.block.mininganchor.networking.AnchorFillStatePayload;
import net.j40climb.florafauna.common.block.mininganchor.pod.AbstractStoragePodBlockEntity;
import net.j40climb.florafauna.common.block.mobbarrier.networking.UpdateMobBarrierConfigPayload;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.entity.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.florafauna.common.item.abilities.networking.*;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueLoader;
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
                    .icon(() -> new ItemStack(FloraFaunaRegistry.HAMMER.get()))
                    .displayItems((pParameters, output) -> {
                        // Items
                        output.accept(FloraFaunaRegistry.HAMMER);
                        output.accept(FloraFaunaRegistry.DORMANT_SYMBIOTE);
                        output.accept(FloraFaunaRegistry.SYMBIOTE_STEW);

                        // Spawn eggs
                        output.accept(FloraFaunaRegistry.GECKO_SPAWN_EGG);
                        output.accept(FloraFaunaRegistry.LIZARD_SPAWN_EGG);
                        output.accept(FloraFaunaRegistry.FRENCHIE_SPAWN_EGG);

                        // Blocks
                        output.accept(FloraFaunaRegistry.TEAL_MOSS_BLOCK);
                        output.accept(FloraFaunaRegistry.SYMBIOTE_CONTAINMENT_CHAMBER);
                        output.accept(FloraFaunaRegistry.COCOON_CHAMBER);
                        output.accept(FloraFaunaRegistry.MOB_BARRIER);
                        output.accept(FloraFaunaRegistry.HUSK);

                        // Item Input System blocks
                        output.accept(FloraFaunaRegistry.STORAGE_ANCHOR);
                        output.accept(FloraFaunaRegistry.ITEM_INPUT);
                        output.accept(FloraFaunaRegistry.FIELD_RELAY);

                        // Mining Anchor System blocks
                        output.accept(FloraFaunaRegistry.TIER1_MINING_ANCHOR);
                        output.accept(FloraFaunaRegistry.TIER2_MINING_ANCHOR);
                        output.accept(FloraFaunaRegistry.TIER2_POD);

                        // Mob Transport System blocks
                        output.accept(FloraFaunaRegistry.MOB_INPUT);
                        output.accept(FloraFaunaRegistry.MOB_OUTPUT);
                        output.accept(FloraFaunaRegistry.MOB_SYMBIOTE);

                        // Wood blocks - iterates through all wood types
                        for (WoodType woodType : WoodType.values()) {
                            WoodBlockSet wood = woodType.getBlockSet();
                            output.accept(wood.log());
                            output.accept(wood.strippedLog());
                            output.accept(wood.wood());
                            output.accept(wood.strippedWood());
                            output.accept(wood.planks());
                            output.accept(wood.slab());
                            output.accept(wood.fence());
                            output.accept(wood.fenceGate());
                        }
                    }).build());

    // ==================== NETWORKING ====================

    /**
     * Registers all network payloads.
     */
    public static void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SpawnLightningPayload.TYPE, SpawnLightningPayload.STREAM_CODEC, SpawnLightningPayload::onServerReceived);
        registrar.playToServer(TeleportToSurfacePayload.TYPE, TeleportToSurfacePayload.STREAM_CODEC, TeleportToSurfacePayload::onServerReceived);
        registrar.playToServer(UpdateToolConfigPayload.TYPE, UpdateToolConfigPayload.STREAM_CODEC, UpdateToolConfigPayload::onServerReceived);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::onServerReceived);
        registrar.playToServer(PutDownFrenchiePayload.TYPE, PutDownFrenchiePayload.STREAM_CODEC, PutDownFrenchiePayload::onServerReceived);
        registrar.playToServer(CocoonActionPayload.TYPE, CocoonActionPayload.STREAM_CODEC, CocoonActionPayload::onServerReceived);
        registrar.playToServer(ThrowItemPayload.TYPE, ThrowItemPayload.STREAM_CODEC, ThrowItemPayload::onServerReceived);
        registrar.playToServer(CycleMiningModePayload.TYPE, CycleMiningModePayload.STREAM_CODEC, CycleMiningModePayload::onServerReceived);
        registrar.playToServer(UpdateMobBarrierConfigPayload.TYPE, UpdateMobBarrierConfigPayload.STREAM_CODEC, UpdateMobBarrierConfigPayload::onServerReceived);

        // Server to client
        registrar.playToClient(ItemInputAnimationPayload.TYPE, ItemInputAnimationPayload.STREAM_CODEC, ItemInputAnimationPayload::onClientReceived);
        registrar.playToClient(OpenCocoonScreenPayload.TYPE, OpenCocoonScreenPayload.STREAM_CODEC, OpenCocoonScreenPayload::onClientReceived);
        registrar.playToClient(AnchorFillStatePayload.TYPE, AnchorFillStatePayload.STREAM_CODEC, AnchorFillStatePayload::handleClient);
    }

    // ==================== COMMANDS ====================

    /**
     * Registers mod commands.
     */
    public static void registerCommands(RegisterCommandsEvent event) {
        FloraFaunaCommands.register(event.getDispatcher());
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
