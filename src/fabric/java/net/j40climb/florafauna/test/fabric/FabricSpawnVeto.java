package net.j40climb.florafauna.test.fabric;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Scoped test veto; only the separate Fabric GameTest mod installs the injection that reads it. */
public final class FabricSpawnVeto {
    private static final IdentityHashMap<ServerLevel, Set<UUID>> BLOCKED = new IdentityHashMap<>();
    private FabricSpawnVeto() {}
    public static Runnable block(ServerLevel level, UUID id) {
        BLOCKED.computeIfAbsent(level, ignored -> new HashSet<>()).add(id);
        return () -> {
            var ids = BLOCKED.get(level);
            if (ids != null) {
                ids.remove(id);
                if (ids.isEmpty()) BLOCKED.remove(level);
            }
        };
    }
    public static boolean blocked(ServerLevel level, Entity entity) {
        return BLOCKED.getOrDefault(level, Collections.emptySet()).contains(entity.getUUID());
    }
}
