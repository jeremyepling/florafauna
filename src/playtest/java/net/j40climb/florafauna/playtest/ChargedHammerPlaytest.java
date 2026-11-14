package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna.common.item.hammer.HammerItem;
import net.j40climb.florafauna.setup.ClientSetup;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

final class ChargedHammerPlaytest {
    static void steps(Playtest t) {
        BlockPos ore = new BlockPos(24, 161, 24);
        t.server("Expose a diamond vein at the end of the throw lane", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-1, -1, -8), ore.offset(1, 1, -1))) {
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            p.level().setBlockAndUpdate(ore.east(), Blocks.DIAMOND_ORE.defaultBlockState());
            p.level().setBlockAndUpdate(ore.south(), Blocks.IRON_ORE.defaultBlockState());
            p.teleportTo(p.level(), ore.getX() + 0.5, ore.getY() - 1, ore.getZ() - 6.5, Set.of(), 0f, 0f, true);
        });
        t.awaitClient("See the charged-throw lane", mc -> mc.player.position().distanceToSqr(ore.north(7).getCenter()) < 6
                && mc.level.getBlockState(ore).is(Blocks.DIAMOND_ORE) && mc.player.getMainHandItem().is(FloraFaunaRegistry.HAMMER.get()));
        t.client("Hold the mapped throw key", mc -> {
            var key = ClientSetup.THROW_ITEM_KEY.get();
            key.setDown(true);
            KeyMapping.click(key.getKey());
        });
        t.awaitServer("Reach the ore-strike threshold with the Hammer still in hand", p -> {
            var hold = p.getExistingDataOrNull(FloraFaunaRegistry.THROW_HOLD);
            return hold != null && p.level().getGameTime() - hold.start() >= ChargedThrowPayload.HOLD_TICKS
                    && p.getMainHandItem().is(FloraFaunaRegistry.HAMMER.get());
        });
        t.client("Release the mapped throw key", mc -> ClientSetup.THROW_ITEM_KEY.get().setDown(false));
        t.awaitServer("Strike only the diamond vein and debit the flying Hammer", p -> {
            if (!p.level().isEmptyBlock(ore) || !p.level().isEmptyBlock(ore.east())) return false;
            Playtest.require(p.level().getBlockState(ore.south()).is(Blocks.IRON_ORE), "The Hammer dug into another ore");
            return p.level().getEntities(FloraFaunaRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p
                    && HammerItem.charge(e.getThrownItem()) == HammerItem.MAX_CHARGE - 2).size() == 1;
        });
        t.awaitClient("Receive the returning Hammer in the hotbar", mc ->
                mc.player.getMainHandItem().is(FloraFaunaRegistry.HAMMER.get())
                        && mc.level.isEmptyBlock(ore) && mc.level.isEmptyBlock(ore.east()));
    }
}