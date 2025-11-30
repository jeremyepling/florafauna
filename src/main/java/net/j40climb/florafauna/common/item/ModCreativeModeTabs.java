package net.j40climb.florafauna.common.item;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FloraFauna.MOD_ID);

    public static final Supplier<CreativeModeTab> FLORAFAUNA_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("florafauna_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.florafauna_items_tab"))
                    .icon(() -> new ItemStack(ModItems.ENERGY_HAMMER.get()))
                    .displayItems((pParameters, output) -> {
                        output.accept(ModItems.TOMATO);
                        output.accept(ModItems.ENERGY_HAMMER);
                        output.accept(ModItems.GECKO_SPAWN_EGG);
                        output.accept(ModItems.LIZARD_SPAWN_EGG);
                        output.accept(ModItems.FRENCHIE_SPAWN_EGG);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
