package net.j40climb.florafauna.common.item.symbiote.dialogue;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.item.symbiote.PlayerSymbioteData;
import net.j40climb.florafauna.common.item.symbiote.observation.ChaosSuppressor;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationArbiter;
import net.j40climb.florafauna.common.item.symbiote.observation.ObservationCategory;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

/**
 * Event handler for symbiote observations.
 * Routes game events through the ObservationArbiter for the tiered voice system.
 *
 * <h2>How Events Flow</h2>
 * <ol>
 *   <li>Game event occurs (damage, sleep, etc.)</li>
 *   <li>This handler determines the {@link ObservationCategory} and severity</li>
 *   <li>{@link ObservationArbiter#observe} is called to process the observation</li>
 *   <li>Arbiter selects a line and routes through {@link SymbioteVoiceService}</li>
 * </ol>
 *
 * @see ObservationArbiter
 * @see ObservationCategory
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class SymbioteDialogueEvents {

    /**
     * Handle damage events and route through the observation system.
     * Maps damage types to categories with appropriate severity.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerSymbioteData symbioteData = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().isBonded()) {
            return;
        }

        // Record damage for chaos suppression
        ChaosSuppressor.recordDamage(player);

        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        int severity = ObservationArbiter.damageToSeverity(damage);

        // Determine category and damage type based on damage source
        ObservationCategory category;
        String damageType;

        if (source.is(DamageTypeTags.IS_FREEZING)) {
            category = ObservationCategory.ENVIRONMENTAL_HAZARD;
            damageType = "freezing";
        } else if (source.is(DamageTypeTags.IS_FALL)) {
            category = ObservationCategory.FALL_DAMAGE;
            damageType = "fall";
        } else if (source.is(DamageTypeTags.IS_DROWNING)) {
            category = ObservationCategory.ENVIRONMENTAL_HAZARD;
            damageType = "drowning";
        } else if (source.getEntity() != null && source.getEntity() != player) {
            category = ObservationCategory.COMBAT_DAMAGE;
            damageType = "mob_attack";
        } else {
            category = ObservationCategory.PLAYER_STATE;
            damageType = source.getMsgId();
        }

        // Route through observation arbiter
        ObservationArbiter.observe(player, category, severity, Map.of(
                "damageType", damageType,
                "damageAmount", damage
        ));
    }

    /**
     * Handle player tick events for cold water detection.
     * Detects when player is in water in a cold biome (even without freeze damage).
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Only check every 20 ticks (1 second) to reduce overhead
        if (serverPlayer.tickCount % 20 != 0) {
            return;
        }

        PlayerSymbioteData symbioteData = serverPlayer.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().isBonded()) {
            return;
        }

        // Check if player is in water in a cold biome
        if (serverPlayer.isInWater()) {
            BlockPos pos = serverPlayer.blockPosition();
            Biome biome = serverPlayer.level().getBiome(pos).value();

            if (biome.coldEnoughToSnow(pos, serverPlayer.level().getSeaLevel())) {
                // Cold water observation with low severity (discomfort, not damage)
                ObservationArbiter.observe(serverPlayer, ObservationCategory.ENVIRONMENTAL_HAZARD, 20, Map.of(
                        "damageType", "cold_water"
                ));
            }
        }
    }

    /**
     * Handle sleep events - triggers when player wakes up.
     * This is a bonding/comfort observation.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerSymbioteData symbioteData = serverPlayer.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().isBonded()) {
            return;
        }

        // Sleep is a bonding moment (low severity, positive observation)
        ObservationArbiter.observe(serverPlayer, ObservationCategory.PLAYER_STATE, 10, Map.of(
                "event", "wakeup"
        ));
    }
}
