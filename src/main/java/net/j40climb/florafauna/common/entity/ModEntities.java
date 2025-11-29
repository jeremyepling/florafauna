package net.j40climb.florafauna.common.entity;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.client.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.client.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.client.lizard.LizardEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FloraFauna.MOD_ID);

    public static ResourceKey<EntityType<?>> GECKO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("gecko"));
    public static ResourceKey<EntityType<?>> LIZARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("lizard"));
    public static ResourceKey<EntityType<?>> FRENCHIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("frenchie"));


    public static final Supplier<EntityType<GeckoEntity>> GECKO =
            ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.35f).build(GECKO_KEY)); // Size is the hitbox

    public static final Supplier<EntityType<LizardEntity>> LIZARD =
            ENTITY_TYPES.register("lizard", () -> EntityType.Builder.of(LizardEntity::new, MobCategory.CREATURE)
                    .sized(1.4f, 1.35f).build(LIZARD_KEY)); // Size is the hitbox

    public static final Supplier<EntityType<FrenchieEntity>> FRENCHIE =
            ENTITY_TYPES.register("frenchie", () -> EntityType.Builder.of(FrenchieEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f)
                    .build(FRENCHIE_KEY)); // Size is the hitbox

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}