package net.j40climb.florafauna.common.entity.fear.blaze;

import net.j40climb.florafauna.Config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Blaze-specific fear behavior handlers.
 * Handles attack suppression, blaze rod drops, particles, and death.
 *
 * Blazes become scared when:
 * 1. Snow golems are nearby (minimum count configurable)
 * 2. Cold blocks (snow/ice) are present in the area (configurable threshold)
 *
 * By default, BOTH conditions must be met to trigger fear.
 */
public final class BlazeFearHandler {

    private static final Random RANDOM = new Random();

    private BlazeFearHandler() {
        // Utility class
    }

    // ==================== STATE ENTRY HANDLERS ====================

    /**
     * Called when a blaze enters the PANICKED state.
     * Plays hissing sound and starts visual cues.
     */
    public static void onEnterPanicked(Blaze blaze) {
        if (!blaze.level().isClientSide()) {
            blaze.level().playSound(
                    null,
                    blaze.getX(),
                    blaze.getY(),
                    blaze.getZ(),
                    SoundEvents.BLAZE_AMBIENT,
                    SoundSource.HOSTILE,
                    1.0f,
                    0.5f  // Low pitch for distressed sound
            );
        }
    }

    // ==================== TICK HANDLERS ====================

    /**
     * Called each fear update tick while in PANICKED state.
     * Spawns smoke/steam particles and optionally suppresses attacks.
     */
    public static void onTickPanicked(Blaze blaze) {
        spawnPanicParticles(blaze);

        // Suppress attacks if configured
        if (Config.blazeSuppressAttacks) {
            suppressAttacks(blaze);
        }
    }

    // ==================== LEAK EVENT ====================

    /**
     * Called when a blaze triggers a LEAK event.
     * Drops blaze rods and plays extinguishing sound.
     */
    public static void onLeakEvent(Blaze blaze) {
        if (blaze.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) blaze.level();

        // Calculate drop amount
        int dropCount = Config.blazeRodDropMin +
                RANDOM.nextInt(Config.blazeRodDropMax - Config.blazeRodDropMin + 1);

        // Spawn blaze rod items
        Vec3 pos = blaze.position();
        for (int i = 0; i < dropCount; i++) {
            // Add slight random offset and velocity for visual spread
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            double velocityY = 0.2 + RANDOM.nextDouble() * 0.2;
            double velocityX = (RANDOM.nextDouble() - 0.5) * 0.1;
            double velocityZ = (RANDOM.nextDouble() - 0.5) * 0.1;

            ItemStack rod = new ItemStack(Items.BLAZE_ROD, 1);
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.x + offsetX,
                    pos.y + 0.8,
                    pos.z + offsetZ,
                    rod
            );
            itemEntity.setDeltaMovement(velocityX, velocityY, velocityZ);
            level.addFreshEntity(itemEntity);
        }

        // Play extinguishing sound
        level.playSound(
                null,
                blaze.getX(),
                blaze.getY(),
                blaze.getZ(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.HOSTILE,
                1.0f,
                1.2f
        );

        // Spawn burst of steam particles
        spawnLeakParticles(blaze, level);
    }

    // ==================== OVERSTRESS ====================

    /**
     * Called when a blaze reaches OVERSTRESS state.
     * The blaze dies from hypothermia (completely extinguished).
     */
    public static void onOverstress(Blaze blaze) {
        if (blaze.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) blaze.level();

        // Play death sound
        level.playSound(
                null,
                blaze.getX(),
                blaze.getY(),
                blaze.getZ(),
                SoundEvents.BLAZE_DEATH,
                SoundSource.HOSTILE,
                1.0f,
                0.5f  // Low pitch for dramatic effect
        );

        // Large burst of steam/smoke
        level.sendParticles(
                ParticleTypes.CLOUD,
                blaze.getX(),
                blaze.getY() + 0.5,
                blaze.getZ(),
                30,
                0.5, 0.5, 0.5,
                0.1
        );

        // Kill the blaze
        blaze.kill(level);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Suppresses blaze attacks by clearing their target.
     * Called during PANICKED state if configured.
     */
    private static void suppressAttacks(Blaze blaze) {
        // Clear attack target to stop fireballs
        if (blaze.getTarget() != null) {
            blaze.setTarget(null);
        }
    }

    /**
     * Spawns steam/smoke particles around the blaze.
     * Called during PANICKED state ticks.
     */
    private static void spawnPanicParticles(Blaze blaze) {
        if (blaze.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) blaze.level();

        // Spawn occasional steam/smoke particles to indicate cooling
        if (RANDOM.nextFloat() < 0.4f) {  // 40% chance per tick
            double x = blaze.getX() + (RANDOM.nextDouble() - 0.5) * 0.8;
            double y = blaze.getY() + RANDOM.nextDouble() * 1.0;
            double z = blaze.getZ() + (RANDOM.nextDouble() - 0.5) * 0.8;

            // Mix of smoke and cloud (steam) particles
            if (RANDOM.nextBoolean()) {
                level.sendParticles(
                        ParticleTypes.SMOKE,
                        x, y, z,
                        1,
                        0.05, 0.1, 0.05,
                        0.02
                );
            } else {
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        x, y, z,
                        1,
                        0.05, 0.1, 0.05,
                        0.01
                );
            }
        }
    }

    /**
     * Spawns a burst of steam particles when a LEAK event occurs.
     */
    private static void spawnLeakParticles(Blaze blaze, ServerLevel level) {
        // Burst of steam/cloud particles
        level.sendParticles(
                ParticleTypes.CLOUD,
                blaze.getX(),
                blaze.getY() + 0.5,
                blaze.getZ(),
                20,
                0.3, 0.4, 0.3,
                0.05
        );

        // Some smoke particles
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                blaze.getX(),
                blaze.getY() + 0.3,
                blaze.getZ(),
                10,
                0.2, 0.2, 0.2,
                0.02
        );
    }
}
