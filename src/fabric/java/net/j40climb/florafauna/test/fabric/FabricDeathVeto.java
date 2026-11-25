package net.j40climb.florafauna.test.fabric;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/** A scoped cancellation through Fabric's real death event, used by the inventory custody fixture. */
public final class FabricDeathVeto {
    private static final Set<ServerPlayer> BLOCKED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean registered;
    private FabricDeathVeto() {}
    public static Runnable block(ServerPlayer player) {
        if (!registered) {
            ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !BLOCKED.contains(entity));
            registered = true;
        }
        BLOCKED.add(player);
        return () -> BLOCKED.remove(player);
    }
}
