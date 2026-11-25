package net.j40climb.florafauna.platform.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModConfigSpecTest {
    @TempDir Path directory;

    @Test void readsExistingTomlKeysAndCorrectsInvalidValuesWithoutDiscardingOtherSettings() throws Exception {
        var builder = new ModConfigSpec.Builder().push("iteminput");
        var radius = builder.defineInRange("collectRadius", 8, 1, 32);
        var ticks = builder.defineInRange("collectIntervalTicks", 10, 1, 100);
        builder.pop().push("mobtransport");
        var capture = builder.defineInRange("captureRadius", 1.5, 0.5, 4.0);
        var unbonded = builder.define("allowUnbondedCapture", false);
        var config = builder.build();
        Path file = directory.resolve("florafauna-common.toml");
        Files.writeString(file, """
                unrelated = "keep me"
                [iteminput]
                collectRadius = 12
                collectIntervalTicks = 0
                [mobtransport]
                captureRadius = 2.25
                allowUnbondedCapture = true
                """);
        config.load(file);
        assertEquals(12, radius.get());
        assertEquals(10, ticks.get());
        assertEquals(2.25, capture.get());
        assertTrue(unbonded.get());
        assertTrue(Files.readString(file).contains("keep me"));
        // Reload the saved correction and confirm that nondefault valid values survived.
        config.load(file);
        assertEquals(12, radius.get());
        assertEquals(10, ticks.get());
    }

    @Test void createsDefaultConfigAndRejectsFractionalIntegersAndNonFiniteNumbers() throws Exception {
        var builder = new ModConfigSpec.Builder();
        var integer = builder.defineInRange("integer", 8, 1, 32);
        var decimal = builder.defineInRange("decimal", 1.5, 0.5, 4.0);
        var config = builder.build();
        Path file = directory.resolve("nested/settings.toml");
        config.load(file);
        assertTrue(Files.isRegularFile(file));
        assertEquals(8, integer.get());
        Files.writeString(file, "integer = 2.5\ndecimal = nan\n");
        config.load(file);
        assertEquals(8, integer.get());
        assertEquals(1.5, decimal.get());
    }
}
