package net.j40climb.florafauna.playtest;

import java.util.Set;
import java.util.UUID;
import net.j40climb.florafauna.common.block.slimegrate.SlimeGrateBlock;
import net.j40climb.florafauna.common.block.slimegrate.SlimeGrateFarm;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;*/
//?}
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class SlimeGratePlaytest {
    private static final BlockPos GRATE = new BlockPos(0, 201, 0);
    private static final BlockPos NOTE = GRATE.east(2).below();
    private static final UUID JELLY = UUID.fromString("88c090a7-8b43-43b7-9aa2-183a121e38e7");

    static void steps(Playtest t, int phase) {
        if (phase == 2) {
            t.awaitServer("Verify the rejoined slime, bond, cooldown and harvest after a full restart", p -> {
                var slimes = p.level().getEntitiesOfClass(Slime.class, new AABB(GRATE).inflate(12), s -> s.getUUID().equals(JELLY));
                if (slimes.size() != 1) return false;
                Slime slime = slimes.getFirst();
                return slime.getSize() == 2 && slime.isPersistenceRequired() && MobSymbioteHelper.hasMobSymbiote(slime)
                        && slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED) >= 0
                        && ((HopperBlockEntity)p.level().getBlockEntity(GRATE.below())).countItem(Items.SLIME_BALL) == 1;
            });
            t.awaitServer("Wait for Jelly's coat to regrow", p -> {
                Slime slime = (Slime)p.level().getEntity(JELLY);
                return p.level().getGameTime() - slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED) >= SlimeGrateFarm.RECOAT_TICKS;
            });
            t.server("Play the opposite floor note with redstone", p -> {
                Slime slime = (Slime)p.level().getEntity(JELLY);
                Vec3 far = Vec3.atBottomCenterOf(GRATE.east(2));
                slime.snapTo(far.x, far.y, far.z, 90, 0);
                slime.setDeltaMovement(Vec3.ZERO);
                p.level().setBlockAndUpdate(GRATE.west(2).below().north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            });
            t.awaitServer("Collect a second slimeball after regrowth", p ->
                    ((HopperBlockEntity)p.level().getBlockEntity(GRATE.below())).countItem(Items.SLIME_BALL) == 2);
            return;
        }
        int[] id = new int[1];
        t.server("Build a two-note slime habitat in a disposable world", p -> {
            ServerLevel level = p.level();
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
            level.getServer().setWeatherParameters(6000, 0, false, false);
            for (int x = -5; x <= 5; x++) {
                for (int z = -3; z <= 5; z++) {
                    BlockPos feet = GRATE.offset(x, 0, z);
                    level.setBlockAndUpdate(feet.below(), Blocks.SMOOTH_STONE.defaultBlockState());
                    boolean fence = (Math.abs(x) == 4 && Math.abs(z) <= 2) || (Math.abs(z) == 2 && Math.abs(x) <= 4);
                    level.setBlockAndUpdate(feet, fence ? Blocks.OAK_FENCE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(GRATE.below(), Blocks.HOPPER.defaultBlockState());
            level.setBlockAndUpdate(NOTE, Blocks.NOTE_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(GRATE.west(2).below(), Blocks.NOTE_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(GRATE, FloraFaunaRegistry.SLIME_GRATE.get().defaultBlockState().setValue(SlimeGrateBlock.AXIS, Direction.Axis.X));
            p.teleportTo(level, 3.9, 201.6, 2.6, Set.of(), 148, 18, true);
            p.setGameMode(GameType.CREATIVE);
            p.getInventory().clearContent();
            Slime slime = EntityType.SLIME.create(level, EntitySpawnReason.COMMAND);
            Playtest.require(slime != null, "Could not spawn slime");
            slime.setSize(2, true);
            slime.setUUID(JELLY);
            Vec3 start = Vec3.atBottomCenterOf(GRATE.west(2));
            slime.snapTo(start.x, start.y, start.z, -90, 0);
            level.addFreshEntity(slime);
            slime.setCustomName(Component.literal("Jelly"));
            MobSymbioteHelper.applyMobSymbioteLevel1(slime, level.getGameTime());
            id[0] = slime.getId();
        });
        t.awaitClient("Face the upright grate once the habitat is on the client", mc -> {
            if (mc.level.getEntity(id[0]) == null || !mc.level.getBlockState(GRATE).is(FloraFaunaRegistry.SLIME_GRATE)) return false;
            //? if <26.3 {
            mc.options.hideGui = true;
            //?} else {
            /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();*/
            //?}
            mc.player.lookAt(Anchor.EYES, Vec3.atCenterOf(GRATE));
            return true;
        });
        t.screenshot("slime-grate-habitat");
        t.server("Position Jelly on the near side for the note", p -> {
            Slime slime = (Slime)p.level().getEntity(id[0]);
            Vec3 near = Vec3.atBottomCenterOf(GRATE.west(2));
            slime.snapTo(near.x, near.y, near.z, -90, 0);
            slime.setDeltaMovement(Vec3.ZERO);
        });
        t.client("Use the far note block with an empty hand", mc -> {
            Vec3 hit = Vec3.atCenterOf(NOTE).add(0, 0.5, 0);
            mc.player.lookAt(Anchor.EYES, hit);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, NOTE, false));
        });
        t.awaitClient("Watch the synchronized split reach full separation", mc -> {
            Slime slime = (Slime)mc.level.getEntity(id[0]);
            mc.player.lookAt(Anchor.EYES, slime.getEyePosition());
            long shed = slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED);
            return shed >= 0 && mc.level.getGameTime() - shed >= 8 && mc.level.getGameTime() - shed < 32;
        });
        t.screenshot("slime-grate-split");
        t.awaitServer("Verify one harvest in the hopper and the original healthy slime", p -> {
            Slime slime = (Slime)p.level().getEntity(id[0]);
            return slime.getSize() == 2 && slime.getHealth() == 4 && ((HopperBlockEntity)p.level().getBlockEntity(GRATE.below())).countItem(Items.SLIME_BALL) == 1;
        });
        t.awaitClient("See Jelly whole after the temporary split", mc -> {
            Slime slime = (Slime)mc.level.getEntity(id[0]);
            mc.player.lookAt(Anchor.EYES, slime.getEyePosition());
            return mc.level.getGameTime() - slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED) > SlimeGrateFarm.SPLIT_TICKS;
        });
        t.screenshot("slime-grate-rejoined");
    }
}
