package net.j40climb.florafauna.common.entity.fear.creeper;

import net.j40climb.florafauna.Config;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Creeper-specific fear behavior handlers.
 * Handles fuse suppression, gunpowder drops, particles, and explosion.
 */
public final class CreeperFearHandler {

    private static final Random RANDOM = new Random();

    private CreeperFearHandler() {
        // Utility class
    }

    // ==================== STATE ENTRY HANDLERS ====================

    /**
     * Called when a creeper enters the PANICKED state.
     * Starts visual/audio cues and suppresses fuse.
     */
    public static void onEnterPanicked(Creeper creeper) {
        suppressFuse(creeper);
    }

    // ==================== TICK HANDLERS ====================

    /**
     * Called each fear update tick while in PANICKED state.
     * Keeps the fuse suppressed and spawns panic particles.
     */
    public static void onTickPanicked(Creeper creeper) {
        suppressFuse(creeper);
        spawnPanicParticles(creeper);
    }

    // ==================== LEAK EVENT ====================

    /**
     * Called when a creeper triggers a LEAK event.
     * Drops gunpowder and plays puff sound.
     */
    public static void onLeakEvent(Creeper creeper) {
        if (creeper.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) creeper.level();

        // Calculate drop amount
        int dropCount = Config.gunpowderDropMin +
                RANDOM.nextInt(Config.gunpowderDropMax - Config.gunpowderDropMin + 1);

        // Spawn gunpowder items
        Vec3 pos = creeper.position();
        for (int i = 0; i < dropCount; i++) {
            // Add slight random offset and velocity for visual spread
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            double velocityY = 0.2 + RANDOM.nextDouble() * 0.2;
            double velocityX = (RANDOM.nextDouble() - 0.5) * 0.1;
            double velocityZ = (RANDOM.nextDouble() - 0.5) * 0.1;

            ItemStack gunpowder = new ItemStack(Items.GUNPOWDER, 1);
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.x + offsetX,
                    pos.y + 0.5,
                    pos.z + offsetZ,
                    gunpowder
            );
            itemEntity.setDeltaMovement(velocityX, velocityY, velocityZ);
            level.addFreshEntity(itemEntity);
        }

        // Play puff sound
        level.playSound(
                null,
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                SoundEvents.CREEPER_HURT,
                SoundSource.HOSTILE,
                1.0f,
                1.5f  // Higher pitch for "puff" effect
        );

        // Spawn burst of particles
        spawnLeakParticles(creeper, level);
    }

    // ==================== OVERSTRESS ====================

    /**
     * Called when a creeper reaches OVERSTRESS state.
     * Triggers the creeper explosion.
     */
    public static void onOverstress(Creeper creeper) {
        if (creeper.level().isClientSide()) {
            return;
        }

        // Cause the creeper to explode
        creeper.ignite();
        // Force immediate explosion by maxing out the swell
        creeper.setSwellDir(1);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Suppresses the creeper's fuse by resetting the swell direction.
     * This prevents the creeper from exploding while scared.
     */
    private static void suppressFuse(Creeper creeper) {
        // Reset swell direction to stop/prevent explosion
        if (creeper.getSwellDir() > 0) {
            creeper.setSwellDir(-1);
        }
    }

    /**
     * Spawns gunpowder puff particles around the creeper.
     * Called during PANICKED state ticks.
     */
    private static void spawnPanicParticles(Creeper creeper) {
        if (creeper.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) creeper.level();

        // Spawn occasional smoke particles to indicate stress
        if (RANDOM.nextFloat() < 0.3f) {  // 30% chance per tick
            double x = creeper.getX() + (RANDOM.nextDouble() - 0.5) * 0.8;
            double y = creeper.getY() + 0.5 + RANDOM.nextDouble() * 0.5;
            double z = creeper.getZ() + (RANDOM.nextDouble() - 0.5) * 0.8;

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    1,  // count
                    0.05, 0.1, 0.05,  // spread
                    0.02  // speed
            );
        }
    }

    /**
     * Spawns a burst of particles when a LEAK event occurs.
     */
    private static void spawnLeakParticles(Creeper creeper, ServerLevel level) {
        // Burst of smoke particles
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                creeper.getX(),
                creeper.getY() + 0.8,
                creeper.getZ(),
                15,  // count
                0.3, 0.3, 0.3,  // spread
                0.05  // speed
        );

        // Some poof particles
        level.sendParticles(
                ParticleTypes.POOF,
                creeper.getX(),
                creeper.getY() + 0.5,
                creeper.getZ(),
                8,
                0.2, 0.2, 0.2,
                0.02
        );
    }
}
