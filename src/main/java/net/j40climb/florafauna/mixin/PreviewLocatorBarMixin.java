//? if >=26.4 {
/*package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.client.LocatorWaypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public class PreviewLocatorBarMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void florafauna$renderWaypoints(GuiGraphicsExtractor gui, CallbackInfo ci) {
        int barTop = gui.guiHeight() - ContextualBar.MARGIN_BOTTOM - ContextualBar.HEIGHT;
        LocatorWaypoints.renderHuskWaypoint(minecraft.player, gui, gui.deltaTracker(), barTop);
        LocatorWaypoints.renderAnchorWaypoint(minecraft.player, gui, gui.deltaTracker(), barTop);
    }
}
*///?}
