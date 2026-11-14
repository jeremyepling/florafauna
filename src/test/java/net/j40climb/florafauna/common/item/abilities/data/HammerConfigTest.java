package net.j40climb.florafauna.common.item.abilities.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HammerConfigTest {
    @Test
    void newHammerDefaults() {
        assertEquals(new MiningModeData(MiningShape.SINGLE, 0, 64, true), MiningModeData.DEFAULT);
        assertEquals(new ToolConfig(true, false, MiningSpeed.EFFICIENCY), ToolConfig.DEFAULT);
    }

    @ParameterizedTest
    @CsvSource({
            "SINGLE, FLAT_3X3, 1",
            "FLAT_3X3, FLAT_5X5, 2",
            "FLAT_5X5, FLAT_7X7, 3",
            "FLAT_7X7, SHAPELESS, 1",
            "SHAPELESS, TUNNEL_UP, 0",
            "TUNNEL_UP, TUNNEL_DOWN, 0",
            "TUNNEL_DOWN, SINGLE, 0"
    })
    void cyclingSetsTheNextShapeAndRadiusAndKeepsCustomRules(MiningShape current, MiningShape next, int radius) {
        var mode = new MiningModeData(current, current.getRadius(), 7, false);
        assertEquals(new MiningModeData(next, radius, 7, false), mode.getNextMode());
    }

    @ParameterizedTest
    @CsvSource({
            "-2147483648, -2147483648, 0, 1",
            "2147483647, 2147483647, 3, 64",
            "0, 64, 0, 64",
            "3, 1, 3, 1",
            "2, 17, 2, 17"
    })
    void oldSaveValuesAreBoundedAndMissingToolFlagDefaultsToTrue(int radius, int cap, int newRadius, int newCap) {
        var saved = new JsonObject();
        saved.addProperty("shape", "shapeless");
        saved.addProperty("radius", radius);
        saved.addProperty("maxBlocksToBreak", cap);
        var decoded = MiningModeData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(new MiningModeData(MiningShape.SHAPELESS, newRadius, newCap, true), decoded);
    }

    @ParameterizedTest
    @EnumSource(MiningSpeed.class)
    void enchantmentTogglesAreReversibleAndPreserveSpeed(MiningSpeed speed) {
        var fortune = new ToolConfig(true, false, speed);
        var silk = new ToolConfig(false, true, speed);
        assertEquals(silk, fortune.withToggledEnchantment());
        assertEquals(fortune, silk.withToggledEnchantment());
    }

    @ParameterizedTest
    @CsvSource({"STANDARD, EFFICIENCY", "EFFICIENCY, INSTABREAK", "INSTABREAK, STANDARD"})
    void speedsCycleInBothDirectionsAndPreserveEnchantment(MiningSpeed first, MiningSpeed next) {
        var current = new ToolConfig(false, true, first);
        var expected = new ToolConfig(false, true, next);
        assertEquals(expected, current.withNextMiningSpeed());
        assertEquals(current, expected.withPreviousMiningSpeed());
        assertEquals(expected, current.withMiningSpeed(next));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothOrNeitherEnchantmentIsRejected(boolean enabled) {
        assertThrows(IllegalArgumentException.class, () -> new ToolConfig(enabled, enabled, MiningSpeed.STANDARD));
    }
}