package net.j40climb.florafauna.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.j40climb.florafauna.common.block.vacuum.BlockDropData;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.List;
import java.util.function.Consumer;

@Mixin(Block.class)
abstract class FabricBlockDropMixin {
    @Unique
    private static final ThreadLocal<Integer> florafauna$blockLootDepth = ThreadLocal.withInitial(() -> 0);

    // Only the loot-emission loop is a block break. Hand harvesting and shearing also call
    // popResource directly, and must remain ordinary items just as on NeoForge.
    @WrapOperation(method = "dropResources", at = @At(value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private static void florafauna$emitBlockLoot(List<ItemStack> drops, Consumer<ItemStack> consumer, Operation<Void> original) {
        int depth = florafauna$blockLootDepth.get();
        florafauna$blockLootDepth.set(depth + 1);
        try {
            original.call(drops, consumer);
        } finally {
            if (depth == 0) florafauna$blockLootDepth.remove();
            else florafauna$blockLootDepth.set(depth);
        }
    }

    @ModifyArg(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), index = 0)
    private static Entity florafauna$tagDrop(Entity entity) {
        if (florafauna$blockLootDepth.get() > 0 && entity instanceof ItemEntity item) {
            AttachmentAccess.set(item, FloraFaunaRegistry.BLOCK_DROP_DATA, BlockDropData.create(item.level().getGameTime()));
        }
        return entity;
    }
}
