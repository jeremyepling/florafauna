package net.j40climb.florafauna.common.entity.frenchie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanionStateTest {
    @Test
    void coatIdsSurviveSaveReloadAndUnknownIdsUseFawn() {
        for (var coat : FrenchieVariant.values()) {
            assertSame(coat, FrenchieVariant.byId(coat.getId()));
        }
        assertSame(FrenchieVariant.FAWN, FrenchieVariant.byId(-1));
        assertSame(FrenchieVariant.FAWN, FrenchieVariant.byId(Integer.MAX_VALUE));
    }

    @Test
    void warningFlashesExactlyTwiceWithAGapBetweenBarks() {
        for (int tick = -1; tick <= FrenchieAwareness.WARNING_TICKS; tick++) {
            boolean expected = tick >= 0 && tick < 5 || tick >= 7 && tick < 12;
            assertEquals(expected, FrenchieAwareness.flashing(tick), "warning tick " + tick);
        }
    }
}
