package net.j40climb.florafauna.mixin;
import net.j40climb.florafauna.common.symbiote.observation.ObservationEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
abstract class FabricPlayerWakeMixin {
 @Inject(method="stopSleepInBed",at=@At("TAIL"))
 private void florafauna$observeWake(boolean immediately,boolean update,CallbackInfo ci){ObservationEvents.onPlayerWakeUp((Player)(Object)this);}
}
