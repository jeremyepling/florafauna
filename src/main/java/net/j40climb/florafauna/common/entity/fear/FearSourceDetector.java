package net.j40climb.florafauna.common.entity.fear;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Detects fear sources for mobs with mob-specific logic.
 * Each mob type can have different fear sources:
 * - Creepers fear cats and ocelots
 * - Enderman (future): water, rain, eye contact
 * - Blaze (future): water, snowballs
 */
public class FearSourceDetector {

    /**
     * Detect the nearest fear source for a mob.
     * Dispatches to mob-specific detection methods.
     *
     * @param mob   The mob to check
     * @param range Detection range in blocks
     * @return The nearest fear source entity, or empty if none found
     */
    public static Optional<Entity> detectFearSource(Mob mob, double range) {
        if (mob instanceof Creeper creeper) {
            return detectCreeperFearSource(creeper, range);
        }
        // Future: Add enderman, blaze detection here
        return Optional.empty();
    }

    /**
     * Detect fear sources for creepers: cats and ocelots with line-of-sight.
     *
     * @param creeper The creeper to check
     * @param range   Detection range in blocks
     * @return The nearest cat or ocelot with line-of-sight, or empty if none found
     */
    public static Optional<Entity> detectCreeperFearSource(Creeper creeper, double range) {
        if (creeper.level().isClientSide()) {
            return Optional.empty();
        }

        Vec3 creeperPos = creeper.getEyePosition();
        AABB searchBox = creeper.getBoundingBox().inflate(range);

        // Find all cats and ocelots in range
        List<Entity> potentialSources = creeper.level().getEntities(
                creeper,
                searchBox,
                entity -> isCreeperFearSource(entity) && entity.isAlive()
        );

        // Find the nearest one with line-of-sight
        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Entity source : potentialSources) {
            double distSq = creeper.distanceToSqr(source);
            if (distSq < nearestDistSq && hasLineOfSight(creeper, source)) {
                nearest = source;
                nearestDistSq = distSq;
            }
        }

        return Optional.ofNullable(nearest);
    }

    /**
     * Check if an entity is a fear source for creepers.
     *
     * @param entity The entity to check
     * @return true if this entity can cause fear in creepers
     */
    public static boolean isCreeperFearSource(Entity entity) {
        // Cats (tamed or wild) and ocelots
        EntityType<?> type = entity.getType();
        return type == EntityType.CAT || type == EntityType.OCELOT;
    }

    /**
     * Check if the mob has line-of-sight to the target.
     * Uses ray casting to check for blocks in the way.
     *
     * @param mob    The mob looking
     * @param target The target entity
     * @return true if there's a clear line of sight
     */
    public static boolean hasLineOfSight(Mob mob, Entity target) {
        Vec3 mobEye = mob.getEyePosition();
        Vec3 targetEye = target.getEyePosition();

        // Ray cast from mob's eye to target's eye
        HitResult result = mob.level().clip(new ClipContext(
                mobEye,
                targetEye,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));

        // If we hit nothing or hit past the target, we have line of sight
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Check if the hit point is beyond the target
        double hitDistSq = result.getLocation().distanceToSqr(mobEye);
        double targetDistSq = targetEye.distanceToSqr(mobEye);

        return hitDistSq >= targetDistSq;
    }
}
