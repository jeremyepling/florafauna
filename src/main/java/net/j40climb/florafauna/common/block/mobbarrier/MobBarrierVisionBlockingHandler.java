package net.j40climb.florafauna.common.block.mobbarrier;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Handles vision blocking for MobBarrier blocks.
 * When a mob tries to target an entity, checks if there's a MobBarrier
 * with blockVision=true between them that would block this mob's vision.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class MobBarrierVisionBlockingHandler {

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        // Only handle Mob entities (not all LivingEntities)
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        LivingEntity newTarget = event.getNewAboutToBeSetTarget();

        // No target being set, nothing to block
        if (newTarget == null) {
            return;
        }

        // Only run on server
        if (mob.level().isClientSide()) {
            return;
        }

        // Check if there's a vision-blocking barrier between mob and target
        if (isVisionBlockedByBarrier(mob, newTarget)) {
            event.setCanceled(true);
        }
    }

    /**
     * Checks if vision between mob and target is blocked by a MobBarrier
     * with blockVision enabled for this mob type.
     */
    private static boolean isVisionBlockedByBarrier(Mob mob, LivingEntity target) {
        Level level = mob.level();
        Vec3 mobEye = mob.getEyePosition();
        Vec3 targetEye = target.getEyePosition();

        // Ray cast from mob's eye to target's eye, looking for blocks
        BlockHitResult result = level.clip(new ClipContext(
                mobEye,
                targetEye,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));

        // If we hit nothing or the hit was past the target, no blocking
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }

        // Check each block along the ray for MobBarrier with blockVision
        // We need to check all barriers, not just the first hit
        return checkRayForVisionBlockingBarrier(level, mobEye, targetEye, mob);
    }

    /**
     * Walks along the ray from start to end, checking for MobBarrier blocks
     * that would block this mob's vision.
     */
    private static boolean checkRayForVisionBlockingBarrier(Level level, Vec3 start, Vec3 end, Mob mob) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        direction = direction.normalize();

        // Step along the ray checking each block position
        // Use small step size to ensure we don't skip blocks
        double stepSize = 0.5;
        double traveled = 0;

        while (traveled < distance) {
            Vec3 currentPos = start.add(direction.scale(traveled));
            BlockPos blockPos = BlockPos.containing(currentPos);

            BlockState state = level.getBlockState(blockPos);
            if (state.getBlock() instanceof MobBarrierBlock) {
                // Found a MobBarrier, check if it should block this mob's vision
                if (level.getBlockEntity(blockPos) instanceof MobBarrierBlockEntity blockEntity) {
                    MobBarrierConfig config = blockEntity.getConfig();
                    if (config.blockVision() && config.shouldBlockEntity(mob)) {
                        return true;
                    }
                }
            }

            traveled += stepSize;
        }

        return false;
    }
}
