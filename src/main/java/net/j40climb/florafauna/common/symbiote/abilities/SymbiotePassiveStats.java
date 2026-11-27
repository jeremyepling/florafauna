package net.j40climb.florafauna.common.symbiote.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteData;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class SymbiotePassiveStats {
    private static final double SPEED_PER_LEVEL = 0.2F;
    public static final double SPEED_BONUS = SymbioteData.MAX_SPEED_LEVEL * SPEED_PER_LEVEL;
    public static final Identifier SPEED_MODIFIER = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "symbiote_speed");
    public static final Identifier SAFE_FALL_MODIFIER = Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "symbiote_safe_fall");

    private SymbiotePassiveStats() {}

    public static float jumpBonus(int level) {
        return Math.max(0, level) * 0.1F;
    }

    public static double speedBonus(int level) {
        return level * SPEED_PER_LEVEL;
    }

    public static float jumpBonus(Player player) {
        var data = AttachmentAccess.existing(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA)
                .orElse(PlayerSymbioteData.DEFAULT);
        return data.symbioteState().areAbilitiesActive() ? jumpBonus(data.jumpBoost()) : 0;
    }

    static void updateAttributes(ServerPlayer player, PlayerSymbioteData data) {
        boolean active = data.symbioteState().areAbilitiesActive();
        updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER,
                active ? speedBonus(data.speedLevel()) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player.getAttribute(Attributes.SAFE_FALL_DISTANCE), SAFE_FALL_MODIFIER,
                active ? Math.max(0, data.jumpBoost()) : 0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void updateModifier(AttributeInstance attribute, Identifier id, double amount,
                                       AttributeModifier.Operation operation) {
        if (attribute == null) return;

        var current = attribute.getModifier(id);
        if (amount == 0) {
            if (current != null) attribute.removeModifier(id);
        } else if (current == null || current.amount() != amount) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
