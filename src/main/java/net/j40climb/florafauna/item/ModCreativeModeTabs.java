package net.j40climb.florafauna.item;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FloraFauna.MOD_ID);

    public static final Supplier<CreativeModeTab> BLACK_OPAL_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("black_opal_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.black_opal_items_tab"))
                    .icon(() -> new ItemStack(ModItems.BLACK_OPAL.get()))
                    .displayItems((pParameters, output) -> {
                        output.accept(ModItems.BLACK_OPAL);
                        output.accept(ModItems.RAW_BLACK_OPAL);

                        output.accept(ModItems.CHAINSAW);
                        output.accept(ModItems.TOMATO);
                        output.accept(ModItems.FROSTFIRE_ICE);

                        output.accept(ModItems.METAL_DETECTOR);
                        output.accept(ModItems.DATA_TABLET);

                        output.accept(ModItems.KAUPEN_BOW);

                        output.accept(ModItems.BLACK_OPAL_SWORD);
                        output.accept(ModItems.BLACK_OPAL_PICKAXE);
                        output.accept(ModItems.BLACK_OPAL_AXE);
                        output.accept(ModItems.BLACK_OPAL_SHOVEL);
                        output.accept(ModItems.BLACK_OPAL_HOE);

                        output.accept(ModItems.BLACK_OPAL_PAXEL);
                        output.accept(ModItems.BLACK_OPAL_HAMMER);

                        output.accept(ModItems.BLACK_OPAL_HELMET);
                        output.accept(ModItems.BLACK_OPAL_CHESTPLATE);
                        output.accept(ModItems.BLACK_OPAL_LEGGINGS);
                        output.accept(ModItems.BLACK_OPAL_BOOTS);

                        output.accept(ModItems.ENERGY_HAMMER);

                        output.accept(ModItems.GECKO_SPAWN_EGG);
                        output.accept(ModItems.LIZARD_SPAWN_EGG);
                        output.accept(ModItems.FRENCHIE_SPAWN_EGG);

                    }).build());

    public static final Supplier<CreativeModeTab> BLACK_OPAL_BLOCKS_TAB =
            CREATIVE_MODE_TABS.register("black_opal_blocks_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.black_opal_blocks_tab"))
                    .icon(() -> new ItemStack(ModBlocks.BLACK_OPAL_BLOCK))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID, "black_opal_items_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModBlocks.BLACK_OPAL_BLOCK);
                        pOutput.accept(ModBlocks.RAW_BLACK_OPAL_BLOCK);

                        pOutput.accept(ModBlocks.BLACK_OPAL_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_DEEPSLATE_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_END_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_NETHER_ORE);

                        pOutput.accept(ModBlocks.MAGIC_BLOCK);

                        pOutput.accept(ModBlocks.BLACK_OPAL_STAIRS);
                        pOutput.accept(ModBlocks.BLACK_OPAL_SLAB);

                        pOutput.accept(ModBlocks.BLACK_OPAL_PRESSURE_PLATE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_BUTTON);

                        pOutput.accept(ModBlocks.BLACK_OPAL_FENCE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_FENCE_GATE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_WALL);

                        pOutput.accept(ModBlocks.BLACK_OPAL_DOOR);
                        pOutput.accept(ModBlocks.BLACK_OPAL_TRAPDOOR);

                        pOutput.accept(ModBlocks.BLACK_OPAL_LAMP);
                        pOutput.accept(ModBlocks.PEDESTAL);

                    }).build());



    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
