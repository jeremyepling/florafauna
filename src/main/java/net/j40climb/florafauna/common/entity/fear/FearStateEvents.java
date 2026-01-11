package net.j40climb.florafauna.common.entity.fear;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.fear.FearSourceDetector.FearSource;
import net.j40climb.florafauna.common.entity.fear.blaze.BlazeFearHandler;
import net.j40climb.florafauna.common.entity.fear.creeper.CreeperFearHandler;
import net.j40climb.florafauna.common.entity.fear.enderman.EndermanFearHandler;
import net.j40climb.florafauna.common.entity.fear.goals.FearAvoidanceGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Optional;

/**
 * Event handler for the fear/stress state machine.
 * Processes fear state transitions for eligible mobs.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class FearStateEvents {

    /**
     * Main tick handler for the fear system.
     * Runs after entity tick to update fear state.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        // Only process on server
        if (entity.level().isClientSide()) {
            return;
        }

        // Only process mobs
        if (!(entity instanceof Mob mob)) {
            return;
        }

        // Check tick interval (skip most ticks for performance)
        long currentTick = entity.level().getGameTime();
        if (currentTick % Config.fearCheckIntervalTicks != 0) {
            return;
        }

        // Only process mobs that can experience fear
        if (!FearHelper.canExperienceFear(mob)) {
            return;
        }

        // Update fear state machine
        updateFearState(mob, currentTick);
    }

    /**
     * Updates the fear state machine for a mob.
     *
     * @param mob         The mob to update
     * @param currentTick Current game tick
     */
    private static void updateFearState(Mob mob, long currentTick) {
        FearState currentState = FearHelper.getFearState(mob);
        Optional<FearSource> fearSource = FearSourceDetector.detectFearSource(mob, Config.fearSourceDetectionRange);

        switch (currentState) {
            case CALM -> handleCalm(mob, fearSource, currentTick);
            case PANICKED -> handlePanicked(mob, fearSource, currentTick);
            case LEAK -> handleLeak(mob, currentTick);
            case EXHAUSTED -> handleExhausted(mob, currentTick);
            case OVERSTRESS -> handleOverstress(mob);
        }
    }

    /**
     * Priority for the fear avoidance goal.
     * Set to 1 (high priority) to override most other movement goals.
     */
    private static final int FEAR_AVOIDANCE_PRIORITY = 1;

    /**
     * CALM state: Normal AI, no fear output.
     * Exit: Fear source detected → PANICKED
     */
    private static void handleCalm(Mob mob, Optional<FearSource> fearSource, long currentTick) {
        if (fearSource.isPresent()) {
            // Transition directly to PANICKED
            FearHelper.setFearState(mob, FearState.PANICKED, currentTick);
            FearHelper.setFearSourcePos(mob, fearSource.get().position());

            // Ensure fear avoidance goal is injected
            ensureFearAvoidanceGoal(mob);

            // Mob-specific handling
            if (mob instanceof Creeper creeper) {
                CreeperFearHandler.onEnterPanicked(creeper);
            } else if (mob instanceof EnderMan enderman) {
                EndermanFearHandler.onEnterPanicked(enderman);
            } else if (mob instanceof Blaze blaze) {
                BlazeFearHandler.onEnterPanicked(blaze);
            }
        }
    }

    /**
     * Ensures the mob has a FearAvoidanceGoal injected.
     * The goal will only be active when the mob is scared.
     */
    private static void ensureFearAvoidanceGoal(Mob mob) {
        // Check if goal already exists
        for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof FearAvoidanceGoal) {
                return;  // Already has the goal
            }
        }

        // Add the goal
        mob.goalSelector.addGoal(FEAR_AVOIDANCE_PRIORITY, new FearAvoidanceGoal(mob));
    }

    /**
     * PANICKED state: Mob shakes, hissing sounds, particles.
     * Exit (up): Duration threshold → LEAK
     * Exit (down): Fear source removed → CALM (with leak count reset)
     */
    private static void handlePanicked(Mob mob, Optional<FearSource> fearSource, long currentTick) {
        if (fearSource.isEmpty()) {
            // Fear source gone - return to CALM and reset leak count
            FearHelper.resetToCalm(mob, currentTick, true);
            return;
        }

        // Update fear source position
        FearHelper.setFearSourcePos(mob, fearSource.get().position());

        // Mob-specific handling
        if (mob instanceof Creeper creeper) {
            CreeperFearHandler.onTickPanicked(creeper);
        } else if (mob instanceof EnderMan enderman) {
            EndermanFearHandler.onTickPanicked(enderman);
        } else if (mob instanceof Blaze blaze) {
            BlazeFearHandler.onTickPanicked(blaze);
        }

        // Check if we should trigger LEAK
        long ticksInState = FearHelper.getTicksInState(mob, currentTick);
        if (ticksInState >= Config.panicDurationForLeak) {
            // Check for OVERSTRESS condition
            int leakCount = FearHelper.getLeakCount(mob);
            if (leakCount >= Config.maxLeaksBeforeOverstress - 1) {
                // Too many consecutive leaks - OVERSTRESS!
                FearHelper.setFearState(mob, FearState.OVERSTRESS, currentTick);
                if (mob instanceof Creeper creeper) {
                    CreeperFearHandler.onOverstress(creeper);
                } else if (mob instanceof EnderMan enderman) {
                    EndermanFearHandler.onOverstress(enderman);
                } else if (mob instanceof Blaze blaze) {
                    BlazeFearHandler.onOverstress(blaze);
                }
            } else {
                // Normal leak event
                FearHelper.setFearState(mob, FearState.LEAK, currentTick);
            }
        }
    }

    /**
     * LEAK state: Output event - drops items.
     * Exit: Immediate → EXHAUSTED
     */
    private static void handleLeak(Mob mob, long currentTick) {
        // Execute the leak event
        if (mob instanceof Creeper creeper) {
            CreeperFearHandler.onLeakEvent(creeper);
        } else if (mob instanceof EnderMan enderman) {
            EndermanFearHandler.onLeakEvent(enderman);
        } else if (mob instanceof Blaze blaze) {
            BlazeFearHandler.onLeakEvent(blaze);
        }

        // Increment leak count
        FearHelper.incrementLeakCount(mob);

        // Immediately transition to EXHAUSTED
        FearHelper.setFearState(mob, FearState.EXHAUSTED, currentTick);
    }

    /**
     * EXHAUSTED state: Cooldown - temporarily immune to fear.
     * Exit: Cooldown expires → CALM (without resetting leak count)
     */
    private static void handleExhausted(Mob mob, long currentTick) {
        long ticksInState = FearHelper.getTicksInState(mob, currentTick);
        if (ticksInState >= Config.exhaustedCooldownTicks) {
            // Cooldown complete - return to CALM but preserve leak count
            FearHelper.setFearState(mob, FearState.CALM, currentTick);
            FearHelper.clearFearSourcePos(mob);
        }
    }

    /**
     * OVERSTRESS state: Failure - mob dies.
     * Terminal state (mob will be dead).
     */
    private static void handleOverstress(Mob mob) {
        // Mob-specific handling (explosion for creepers, death for endermen, hypothermia for blazes)
        if (mob instanceof Creeper creeper) {
            CreeperFearHandler.onOverstress(creeper);
        } else if (mob instanceof EnderMan enderman) {
            EndermanFearHandler.onOverstress(enderman);
        } else if (mob instanceof Blaze blaze) {
            BlazeFearHandler.onOverstress(blaze);
        }
        // Note: The mob will be dead after this, no state transition needed
    }
}
