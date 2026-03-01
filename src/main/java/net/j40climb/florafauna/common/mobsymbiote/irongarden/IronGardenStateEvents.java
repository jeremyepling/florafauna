package net.j40climb.florafauna.common.mobsymbiote.irongarden;

import net.j40climb.florafauna.Config;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.goals.DepositHarvestGoal;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.goals.HarvestPoppyGoal;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.goals.IronGardenWanderGoal;
import net.j40climb.florafauna.common.mobsymbiote.irongarden.goals.PlantPoppyGoal;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Event handler for the Iron Garden state machine.
 * Manages state transitions and goal injection for gardening Iron Golems.
 */
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class IronGardenStateEvents {

    // Goal priorities (lower = higher priority)
    // Harvest sits below planting by default; PlantPoppyGoal yields when 3+ ferric poppies exist,
    // which lets harvesting become the dominant activity once enough poppies have accumulated.
    private static final int DEPOSIT_PRIORITY = 2;
    private static final int PLANT_PRIORITY = 3;
    private static final int HARVEST_PRIORITY = 4;
    private static final int WANDER_PRIORITY = 5;

    /**
     * Main tick handler for the iron garden system.
     * Runs after entity tick to update garden state.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        // Only process on server
        if (entity.level().isClientSide()) {
            return;
        }

        // Only process Iron Golems
        if (!(entity instanceof IronGolem golem)) {
            return;
        }

        // Check tick interval (skip most ticks for performance)
        long currentTick = entity.level().getGameTime();
        if (currentTick % Config.ironGardenCheckIntervalTicks != 0) {
            return;
        }

        // Update iron garden state machine
        updateIronGardenState(golem, currentTick);
    }

    /**
     * Combat detection - when an Iron Golem takes damage.
     * Breaks calmness and returns to BONDED_NOT_CALM state.
     * Note: Only taking damage breaks calm, not attacking.
     */
    @SubscribeEvent
    public static void onGolemDamaged(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();

        // Only process Iron Golems on server
        if (entity.level().isClientSide() || !(entity instanceof IronGolem golem)) {
            return;
        }

        // Only process if bonded and eligible
        if (!IronGardenHelper.isEligible(golem)) {
            return;
        }

        // Record combat (this will break calmness if in calm state)
        long currentTick = entity.level().getGameTime();
        IronGardenHelper.recordCombat(golem, currentTick);
    }

    /**
     * Updates the iron garden state machine for a golem.
     *
     * @param golem       The golem to update
     * @param currentTick Current game tick
     */
    private static void updateIronGardenState(IronGolem golem, long currentTick) {
        IronGardenData data = IronGardenHelper.getData(golem);
        IronGardenState currentState = data.ironGardenState();

        // Check if golem is eligible (has MobSymbiote)
        boolean eligible = IronGardenHelper.isEligible(golem);

        switch (currentState) {
            case UNBONDED -> handleUnbonded(golem, eligible, currentTick);
            case BONDED_NOT_CALM -> handleBondedNotCalm(golem, eligible, currentTick);
            case CALM, CALM_PLANTING, CALM_HARVESTING -> handleCalm(golem, eligible, currentTick);
        }

        // Convert nearby vanilla poppies to ferric poppies when calm
        if (data.ironGardenState().isCalm()) {
            convertNearbyPoppies(golem);
        }
    }

    /**
     * UNBONDED state: No MobSymbiote attached.
     * Exit: MobSymbiote applied → CALM (immediately calm)
     */
    private static void handleUnbonded(IronGolem golem, boolean eligible, long currentTick) {
        if (eligible) {
            // MobSymbiote was just applied - transition directly to calm
            IronGardenHelper.transitionState(golem, IronGardenState.CALM, currentTick);
            ensureGardeningGoals(golem);
            // Set garden center to current position
            IronGardenData data = IronGardenHelper.getData(golem);
            if (!data.hasGardenCenter()) {
                IronGardenHelper.setData(golem, data.withGardenCenter(golem.blockPosition()));
            }
        }
    }

    /**
     * BONDED_NOT_CALM state: Bonded but recently in combat.
     * Exit (up): Calm period elapsed → CALM
     * Exit (down): MobSymbiote removed → UNBONDED
     */
    private static void handleBondedNotCalm(IronGolem golem, boolean eligible, long currentTick) {
        if (!eligible) {
            // MobSymbiote was removed - return to unbonded
            IronGardenHelper.transitionState(golem, IronGardenState.UNBONDED, currentTick);
            removeGardeningGoals(golem);
            return;
        }

        // Check if calm period has elapsed
        if (IronGardenHelper.hasBeenCalmLongEnough(golem, currentTick)) {
            // Transition to calm state
            IronGardenHelper.transitionState(golem, IronGardenState.CALM, currentTick);
            ensureGardeningGoals(golem);
            // Set garden center to current position if not already set
            IronGardenData data = IronGardenHelper.getData(golem);
            if (!data.hasGardenCenter()) {
                IronGardenHelper.setData(golem, data.withGardenCenter(golem.blockPosition()));
            }
        }
    }

    /**
     * CALM state: Actively gardening - planting and harvesting ferric poppies.
     * Exit (down): Combat detected → BONDED_NOT_CALM (via combat event)
     * Exit (down): MobSymbiote removed → UNBONDED
     */
    private static void handleCalm(IronGolem golem, boolean eligible, long currentTick) {
        if (!eligible) {
            // MobSymbiote was removed - return to unbonded
            IronGardenHelper.transitionState(golem, IronGardenState.UNBONDED, currentTick);
            removeGardeningGoals(golem);
            return;
        }

        // Ensure gardening goals are active
        ensureGardeningGoals(golem);
    }

    /**
     * Ensures the golem has all gardening goals injected.
     */
    private static void ensureGardeningGoals(IronGolem golem) {
        boolean hasWanderGoal = false;
        boolean hasPlantGoal = false;
        boolean hasHarvestGoal = false;
        boolean hasDepositGoal = false;

        // Check existing goals
        for (WrappedGoal wrappedGoal : golem.goalSelector.getAvailableGoals()) {
            if (wrappedGoal.getGoal() instanceof IronGardenWanderGoal) hasWanderGoal = true;
            if (wrappedGoal.getGoal() instanceof PlantPoppyGoal) hasPlantGoal = true;
            if (wrappedGoal.getGoal() instanceof HarvestPoppyGoal) hasHarvestGoal = true;
            if (wrappedGoal.getGoal() instanceof DepositHarvestGoal) hasDepositGoal = true;
        }

        // Add missing goals
        if (!hasWanderGoal) {
            golem.goalSelector.addGoal(WANDER_PRIORITY, new IronGardenWanderGoal(golem));
        }
        if (!hasPlantGoal) {
            golem.goalSelector.addGoal(PLANT_PRIORITY, new PlantPoppyGoal(golem));
        }
        if (!hasHarvestGoal) {
            golem.goalSelector.addGoal(HARVEST_PRIORITY, new HarvestPoppyGoal(golem));
        }
        if (!hasDepositGoal) {
            golem.goalSelector.addGoal(DEPOSIT_PRIORITY, new DepositHarvestGoal(golem));
        }
    }

    /**
     * Removes all gardening goals from the golem.
     */
    private static void removeGardeningGoals(IronGolem golem) {
        golem.goalSelector.getAvailableGoals().removeIf(wrappedGoal ->
                wrappedGoal.getGoal() instanceof IronGardenWanderGoal ||
                wrappedGoal.getGoal() instanceof PlantPoppyGoal ||
                wrappedGoal.getGoal() instanceof HarvestPoppyGoal ||
                wrappedGoal.getGoal() instanceof DepositHarvestGoal
        );
    }

    /**
     * Converts vanilla poppies near a calm golem to ferric poppies.
     * Uses {@code Config.ferricPoppyConversionChance} for probability roll.
     */
    private static void convertNearbyPoppies(IronGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos golemPos = golem.blockPosition();
        int range = (int) Config.ferricPoppyGolemRange;

        // Search for vanilla poppies within range
        for (int x = -range; x <= range; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = golemPos.offset(x, y, z);

                    // Check if it's a vanilla poppy
                    if (!level.getBlockState(checkPos).is(Blocks.POPPY)) {
                        continue;
                    }

                    // Roll for conversion
                    if (level.random.nextInt(Config.ferricPoppyConversionChance) != 0) {
                        continue;
                    }

                    // Convert to ferric poppy
                    level.setBlock(checkPos, FloraFaunaRegistry.FERRIC_POPPY.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
