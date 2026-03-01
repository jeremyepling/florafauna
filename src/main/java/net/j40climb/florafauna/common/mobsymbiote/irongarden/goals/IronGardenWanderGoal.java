package net.j40climb.florafauna.common.mobsymbiote.irongarden.goals;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenActivity;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenData;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.IronGardenHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Goal for calm Iron Golems to wander peacefully within their garden area.
 * Lower priority than planting/harvesting goals.
 */
public class IronGardenWanderGoal extends Goal {
    private final IronGolem golem;
    private Vec3 targetPos;
    private int cooldown;
    private static final int COOLDOWN_TICKS = 40; // 2 seconds between wanders

    public IronGardenWanderGoal(IronGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only when calm
        IronGardenData data = IronGardenHelper.getData(golem);
        if (!data.ironGardenState().isCalm()) {
            return false;
        }

        // Cooldown check
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Find a random position within garden radius
        Optional<BlockPos> center = data.getGardenCenter();
        if (center.isEmpty()) {
            return false;
        }

        Vec3 randomPos = DefaultRandomPos.getPosTowards(golem, Config.ironGardenWanderRadius, 7,
                Vec3.atBottomCenterOf(center.get()), Math.PI / 2);

        if (randomPos == null) {
            return false;
        }

        // Check if within garden bounds
        double distSq = randomPos.distanceToSqr(Vec3.atBottomCenterOf(center.get()));
        if (distSq > Config.ironGardenWanderRadius * Config.ironGardenWanderRadius) {
            return false;
        }

        targetPos = randomPos;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if no longer calm or navigation complete
        IronGardenData data = IronGardenHelper.getData(golem);
        if (!data.ironGardenState().isCalm()) {
            return false;
        }
        return !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        golem.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 0.6);
        IronGardenHelper.setActivity(golem, IronGardenActivity.WANDERING);
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        cooldown = COOLDOWN_TICKS;
        targetPos = null;
        IronGardenHelper.setActivity(golem, IronGardenActivity.IDLE);
    }
}
