package net.j40climb.florafauna.common.entity;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieModel;
import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.gecko.GeckoModel;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardModel;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class RegisterEntityEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GeckoModel.GECKO, GeckoModel::createBodyLayer);
        event.registerLayerDefinition(LizardModel.LIZARD, LizardModel::createBodyLayer);
        event.registerLayerDefinition(FrenchieModel.FRENCHIE, FrenchieModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FloraFaunaRegistry.GECKO.get(), GeckoEntity.createAttributes().build());
        event.put(FloraFaunaRegistry.LIZARD.get(), LizardEntity.createAttributes().build());
        event.put(FloraFaunaRegistry.FRENCHIE.get(), FrenchieEntity.createAttributes().build());
    }

    // Manually create the spawn file since I don't have worldgen. Create file in src/generated/resources/data/florafauna/neoforge/biome_modifier/spawn_gecko.json
    // https://github.com/Tutorials-By-Kaupenjoe/NeoForge-Tutorial-1.21.X/compare/40-entityVariant...41-spawnEntity
    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(FloraFaunaRegistry.GECKO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(FloraFaunaRegistry.LIZARD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}