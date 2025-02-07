package net.j40climb.florafauna;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.client.gui.ModMenuTypes;
import net.j40climb.florafauna.client.gui.custom.PedestalScreen;
import net.j40climb.florafauna.common.block.ModBlocks;
import net.j40climb.florafauna.common.block.entity.ModBlockEntities;
import net.j40climb.florafauna.common.block.entity.renderer.PedestalBlockEntityRenderer;
import net.j40climb.florafauna.common.component.ModDataComponentTypes;
import net.j40climb.florafauna.common.entity.ModEntities;
import net.j40climb.florafauna.common.entity.client.FrenchieRenderer;
import net.j40climb.florafauna.common.entity.client.GeckoRenderer;
import net.j40climb.florafauna.common.entity.client.LizardRenderer;
import net.j40climb.florafauna.common.item.ModArmorMaterials;
import net.j40climb.florafauna.common.item.ModCreativeModeTabs;
import net.j40climb.florafauna.common.item.ModItems;
import net.j40climb.florafauna.common.util.ModItemProperties;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModArmorMaterials.register(modEventBus);
        ModDataComponentTypes.register(modEventBus);

        ModEntities.register(modEventBus);

        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BLACK_OPAL);
            event.accept(ModItems.RAW_BLACK_OPAL);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModItemProperties.addCustomItemProperties();
            EntityRenderers.register(ModEntities.GECKO.get(), GeckoRenderer::new);
            EntityRenderers.register(ModEntities.LIZARD.get(), LizardRenderer::new);
            EntityRenderers.register(ModEntities.FRENCHIE.get(), FrenchieRenderer::new);
        }

        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.PEDESTAL_MENU.get(), PedestalScreen::new);
        }
    }
}
