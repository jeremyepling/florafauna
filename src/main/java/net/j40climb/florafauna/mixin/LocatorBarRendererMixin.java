package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render husk waypoint on the locator bar.
 */
@Mixin(LocatorBarRenderer.class)
public class LocatorBarRendererMixin {

    // Distance-based sprites (matching our waypoint_style pattern)
    @Unique
    private static final Identifier HUSK_SPRITE_NEAR = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/husk_near");
    @Unique
    private static final Identifier HUSK_SPRITE_MID = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/husk_mid");
    @Unique
    private static final Identifier HUSK_SPRITE_FAR = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/husk_far");

    // Distance thresholds for sprite selection
    @Unique
    private static final float NEAR_DISTANCE = 50.0f;
    @Unique
    private static final float FAR_DISTANCE = 200.0f;

    @Unique
    private static final Identifier LOCATOR_BAR_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");

    @Unique
    private static final Identifier LOCATOR_BAR_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");

    // Sculk teal color in ARGB format
    @Unique
    private static final int TEAL_COLOR = 0xFF0F8080;

    @Unique
    private static final int VISIBLE_DEGREE_RANGE = 60;

    @Unique
    private static final int DOT_SIZE = 9;

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * Inject at the end of render() to add our husk waypoint.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void florafauna$renderHuskWaypoint(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }

        PlayerSymbioteData data = player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Check conditions: active husk, BONDED_WEAKENED state, same dimension
        if (!data.restorationHuskActive()) {
            return;
        }

        if (data.symbioteState() != SymbioteState.BONDED_WEAKENED) {
            return;
        }

        BlockPos huskPos = data.restorationHuskPos();
        if (huskPos == null) {
            return;
        }

        // Check same dimension
        if (data.restorationHuskDim() == null || !data.restorationHuskDim().equals(player.level().dimension())) {
            return;
        }

        // Calculate angle to husk
        Vec3 playerPos = player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true));
        Vec3 huskVec = Vec3.atCenterOf(huskPos);
        Vec3 direction = huskVec.subtract(playerPos);

        // Get player's look direction (yaw)
        float playerYaw = player.getYRot();

        // Calculate angle to husk in world space
        double angleToHusk = Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Calculate relative angle (how far off from where player is looking)
        double relativeAngle = Mth.wrapDegrees(angleToHusk - playerYaw);

        // Only render if within visible range (-60 to +60 degrees)
        if (relativeAngle <= -VISIBLE_DEGREE_RANGE || relativeAngle > VISIBLE_DEGREE_RANGE) {
            return;
        }

        // Calculate distance to husk for sprite selection
        float distance = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);

        // Select sprite and size based on distance (like vanilla WaypointStyle.sprite())
        Identifier sprite;
        int spriteSize;
        if (distance < NEAR_DISTANCE) {
            sprite = HUSK_SPRITE_NEAR;
            spriteSize = 9;  // Near sprite is 9x9
        } else if (distance >= FAR_DISTANCE) {
            sprite = HUSK_SPRITE_FAR;
            spriteSize = 5;  // Far sprite is 5x5
        } else {
            sprite = HUSK_SPRITE_MID;
            spriteSize = 7;  // Mid sprite is 7x7
        }

        // Calculate position on the locator bar
        // The bar is at the BOTTOM of the screen, above the hotbar
        // From ContextualBarRenderer: top = guiHeight - MARGIN_BOTTOM(24) - HEIGHT(5)
        int barTop = guiGraphics.guiHeight() - 24 - 5;
        // Center the sprite horizontally, accounting for its size
        int halfWidth = Mth.ceil((guiGraphics.guiWidth() - spriteSize) / 2.0F);

        // Map angle to bar position (-60 to +60 maps to -86.5 to +86.5 pixels)
        int xOffset = Mth.floor(relativeAngle * 173.0 / 2.0 / VISIBLE_DEGREE_RANGE);

        // Render the husk sprite with teal color
        // Vanilla renders 9x9 waypoints at (x, top - 2)
        // Adjust Y offset to vertically center smaller sprites at the same position
        // 9x9: offset -2, 7x7: offset -1, 5x5: offset 0
        int yOffset = -2 + (9 - spriteSize) / 2;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, halfWidth + xOffset, barTop + yOffset, spriteSize, spriteSize, TEAL_COLOR);

        // Render up/down arrows if husk is above or below player
        double heightDiff = huskVec.y - playerPos.y;
        double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        // Calculate pitch angle to target
        float pitchToHusk = (float) Math.toDegrees(Math.atan2(heightDiff, horizontalDist));

        // If significantly above (more than 30 degrees), show up arrow
        if (pitchToHusk > 30) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_ARROW_UP, halfWidth + xOffset + 1, barTop - 6, 7, 5);
        }
        // If significantly below, show down arrow
        else if (pitchToHusk < -30) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_ARROW_DOWN, halfWidth + xOffset + 1, barTop + 6, 7, 5);
        }
    }
}
