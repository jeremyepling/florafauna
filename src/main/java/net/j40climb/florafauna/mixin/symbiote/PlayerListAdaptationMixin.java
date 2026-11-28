package net.j40climb.florafauna.mixin.symbiote;

import net.j40climb.florafauna.common.symbiote.onboarding.SymbioteOnboarding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
abstract class PlayerListAdaptationMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void florafauna$flushBeforeSave(ServerPlayer player, CallbackInfo info) {
        SymbioteOnboarding.flushSprintProgress(player);
    }

    @Inject(method = "respawn", at = @At("HEAD"))
    private void florafauna$flushBeforeRespawn(ServerPlayer player, boolean keepAllPlayerData,
                                               Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> result) {
        SymbioteOnboarding.flushSprintProgress(player);
    }
}
