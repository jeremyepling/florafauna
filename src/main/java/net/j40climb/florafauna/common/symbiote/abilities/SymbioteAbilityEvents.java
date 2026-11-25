package net.j40climb.florafauna.common.symbiote.abilities;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

import java.util.List;

/**
 * Event handler for symbiote abilities.
 * Handles creeper detection, speed effect, fall damage negation, and jump boost.
 */
//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public class SymbioteAbilityEvents {

    /**
     * Handle player tick events for abilities that need periodic checking
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        onPlayerTick(event.getEntity());
    }
    //?}

    public static void onPlayerTick(net.minecraft.world.entity.player.Player subject) {
        Player player = subject;

        // Only process on server side
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(serverPlayer, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().areAbilitiesActive()) {
            return;
        }

        // Check every 20 ticks (1 second) for performance
        if (serverPlayer.tickCount % 20 == 0) {
            // Creeper detection
            detectNearbyCreepers(serverPlayer);
        }

        // Apply speed effect continuously when enabled
        if (symbioteData.speed()) {
            applySpeedEffect(serverPlayer);
        }

        // Apply jump boost effect when enabled
        if (symbioteData.jumpBoost() > 0) {
            applyJumpBoostEffect(serverPlayer, symbioteData.jumpBoost());
        }
    }

    /**
     * Detects creepers within 10 blocks and warns the player
     */
    private static void detectNearbyCreepers(ServerPlayer player) {
        // Create bounding box 10 blocks in all directions
        AABB searchBox = player.getBoundingBox().inflate(10.0);

        // Find all creepers in range
        List<Creeper> nearbyCreeepers = player.level().getEntitiesOfClass(
                Creeper.class,
                searchBox,
                creeper -> creeper.isAlive() && !creeper.isRemoved()
        );

        if (!nearbyCreeepers.isEmpty()) {
            Creeper closestCreeper = nearbyCreeepers.get(0);
            double closestDistance = player.distanceTo(closestCreeper);

            // Find the closest creeper
            for (Creeper creeper : nearbyCreeepers) {
                double distance = player.distanceTo(creeper);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestCreeper = creeper;
                }
            }

            // Send warning message
            Component direction = Component.translatable("direction.florafauna." + getDirectionToEntity(player, closestCreeper));
            player.sendOverlayMessage(
                    Component.translatable("message.florafauna.creeper_detected", direction,
                            String.format(java.util.Locale.ROOT, "%.1f", closestDistance))
                            .withStyle(style -> style.withColor(0xFF6B6B))
            );
        }
    }

    /**
     * Gets a rough directional description (N, S, E, W, etc.) from player to entity
     */
    private static String getDirectionToEntity(Player player, Creeper creeper) {
        double dx = creeper.getX() - player.getX();
        double dz = creeper.getZ() - player.getZ();

        // Determine primary direction
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "east" : "west";
        } else {
            return dz > 0 ? "south" : "north";
        }
    }

    /**
     * Applies Speed 2 effect to the player
     */
    private static void applySpeedEffect(ServerPlayer player) {
        // Apply Speed 2 for 2 seconds (40 ticks), refresh every tick
        // Using ambient=true and showParticles=false for a cleaner effect
        MobEffectInstance speedEffect = new MobEffectInstance(
                MobEffects.SPEED,
                40,    // duration: 2 seconds
                1,     // amplifier: 1 = Speed 2
                true,  // ambient
                false  // showParticles
        );
        player.addEffect(speedEffect);
    }

    /**
     * Applies Jump Boost effect to the player based on jumpBoost level
     * jumpBoost 1 = Jump Boost I, jumpBoost 2 = Jump Boost II, etc.
     */
    private static void applyJumpBoostEffect(ServerPlayer player, int jumpBoost) {
        // Apply Jump Boost for 2 seconds (40 ticks), refresh every tick
        // Amplifier is jumpBoost - 1 (jumpBoost 1 = amplifier 0 = Jump Boost I)
        MobEffectInstance jumpEffect = new MobEffectInstance(
                MobEffects.JUMP_BOOST,
                40,             // duration: 2 seconds
                jumpBoost - 1,  // amplifier: jumpBoost - 1
                true,           // ambient
                false           // showParticles
        );
        player.addEffect(jumpEffect);
    }

    /**
     * Handle fall damage to negate it when featherFalling is enabled
     */
    //? if neoforge {
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        event.setNewDamage(onLivingDamage(event.getEntity(), event.getSource(), event.getNewDamage()));
    }
    //?}

    public static float onLivingDamage(net.minecraft.world.entity.LivingEntity subject, net.minecraft.world.damagesource.DamageSource source, float damage) {
        // Only process for server-side players with bonded symbiotes
        if (!(subject instanceof ServerPlayer player)) {
            return damage;
        }

        PlayerSymbioteData symbioteData = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
        if (!symbioteData.symbioteState().areAbilitiesActive()) {
            return damage;
        }

        // Check if this is fall damage and featherFalling is enabled
        if (symbioteData.featherFalling() && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            // Cancel the fall damage
            return 0;
        }
        return damage;
    }
}
