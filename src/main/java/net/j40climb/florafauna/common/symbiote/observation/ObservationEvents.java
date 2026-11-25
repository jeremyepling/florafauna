package net.j40climb.florafauna.common.symbiote.observation;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

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
 *   <li>Arbiter selects a line and routes through {@link net.j40climb.florafauna.common.symbiote.voice.SymbioteVoiceService}</li>
 * </ol>
 *
 * @see ObservationArbiter
 * @see ObservationCategory
 */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public class ObservationEvents {

    /**
     * Handle damage events and route through the observation system.
     * Maps damage types to categories with appropriate severity.
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        onLivingDamage(event.getEntity(), event.getSource(), event.getNewDamage());
    }
    //?}

    public static void onLivingDamage(net.minecraft.world.entity.LivingEntity subject, DamageSource source, float damage) {
        if (!(subject instanceof ServerPlayer player)) {
            return;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().isBonded()) {
            return;
        }

        // Record damage for chaos suppression
        ChaosSuppressor.recordDamage(player);

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
    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        onPlayerTick(event.getEntity());
    }
    //?}

    public static void onPlayerTick(net.minecraft.world.entity.player.Player subject) {
        Player player = subject;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Only check every 20 ticks (1 second) to reduce overhead
        if (serverPlayer.tickCount % 20 != 0) {
            return;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(serverPlayer, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
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
    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        onPlayerWakeUp(event.getEntity());
    }
    //?}

    public static void onPlayerWakeUp(net.minecraft.world.entity.player.Player subject) {
        Player player = subject;

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(serverPlayer, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().isBonded()) {
            return;
        }

        // Sleep is a bonding moment (low severity, positive observation)
        ObservationArbiter.observe(serverPlayer, ObservationCategory.PLAYER_STATE, 10, Map.of(
                "event", "wakeup"
        ));
    }
}
