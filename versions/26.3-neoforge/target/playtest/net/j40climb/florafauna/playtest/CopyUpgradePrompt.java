package net.j40climb.florafauna.playtest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Handles vanilla migration dialogs after verifying the disposable copy. */
final class CopyUpgradePrompt {
    static Screen handle(Minecraft mc, Screen lastUpgradePrompt, Runnable verifyCopyDirectory) {
        Screen screen = mc.gui.screen();
        if (screen == null || screen == lastUpgradePrompt) return lastUpgradePrompt;
        Component label;
        if (screen instanceof BackupConfirmScreen && screen.getTitle().equals(Component.translatable("selectWorld.backupQuestion.file_fixing_required"))) {
            label = BackupConfirmScreen.BACKUP_AND_JOIN;
        } else if (screen instanceof ConfirmScreen && screen.getTitle().equals(Component.translatable("upgradeWorld.done"))) {
            label = CommonComponents.GUI_YES;
        } else {
            return lastUpgradePrompt;
        }
        verifyCopyDirectory.run();
        for (var child : screen.children()) {
            if (child instanceof Button button && button.active && button.visible && label.equals(button.getMessage())) {
                var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                        button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                Playtest.require(screen.mouseClicked(click, false), "World upgrade button did not accept click");
                return screen;
            }
        }
        return lastUpgradePrompt;
    }
}
