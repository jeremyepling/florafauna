package net.j40climb.florafauna.entity.custom;

import net.j40climb.florafauna.entity.FrenchieVariant;
import net.j40climb.florafauna.entity.ModEntities;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrenchieEntity extends TamableAnimal {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState swimIdleAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();

    public final AnimationState sitDownAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState sitUpAnimationState = new AnimationState();

    public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.LONG);

    private int idleAnimationTimeout = 0;

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(FrenchieEntity.class, EntityDataSerializers.INT);

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

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 10D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24D);
    }

    @Override
    public boolean isSwimming() {
        return this.isInWater(); // return true if entity is in water
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES.asItem());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, AgeableMob otherParent) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        FrenchieEntity baby = ModEntities.FRENCHIE.get().create(level);
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
    }

    private int getTypeVariant() {
        return this.entityData.get(VARIANT);
    }

    public FrenchieVariant getVariant() {
        return FrenchieVariant.byId(this.getTypeVariant() & 255);
    }

    private void setVariant(FrenchieVariant variant) {
        this.entityData.set(VARIANT, variant.getId() & 255);
    }

    /* RIGHT CLICKING */

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
        return this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
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
            this.resetLastPoseChangeTick(-this.level().getGameTime());
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
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Variant", this.getTypeVariant());
        compound.putString("Pose", this.getPose().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(VARIANT, compound.getInt("Variant"));
        // Save the pose data, like sleeping
        if (compound.contains("Pose")) {
            this.setPose(Pose.valueOf(compound.getString("Pose")));
        }
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, DifficultyInstance difficulty,
                                                 MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        FrenchieVariant variant = Util.getRandom(FrenchieVariant.values(), this.random);
        this.setVariant(variant);
        this.resetLastPoseChangeTickToFullStand(level.getLevel().getGameTime());
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /* Animation */

    @Override
    public void tick() {
        super.tick();

        if(this.level().isClientSide()) {
            // Setup animation states
            if (this.idleAnimationTimeout <= 0) {
                this.idleAnimationTimeout = 40; // 40 ticks is to seconds and that's the length of the idle animation
                this.swimIdleAnimationState.animateWhen(this.isInWaterOrBubble() && !this.walkAnimation.isMoving(), this.tickCount);

                this.idleAnimationState.start(this.tickCount);
            } else {
                --this.idleAnimationTimeout;
            }
            if (this.isVisuallySitting()) {
                this.sitUpAnimationState.stop();
                if (this.isVisuallySittingDown()) {
                    this.sitDownAnimationState.startIfStopped(this.tickCount);
                    this.sitPoseAnimationState.stop();
                } else {
                    this.sitDownAnimationState.stop();
                    this.sitPoseAnimationState.startIfStopped(this.tickCount);
                }
            } else {
                this.sitDownAnimationState.stop();
                this.sitPoseAnimationState.stop();
                this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
            }
        }
        if (!this.level().isClientSide()) { // Server side only
            long time = this.level().getDayTime() % 24000;
            if (time >= 13000 && time <= 23000 && random.nextFloat() < 0.01f && !this.isInWaterOrBubble()) {
                this.setPose(Pose.SLEEPING);
            } else if (time < 13000 || time > 23000 || this.isInWaterOrBubble()) {
                this.setPose(Pose.STANDING);
            }
        }

    }

    public boolean isInPoseTransition() {
        long i = this.getPoseTime();
        return i < (long) (this.isSitting() ? 40 : 52);
    }
    public boolean isVisuallySitting() {
        return this.getPoseTime() < 0L != this.isSitting();
    }
    private boolean isVisuallySittingDown() {
        return this.isSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
    }
    public void resetLastPoseChangeTick(long pLastPoseChangeTick) {
        this.entityData.set(LAST_POSE_CHANGE_TICK, pLastPoseChangeTick);
    }
    public long getPoseTime() {
        return this.level().getGameTime() - Math.abs(this.entityData.get(LAST_POSE_CHANGE_TICK));
    }
    private void resetLastPoseChangeTickToFullStand(long pLastPoseChangedTick) {
        this.resetLastPoseChangeTick(Math.max(0L, pLastPoseChangedTick - 52L - 1L));
    }

    /* Sleeping */

    @Override
    public @Nullable Direction getBedOrientation() {
        return Direction.NORTH;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.getPose() == Pose.SLEEPING;
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