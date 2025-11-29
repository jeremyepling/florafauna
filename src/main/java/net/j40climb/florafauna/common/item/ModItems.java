package net.j40climb.florafauna.common.item;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.ModEntities;
import net.j40climb.florafauna.common.item.custom.EnergyHammerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FloraFauna.MOD_ID);

    // Food
    public static final DeferredItem<Item> TOMATO = ITEMS.registerItem("tomato", properties ->
            new Item(properties.component(DataComponents.CONSUMABLE,
                    Consumable.builder()
                            .consumeSeconds(1.6f)
                            .animation(ItemUseAnimation.EAT)
                            .sound(SoundEvents.GENERIC_EAT)
                            .soundAfterConsume(SoundEvents.GENERIC_DRINK)
                            .hasConsumeParticles(true)
                            .onConsume(
                                    new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F)
                            )
                            .build()
            )) {
                // Using an anonymous class to create a tooltip inline instead of using a full class in ModItems
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                    tooltipComponents.add(Component.translatable("tooltip.florafauna.tomato"));
                }
            }
            );


    /*
    / Mod items
    */
    public static final DeferredItem<Item> ENERGY_HAMMER = ITEMS.registerItem("energy_hammer", EnergyHammerItem::new);

    /*
    / Entities
     */
    public static final DeferredItem<Item> GECKO_SPAWN_EGG = ITEMS.registerItem("gecko_spawn_egg", properties ->
            new SpawnEggItem(ModEntities.GECKO.get(), 0x31afaf, 0xffac00, properties));

    public static final DeferredItem<Item> LIZARD_SPAWN_EGG = ITEMS.registerItem("lizard_spawn_egg", properties ->
            new SpawnEggItem(ModEntities.LIZARD.get(), 0xffac00, 0x7e5b41, properties));

    public static final DeferredItem<Item> FRENCHIE_SPAWN_EGG = ITEMS.registerItem("frenchie_spawn_egg", properties ->
            new SpawnEggItem(ModEntities.FRENCHIE.get(), 0xe7d7a5, 0xffac00, properties));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
