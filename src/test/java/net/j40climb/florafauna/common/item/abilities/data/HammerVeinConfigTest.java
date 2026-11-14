package net.j40climb.florafauna.common.item.abilities.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HammerVeinConfigTest {
    @Test
    void olderSavesUseSameMaterialMatching() {
        var saved = new JsonObject();
        saved.addProperty("fortune", false);
        saved.addProperty("silkTouch", true);
        saved.addProperty("miningSpeed", "standard");
        assertEquals(new ToolConfig(false, true, MiningSpeed.STANDARD, false),
                ToolConfig.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
        assertFalse(ToolConfig.DEFAULT.exactVein());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preferenceSurvivesSaveAndConfigPacket(boolean exact) {
        var config = new ToolConfig(false, true, MiningSpeed.STANDARD, exact);
        var encoded = ToolConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
        assertEquals(config, ToolConfig.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());

        var packet = new UpdateToolConfigPayload(config);
        var buffer = Unpooled.buffer();
        try {
            UpdateToolConfigPayload.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, UpdateToolConfigPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void togglesIndependentlyOfSpeedAndEnchantments() {
        var config = new ToolConfig(true, false, MiningSpeed.INSTABREAK, true);
        assertEquals(new ToolConfig(true, false, MiningSpeed.INSTABREAK, false), config.withToggledVeinMatching());
        assertEquals(config, config.withToggledVeinMatching().withToggledVeinMatching());
        assertEquals(new ToolConfig(false, true, MiningSpeed.INSTABREAK, true), config.withToggledEnchantment());
        assertEquals(new ToolConfig(true, false, MiningSpeed.STANDARD, true), config.withNextMiningSpeed());
    }
}
