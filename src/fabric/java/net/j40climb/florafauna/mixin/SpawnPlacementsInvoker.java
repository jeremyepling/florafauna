package net.j40climb.florafauna.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's registration method for our two new entity types. */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsInvoker {
    @Invoker("register")
    static <T extends Mob> void florafauna$register(EntityType<T> type, SpawnPlacementType placement,
            Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        throw new AssertionError("Mixin was not applied");
    }
}
