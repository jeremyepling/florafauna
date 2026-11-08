package net.j40climb.florafauna.playtest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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
    private Screen lastUpgradePrompt;

    private record Step(String name, Function<Minecraft, CompletableFuture<Boolean>> run) {}

    private Playtest(Path directory, String scenario, int phase) {
        this.directory = directory;
        this.scenario = scenario;
        this.phase = phase;
        awaitClient("Join the copied world", mc -> {
            handleCopyUpgradePrompt(mc);
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.gui.screen() != null) return false;
            try {
                Path expected = directory.resolve("saves/playtest").toRealPath();
                verifyCopyDirectory(mc);
                require(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(expected), "Wrong save");
            } catch (IOException e) {
                throw new IllegalStateException("Cannot verify the copied world", e);
            }
            return true;
        });
        switch (scenario) {
            case "recovery" -> RecoveryPlaytest.steps(this, phase);
            case "hammer" -> HammerPlaytest.steps(this, phase);
            case "pods" -> PodPlaytest.steps(this, phase);
            case "frenchie" -> FrenchiePlaytest.steps(this, phase);
            case "transport" -> MobTransportPlaytest.steps(this, phase);
            default -> throw new IllegalArgumentException("Unknown playtest " + scenario);
        }
        deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
    }

    /** Follow vanilla's backup and file migration only for the runner's disposable copy. */
    private void handleCopyUpgradePrompt(Minecraft mc) {
        Screen screen = mc.gui.screen();
        if (screen == null || screen == lastUpgradePrompt) return;
        Component label;
        if (screen instanceof BackupConfirmScreen && screen.getTitle().equals(Component.translatable("selectWorld.backupQuestion.file_fixing_required"))) {
            label = BackupConfirmScreen.BACKUP_AND_JOIN;
        } else if (screen instanceof ConfirmScreen && screen.getTitle().equals(Component.translatable("upgradeWorld.done"))) {
            label = CommonComponents.GUI_YES;
        } else {
            return;
        }
        verifyCopyDirectory(mc);
        for (var child : screen.children()) {
            if (child instanceof Button button && button.active && button.visible && label.equals(button.getMessage())) {
                var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                require(screen.mouseClicked(click, false), "World upgrade button did not accept click");
                lastUpgradePrompt = screen;
                return;
            }
        }
    }

    private void verifyCopyDirectory(Minecraft mc) {
        try {
            require(mc.gameDirectory.toPath().toRealPath().equals(directory.toRealPath()), "Wrong game directory");
            require(Files.readString(directory.resolve(".florafauna-playtest")).trim().equals(scenario), "Missing test marker");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot verify the copy's game directory", e);
        }
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
                Screenshot.grab(mc.gameDirectory, filename, mc.gameRenderer.mainRenderTarget(), 1, ignored -> {}));
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
            if (mc.gui.screen() instanceof PauseScreen) mc.gui.setScreen(null);
            if (System.nanoTime() > deadline) {
                Screen screen = mc.gui.screen();
                throw new AssertionError("Timed out; screen=" + (screen == null ? "none"
                        : screen.getClass().getSimpleName() + " (" + screen.getTitle().getString() + ")"));
            }
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
