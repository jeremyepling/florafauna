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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class SpiderSilkEvents {
    @SubscribeEvent
    public static void onSpiderTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Spider spider) || spider.getType() != EntityType.SPIDER
                || spider.level().isClientSide() || spider.tickCount % 20 != 1 || !MobSymbioteHelper.hasMobSymbiote(spider)) return;
        if (!spider.isPersistenceRequired()) spider.setPersistenceRequired();
        if (spider.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof SpinSilkGoal)) {
            spider.goalSelector.addGoal(1, new SpinSilkGoal(spider));
        }
    }

    // The vanilla dispenser applies the returned state, charges its shears and emits the shear game event.
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

    private SpiderSilkEvents() {}
}
