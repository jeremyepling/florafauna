package net.j40climb.florafauna.common.entity.fear;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.util.AreaScanner;
import net.j40climb.florafauna.setup.FloraFaunaTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Detects fear sources for mobs with mob-specific logic.
 * Each mob type can have different fear sources:
 * - Creepers fear cats and ocelots
 * - Endermen fear armor stands with staring faces (player heads, jack-o-lanterns)
 *   and reflective blocks where they see their reflection
 * - Blazes fear snow golems and cold environments (snow/ice blocks)
 */
public class FearSourceDetector {

    /**
     * Result of fear source detection.
     * Contains either an entity or just a position (for block-based fear sources).
     */
    public record FearSource(@Nullable Entity entity, BlockPos position) {
        public static FearSource fromEntity(Entity entity) {
            return new FearSource(entity, entity.blockPosition());
        }

        public static FearSource fromBlockPos(BlockPos pos) {
            return new FearSource(null, pos);
        }
    }

    /**
     * Detect the nearest fear source for a mob.
     * Dispatches to mob-specific detection methods.
     *
     * @param mob   The mob to check
     * @param range Detection range in blocks
     * @return The nearest fear source, or empty if none found
     */
    public static Optional<FearSource> detectFearSource(Mob mob, double range) {
        if (mob instanceof Creeper creeper) {
            return detectCreeperFearSource(creeper, range);
        }
        if (mob instanceof EnderMan enderman) {
            return detectEndermanFearSource(enderman, range);
        }
        if (mob instanceof Blaze blaze) {
            return detectBlazeFearSource(blaze, range);
        }
        return Optional.empty();
    }

    /**
     * Detect fear sources for creepers: cats and ocelots with line-of-sight.
     *
     * @param creeper The creeper to check
     * @param range   Detection range in blocks
     * @return The nearest cat or ocelot with line-of-sight, or empty if none found
     */
    public static Optional<FearSource> detectCreeperFearSource(Creeper creeper, double range) {
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

        return nearest != null ? Optional.of(FearSource.fromEntity(nearest)) : Optional.empty();
    }

