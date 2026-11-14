package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna.common.item.abilities.menu.HammerActionWheel;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

final class HammerActionWheelPlaytest {
    static void steps(Playtest t, BlockPos target) {
        t.server("Prepare ordinary dirt with both till and path enabled", p -> {
            p.level().setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            p.level().setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
            p.getMainHandItem().set(FloraFaunaRegistry.MULTI_TOOL_ABILITY, MultiToolAbilityData.DEFAULT);
            p.inventoryMenu.broadcastChanges();
        });
        choose(t, HammerAction.TILL);
        t.awaitClient("Aim at the dirt with Till selected", mc -> mc.level.getBlockState(target).is(Blocks.DIRT)
                && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target));
        t.client("Use the wheel's Till action", mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer("Till won over Path and kept the selected mining shape", p -> p.level().getBlockState(target).is(Blocks.FARMLAND)
                && p.getMainHandItem().get(FloraFaunaRegistry.MULTI_BLOCK_MINING).shape() == MiningShape.FLAT_5X5);
        choose(t, HammerAction.AUTO);
    }

    private static void choose(Playtest t, HammerAction action) {
        t.client("Open the Hammer action wheel", mc -> KeyMapping.click(ClientSetup.HAMMER_ACTION_WHEEL_KEY.get().getKey()));
        t.awaitClient("Show the five wheel actions", mc -> mc.screen instanceof HammerActionWheel
                && mc.screen.children().stream().filter(Button.class::isInstance).count() == HammerAction.values().length);
        if (action == HammerAction.TILL) t.screenshot("hammer-action-wheel");
        t.client("Click " + action.getSerializedName(), mc -> {
            var button = mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getMessage().equals(action.label())).findFirst().orElseThrow();
            var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0,
                    new MouseButtonInfo(0, 0));
            Playtest.require(mc.screen.mouseClicked(click, false), "Wheel ignored mouse click");
        });
        t.awaitServer("Persist " + action.getSerializedName() + " on the server's Hammer", p -> p.getMainHandItem().get(FloraFaunaRegistry.HAMMER_ACTION) == action);
        t.awaitClient("Sync " + action.getSerializedName() + " back to the held tool", mc -> mc.screen == null
                && mc.player.getMainHandItem().get(FloraFaunaRegistry.HAMMER_ACTION) == action);
    }
}
