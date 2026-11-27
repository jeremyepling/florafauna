package net.j40climb.florafauna.mixin;

import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberMenu;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberScreen;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuScreens.class)
abstract class MenuScreensMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void florafauna$registerScreens(WritableRegistry<MenuScreens.ScreenConstructor<?, ?>> registry, CallbackInfo ci) {
        Registry.register(registry, BuiltInRegistries.MENU.getKey(FloraFaunaRegistry.CONTAINMENT_CHAMBER_MENU.get()),
                (MenuScreens.ScreenConstructor<ContainmentChamberMenu, ContainmentChamberScreen>) ContainmentChamberScreen::new);
    }
}
