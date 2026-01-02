package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.FloraFaunaCommands;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.entity.frontpack.networking.PutDownFrenchiePayload;
import net.j40climb.florafauna.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Handles common mod setup: networking, creative tabs, event registration.
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
                        output.accept(FloraFaunaRegistry.COPPER_GOLEM_BARRIER);
                        output.accept(FloraFaunaRegistry.HUSK);

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
    }

    // ==================== COMMANDS ====================

    /**
     * Registers mod commands.
     */
    public static void registerCommands(RegisterCommandsEvent event) {
        FloraFaunaCommands.register(event.getDispatcher());
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

        // Register to NeoForge event bus for game events
        NeoForge.EVENT_BUS.addListener(FloraFaunaSetup::registerCommands);
        NeoForge.EVENT_BUS.addListener(SymbioteDialogueLoader::registerReloadListener);
    }
}
