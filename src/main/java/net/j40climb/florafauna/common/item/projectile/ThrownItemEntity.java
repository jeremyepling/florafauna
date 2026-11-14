package net.j40climb.florafauna.common.item.projectile;

import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A throwable item entity that can be configured to either return like a trident with loyalty
 * or stay in place like an arrow for manual pickup.
 */
public class ThrownItemEntity extends AbstractArrow {

    // Synced entity data
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_MAX_RANGE =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_BREAK_BLOCKS =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AUTO_RETURN =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_RETURN_SPEED =
            SynchedEntityData.defineId(ThrownItemEntity.class, EntityDataSerializers.FLOAT);

    // State tracking
    private boolean returning = false;
    private boolean dealtDamage = false;
    private Vec3 startPos;
    public int clientSideReturnTickCount = 0;

    public ThrownItemEntity(EntityType<? extends ThrownItemEntity> entityType, Level level) {
        super(entityType, level);
        this.startPos = this.position();
    }

    public ThrownItemEntity(Level level, LivingEntity shooter, ItemStack thrownItem, ThrowableAbilityData abilityData) {
        super(FloraFaunaRegistry.THROWN_ITEM.get(), shooter, level, thrownItem.copyWithCount(1), null);
        this.startPos = shooter.position();

        // Set entity data from ability config
        this.entityData.set(DATA_ITEM, getPickupItem());
        this.entityData.set(DATA_DAMAGE, abilityData.damage());
        this.entityData.set(DATA_MAX_RANGE, abilityData.maxRange());
        this.entityData.set(DATA_BREAK_BLOCKS, abilityData.breakBlocks());
        this.entityData.set(DATA_AUTO_RETURN, abilityData.autoReturn());
        this.entityData.set(DATA_RETURN_SPEED, abilityData.returnSpeed());

        // Allow pickup when not auto-returning
        this.pickup = shooter instanceof Player player && player.hasInfiniteMaterials()
                ? Pickup.CREATIVE_ONLY : Pickup.ALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_MAX_RANGE, 32.0f);
        builder.define(DATA_DAMAGE, 8.0f);
        builder.define(DATA_BREAK_BLOCKS, false);
        builder.define(DATA_AUTO_RETURN, true);
        builder.define(DATA_RETURN_SPEED, 1.0f);
    }

    @Override
    public void tick() {
        // Track ground time for dealt damage state
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        boolean autoReturn = getAutoReturn();
        float maxRange = getMaxRange();
        float returnSpeed = getReturnSpeed();

        // Check if we should start returning (exceeded max range)
        if (autoReturn && !returning && startPos != null && startPos.distanceTo(position()) >= maxRange) {
            returning = true;
        }

        if (autoReturn && (returning || dealtDamage || isNoPhysics())) {
            if (!isAcceptableReturnOwner(owner)) {
                if (level() instanceof ServerLevel serverLevel) {
                    if (pickup != Pickup.CREATIVE_ONLY) spawnAtLocation(serverLevel, getThrownItem());
                    discard();
                }
                return;
            }

            setNoPhysics(true);
            Vec3 toOwner = owner.getEyePosition().subtract(position());
            setPosRaw(getX(), getY() + toOwner.y * 0.015 * returnSpeed, getZ());
            setDeltaMovement(getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(0.05 * returnSpeed)));
            if (clientSideReturnTickCount++ == 0) {
                playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }
            if (distanceTo(owner) < 1.5 && level() instanceof ServerLevel serverLevel) {
                if (pickup != Pickup.CREATIVE_ONLY) {
                    if (owner instanceof Player player) {
                        player.getInventory().placeItemBackInInventory(getThrownItem().copy());
                    } else {
                        spawnAtLocation(serverLevel, getThrownItem());
                    }
                }
                discard();
                return;
            }
        }

        super.tick();
    }

    private boolean isAcceptableReturnOwner(@Nullable Entity owner) {
        if (owner == null || !owner.isAlive()) return false;
        if (owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return !serverPlayer.isSpectator();
        }
        return true;
    }


    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 startVec, Vec3 endVec) {
        // Don't hit entities if we've already dealt damage (like trident)
        return this.dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = getDamage();
        Entity owner = this.getOwner();
        DamageSource damageSource = this.damageSources().thrown(this, owner != null ? owner : this);

        this.dealtDamage = true;

        if (damage > 0 && target.hurtOrSimulate(damageSource, damage)) {
            if (target instanceof LivingEntity livingTarget) {
                this.doKnockback(livingTarget, damageSource);
                this.doPostHurtEffects(livingTarget);
            }
        }

        // Bounce back slightly like trident
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.02, 0.2, 0.02));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.0f);

        if (getAutoReturn()) {
            returning = true;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        boolean breakBlocks = getBreakBlocks();
        boolean autoReturn = getAutoReturn();

        if (breakBlocks && level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner
                && breakImpactBlock(level, owner, result.getBlockPos())) {
            return;
        }

        if (autoReturn) {
            // Start returning
            this.dealtDamage = true;
            returning = true;
        }

        // Call super for normal arrow ground behavior (sticks in ground)
        super.onHitBlock(result);
        this.playSound(getDefaultHitGroundSoundEvent(), 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
    }

    private boolean breakImpactBlock(ServerLevel level, ServerPlayer owner, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        ItemStack tool = getThrownItem();
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !level.mayInteract(owner, pos)
                || !tool.canDestroyBlock(state, level, pos, owner)
                || CommonHooks.fireBlockBreak(level, owner.gameMode.getGameModeForPlayer(), owner, pos, state).isCanceled()) {
            return false;
        }
        var block = state.getBlock();
        var blockEntity = level.getBlockEntity(pos);
        boolean harvest = !owner.preventsBlockDrops()
                && (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state));
        BlockState adjusted = block.playerWillDestroy(level, pos, state, owner);
        if (!adjusted.onDestroyedByPlayer(level, pos, owner, tool, harvest, level.getFluidState(pos))) return false;
        block.destroy(level, pos, adjusted);
        if (harvest) block.playerDestroy(level, owner, pos, adjusted, blockEntity, tool);
        return true;
    }

    @Override
    public void playerTouch(Player player) {
        if (!getAutoReturn() && (ownedBy(player) || getOwner() == null)) {
            super.playerTouch(player);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void setPickupItemStack(ItemStack stack) {
        super.setPickupItemStack(stack.copyWithCount(1));
        entityData.set(DATA_ITEM, getPickupItem());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void tickDespawn() {
        // Don't despawn if auto-return is enabled
        if (!getAutoReturn()) {
            super.tickDespawn();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.dealtDamage = input.getBooleanOr("DealtDamage", false);
        this.returning = input.getBooleanOr("Returning", false);

        // Read start position
        double startX = input.getDoubleOr("StartX", this.getX());
        double startY = input.getDoubleOr("StartY", this.getY());
        double startZ = input.getDoubleOr("StartZ", this.getZ());
        this.startPos = new Vec3(startX, startY, startZ);

        // Read ability data
        this.entityData.set(DATA_DAMAGE, input.getFloatOr("AbilityDamage", 8.0f));
        this.entityData.set(DATA_MAX_RANGE, input.getFloatOr("MaxRange", 32.0f));
        this.entityData.set(DATA_BREAK_BLOCKS, input.getBooleanOr("BreakBlocks", false));
        this.entityData.set(DATA_AUTO_RETURN, input.getBooleanOr("AutoReturn", true));
        this.entityData.set(DATA_RETURN_SPEED, input.getFloatOr("ReturnSpeed", 1.0f));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DealtDamage", this.dealtDamage);
        output.putBoolean("Returning", this.returning);

        // Save start position
        if (this.startPos != null) {
            output.putDouble("StartX", this.startPos.x);
            output.putDouble("StartY", this.startPos.y);
            output.putDouble("StartZ", this.startPos.z);
        }

        // Save ability data
        output.putFloat("AbilityDamage", getDamage());
        output.putFloat("MaxRange", getMaxRange());
        output.putBoolean("BreakBlocks", getBreakBlocks());
        output.putBoolean("AutoReturn", getAutoReturn());
        output.putFloat("ReturnSpeed", getReturnSpeed());
    }

    // Getters for synced data
    public ItemStack getThrownItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public float getMaxRange() {
        return this.entityData.get(DATA_MAX_RANGE);
    }

    public boolean getBreakBlocks() {
        return this.entityData.get(DATA_BREAK_BLOCKS);
    }

    public boolean getAutoReturn() {
        return this.entityData.get(DATA_AUTO_RETURN);
    }

    public float getReturnSpeed() {
        return this.entityData.get(DATA_RETURN_SPEED);
    }

    public boolean isReturning() {
        return this.returning;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
