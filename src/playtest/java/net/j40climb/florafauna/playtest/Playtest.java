package net.j40climb.florafauna.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Drives one real client and its integrated server. Only loaded by the playtest run. */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class Playtest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final long TIMEOUT = Duration.ofSeconds(35).toNanos();
    private static Playtest active;
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Path directory;
    private final String scenario;
    private final int phase;
    private long deadline;
    private CompletableFuture<Boolean> pending;
    private boolean finished;

    private record Step(String name, Function<Minecraft, CompletableFuture<Boolean>> run) {}

    private Playtest(Path directory, String scenario, int phase) {
        this.directory = directory;
        this.scenario = scenario;
        this.phase = phase;
        awaitClient("Join the copied world", mc -> {
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.screen != null) return false;
            try {
                Path expected = directory.resolve("saves/playtest").toRealPath();
                require(mc.gameDirectory.toPath().toRealPath().equals(directory.toRealPath()), "Wrong game directory");
                require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(expected), "Wrong save");
                require(Files.readString(directory.resolve(".florafauna-playtest")).trim().equals(scenario), "Missing test marker");
            } catch (IOException e) {
                throw new IllegalStateException("Cannot verify the copied world", e);
            }
            return true;
        });
        switch (scenario) {
            case "recovery" -> RecoveryPlaytest.steps(this, phase);
            case "hammer" -> HammerPlaytest.steps(this, phase);
            case "pods" -> PodPlaytest.steps(this, phase);
            case "frontpack" -> FrontpackPlaytest.steps(this, phase);
            case "slime" -> SlimeGratePlaytest.steps(this, phase);
            case "frenchie" -> FrenchiePlaytest.steps(this, phase);
            case "spider" -> SpiderSilkPlaytest.steps(this, phase);
            case "transport" -> MobTransportPlaytest.steps(this, phase);
            default -> throw new IllegalArgumentException("Unknown playtest " + scenario);
        }
        deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
    }

    void client(String name, Consumer<Minecraft> action) {
        awaitClient(name, mc -> { action.accept(mc); return true; });
    }

    void awaitClient(String name, Predicate<Minecraft> ready) {
        steps.add(new Step(name, mc -> CompletableFuture.completedFuture(ready.test(mc))));
    }

    void screenshot(String stem) {
        String filename = stem + ".png";
        client("Capture " + filename, mc ->
                Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), 1, ignored -> {}));
        awaitClient("Write " + filename, mc ->
                Files.isRegularFile(directory.resolve("screenshots").resolve(filename)));
    }

    void server(String name, Consumer<ServerPlayer> action) {
        awaitServer(name, player -> { action.accept(player); return true; });
    }

    void awaitServer(String name, Predicate<ServerPlayer> ready) {
        steps.add(new Step(name, mc -> {
            var server = mc.getSingleplayerServer();
            if (server == null || mc.player == null) return CompletableFuture.completedFuture(false);
            UUID id = mc.player.getUUID();
            return server.submit(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                return player != null && ready.test(player);
            });
        }));
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        String path = System.getProperty("florafauna.playtest.directory");
        if (path == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (active == null) {
            active = new Playtest(Path.of(path), System.getProperty("florafauna.playtest.scenario"),
                    Integer.getInteger("florafauna.playtest.phase"));
        }
        if (!active.finished) active.advance(mc);
    }

    private void advance(Minecraft mc) {
        Step step = steps.peek();
        try {
            if (step == null) {
                finish(mc, "PASS");
                return;
            }
            if (mc.screen instanceof PauseScreen) mc.setScreen(null);
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out; screen="
                    + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
            if (pending == null) pending = step.run().apply(mc);
            if (!pending.isDone()) return;
            if (pending.join()) {
                LOG.info("PLAYTEST {} phase {} OK: {}", scenario, phase, step.name());
                steps.remove();
                deadline = System.nanoTime() + TIMEOUT;
            }
            pending = null;
        } catch (AssertionError | RuntimeException e) {
            LOG.error("Playtest failed at {}", step == null ? "completion" : step.name(), e);
            finish(mc, "FAIL " + (step == null ? "completion" : step.name()) + ": " + e);
        }
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        LOG.info("PLAYTEST {} phase {}: {}", scenario, phase, result);
        try {
            Files.writeString(directory.resolve("phase-" + phase + ".result"), result + System.lineSeparator());
        } catch (IOException e) {
            LOG.error("Cannot write playtest result", e);
        } finally {
            mc.stop(); // Normal game shutdown saves the world and player before the next phase starts.
        }
    }
}
