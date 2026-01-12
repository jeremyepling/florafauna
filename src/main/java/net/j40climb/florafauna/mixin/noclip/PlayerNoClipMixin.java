package net.j40climb.florafauna.mixin.noclip;

import net.j40climb.florafauna.noclip.ClippingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that implements ClippingEntity on Player to enable no-clip functionality.
 *
 * Key behaviors:
 * - Tracks clipping state per player
 * - Injects after vanilla's noPhysics = isSpectator() assignment
 * - Only enables noPhysics when BOTH clipping is enabled AND player is flying
 * - Walking with clipping enabled has normal collision
 */
@Mixin(Player.class)
public abstract class PlayerNoClipMixin extends LivingEntity implements ClippingEntity {

    @Unique
    private boolean florafauna$clipping = false;

    // Required constructor for extending LivingEntity
    protected PlayerNoClipMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    @Override
    public boolean canClip() {
        // Only allow clipping in creative mode
        Player self = (Player) (Object) this;
        return self.isCreative();
    }

    @Unique
    @Override
    public boolean isClipping() {
        return this.florafauna$clipping;
    }

    @Unique
    @Override
    public void setClipping(boolean clipping) {
        this.florafauna$clipping = clipping;
        // When disabling clipping, ensure noPhysics is reset
        if (!clipping) {
            this.noPhysics = false;
        }
    }

    /**
     * Injects AFTER vanilla sets noPhysics = isSpectator() in Player.tick().
     * Only enables noclip when the player is both clipping AND flying.
     * Walking has normal collision even with clipping enabled.
     */
    @Inject(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
                    shift = At.Shift.AFTER
            )
    )
    private void florafauna$onTickAfterNoPhysics(CallbackInfo ci) {
        if (this.florafauna$clipping && this.canClip()) {
            Player self = (Player) (Object) this;
            boolean isFlying = self.getAbilities().flying;

            // Only enable noclip when flying
            if (isFlying) {
                this.noPhysics = true;
                this.setOnGround(false);
                this.fallDistance = 0;
            }
            // When walking, keep normal collision (noPhysics stays false from vanilla)
        } else if (this.florafauna$clipping && !this.canClip()) {
            // Auto-disable clipping if player is no longer in creative mode
            this.florafauna$clipping = false;
        }
    }
}
