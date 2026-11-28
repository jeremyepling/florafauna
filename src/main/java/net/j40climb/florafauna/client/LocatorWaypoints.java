package net.j40climb.florafauna.client;

import net.j40climb.florafauna.common.block.mininganchor.AnchorFillState;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LocatorWaypoints {
    private static final Identifier XAERO_GUI = Identifier.fromNamespaceAndPath("xaerobetterpvp", "gui/guis.png");
    private static final Identifier SKELETON_TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
    private static final int SKULL_SIZE = 9;

    // Distance thresholds for sprite selection
    private static final float NEAR_DISTANCE = 50.0f;
    private static final float FAR_DISTANCE = 200.0f;

    private static final Identifier LOCATOR_BAR_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");

    private static final Identifier LOCATOR_BAR_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");

    private static final Identifier ANCHOR_SPRITE_NEAR = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/anchor_near");
    private static final Identifier ANCHOR_SPRITE_MID = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/anchor_mid");
    private static final Identifier ANCHOR_SPRITE_FAR = Identifier.fromNamespaceAndPath("florafauna", "hud/locator_bar_dot/anchor_far");

    // Fill state colors
    private static final int ANCHOR_COLOR_NORMAL = 0xFF0F8080;   // Teal
    private static final int ANCHOR_COLOR_WARNING = 0xFFFF8800;  // Orange
    private static final int ANCHOR_COLOR_FULL = 0xFFFF0000;     // Red

    private static final int VISIBLE_DEGREE_RANGE = 60;

    private LocatorWaypoints() {}

    /**
     * Draw the player's active husk on the locator bar.
     */
    public static void renderHuskWaypoint(LocalPlayer player, GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, int barTop) {
        if (player == null) {
            return;
        }

        PlayerSymbioteData data = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

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

        int halfWidth = Mth.ceil((guiGraphics.guiWidth() - SKULL_SIZE) / 2.0F);
        int xOffset = Mth.floor(relativeAngle * 173.0 / 2.0 / VISIBLE_DEGREE_RANGE);
        int x = halfWidth + xOffset;
        int y = barTop - 2;
        // Use Xaero's own nine-pixel death waypoint glyph, including its one-pixel shadow.
        if (Minecraft.getInstance().getResourceManager().getResource(XAERO_GUI).isPresent()) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, XAERO_GUI, x + 1, y + 1, 0, 78,
                    SKULL_SIZE, SKULL_SIZE, 9, 9, 256, 256, 0xFF3E3E3E);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, XAERO_GUI, x, y, 0, 78,
                    SKULL_SIZE, SKULL_SIZE, 9, 9, 256, 256, 0xFFFCFCFC);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SKELETON_TEXTURE, x, y, 8, 8,
                    SKULL_SIZE, SKULL_SIZE, 8, 8, 64, 32);
        }

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

    /**
     * Draw the player's active mining anchor on the locator bar.
     */
    public static void renderAnchorWaypoint(LocalPlayer player, GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, int barTop) {
        if (player == null) {
            return;
        }

        PlayerSymbioteData data = net.j40climb.florafauna.platform.AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);

        // Check conditions: active waypoint anchor, same dimension
        if (!data.hasActiveWaypointAnchor()) {
            return;
        }

        BlockPos anchorPos = data.activeWaypointAnchorPos();
        ResourceKey<Level> anchorDim = data.activeWaypointAnchorDim();
        if (anchorPos == null || anchorDim == null) {
            return;
        }

        // Check same dimension
        if (!anchorDim.equals(player.level().dimension())) {
            return;
        }

        // Calculate angle to anchor
        Vec3 playerPos = player.getEyePosition(deltaTracker.getGameTimeDeltaPartialTick(true));
        Vec3 anchorVec = Vec3.atCenterOf(anchorPos);
        Vec3 direction = anchorVec.subtract(playerPos);

        // Get player's look direction (yaw)
        float playerYaw = player.getYRot();

        // Calculate angle to anchor in world space
        double angleToAnchor = Math.toDegrees(Math.atan2(-direction.x, direction.z));

        // Calculate relative angle (how far off from where player is looking)
        double relativeAngle = Mth.wrapDegrees(angleToAnchor - playerYaw);

        // Only render if within visible range (-60 to +60 degrees)
        if (relativeAngle <= -VISIBLE_DEGREE_RANGE || relativeAngle > VISIBLE_DEGREE_RANGE) {
            return;
        }

        // Calculate distance to anchor for sprite selection
        float distance = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y + direction.z * direction.z);

        // Select sprite and size based on distance
        Identifier sprite;
        int spriteSize;
        if (distance < NEAR_DISTANCE) {
            sprite = ANCHOR_SPRITE_NEAR;
            spriteSize = 9;  // Near sprite is 9x9
        } else if (distance >= FAR_DISTANCE) {
            sprite = ANCHOR_SPRITE_FAR;
            spriteSize = 5;  // Far sprite is 5x5
        } else {
            sprite = ANCHOR_SPRITE_MID;
            spriteSize = 7;  // Mid sprite is 7x7
        }

        // Determine color based on fill state
        int color = getAnchorColor(data.lastAnnouncedFillState());

        int halfWidth = Mth.ceil((guiGraphics.guiWidth() - spriteSize) / 2.0F);

        // Map angle to bar position (-60 to +60 maps to -86.5 to +86.5 pixels)
        int xOffset = Mth.floor(relativeAngle * 173.0 / 2.0 / VISIBLE_DEGREE_RANGE);

        // Render the anchor sprite with fill state color
        int yOffset = -2 + (9 - spriteSize) / 2;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, halfWidth + xOffset, barTop + yOffset, spriteSize, spriteSize, color);

        // Render up/down arrows if anchor is above or below player
        double heightDiff = anchorVec.y - playerPos.y;
        double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        // Calculate pitch angle to target
        float pitchToAnchor = (float) Math.toDegrees(Math.atan2(heightDiff, horizontalDist));

        // If significantly above (more than 30 degrees), show up arrow
        if (pitchToAnchor > 30) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_ARROW_UP, halfWidth + xOffset + 1, barTop - 6, 7, 5);
        }
        // If significantly below, show down arrow
        else if (pitchToAnchor < -30) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_ARROW_DOWN, halfWidth + xOffset + 1, barTop + 6, 7, 5);
        }
    }

    /**
     * Returns the color for the anchor waypoint based on fill state.
     */
    private static int getAnchorColor(AnchorFillState fillState) {
        return switch (fillState) {
            case NORMAL -> ANCHOR_COLOR_NORMAL;
            case WARNING -> ANCHOR_COLOR_WARNING;
            case FULL -> ANCHOR_COLOR_FULL;
        };
    }
}