    /**
     * Detect fear sources for endermen:
     * 1. Armor stands with player heads or jack-o-lanterns (being stared at)
     * 2. Reflective blocks within stare distance (seeing their reflection)
     *
     * @param enderman The enderman to check
     * @param range    Detection range in blocks
     * @return The nearest fear source (armor stand or block position)
     */
    public static Optional<FearSource> detectEndermanFearSource(EnderMan enderman, double range) {
        if (enderman.level().isClientSide()) {
            return Optional.empty();
        }

        Level level = enderman.level();
        Vec3 endermanEye = enderman.getEyePosition();
        double stareDistance = Config.endermanStareDistance;

        // Track nearest fear source
        FearSource nearestSource = null;
        double nearestDistSq = Double.MAX_VALUE;

        // Check for armor stands with staring faces within range
        AABB searchBox = enderman.getBoundingBox().inflate(range);
        List<ArmorStand> armorStands = level.getEntitiesOfClass(
                ArmorStand.class,
                searchBox,
                stand -> isStaringArmorStand(stand) && stand.isAlive()
        );

        for (ArmorStand stand : armorStands) {
            double distSq = enderman.distanceToSqr(stand);
            if (distSq < nearestDistSq && hasLineOfSight(enderman, stand)) {
                nearestSource = FearSource.fromEntity(stand);
                nearestDistSq = distSq;
            }
        }

        // Check for reflective blocks within stare distance
        BlockPos endermanPos = enderman.blockPosition();
        int stareDistInt = (int) Math.ceil(stareDistance);

        for (int dx = -stareDistInt; dx <= stareDistInt; dx++) {
            for (int dy = -2; dy <= 2; dy++) {  // Check around eye level
                for (int dz = -stareDistInt; dz <= stareDistInt; dz++) {
                    BlockPos checkPos = endermanPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(FloraFaunaTags.Blocks.REFLECTIVE_BLOCKS)) {
                        Vec3 blockCenter = Vec3.atCenterOf(checkPos);
                        double distSq = endermanEye.distanceToSqr(blockCenter);

                        // Must be within stare distance AND closer than current nearest
                        if (distSq <= stareDistance * stareDistance && distSq < nearestDistSq) {
                            // Check line of sight to block
                            if (hasLineOfSightToBlock(enderman, checkPos)) {
                                nearestDistSq = distSq;
                                nearestSource = FearSource.fromBlockPos(checkPos);
                            }
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(nearestSource);
    }

    /**
     * Detect fear sources for blazes:
     * 1. Snow golems nearby (count >= config minimum)
     * 2. Cold blocks in area (count >= config minimum)
     *
     * By default, BOTH conditions must be met (configurable).
     *
     * @param blaze The blaze to check
     * @param range Detection range in blocks for snow golems
     * @return The nearest snow golem as fear source, or empty if conditions not met
     */
    public static Optional<FearSource> detectBlazeFearSource(Blaze blaze, double range) {
        if (blaze.level().isClientSide()) {
            return Optional.empty();
        }

        Level level = blaze.level();
        BlockPos blazePos = blaze.blockPosition();

        // Count snow golems within range
        AABB searchBox = blaze.getBoundingBox().inflate(range);
        List<SnowGolem> snowGolems = level.getEntitiesOfClass(
                SnowGolem.class,
                searchBox,
                golem -> golem.isAlive()
        );

        int golemCount = snowGolems.size();
        boolean hasEnoughGolems = golemCount >= Config.blazeMinSnowGolems;

        // Count cold blocks in area
        int coldBlockCount = AreaScanner.countBlocksWithTag(
                level,
                blazePos,
                Config.blazeColdScanRadius,
                FloraFaunaTags.Blocks.COLD_BLOCKS
        );
        boolean hasEnoughColdBlocks = coldBlockCount >= Config.blazeMinColdBlocks;

        // Check if fear conditions are met
        boolean fearTriggered;
        if (Config.blazeRequireBothConditions) {
            // Need BOTH golems AND cold blocks
            fearTriggered = hasEnoughGolems && hasEnoughColdBlocks;
        } else {
            // Either condition triggers fear
            fearTriggered = hasEnoughGolems || hasEnoughColdBlocks;
        }

        if (!fearTriggered) {
            return Optional.empty();
        }

        // Find the nearest snow golem as the fear source to flee from
        SnowGolem nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (SnowGolem golem : snowGolems) {
            double distSq = blaze.distanceToSqr(golem);
            if (distSq < nearestDistSq && hasLineOfSight(blaze, golem)) {
                nearest = golem;
                nearestDistSq = distSq;
            }
        }

        // If we have golems, use nearest as fear source
        // If only cold blocks triggered fear (requireBoth=false), use blaze position
        if (nearest != null) {
            return Optional.of(FearSource.fromEntity(nearest));
        } else if (hasEnoughColdBlocks && !Config.blazeRequireBothConditions) {
            // Fear from cold environment - flee from current position
            return Optional.of(FearSource.fromBlockPos(blazePos));
        }

        return Optional.empty();
    }

    /**
     * Check if an armor stand has a staring face (player head or carved pumpkin).
     * Jack-o-lanterns don't count - only the carved pumpkin's empty eye sockets scare endermen.
     *
     * @param stand The armor stand to check
     * @return true if the armor stand has a face that can scare endermen
     */
    public static boolean isStaringArmorStand(ArmorStand stand) {
        ItemStack headSlot = stand.getItemBySlot(EquipmentSlot.HEAD);

        if (headSlot.isEmpty()) {
            return false;
        }

        // Check for player heads (including skull items)
        if (headSlot.is(Items.PLAYER_HEAD)) {
            return true;
        }

        // Check for carved pumpkin (NOT jack-o-lantern - the light ruins the effect)
        if (headSlot.is(Items.CARVED_PUMPKIN)) {
            return true;
        }

        return false;
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
     * Uses ray-stepping to check for blocks in the way, skipping transparent blocks (glass).
     *
     * @param mob    The mob looking
     * @param target The target entity
     * @return true if there's a clear line of sight
     */
    public static boolean hasLineOfSight(Mob mob, Entity target) {
        return hasLineOfSightBetween(mob.level(), mob.getEyePosition(), target.getEyePosition());
    }

    /**
     * Check if the mob has line-of-sight to a block position.
     * Uses ray-stepping to check for blocks in the way, skipping transparent blocks (glass).
     *
     * @param mob      The mob looking
     * @param blockPos The target block position
     * @return true if there's a clear line of sight
     */
    public static boolean hasLineOfSightToBlock(Mob mob, BlockPos blockPos) {
        return hasLineOfSightBetween(mob.level(), mob.getEyePosition(), Vec3.atCenterOf(blockPos));
    }

    /**
     * Ray-step between two positions to check for line-of-sight.
     * Skips blocks tagged as FEAR_LOS_TRANSPARENT (glass, etc.).
     *
     * @param level The level to check in
     * @param start Starting position (e.g., mob's eye)
     * @param end   Ending position (e.g., target's eye)
     * @return true if there's a clear line of sight
     */
    private static boolean hasLineOfSightBetween(Level level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();

        if (distance < 0.01) {
            return true;  // Same position
        }

        direction = direction.normalize();
        double stepSize = 0.5;
        BlockPos lastChecked = null;

        for (double traveled = 0; traveled < distance; traveled += stepSize) {
            Vec3 currentPos = start.add(direction.scale(traveled));
            BlockPos blockPos = BlockPos.containing(currentPos);

            // Skip if we already checked this block position
            if (blockPos.equals(lastChecked)) {
                continue;
            }
            lastChecked = blockPos;

            BlockState state = level.getBlockState(blockPos);

            // Skip air and transparent blocks (glass)
            if (state.isAir() || state.is(FloraFaunaTags.Blocks.FEAR_LOS_TRANSPARENT)) {
                continue;
            }

            // Check if block occludes vision
            if (state.canOcclude()) {
                return false;
            }
        }

        return true;
    }
}
