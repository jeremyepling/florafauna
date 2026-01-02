package net.j40climb.florafauna.common.entity.frenchie;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrenchieEntity extends TamableAnimal {

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState standToSitAnimationState = new AnimationState();
    public final AnimationState sitToStandAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState standToSleepAnimationState = new AnimationState();
    public final AnimationState sleepToStandAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Long> LAST_SLEEP_CHANGE_TICK =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_SLEEPING =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.BOOLEAN);

    public FrenchieEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, Ingredient.of(Items.GLOW_BERRIES), false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.25d, 18f, 7f));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0D);
    }

    @Override
    public boolean isSwimming() {
        return this.isInWater(); // return true if entity is in water
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // Frenchie can breathe underwater to prevent drowning
    }

    @Override
    public boolean canStartSwimming() {
        return true; // Allow swimming
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES.asItem());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnData) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        this.setVariant(variant);
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, AgeableMob otherParent) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        FrenchieEntity baby = FloraFaunaRegistry.FRENCHIE.get().create(level, EntitySpawnReason.BREEDING);
        assert baby != null;
        baby.setVariant(variant);
        return baby;
    }

    /* VARIANT */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(LAST_POSE_CHANGE_TICK, 0L);
        builder.define(LAST_SLEEP_CHANGE_TICK, 0L);
        builder.define(IS_SLEEPING, false);
    }

    public boolean isFrenchieSleeping() {
        return this.entityData.get(IS_SLEEPING);
    }

    public void setFrenchieSleeping(boolean sleeping) {
        if (this.entityData.get(IS_SLEEPING) != sleeping) {
            this.entityData.set(IS_SLEEPING, sleeping);
            this.resetLastSleepChangeTick(this.level().getGameTime());
        }
    }

    private int getTypeVariant() {
        return this.entityData.get(VARIANT);
    }

    public FrenchieVariant getVariant() {
        return FrenchieVariant.byId(this.getTypeVariant() & 255); // used to compress the size with the 255
    }

    private void setVariant(FrenchieVariant variant) {
        this.entityData.set(VARIANT, variant.getId() & 255);
    }

    /* Interact */

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();
        Item itemForTaming = Items.BONE;
        if(item == itemForTaming && !isTame()) {
            if(this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            } else {
                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                if (!EventHooks.onAnimalTame(this, pPlayer)) {
                    super.tame(pPlayer);
                    this.navigation.recomputePath();
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte)7);
                    toggleSitting();
                }
                return InteractionResult.SUCCESS;
            }
        }
        if(isTame() && pHand == InteractionHand.MAIN_HAND && !isFood(itemstack)) {
            toggleSitting();
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(pPlayer, pHand);
    }
    /* SITTING */
    public boolean isSitting() {
        return this.getPose() == Pose.SITTING;
    }

    public void toggleSitting() {
        if (this.isSitting()) {
            standUp();
        } else {
            sitDown();
        }
    }
    public void sitDown() {
        if (!this.isSitting()) {
            //this.makeSound(SoundEvents.CAMEL_SIT);
            this.setPose(Pose.SITTING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(this.level().getGameTime());
        }
        setOrderedToSit(true);
        setInSittingPose(true);
    }
    public void standUp() {
        if (this.isSitting()) {
            //this.makeSound(SoundEvents.CAMEL_STAND);
            this.setPose(Pose.STANDING);
            this.gameEvent(GameEvent.ENTITY_ACTION);
            this.resetLastPoseChangeTick(this.level().getGameTime());
        }
        setOrderedToSit(false);
        setInSittingPose(false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getTypeVariant());
        output.putString("Pose", this.getPose().name());
        output.putBoolean("IsSleeping", this.isFrenchieSleeping());
        output.putLong("LastPoseChangeTick", this.entityData.get(LAST_POSE_CHANGE_TICK));
        output.putLong("LastSleepChangeTick", this.entityData.get(LAST_SLEEP_CHANGE_TICK));
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Integer variantID = input.getIntOr("Variant", 0);
        this.entityData.set(VARIANT, variantID);
        String poseName = input.getStringOr("Pose", Pose.STANDING.name());
        this.setPose(Pose.valueOf(poseName));
        boolean sleeping = input.getBooleanOr("IsSleeping", false);
        this.setFrenchieSleeping(sleeping);
        long lastPoseChangeTick = input.getLongOr("LastPoseChangeTick", 0L);
        this.entityData.set(LAST_POSE_CHANGE_TICK, lastPoseChangeTick);
        long lastSleepChangeTick = input.getLongOr("LastSleepChangeTick", 0L);
        this.entityData.set(LAST_SLEEP_CHANGE_TICK, lastSleepChangeTick);
    }

    /* Animation */

    @Override
    public void tick() {
        super.tick();

        if(this.level().isClientSide()) {
            // Idle animation
            if (this.idleAnimationTimeout <= 0) {
                this.idleAnimationTimeout = 40; // 40 ticks = 2 seconds
                this.idleAnimationState.start(this.tickCount);
            } else {
                --this.idleAnimationTimeout;
            }

            // Swim animation (simple boolean state)
            this.swimAnimationState.animateWhen(this.isInWater(), this.tickCount);

            // Sitting transitions and pose
            handleSittingAnimations();

            // Sleeping transitions and pose
            handleSleepingAnimations();
        }
        if (!this.level().isClientSide()) {
            updateSleepState();
        }
    }

    private void handleSittingAnimations() {
        boolean sitting = this.isSitting();
        long poseTime = this.getPoseTime();

        if (sitting) {
            // Transitioning from standing to sitting
            if (poseTime < 10L) { // 0.5s = 10 ticks
                this.standToSitAnimationState.startIfStopped(this.tickCount);
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.stop();
            } else {
                // Sitting pose (looping)
                this.standToSitAnimationState.stop();
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            // Transitioning from sitting to standing
            if (poseTime < 10L) { // 0.5s = 10 ticks
                this.sitToStandAnimationState.startIfStopped(this.tickCount);
                this.standToSitAnimationState.stop();
                this.sitPoseAnimationState.stop();
            } else {
                // Standing - all sitting animations off
                this.standToSitAnimationState.stop();
                this.sitToStandAnimationState.stop();
                this.sitPoseAnimationState.stop();
            }
        }
    }

    private void handleSleepingAnimations() {
        boolean sleeping = this.isFrenchieSleeping();
        long sleepTime = this.getSleepTime();

        if (sleeping) {
            // Transitioning from standing to sleeping
            if (sleepTime < 20L) { // 1.0s = 20 ticks
                this.standToSleepAnimationState.startIfStopped(this.tickCount);
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.stop();
            } else {
                // Sleeping pose (looping)
                this.standToSleepAnimationState.stop();
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.startIfStopped(this.tickCount);
            }
        } else {
            // Transitioning from sleeping to standing
            if (sleepTime < 20L) { // 1.0s = 20 ticks
                this.sleepToStandAnimationState.startIfStopped(this.tickCount);
                this.standToSleepAnimationState.stop();
                this.sleepAnimationState.stop();
            } else {
                // Awake - all sleeping animations off
                this.standToSleepAnimationState.stop();
                this.sleepToStandAnimationState.stop();
                this.sleepAnimationState.stop();
            }
        }
    }

    public void resetLastPoseChangeTick(long pLastPoseChangeTick) {
        this.entityData.set(LAST_POSE_CHANGE_TICK, pLastPoseChangeTick);
    }

    public long getPoseTime() {
        return this.level().getGameTime() - this.entityData.get(LAST_POSE_CHANGE_TICK);
    }

    public void resetLastSleepChangeTick(long pLastSleepChangeTick) {
        this.entityData.set(LAST_SLEEP_CHANGE_TICK, pLastSleepChangeTick);
    }

    public long getSleepTime() {
        return this.level().getGameTime() - this.entityData.get(LAST_SLEEP_CHANGE_TICK);
    }


    /* Sleeping */

    private void updateSleepState() {
        if (this.isOrderedToSit()) {
            // When ordered to sit, don't sleep
            if (this.isFrenchieSleeping()) {
                this.setFrenchieSleeping(false);
            }
            return;
        }

        boolean shouldSleep = isPlayerSleepTime() && !this.isInWater();

        if (shouldSleep && !this.isFrenchieSleeping()) {
            this.setFrenchieSleeping(true);
        } else if (!shouldSleep && this.isFrenchieSleeping()) {
            this.setFrenchieSleeping(false);
        }
    }

    /**
     * Checks if it's night time when players can sleep in beds.
     * In clear weather: ticks 12542-23459 (when stars appear)
     * In rainy weather: ticks 12010-23991 (earlier sleep window)
     */
    private boolean isPlayerSleepTime() {
        long time = this.level().getDayTime() % 24000;
        if (this.level().isRaining()) {
            return time >= 12010 && time < 23991;
        } else {
            return time >= 12542 && time < 23459;
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isFrenchieSleeping();
    }

    /*
    Sounds
     */

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.PANDA_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
}