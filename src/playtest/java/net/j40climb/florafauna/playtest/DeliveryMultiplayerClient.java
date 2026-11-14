package net.j40climb.florafauna.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.DeliveryScreen;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

import java.nio.file.Files;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class DeliveryMultiplayerClient {
    private static final String ROLE = System.getProperty("florafauna.deliveryTest.client");
    private enum Step { IDLE, USE_BUNDLE, CHOOSE, PREVIEW, CONFIRM, FRAME, CAPTURE, ACCEPT, EXIT }
    private static Step step = Step.IDLE;
    private static volatile boolean heardBark;
    private static int frames;

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        if (ROLE == null) return;
        switch (event.getMessage().getString()) {
            case DeliveryMultiplayerServer.SEND -> step = Step.USE_BUNDLE;
            case DeliveryMultiplayerServer.ACCEPT -> step = Step.FRAME;
            case DeliveryMultiplayerServer.DONE -> step = Step.EXIT;
        }
    }

    @SubscribeEvent
    public static void sound(PlaySoundSourceEvent event) {
        if (!"Visitor".equals(ROLE)) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var expected = mc.level.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().ambientSound().value().location();
        var sound = event.getSound();
        if (sound.getIdentifier().equals(expected)
                && mc.player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < 25) {
            heardBark = true;
            LogUtils.getLogger().info("Recipient client received the courier's arrival bark at its landing point");
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ROLE == null) return;
        var mc = Minecraft.getInstance();
        if (step == Step.EXIT) {
            mc.stop();
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null || step == Step.IDLE) return;
        var dog = (FrenchieEntity) mc.level.getEntity(DeliveryMultiplayerServer.DOG);
        switch (step) {
            case USE_BUNDLE -> {
                if (!ItemStack.matches(mc.player.getMainHandItem(), CourierParcel.parcel()) || !aim(mc, dog)) return;
                KeyMapping.click(mc.options.keyUse.getKey());
                step = Step.CHOOSE;
            }
            case CHOOSE -> {
                if (DeliveryPlaytest.pressLabel(mc, "FFVisitor")) step = Step.PREVIEW;
            }
            case PREVIEW -> {
                if (!(mc.screen instanceof DeliveryScreen) || !mc.screen.getNarrationMessage().getString().contains("FFVisitor")
                        || !mc.screen.getNarrationMessage().getString().contains(Component.translatable("dimension.minecraft.overworld").getString())
                        || !mc.screen.getNarrationMessage().getString().contains(DeliveryPlaytest.eta(DeliveryMultiplayerServer.ETA))
                        || ++frames < 5) return;
                Screenshot.grab(mc.gameDirectory, "courier-envelope.png", mc.getMainRenderTarget(), 1, ignored -> {});
                step = Step.CONFIRM;
            }
            case CONFIRM -> {
                if (!Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots/courier-envelope.png"))) return;
                if (DeliveryPlaytest.pressLabel(mc, Component.translatable("frenchie.florafauna.delivery.confirm").getString())) {
                    LogUtils.getLogger().info("Owner confirmed delivery to FFVisitor at the displayed {} s ETA", DeliveryMultiplayerServer.ETA);
                    step = Step.IDLE;
                }
            }
            case FRAME -> {
                if (dog != null) mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
                if (!heardBark || !aim(mc, dog) || !mc.player.getMainHandItem().isEmpty()
                        || !CompanionIconsPlaytest.shows(mc, dog, "parcel") || ++frames < 10) return;
                step = Step.CAPTURE;
            }
            case CAPTURE -> {
                Screenshot.grab(mc.gameDirectory, "courier-arrival.png", mc.getMainRenderTarget(), 1, ignored -> {});
                step = Step.ACCEPT;
            }
            case ACCEPT -> {
                if (!Files.isRegularFile(mc.gameDirectory.toPath().resolve("screenshots/courier-arrival.png")) || !aim(mc, dog)) return;
                KeyMapping.click(mc.options.keyUse.getKey());
                LogUtils.getLogger().info("Recipient right-clicked the visible Frenchie to accept the parcel");
                step = Step.IDLE;
            }
        }
    }

    private static boolean aim(Minecraft mc, FrenchieEntity dog) {
        if (dog == null || mc.screen != null) return false;
        mc.player.lookAt(Anchor.EYES, dog.getEyePosition());
        return mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == dog;
    }
}
