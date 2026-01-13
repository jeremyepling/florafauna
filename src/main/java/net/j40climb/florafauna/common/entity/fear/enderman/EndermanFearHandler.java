package net.j40climb.florafauna.common.entity.fear.enderman;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.entity.fear.FearSourceDetector.FearSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * Enderman-specific fear behavior handlers.
 * Handles teleport suppression, ender pearl drops, particles, and death.
 *
 * Endermen become scared when:
 * 1. Armor stands with player heads or carved pumpkins nearby (being stared at)
 * 2. Reflective blocks within stare distance (seeing their reflection)
 */
public final class EndermanFearHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndermanFearHandler.class);
    private static final Random RANDOM = new Random();

    // Cached reference to EnderMan.DATA_CREEPY for setting creepy state directly
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Boolean> DATA_CREEPY = getDataCreepyAccessor();

    private EndermanFearHandler() {
        // Utility class
    }

    /**
     * Gets the DATA_CREEPY field from EnderMan via reflection.
     * This allows us to trigger the creepy animation without needing a target entity.
     */
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> getDataCreepyAccessor() {
        try {
            Field field = EnderMan.class.getDeclaredField("DATA_CREEPY");
            field.setAccessible(true);
            return (EntityDataAccessor<Boolean>) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to access EnderMan.DATA_CREEPY field", e);
            return null;
        }
    }

    /**
     * Sets the creepy state on an enderman directly.
     * This triggers the mouth-open, jittering animation.
     */
    private static void setCreepyState(EnderMan enderman, boolean creepy) {
        if (DATA_CREEPY != null) {
            enderman.getEntityData().set(DATA_CREEPY, creepy);
        }
    }

    // ==================== STATE ENTRY HANDLERS ====================

    /**
     * Called when an enderman enters the PANICKED state.
     * Enables the creepy animation (mouth open, jittering) and plays stare sound.
     *
     * @param enderman   The enderman entering panic
     * @param fearSource The detected fear source
     */
    public static void onEnterPanicked(EnderMan enderman, FearSource fearSource) {
        if (enderman.level().isClientSide()) {
            return;
        }

        // Enable creepy state directly (mouth open, jittering animation)
        // Works for both entity-based fears (armor stands) and block-based fears (reflective blocks)
        setCreepyState(enderman, true);

        // Play stare sound when first becoming scared
        enderman.level().playSound(
                null,
                enderman.getX(),
                enderman.getY(),
                enderman.getZ(),
                SoundEvents.ENDERMAN_STARE,
                SoundSource.HOSTILE,
                1.0f,
                1.0f
        );
    }

    /**
     * Called when an enderman exits the PANICKED state.
     * Disables the creepy animation.
     */
    public static void onExitPanicked(EnderMan enderman) {
        if (enderman.level().isClientSide()) {
            return;
        }

        // Disable creepy state
        setCreepyState(enderman, false);
    }

    // ==================== TICK HANDLERS ====================

    /**
     * Called each fear update tick while in PANICKED state.
     * Spawns panic particles and prevents teleportation.
     */
    public static void onTickPanicked(EnderMan enderman) {
        spawnPanicParticles(enderman);
    }

    // ==================== LEAK EVENT ====================

    /**
     * Called when an enderman triggers a LEAK event.
     * Drops ender pearls and plays teleport sound.
     */
    public static void onLeakEvent(EnderMan enderman) {
        if (enderman.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) enderman.level();

        // Calculate drop amount
        int dropCount = Config.enderPearlDropMin +
                RANDOM.nextInt(Config.enderPearlDropMax - Config.enderPearlDropMin + 1);

        // Spawn ender pearl items
        Vec3 pos = enderman.position();
        for (int i = 0; i < dropCount; i++) {
            // Add slight random offset and velocity for visual spread
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            double velocityY = 0.2 + RANDOM.nextDouble() * 0.2;
            double velocityX = (RANDOM.nextDouble() - 0.5) * 0.1;
            double velocityZ = (RANDOM.nextDouble() - 0.5) * 0.1;

            ItemStack pearl = new ItemStack(Items.ENDER_PEARL, 1);
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.x + offsetX,
                    pos.y + 1.5,  // Higher spawn point for tall enderman
                    pos.z + offsetZ,
                    pearl
            );
            itemEntity.setDeltaMovement(velocityX, velocityY, velocityZ);
            level.addFreshEntity(itemEntity);
        }

        // Play teleport sound (without actually teleporting)
        level.playSound(
                null,
                enderman.getX(),
                enderman.getY(),
                enderman.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1.0f,
                0.8f  // Lower pitch for stressed effect
        );

        // Spawn burst of particles
        spawnLeakParticles(enderman, level);
    }

    // ==================== OVERSTRESS ====================

    /**
     * Called when an enderman reaches OVERSTRESS state.
     * The enderman dies from the stress (no explosion like creeper).
     */
    public static void onOverstress(EnderMan enderman) {
        if (enderman.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) enderman.level();

        // Play death scream
        level.playSound(
                null,
                enderman.getX(),
                enderman.getY(),
                enderman.getZ(),
                SoundEvents.ENDERMAN_DEATH,
                SoundSource.HOSTILE,
                1.0f,
                0.5f  // Very low pitch for dramatic effect
        );

        // Large particle burst
        level.sendParticles(
                ParticleTypes.PORTAL,
                enderman.getX(),
                enderman.getY() + 1.5,
                enderman.getZ(),
                50,
                0.5, 1.0, 0.5,
                0.5
        );

        // Kill the enderman
        enderman.kill(level);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Spawns portal particles around the enderman.
     * Called during PANICKED state ticks.
     */
    private static void spawnPanicParticles(EnderMan enderman) {
        if (enderman.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) enderman.level();

        // Spawn occasional portal particles to indicate stress
        if (RANDOM.nextFloat() < 0.3f) {  // 30% chance per tick
            double x = enderman.getX() + (RANDOM.nextDouble() - 0.5) * 0.8;
            double y = enderman.getY() + 0.5 + RANDOM.nextDouble() * 2.0;  // Taller range for enderman
            double z = enderman.getZ() + (RANDOM.nextDouble() - 0.5) * 0.8;

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    x, y, z,
                    2,  // count
                    0.1, 0.2, 0.1,  // spread
                    0.1  // speed
            );
        }
    }

    /**
     * Spawns a burst of particles when a LEAK event occurs.
     */
    private static void spawnLeakParticles(EnderMan enderman, ServerLevel level) {
        // Burst of portal particles
        level.sendParticles(
                ParticleTypes.PORTAL,
                enderman.getX(),
                enderman.getY() + 1.5,
                enderman.getZ(),
                25,  // count
                0.4, 0.8, 0.4,  // spread
                0.3  // speed
        );

        // Some reverse portal particles (going inward)
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                enderman.getX(),
                enderman.getY() + 1.0,
                enderman.getZ(),
                15,
                0.3, 0.5, 0.3,
                0.1
        );
    }
}
