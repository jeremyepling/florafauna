package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.item.abilities.multiblock.RemoteOreStrike;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Both player equipment and the 26.1 harvest check read Inventory.getSelectedItem().
@Mixin(Inventory.class)
abstract class RemoteHammerToolMixin {
    @Shadow @Final public Player player;

    @Inject(method = "getSelectedItem", at = @At("HEAD"), cancellable = true)
    private void florafauna$remoteMiningTool(CallbackInfoReturnable<ItemStack> result) {
        var tool = RemoteOreStrike.toolFor(player);
        if (tool != null) result.setReturnValue(tool);
    }
}