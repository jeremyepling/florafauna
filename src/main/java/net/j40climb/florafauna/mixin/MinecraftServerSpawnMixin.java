package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.block.resonantbloom.SpawnBlooms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSpawnMixin {
    // Only called before vanilla persists the Overworld as initialized.
    @Inject(method = "setInitialSpawn", at = @At("RETURN"))
    private static void florafauna$placeStarterBlooms(ServerLevel level, ServerLevelData data,
                                                    boolean bonusChest, boolean debugWorld,
                                                    LevelLoadListener progress, CallbackInfo info) {
        if (!debugWorld) {
            SpawnBlooms.place(level, data.getRespawnData().pos());
        }
    }
}
