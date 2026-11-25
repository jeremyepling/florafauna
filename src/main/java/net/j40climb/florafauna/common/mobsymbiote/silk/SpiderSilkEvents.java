package net.j40climb.florafauna.common.mobsymbiote.silk;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.minecraft.server.level.ServerLevel;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.state.BlockState;
//? if neoforge {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if neoforge {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if neoforge {
import net.neoforged.neoforge.common.ItemAbilities;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.level.BlockEvent;
//?}
//? if neoforge {
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

//? if neoforge {
@EventBusSubscriber(modid = FloraFauna.MOD_ID)
//?}
public final class SpiderSilkEvents {
    //? if neoforge {
    @SubscribeEvent
    public static void onSpiderTick(EntityTickEvent.Post event) {
        onSpiderTick(event.getEntity());
    }
    //?}

    public static void onSpiderTick(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof Spider spider) || spider.getType() != EntityType.SPIDER
                || spider.level().isClientSide() || spider.tickCount % 20 != 1 || !MobSymbioteHelper.hasMobSymbiote(spider)) return;
        if (!spider.isPersistenceRequired()) spider.setPersistenceRequired();
        if (spider.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof SpinSilkGoal)) {
            spider.goalSelector.addGoal(1, new SpinSilkGoal(spider));
        }
    }

    // The vanilla dispenser applies the returned state, charges its shears and emits the shear game event.
    //? if neoforge {
    @SubscribeEvent
    public static void onDispenserShear(BlockEvent.BlockToolModificationEvent event) {
        if (event.getPlayer() != null || event.getItemAbility() != ItemAbilities.SHEARS_HARVEST ||
                !(event.getLevel() instanceof ServerLevel level)) return;
        BlockState silk = level.getBlockState(event.getPos());
        if (SilkLineBlock.canCut(level, event.getPos(), silk)) {
            event.setFinalState(silk.setValue(SilkLineBlock.CUT, true));
            if (!event.isSimulated()) level.scheduleTick(event.getPos(), silk.getBlock(), 1);
        }
    }
    //?}


    private SpiderSilkEvents() {}
}
