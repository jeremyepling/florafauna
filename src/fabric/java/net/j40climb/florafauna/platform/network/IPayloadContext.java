package net.j40climb.florafauna.platform.network;
import net.minecraft.world.entity.player.Player;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
public record IPayloadContext(Player player, Executor executor) {
 public CompletableFuture<Void> enqueueWork(Runnable task) { return CompletableFuture.runAsync(task, executor); }
}
