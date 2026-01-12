package net.j40climb.florafauna.mixin.noclip;

import net.j40climb.florafauna.noclip.ClippingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * THE KEY FIX FOR NOCLIP JITTER.
 *
 * This mixin prevents the server from rubber-banding players who are in noclip mode.
 * The server normally kicks players who "float" for more than 80 ticks (getMaximumFlyingTicks).
 * When a player is clipping, we return Integer.MAX_VALUE to effectively disable this check.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private int aboveGroundTickCount;

    /**
     * Intercepts getMaximumFlyingTicks to return MAX_VALUE when the player is clipping.
     * This prevents the server from thinking the player is cheating and rubber-banding them.
     */
    @Inject(method = "getMaximumFlyingTicks", at = @At("HEAD"), cancellable = true)
    private void florafauna$onGetMaximumFlyingTicks(Entity vehicle, CallbackInfoReturnable<Integer> cir) {
        if (this.player instanceof ClippingEntity clippingPlayer && clippingPlayer.isClipping()) {
            // Reset the floating tick counter to prevent any accumulation
            this.aboveGroundTickCount = 0;
            // Return MAX_VALUE to effectively disable the floating check
            cir.setReturnValue(Integer.MAX_VALUE);
        }
    }
}
