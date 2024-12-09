package net.j40climb.florafauna.event;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.items.interfaces.DiggerTool;
import net.j40climb.florafauna.component.DataComponentTypes;
import net.j40climb.florafauna.item.ModItems;
import net.j40climb.florafauna.item.custom.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class Events {
    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    @SubscribeEvent
    public static void onHammerUsage(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer) {
            BlockPos initialBlockPos = event.getPos();
            if(HARVESTED_BLOCKS.contains(initialBlockPos)) {
                return;
            }

            int range = Objects.requireNonNull(mainHandItem.get(DataComponentTypes.HIT_RANGE)).range();

            // Only do a hammer mine if the block being mined is the correct tool
            if (hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(initialBlockPos))) {
                for(BlockPos pos : DiggerTool.getBlocksToBeBroken(range, initialBlockPos, serverPlayer)) {
                    // Don't mine the position that was just mined, or a different type of block
                    if(pos == initialBlockPos || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))) {
                        continue;
                    }

                    HARVESTED_BLOCKS.add(pos);
                    // Call destroy which runs this event again so we need the fast return above in if(HARVESTED_BLOCKS.contains(initialBlockPos))
                    // If not, it would keep deleting and not move through the set, creating an infinite loop
                    // https://courses.kaupenjoe.net/courses/modding-by-kaupenjoe-neoforge-modding-for-minecraft-1-21-x/lectures/55341862 at 10min in
                    serverPlayer.gameMode.destroyBlock(pos);
                    HARVESTED_BLOCKS.remove(pos);
                }
            }


        }
    }

    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent.Pre event) {
        if(event.getEntity() instanceof Sheep sheep) {
            if(event.getSource().getDirectEntity() instanceof Player player) {
                if(player.getMainHandItem().getItem() == ModItems.METAL_DETECTOR.get()) {
                    player.sendSystemMessage(Component.literal(player.getName().getString() + " just hit a freaking Sheep with a Metal Detector!"));
                }
                if(player.getMainHandItem().getItem() == ModItems.TOMATO.get()) {
                    player.sendSystemMessage(Component.literal(player.getName().getString() + " just hit a freaking Sheep with a tomato!"));
                    sheep.addEffect(new MobEffectInstance(MobEffects.JUMP, 600, 50));
                    player.getMainHandItem().shrink(1); // Remove item from main hand
                }
                if(player.getMainHandItem().getItem() == Items.END_ROD) {
                    player.sendSystemMessage(Component.literal(player.getName().getString() + " just hit a freaking Sheep with AN END ROD WHAT?!!"));
                    sheep.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 50));
                    player.getMainHandItem().shrink(1); // Remove item from main hand
                }
            }
        }
    }
}
