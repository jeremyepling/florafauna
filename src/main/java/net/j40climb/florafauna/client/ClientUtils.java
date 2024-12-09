package net.j40climb.florafauna.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ClientUtils {
    public static HitResult raycastFromPlayer(Player player, float maxDistance) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 endPosition = eyePosition.add(lookVector.scale(maxDistance));

        ClipContext context = new ClipContext(
                eyePosition,
                endPosition,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );
        return player.level().clip(context);
    }
}

