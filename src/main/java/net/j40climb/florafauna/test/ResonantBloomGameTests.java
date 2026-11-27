package net.j40climb.florafauna.test;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
//?} else {
/*import net.minecraft.world.level.levelgen.feature.Feature;
*///?}
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.minecraft.world.phys.AABB;

import java.util.List;

/** Verifies the natural Survival discovery path, independently of the client fixture. */
final class ResonantBloomGameTests {
    private static final ResourceKey<PlacedFeature> BLOOM_FEATURE = ResourceKey.create(
            Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("florafauna", "resonant_bloom"));
    //? if <26.3 {
    private static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_BLOOM = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath("florafauna", "resonant_bloom"));
    //?} else {
    /*private static final ResourceKey<Feature> CONFIGURED_BLOOM = ResourceKey.create(
            Registries.FEATURE, Identifier.fromNamespaceAndPath("florafauna", "resonant_bloom"));
    *///?}

    private ResonantBloomGameTests() {}

    static void dropsPollen(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(pos, FloraFaunaRegistry.RESONANT_BLOOM.get());

        ServerLevel level = helper.getLevel();
        BlockPos absolutePos = helper.absolutePos(pos);
        level.destroyBlock(absolutePos, true);
        int pollen = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolutePos).inflate(1),
                        entity -> entity.getItem().is(FloraFaunaRegistry.SYMBIOTIC_POLLEN.get()))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        if (pollen != 2) {
            throw helper.assertionException("Expected two Symbiotic Pollen from one Resonant Bloom, got " + pollen);
        }
        helper.succeed();
    }

    static void naturalDiscovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> biomeKey : List.of(Biomes.PLAINS, Biomes.FOREST)) {
            Holder<Biome> biome = biomes.getOrThrow(biomeKey);
            boolean included = biome.value().getGenerationSettings().features().stream()
                    .flatMap(HolderSet::stream).anyMatch(feature -> feature.is(BLOOM_FEATURE));
            if (!included) {
                throw helper.assertionException("Resonant Bloom is absent from " + biomeKey.identifier()
                        + " vegetation features");
            }
        }

        //? if <26.3 {
        var configuredFeatures = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> bloom = configuredFeatures.getOrThrow(CONFIGURED_BLOOM).value();
        //?} else {
        /*var configuredFeatures = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        Feature bloom = configuredFeatures.getOrThrow(CONFIGURED_BLOOM).value();
        *///?}
        BlockPos testOrigin = helper.absolutePos(BlockPos.ZERO);
        BlockPos origin = new BlockPos((testOrigin.getX() & ~15) + 8, 200, (testOrigin.getZ() & ~15) + 8);
        // A configured feature can be exercised in a headless test world. The
        // outer placed feature's biome filter and worldgen heightmap need a fresh
        // generation context, which GameTest's already-loaded chunks do not have.
        level.getChunkAt(origin);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 2, 1))) {
            level.setBlockAndUpdate(pos, pos.getY() == 199
                    ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        if (!bloom.place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin)) {
            throw helper.assertionException("Registered Resonant Bloom configured feature could not place on grass");
        }
        if (!level.getBlockState(origin).is(FloraFaunaRegistry.RESONANT_BLOOM.get())) {
            throw helper.assertionException("Configured feature reported success but produced no Resonant Bloom");
        }
        helper.succeed();
    }
}
