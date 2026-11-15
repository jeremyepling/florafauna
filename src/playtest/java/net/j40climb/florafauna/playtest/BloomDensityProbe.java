package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test-only measurement of blooms in freshly generated normal-terrain chunks. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class BloomDensityProbe {
    private static final String REPORT_ENV = "FLORAFAUNA_BLOOM_DENSITY_REPORT";
    private static final BlockPos SEARCH_ORIGIN = new BlockPos(4096, 64, 4096);
    private static final int RADIUS_CHUNKS = 1; // 3 x 3 around each located biome.

    private record Sample(int chunkX, int chunkZ, String biome, List<BlockPos> blooms) {}

    private BloomDensityProbe() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        String reportName = System.getenv(REPORT_ENV);
        if (reportName == null || reportName.isBlank()) return;
        Path report = Path.of(reportName);
        try {
            Files.createDirectories(report.getParent());
            Files.writeString(report, measure(event.getServer().overworld()));
        } catch (Exception e) {
            try {
                Files.writeString(report, "FAIL: " + e + System.lineSeparator());
            } catch (IOException ignored) {}
            throw new IllegalStateException("Bloom density probe failed", e);
        }
    }

    private static String measure(ServerLevel level) {
        String generator = level.getChunkSource().getGenerator().getClass().getSimpleName();
        if (!generator.toLowerCase().contains("noise")) {
            throw new IllegalStateException("Expected normal noise terrain, found " + generator);
        }

        List<ResourceKey<Biome>> targets = List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.RIVER);
        Map<ResourceKey<Biome>, BlockPos> anchors = new LinkedHashMap<>();
        Set<Long> requested = new LinkedHashSet<>();
        for (ResourceKey<Biome> target : targets) {
            var nearest = level.findClosestBiome3d(holder -> holder.is(target), SEARCH_ORIGIN, 2048, 32, 32);
            if (nearest == null) {
                throw new IllegalStateException("Could not locate " + target.identifier() + " near " + SEARCH_ORIGIN);
            }
            BlockPos found = nearest.getFirst();
            anchors.put(target, found);
            int centerX = Math.floorDiv(found.getX(), 16);
            int centerZ = Math.floorDiv(found.getZ(), 16);
            for (int dz = -RADIUS_CHUNKS; dz <= RADIUS_CHUNKS; dz++) {
                for (int dx = -RADIUS_CHUNKS; dx <= RADIUS_CHUNKS; dx++) {
                    requested.add(ChunkPos.pack(centerX + dx, centerZ + dz));
                }
            }
        }

        List<Sample> samples = new ArrayList<>();
        for (long packed : requested) {
            int cx = ChunkPos.getX(packed);
            int cz = ChunkPos.getZ(packed);
            LevelChunk chunk = level.getChunk(cx, cz);
            int x = cx * 16 + 8;
            int z = cz * 16 + 8;
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            String biome = level.getBiome(new BlockPos(x, surface, z)).unwrapKey()
                    .map(key -> key.identifier().toString()).orElse("unregistered");
            samples.add(new Sample(cx, cz, biome, countBlooms(level, chunk)));
        }

        StringBuilder report = new StringBuilder();
        report.append("generator=").append(generator).append("; searchOrigin=").append(SEARCH_ORIGIN)
                .append("; radiusChunks=").append(RADIUS_CHUNKS)
                .append("; requestedFreshChunks=").append(samples.size()).append('\n');
        report.append("Fresh means this probe ran in a newly created world and requested chunks around X/Z 4096. ")
                .append("Chunk-center surface biome is the grouping key; blooms anywhere in each chunk are counted.\n");
        anchors.forEach((target, pos) -> report.append("anchor ").append(target.identifier())
                .append("=").append(pos.toShortString()).append('\n'));
        Map<String, int[]> totals = new LinkedHashMap<>();
        List<BlockPos> allBlooms = new ArrayList<>();
        for (Sample sample : samples) {
            int count = sample.blooms().size();
            report.append("chunk ").append(sample.chunkX()).append(',').append(sample.chunkZ())
                    .append(" biome=").append(sample.biome()).append(" blooms=").append(count)
                    .append(" positions=").append(sample.blooms()).append('\n');
            int[] total = totals.computeIfAbsent(sample.biome(), ignored -> new int[2]);
            total[0]++;
            total[1] += count;
            allBlooms.addAll(sample.blooms());
        }
        totals.forEach((biome, total) -> report.append("total ").append(biome)
                .append(" chunks=").append(total[0]).append(" blooms=").append(total[1])
                .append(" bloomsPerChunk=").append((double) total[1] / total[0]).append('\n'));
        report.append("nearestBloomSpacingXZ=").append(nearestSpacing(allBlooms)).append(" blocks\n");
        return report.toString();
    }

    private static List<BlockPos> countBlooms(ServerLevel level, LevelChunk chunk) {
        List<BlockPos> blooms = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.maybeHas(state -> state.is(FloraFaunaRegistry.RESONANT_BLOOM.get()))) continue;
            int baseY = level.getMinY() + sectionIndex * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).is(FloraFaunaRegistry.RESONANT_BLOOM.get())) {
                            blooms.add(new BlockPos(chunk.getPos().x() * 16 + x, baseY + y,
                                    chunk.getPos().z() * 16 + z));
                        }
                    }
                }
            }
        }
        return blooms;
    }

    private static String nearestSpacing(List<BlockPos> blooms) {
        if (blooms.size() < 2) return "n/a";
        double nearest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < blooms.size(); i++) {
            for (int j = i + 1; j < blooms.size(); j++) {
                int dx = blooms.get(i).getX() - blooms.get(j).getX();
                int dz = blooms.get(i).getZ() - blooms.get(j).getZ();
                nearest = Math.min(nearest, Math.sqrt(dx * dx + dz * dz));
            }
        }
        return String.format("%.1f", nearest);
    }
}
