package net.j40climb.florafauna.common.entity.lizard;

import net.j40climb.florafauna.common.entity.RegisterEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LizardEntity extends AbstractChestedHorse {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public LizardEntity(EntityType<? extends AbstractChestedHorse> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes().add(Attributes.MAX_HEALTH, 35D)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 2f)
                .add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.TEMPT_RANGE, 10D)
                .add(Attributes.JUMP_STRENGTH, 0.7f);
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Float in water
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Priority 1: Panic when damaged
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2));

        // Priority 1: Run around when untamed and player tries to mount
        this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2));

        // Priority 2: Breed with other lizards
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0, AbstractChestedHorse.class));

        // Priority 3: Tempted by glow berries
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25,
            stack -> stack.is(Items.GLOW_BERRIES), false));

        // Priority 4: Baby lizards follow parents
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));

        // Priority 6: Wander around randomly
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));

        // Priority 7: Look at nearby players
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // Priority 8: Look around randomly
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 30;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return RegisterEntities.LIZARD.get().create(pLevel, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES.asItem());
    }

    @Override
    protected boolean handleEating(Player player, ItemStack stack) {
        // If feeding glow berries, handle it specially for lizards
        if (stack.is(Items.GLOW_BERRIES)) {
            boolean flag = false;

            // Heal the lizard
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(2.0F);
                flag = true;
            }

            // Increase temper for taming (if not tamed yet and not at max temper)
            if (!this.isTamed() && this.getTemper() < this.getMaxTemper() && !this.level().isClientSide()) {
                this.modifyTemper(5);
                flag = true;
            }

            // Breeding (if tamed, adult, and not in love)
            if (!this.level().isClientSide() && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
                this.setInLove(player);
                flag = true;
            }

            // Trigger eating animation/sounds via game event
            if (flag) {
                this.setEating(true); // Triggers eating animation
                this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
            }

            return flag;
        }

        // Call parent for other foods
        return super.handleEating(player, stack);
    }

// rideable


    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        // Only allow saddle when alive, adult, and tamed
        if (slot == EquipmentSlot.SADDLE) {
            return this.isAlive() && !this.isBaby() && this.isTamed();
        }
        // Body armor slot (optional - can remove this if lizards don't wear armor)
        if (slot == EquipmentSlot.BODY) {
            return this.isAlive() && !this.isBaby() && this.isTamed();
        }
        // No other slots allowed (no weapons, shields, etc.)
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return super.getPassengerAttachmentPoint(entity, dimensions, partialTick)
                .add(new Vec3(0.0, -0.6, -0.5 * (double)partialTick)
                        .yRot(-this.getYRot() * (float) (Math.PI / 180.0)));
    }
}