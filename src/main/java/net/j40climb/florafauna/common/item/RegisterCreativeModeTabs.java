package net.j40climb.florafauna.common.item;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.RegisterBlocks;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RegisterCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FloraFauna.MOD_ID);

    public static final Supplier<CreativeModeTab> FLORAFAUNA_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("florafauna_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.florafauna_items_tab"))
                    .icon(() -> new ItemStack(RegisterItems.HAMMER.get()))
                    .displayItems((pParameters, output) -> {
                        output.accept(RegisterItems.TOMATO);
                        output.accept(RegisterItems.HAMMER);
                        output.accept(RegisterItems.DORMANT_SYMBIOTE);
                        output.accept(RegisterItems.SYMBIOTE_STEW);

                        output.accept(RegisterItems.GECKO_SPAWN_EGG);
                        output.accept(RegisterItems.LIZARD_SPAWN_EGG);
                        output.accept(RegisterItems.FRENCHIE_SPAWN_EGG);

                        output.accept(RegisterBlocks.TEAL_MOSS_BLOCK);
                        output.accept(RegisterBlocks.SYMBIOTE_CONTAINMENT_CHAMBER);
                        output.accept(RegisterBlocks.COCOON_CHAMBER);
                        output.accept(RegisterBlocks.COPPER_GOLEM_BARRIER);

                        // Wood blocks - iterates through all wood types
                        for (ModWoodType woodType : ModWoodType.values()) {
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

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
