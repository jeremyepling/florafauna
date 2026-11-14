package net.j40climb.florafauna.common.item.abilities.multiblock;

import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna.common.item.hammer.HammerItem;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/** A synchronous player break sees the projectile's tool without moving or replacing any inventory slot. */
public final class RemoteOreStrike {
    private record Strike(ServerPlayer owner, ItemStack hammer) {}
    private static final ThreadLocal<Strike> ACTIVE = new ThreadLocal<>();

    private RemoteOreStrike() {}

    public static @Nullable ItemStack toolFor(Object entity) {
        Strike strike = ACTIVE.get();
        return strike != null && strike.owner == entity ? strike.hammer : null;
    }

    public static void hit(ServerPlayer owner, ItemStack hammer, BlockPos origin) {
        var level = owner.level();
        int charge = HammerItem.charge(hammer);
        if (!owner.isAlive() || owner.isSpectator() || !hammer.is(FloraFaunaRegistry.HAMMER.get()) || charge == 0
                || !MultiBlockPatterns.canInteract(level, owner, origin) || !level.getBlockState(origin).is(Tags.Blocks.ORES)) return;
        var settings = hammer.getOrDefault(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);
        var vein = new MiningModeData(MiningShape.SHAPELESS, MiningShape.SHAPELESS.getRadius(),
                settings.maxBlocksToBreak(), settings.ignoreToolRestrictions());
        if (!MultiBlockPatterns.isMineable(level, owner, hammer, vein, origin)) return;
        var originState = level.getBlockState(origin);
        var matches = MultiBlockPatterns.veinMatcher(originState, hammer.getOrDefault(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT).exactVein());
        var positions = MultiBlockPatterns.vein(owner, hammer, vein, origin);
        var previous = ACTIVE.get();
        ACTIVE.set(new Strike(owner, hammer));
        try {
            for (BlockPos pos : positions) {
                if (!MultiBlockPatterns.canInteract(level, owner, pos)) continue;
                var before = level.getBlockState(pos);
                boolean mined = matches.test(before) && before.is(Tags.Blocks.ORES)
                        && MultiBlockPatterns.isMineable(level, owner, hammer, vein, pos)
                        && owner.gameMode.destroyBlock(pos) && !level.getBlockState(pos).is(before.getBlock());
                if (mined) {
                    hammer.set(FloraFaunaRegistry.HAMMER_CHARGE, --charge);
                    if (charge == 0) break;
                } else if (pos.equals(origin)) {
                    break;
                }
            }
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }
}