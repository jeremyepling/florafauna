package net.j40climb.florafauna.common.item.abilities.data;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HammerActionTest {
    @ParameterizedTest
    @EnumSource(HammerAction.class)
    void eachChoiceRoundTripsAndRespectsEnabledToolActions(HammerAction action) {
        var saved = HammerAction.CODEC.encodeStart(JsonOps.INSTANCE, action).getOrThrow();
        assertEquals(action, HammerAction.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
        for (int mask = 0; mask < 8; mask++) {
            var enabled = new MultiToolAbilityData((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            var selected = action.toolActions(enabled);
            assertEquals(enabled.strip() && (action == HammerAction.AUTO || action == HammerAction.STRIP), selected.strip());
            assertEquals(enabled.flatten() && (action == HammerAction.AUTO || action == HammerAction.PATH), selected.flatten());
            assertEquals(enabled.till() && (action == HammerAction.AUTO || action == HammerAction.TILL), selected.till());
        }
    }
}
