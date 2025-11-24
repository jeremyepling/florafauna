package net.j40climb.florafauna.common.item;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.ModEntities;
import net.j40climb.florafauna.common.item.custom.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FloraFauna.MOD_ID);

    // Food
    public static final DeferredItem<Item> TOMATO =
            ITEMS.registerItem("tomato", properties -> new Item(properties) {
                // Using an anonymous class to create a tooltip inline instead of using a full class in ModItems
                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    pTooltipComponents.add(Component.translatable("tooltip.florafauna.tomato.1"));
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            }, new Item.Properties().food(ModFoodProperties.TOMATO)); //This enables the tomato to be eaten


    // Fuel
    public static final DeferredItem<Item> FROSTFIRE_ICE =
            ITEMS.registerItem("frostfire_ice", properties -> new FuelItem(properties, 800), new Item.Properties());


    // Tutorial items
    public static final DeferredItem<Item> CHAINSAW = ITEMS.register("chainsaw", ChainsawItem::new);

    public static final DeferredItem<Item> METAL_DETECTOR = ITEMS.register("metal_detector",
            () -> new MetalDetectorItem(new Item.Properties().durability(100)));

    public static final DeferredItem<Item> DATA_TABLET = ITEMS.register("data_tablet",
            () -> new DataTabletItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> KAUPEN_BOW = ITEMS.register("kaupen_bow",
            () -> new BowItem(new Item.Properties().durability(500)));


    /*
    / Mod items
    */
    public static final DeferredItem<Item> ENERGY_HAMMER = ITEMS.register("energy_hammer", EnergyHammerItem::new);

    /*
    / Entities
     */
    public static final DeferredItem<Item> GECKO_SPAWN_EGG = ITEMS.register("gecko_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GECKO, 0x31afaf, 0xffac00,
                    new Item.Properties()));

    public static final DeferredItem<Item> LIZARD_SPAWN_EGG = ITEMS.register("lizard_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LIZARD, 0xe7d7a5, 0x7e5b41,
                    new Item.Properties()));

    public static final DeferredItem<Item> FRENCHIE_SPAWN_EGG = ITEMS.register("frenchie_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FRENCHIE, 0xe7d7a5, 0x7e5b41,
                    new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
