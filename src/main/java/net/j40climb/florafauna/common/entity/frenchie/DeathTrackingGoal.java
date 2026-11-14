package net.j40climb.florafauna.common.entity.frenchie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Leads in short legs so even a faraway death can be tracked through loaded terrain. */
public final class DeathTrackingGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable GlobalPos target;
    private int nextPathTick;
    private int stalledTicks;
    private double closestDistance = Double.POSITIVE_INFINITY;

    public DeathTrackingGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean startTracking(ServerPlayer owner) {
        if (!dog.isOwnedBy(owner)) return false;
        var death = owner.getLastDeathLocation();
        if (death.isEmpty()) {
            tell(owner, "no_scent");
            return false;
        }
        if (!death.get().dimension().equals(dog.level().dimension())) {
            tell(owner, "other_dimension");
            return false;
        }
        cancel();
        target = death.get();
        dog.standUp();
        dog.setFrenchieSleeping(false);
        tell(owner, "start");
        return true;
    }

    public boolean active() {
        return target != null;
    }

    public void cancel() {
        target = null;
        dog.getNavigation().stop();
        nextPathTick = 0;
        stalledTicks = 0;
        closestDistance = Double.POSITIVE_INFINITY;
    }

    @Override
    public boolean canUse() {
        return active();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;
        if (!target.dimension().equals(dog.level().dimension())) {
            if (dog.getOwner() instanceof ServerPlayer owner) tell(owner, "other_dimension");
            cancel();
            return;
        }
        if (!(dog.getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || dog.distanceToSqr(owner) > 100) {
            dog.getNavigation().stop();
            nextPathTick = 0;
            return;
        }

        Vec3 destination = Vec3.atBottomCenterOf(target.pos());
        Vec3 remaining = destination.subtract(dog.position());
        double distance = remaining.length();
        if (distance <= 2.5) {
            dog.getLookControl().setLookAt(destination.x, destination.y, destination.z);
            tell(owner, "arrived");
            cancel();
            return;
        }

        if (distance < closestDistance - 0.5) {
            closestDistance = distance;
            stalledTicks = 0;
        } else if (++stalledTicks >= 20 * 15) {
            tell(owner, "blocked");
            cancel();
            return;
        }

        if (--nextPathTick > 0) return;
        nextPathTick = 20;
        if (!dog.getNavigation().isDone()) return;
        Vec3 waypoint = dog.position().add(remaining.scale(Math.min(12.0, distance) / distance));
        dog.getNavigation().moveTo(dog.getNavigation().createPath(BlockPos.containing(waypoint), 1), 1.15);
    }

    public void save(ValueOutput output) {
        if (target != null) output.store("FrenchieDeathTrail", GlobalPos.CODEC, target);
    }

    public void load(ValueInput input) {
        cancel();
        target = input.read("FrenchieDeathTrail", GlobalPos.CODEC).orElse(null);
    }

    private void tell(ServerPlayer owner, String key) {
        owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.tracking." + key, dog.getName()));
    }
}
