package net.j40climb.florafauna.entity;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.entity.custom.FrenchieEntity;
import net.j40climb.florafauna.entity.custom.GeckoEntity;
import net.j40climb.florafauna.entity.custom.LizardEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FloraFauna.MOD_ID);

    public static final Supplier<EntityType<GeckoEntity>> GECKO =
            ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.35f).build("gecko")); // Size is the hitbox

    public static final Supplier<EntityType<LizardEntity>> LIZARD =
            ENTITY_TYPES.register("lizard", () -> EntityType.Builder.of(LizardEntity::new, MobCategory.CREATURE)
                    .sized(1.4f, 1.35f).build("lizard")); // Size is the hitbox

    public static final Supplier<EntityType<FrenchieEntity>> FRENCHIE =
            ENTITY_TYPES.register("frenchie", () -> EntityType.Builder.of(FrenchieEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f).build("frenchie")); // Size is the hitbox

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}