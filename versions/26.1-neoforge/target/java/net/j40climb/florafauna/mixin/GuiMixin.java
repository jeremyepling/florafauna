package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to force the locator bar to show when player has an active waypoint.
 * - Restoration husk: BONDED_WEAKENED state with active husk
 * - Mining anchor: Active waypoint anchor in same dimension
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * Inject at the head of nextContextualInfoState() to return LOCATOR
     * when player has active restoration husk in BONDED_WEAKENED state,
     * or when player has an active waypoint anchor in the same dimension.
     */
    @Inject(method = "nextContextualInfoState", at = @At("HEAD"), cancellable = true)
    private void florafauna$forceLocatorBar(CallbackInfoReturnable<Gui.ContextualInfo> cir) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Check conditions: active husk, BONDED_WEAKENED state, same dimension
        if (data.restorationHuskActive() &&
            data.symbioteState() == SymbioteState.BONDED_WEAKENED &&
            data.restorationHuskDim() != null &&
            data.restorationHuskPos() != null &&
            data.restorationHuskDim().equals(player.level().dimension())) {

            // Force the locator bar to show for husk waypoint
            cir.setReturnValue(Gui.ContextualInfo.LOCATOR);
            return;
        }

        // Check conditions: active waypoint anchor, same dimension
        if (data.hasActiveWaypointAnchor() &&
            data.activeWaypointAnchorDim() != null &&
            data.activeWaypointAnchorPos() != null &&
            data.activeWaypointAnchorDim().equals(player.level().dimension())) {

            // Force the locator bar to show for anchor waypoint
            cir.setReturnValue(Gui.ContextualInfo.LOCATOR);
        }
    }
}
