package net.j40climb.florafauna.common.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.BlockBreakUtils;
import net.j40climb.florafauna.common.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public class BlockEvents {

    @SubscribeEvent
    public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = player.level();
        GameType type = player.getAbilities().instabuild ? GameType.CREATIVE : GameType.SURVIVAL;
        ItemStack mainHandItem = player.getMainHandItem();
        BlockPos initialBlockPos = event.getPos();

        // It's on the server and the action isn't restricted, like Spectator mode
        if (player instanceof ServerPlayer serverPlayer &&
                !serverPlayer.blockActionRestricted(level, initialBlockPos, type)) {
            BlockBreakUtils.breakWithMiningMode(mainHandItem, initialBlockPos, serverPlayer, level);
        }
    }

    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent.Pre event) {
        if(event.getEntity() instanceof Sheep sheep) {
            if(event.getSource().getDirectEntity() instanceof Player player) {
                if(player.getMainHandItem().getItem() == ModItems.TOMATO.get()) {
                    player.displayClientMessage(Component.literal(player.getName().getString() + " just hit a freaking Sheep with a tomato!"), false);
                    sheep.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 600, 50));
                    player.getMainHandItem().shrink(1); // Remove item from main hand
                }
            }
        }
    }
}
