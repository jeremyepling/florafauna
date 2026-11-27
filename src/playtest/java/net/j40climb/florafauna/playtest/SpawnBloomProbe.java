package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?}

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Test-only probe that observes the real dedicated-server world creation and restart paths. */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public final class SpawnBloomProbe {
    private static final String PHASE = "florafauna.spawnBloomProbe.phase";
    private static final int RADIUS = 100;

    private SpawnBloomProbe() {}

    //? if neoforge {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) { check(event.getServer()); }
    //?}

    static void check(MinecraftServer server) {
        String phase = System.clearProperty(PHASE);
        if (phase == null) return;
        Path report = Path.of("phase-" + phase + ".txt");
        try {
            ServerLevel level = server.overworld();
            BlockPos spawn = server.getRespawnData().pos();
            List<BlockPos> blooms = bloomsInRange(level, spawn);
            String result = switch (phase) {
                case "1" -> inspectAndBreak(level, spawn, blooms);
                case "2" -> {
                    require(blooms.isEmpty(), "Blooms appeared after reopening the same world: " + blooms);
                    yield "PASS reopen: zero blooms after removing all five and saving";
                }
                default -> throw new IllegalArgumentException("Unknown spawn-bloom probe phase: " + phase);
            };
            Files.writeString(report, result + System.lineSeparator());
        } catch (RuntimeException | IOException error) {
            try {
                Files.writeString(report, "FAIL: " + error + System.lineSeparator());
            } catch (IOException writeFailure) {
                error.addSuppressed(writeFailure);
            }
            if (error instanceof IOException io) throw new UncheckedIOException(io);
            throw (RuntimeException) error;
        } finally {
            server.halt(false);
        }
    }

    private static String inspectAndBreak(ServerLevel level, BlockPos spawn, List<BlockPos> blooms) {
        require(blooms.size() == 5, "Expected exactly five blooms in the undecorated world, found " + blooms);
        List<Double> bearings = new ArrayList<>();
        List<Long> radii = new ArrayList<>();
        for (BlockPos bloom : blooms) {
            require(level.getBlockState(bloom).canSurvive(level, bloom), "Bloom cannot survive at " + bloom);
            require(level.getBlockState(bloom.above()).isAir(), "Bloom is covered at " + bloom);
            double distance = Math.sqrt(distanceSquared(spawn, bloom));
            require(distance > 0 && distance <= RADIUS, "Bloom is outside the spawn radius: " + bloom);
            radii.add(Math.round(distance));
            bearings.add(Math.atan2(bloom.getZ() - spawn.getZ(), bloom.getX() - spawn.getX()));
        }
        bearings.sort(Comparator.naturalOrder());
        double largestGap = bearings.getFirst() + Math.PI * 2 - bearings.getLast();
        for (int i = 1; i < bearings.size(); i++) {
            largestGap = Math.max(largestGap, bearings.get(i) - bearings.get(i - 1));
        }
        require(Math.toDegrees(largestGap) < 144, "Blooms leave too much uncovered direction from spawn: " + bearings);
        require(radii.stream().distinct().count() >= 3, "Blooms are at nearly uniform distances: " + radii);
        for (BlockPos bloom : blooms) {
            require(level.destroyBlock(bloom, false), "Could not break bloom at " + bloom);
        }
        require(bloomsInRange(level, spawn).isEmpty(), "Expected spawn area to be clear before saving");
        return "PASS fresh world: spawn=" + spawn.toShortString() + ", five blooms=" + blooms
                + ", distances=" + radii + ", largest angular gap=" + Math.round(Math.toDegrees(largestGap))
                + " degrees; broke all five before saving";
    }

    private static List<BlockPos> bloomsInRange(ServerLevel level, BlockPos spawn) {
        var bloomBlock = FloraFaunaRegistry.RESONANT_BLOOM.get();
        List<BlockPos> blooms = new ArrayList<>();
        for (int cz = (spawn.getZ() - RADIUS) >> 4; cz <= (spawn.getZ() + RADIUS) >> 4; cz++) {
            for (int cx = (spawn.getX() - RADIUS) >> 4; cx <= (spawn.getX() + RADIUS) >> 4; cx++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                for (int index = 0; index < sections.length; index++) {
                    LevelChunkSection section = sections[index];
                    if (!section.maybeHas(state -> state.is(bloomBlock))) continue;
                    int baseY = level.getMinY() + index * 16;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                if (!section.getBlockState(x, y, z).is(bloomBlock)) continue;
                                BlockPos pos = new BlockPos(cx * 16 + x, baseY + y, cz * 16 + z);
                                if (distanceSquared(spawn, pos) <= RADIUS * RADIUS) blooms.add(pos);
                            }
                        }
                    }
                }
            }
        }
        return blooms;
    }

    private static double distanceSquared(BlockPos origin, BlockPos pos) {
        double dx = pos.getX() - (double) origin.getX();
        double dz = pos.getZ() - (double) origin.getZ();
        return dx * dx + dz * dz;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
