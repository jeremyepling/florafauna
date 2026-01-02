package net.j40climb.florafauna;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.common.RegisterAttachmentTypes;
import net.j40climb.florafauna.common.RegisterDataComponentTypes;
import net.j40climb.florafauna.common.RegisterMenus;
import net.j40climb.florafauna.common.RegisterNetworking;
import net.j40climb.florafauna.common.block.RegisterBlockEntities;
import net.j40climb.florafauna.common.block.RegisterBlocks;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberScreen;
import net.j40climb.florafauna.common.datagen.RegisterDataGenerators;
import net.j40climb.florafauna.common.entity.RegisterEntities;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.florafauna.common.entity.gecko.GeckoRenderer;
import net.j40climb.florafauna.common.entity.lizard.LizardRenderer;
import net.j40climb.florafauna.common.item.RegisterCreativeModeTabs;
import net.j40climb.florafauna.common.item.RegisterItems;
import net.j40climb.florafauna.common.item.symbiote.SymbioteCommand;
import net.j40climb.florafauna.common.item.symbiote.dialogue.SymbioteDialogueLoader;
import net.j40climb.florafauna.test.FloraFaunaGameTests;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(FloraFauna.MOD_ID)
public class FloraFauna {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "florafauna";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FloraFauna(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        RegisterCreativeModeTabs.register(modEventBus);
        RegisterBlocks.register(modEventBus);
        RegisterItems.register(modEventBus);
        RegisterDataComponentTypes.register(modEventBus);
        RegisterAttachmentTypes.register(modEventBus);
        RegisterEntities.register(modEventBus);
        RegisterBlockEntities.register(modEventBus);
        RegisterMenus.register(modEventBus);

        // Register network payloads
        modEventBus.addListener(RegisterNetworking::register);

        // Register datagen events on the mod bus
        modEventBus.addListener(RegisterDataGenerators::gatherClientData);
        modEventBus.addListener(RegisterDataGenerators::gatherServerData);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register symbiote dialogue loader for datapack reloading
        NeoForge.EVENT_BUS.addListener(SymbioteDialogueLoader::registerReloadListener);

        // Register game tests only when gametest namespace is enabled
        String enabledNamespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (enabledNamespaces.contains(MOD_ID)) {
            FloraFaunaGameTests.register(modEventBus);
        }

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Stripping behavior is registered via ModEvents.registerStrippables
    }

    // Add items to vanilla creative tabs
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // No items to add to vanilla tabs currently
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SymbioteCommand.register(event.getDispatcher());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(RegisterEntities.GECKO.get(), GeckoRenderer::new);
            EntityRenderers.register(RegisterEntities.LIZARD.get(), LizardRenderer::new);
            EntityRenderers.register(RegisterEntities.FRENCHIE.get(), FrenchieRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(RegisterMenus.CONTAINMENT_CHAMBER.get(),
                    ContainmentChamberScreen::new);
            event.register(RegisterMenus.COCOON_CHAMBER.get(),
                    CocoonChamberScreen::new);
        }
    }
}
