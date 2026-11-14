package net.j40climb.florafauna.common.mobsymbiote.silk;

import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.EnumSet;

final class SpinSilkGoal extends Goal {
    private static final int HANG_TICKS = 100;
    private static final int SPIN_COOLDOWN = 1200;
    private static final String NEXT_SPIN = "florafauna:silk_ready_at";

    private final Spider spider;
    private SilkSpan span;
    private Vec3 hang;
    private BlockPos shelter;
    private long nextSearch;
    private long phaseStarted;
    private Phase phase;
    private enum Phase { APPROACH, HANG, RETREAT, FINISHED }

    SpinSilkGoal(Spider spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean canUse() {
        ServerLevel level = (ServerLevel) spider.level();
        long time = level.getGameTime();
        if (time < nextSearch || time < spider.getPersistentData().getLongOr(NEXT_SPIN, 0) ||
                !MobSymbioteHelper.hasMobSymbiote(spider) || MobSymbioteHelper.isBeingLured(spider)) return false;
        nextSearch = time + 40;
        if (!EventHooks.canEntityGrief(level, spider)) return false;
        BlockPos center = spider.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -1, -5), center.offset(5, 1, 5))) {
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(BlockTags.FENCES)
                    || level.getBlockState(pos.above()).is(BlockTags.FENCES)) continue;
            for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH}) {
                SilkSpan candidate = SilkSpan.emptyGap(level, pos, dir);
                if (candidate == null) continue;
                BlockPos covered = candidate.coveredPost(level);
                if (covered == null) continue;
                Vec3 midpoint = Vec3.atBottomCenterOf(candidate.first())
                        .relative(dir, (candidate.length() - 1) * 0.5)
                        .add(0, 0.78 - spider.getBbHeight(), 0);
                if (!level.noCollision(spider, spider.getBoundingBox().move(midpoint.subtract(spider.position())))) {
                    // One-high posts can put the spider's feet just inside the floor.
                    midpoint = midpoint.add(0, 0.25, 0);
                    if (!level.noCollision(spider, spider.getBoundingBox().move(midpoint.subtract(spider.position())))) continue;
                }
                // Seek a walkable spot beside the covered post; taller fence stacks still allow a floor below.
                for (Direction side : new Direction[]{dir.getClockWise(), dir.getCounterClockWise()}) {
                    BlockPos spot = covered.relative(side);
                    if (level.isEmptyBlock(spot.below())) spot = spot.below();
                    if (!level.isEmptyBlock(spot) || !level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP)) continue;
                    span = candidate;
                    shelter = spot;
                    hang = midpoint;
                    return true;
                }
            }
        }
        return false;
    }

    @Override public void start() {
        phase = Phase.APPROACH;
        phaseStarted = spider.level().getGameTime();
        spider.getNavigation().moveTo(hang.x, hang.y, hang.z, 1.1);
    }

    @Override public boolean canContinueToUse() {
        return phase != Phase.FINISHED && MobSymbioteHelper.hasMobSymbiote(spider) && !MobSymbioteHelper.isBeingLured(spider);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void tick() {
        ServerLevel level = (ServerLevel) spider.level();
        long elapsed = level.getGameTime() - phaseStarted;
        switch (phase) {
            case APPROACH -> {
                if (elapsed > 80 || !EventHooks.canEntityGrief(level, spider)) { phase = Phase.FINISHED; return; }
                if (spider.position().distanceToSqr(hang) > 1.6) return;
                if (span.coveredPost(level) == null ||
                        !level.noCollision(spider, spider.getBoundingBox().move(hang.subtract(spider.position())))) { phase = Phase.FINISHED; return; }
                // Recheck each space immediately before placement: two spiders may have chosen the same posts.
                if (!span.equals(SilkSpan.emptyGap(level, span.first().relative(span.along().getOpposite()), span.along()))) {
                    phase = Phase.FINISHED; return;
                }
                BlockState silk = FloraFaunaRegistry.SILK_LINE.get().defaultBlockState().setValue(SilkLineBlock.AXIS, span.along().getAxis());
                for (int i = 0; i < span.length(); i++) {
                    level.setBlockAndUpdate(span.piece(i), silk.setValue(SilkLineBlock.FIRST, i == 0)
                            .setValue(SilkLineBlock.LAST, i == span.length() - 1));
                }
                spider.getPersistentData().putLong(NEXT_SPIN, level.getGameTime() + SPIN_COOLDOWN);
                spider.getNavigation().stop();
                spider.setData(FloraFaunaRegistry.SILK_HANGING, true);
                phaseStarted = level.getGameTime();
                phase = Phase.HANG;
                holdPosition();
            }
            case HANG -> {
                boolean cut = !SilkLineBlock.matches(level, span.first(), span.along());
                if (cut || elapsed >= HANG_TICKS || !level.noCollision(spider, spider.getBoundingBox().move(hang.subtract(spider.position())))) {
                    spider.setData(FloraFaunaRegistry.SILK_HANGING, false);
                    if (cut) {
                        level.sendParticles(ParticleTypes.POOF, spider.getX(), spider.getY(), spider.getZ(), 6, 0.25, 0.08, 0.25, 0.02);
                        level.playSound(null, spider.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.7F, 1.35F);
                        spider.setDeltaMovement(0, 0.16, 0);
                    }
                    spider.fallDistance = 0;
                    phase = Phase.RETREAT;
                    phaseStarted = level.getGameTime();
                    spider.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, 1);
                } else holdPosition();
            }
            case RETREAT -> {
                if (spider.blockPosition().distSqr(shelter) <= 1 || elapsed > 80) phase = Phase.FINISHED;
            }
            case FINISHED -> {}
        }
    }

    private void holdPosition() {
        spider.setPos(hang);
        spider.setDeltaMovement(Vec3.ZERO);
        spider.fallDistance = 0;
    }

    @Override public void stop() {
        spider.setData(FloraFaunaRegistry.SILK_HANGING, false);
        spider.getNavigation().stop();
        span = null;
        phase = Phase.FINISHED;
    }
}
