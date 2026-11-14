package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

final class SpiderSilkPlaytest {
    private static final BlockPos PEN = new BlockPos(0, 201, 0);

    static void steps(Playtest t, int phase) {
        Playtest.require(phase == 1, "Silk scenario runs in one session");
        int[] id = new int[1];
        t.server("Build an illuminated spider habitat and equip a Mob Symbiote", p -> {
            var level = p.level().getServer().overworld();
            for (BlockPos pos : BlockPos.betweenClosed(PEN.offset(-4,-1,-3), PEN.offset(4,3,4))) {
                level.setBlockAndUpdate(pos, pos.getY() < PEN.getY() ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            for (int x : new int[]{-2, 2}) {
                level.setBlockAndUpdate(PEN.offset(x,0,0), Blocks.OAK_FENCE.defaultBlockState());
                level.setBlockAndUpdate(PEN.offset(x,1,0), Blocks.OAK_FENCE.defaultBlockState());
            }
            level.setBlockAndUpdate(PEN.west(2).above(2), Blocks.SPRUCE_PLANKS.defaultBlockState());
            p.teleportTo(level, 0.5, 201, 3.6, Set.of(), 180, 5, true);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FloraFaunaRegistry.MOB_SYMBIOTE.get()));
            p.inventoryMenu.broadcastChanges();
            Spider spider = EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
            Playtest.require(spider != null, "Spider spawn failed");
            spider.snapTo(Vec3.atBottomCenterOf(PEN));
            spider.setCustomName(Component.literal("Charlotte"));
            level.addFreshEntity(spider);
            id[0] = spider.getId();
        });
        t.awaitClient("Receive spider and starter item", mc -> mc.level.getEntity(id[0]) instanceof Spider
                && mc.player.getMainHandItem().is(FloraFaunaRegistry.MOB_SYMBIOTE.get()));
        t.client("Bond the spider through the interaction packet", mc -> {
            var spider = mc.level.getEntity(id[0]);
            Playtest.require(mc.gameMode.interact(mc.player, spider, new EntityHitResult(spider), InteractionHand.MAIN_HAND).consumesAction(), "Bond rejected");
        });
        t.awaitClient("See three silk segments and the upside-down spider render state", mc -> {
            if (!(mc.level.getEntity(id[0]) instanceof Spider spider) || spider.getY() < PEN.getY() + 0.75
                    || !mc.level.getBlockState(PEN.east().above()).is(FloraFaunaRegistry.SILK_LINE.get())) return false;
            var render = mc.getEntityRenderDispatcher().getRenderer(spider).createRenderState(spider, 1F);
            return render instanceof LivingEntityRenderState living && living.isUpsideDown;
        });
        t.screenshot("spider-on-silk");
        t.server("Equip shears", p -> {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive shears", mc -> mc.player.getMainHandItem().is(Items.SHEARS));
        t.client("Shear the exposed end of the silk", mc -> {
            BlockPos pos = PEN.east().above();
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.2, 0), Direction.SOUTH, pos, false);
            Playtest.require(mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit).consumesAction(), "Shearing rejected");
        });
        t.awaitServer("Get one string, one use of shears and a living upright spider", p -> {
            var spider = (Spider) p.level().getEntity(id[0]);
            int drops = p.level().getEntitiesOfClass(ItemEntity.class, new AABB(PEN).inflate(4), e -> e.getItem().is(Items.STRING))
                    .stream().mapToInt(i -> i.getItem().getCount()).sum();
            return spider != null && spider.isAlive() && !spider.getData(FloraFaunaRegistry.SILK_HANGING)
                    && p.level().isEmptyBlock(PEN.above()) && drops + p.getInventory().countItem(Items.STRING) == 1
                    && p.getMainHandItem().getDamageValue() == 1;
        });
    }
}
