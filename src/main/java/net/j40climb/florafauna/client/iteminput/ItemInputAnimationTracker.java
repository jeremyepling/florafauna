package net.j40climb.florafauna.client.iteminput;

import net.j40climb.florafauna.FloraFauna;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and renders item absorption animations on the client side.
 * When an item is claimed by an Item Input block, this tracker:
 * - Interpolates the item's visual position toward the block
 * - Applies a shrinking/sinking effect
 * - Removes the animation when complete
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public class ItemInputAnimationTracker {

    /**
     * Data for an active animation.
     */
    public record AnimationData(
            BlockPos targetPos,
            Vec3 startPos,
            long startTick,
            int durationTicks
    ) {
        public float getProgress(long currentTick) {
            float elapsed = currentTick - startTick;
            return Math.min(1.0f, elapsed / durationTicks);
        }

        public boolean isComplete(long currentTick) {
            return getProgress(currentTick) >= 1.0f;
        }

        /**
         * Gets the interpolated position for the given progress.
         */
        public Vec3 getInterpolatedPosition(float progress) {
            Vec3 target = Vec3.atCenterOf(targetPos);
            // Ease-in curve for acceleration effect
            float easedProgress = progress * progress;
            return startPos.lerp(target, easedProgress);
        }

        /**
         * Gets the scale factor for the shrinking effect.
         */
        public float getScale(float progress) {
            // Shrink from 1.0 to 0.3 as it approaches the block
            return 1.0f - (progress * 0.7f);
        }
    }

    // Map of entity ID to animation data
    private static final Map<Integer, AnimationData> activeAnimations = new ConcurrentHashMap<>();

    /**
     * Starts tracking an animation for an item entity.
     * Called from the network payload handler.
     */
    public static void startAnimation(int entityId, BlockPos targetPos, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof ItemEntity itemEntity) {
            Vec3 startPos = itemEntity.position();
            long currentTick = mc.level.getGameTime();

            activeAnimations.put(entityId, new AnimationData(
                    targetPos,
                    startPos,
                    currentTick,
                    durationTicks
            ));
        }
    }

    /**
     * Gets the animation data for an entity, if any.
     */
    public static AnimationData getAnimation(int entityId) {
        return activeAnimations.get(entityId);
    }

    /**
     * Checks if an entity has an active animation.
     */
    public static boolean hasAnimation(int entityId) {
        return activeAnimations.containsKey(entityId);
    }

    /**
     * Updates animations and spawns particles on level tick.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long currentTick = mc.level.getGameTime();

        Iterator<Map.Entry<Integer, AnimationData>> iterator = activeAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AnimationData> entry = iterator.next();
            int entityId = entry.getKey();
            AnimationData data = entry.getValue();

            if (data.isComplete(currentTick)) {
                // Spawn burst of particles at target when complete
                Vec3 target = Vec3.atCenterOf(data.targetPos());
                for (int i = 0; i < 8; i++) {
                    double offsetX = (mc.level.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (mc.level.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (mc.level.random.nextDouble() - 0.5) * 0.5;
                    mc.level.addParticle(ParticleTypes.PORTAL,
                            target.x + offsetX, target.y + offsetY, target.z + offsetZ,
                            0, 0.1, 0);
                }
                iterator.remove();
            } else {
                // Spawn trail particles during animation
                Entity entity = mc.level.getEntity(entityId);
                if (entity instanceof ItemEntity itemEntity) {
                    float progress = data.getProgress(currentTick);
                    Vec3 pos = data.getInterpolatedPosition(progress);

                    // Spawn particle along the path
                    if (currentTick % 2 == 0) { // Every other tick
                        mc.level.addParticle(ParticleTypes.ENCHANT,
                                pos.x, pos.y + 0.25, pos.z,
                                (mc.level.random.nextDouble() - 0.5) * 0.1,
                                0.05,
                                (mc.level.random.nextDouble() - 0.5) * 0.1);
                    }
                }
            }
        }
    }

    /**
     * Clears all animations (e.g., on world unload).
     */
    public static void clearAll() {
        activeAnimations.clear();
    }
}
