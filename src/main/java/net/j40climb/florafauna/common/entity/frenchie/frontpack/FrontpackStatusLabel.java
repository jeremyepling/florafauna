package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import com.google.common.reflect.TypeToken;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieStatusIcon;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID, value = Dist.CLIENT)
public final class FrontpackStatusLabel {
    private static final ContextKey<Component> LABEL = new ContextKey<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frontpack_status"));

    @SubscribeEvent
    public static void extractLabel(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<AvatarRenderer<?>>() {}, (avatar, state) -> {
            var pack = avatar.getExistingDataOrNull(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
            if (pack == null || !pack.hasCarriedFrenchie() || state.isInvisible || !Minecraft.renderNames()) return;
            var warning = avatar.getExistingDataOrNull(FloraFaunaRegistry.FRENCHIE_WARNING);
            var appearance = pack.frenchieNBT();
            FrenchieStatusIcon icon;
            if (warning != null && warning.visible(avatar.level().getGameTime())) icon = FrenchieStatusIcon.CREEPER;
            else if (appearance.contains("FrenchieParcel")) icon = FrenchieStatusIcon.PARCEL;
            else if (appearance.contains("FrenchieDeathTrail")) icon = FrenchieStatusIcon.COMPASS;
            else return;
            var name = appearance.read("CustomName", ComponentSerialization.CODEC).orElse(null);
            state.setRenderData(LABEL, icon.label(name));
        });
    }

    @SubscribeEvent
    public static void drawLabel(RenderPlayerEvent.Post<?> event) {
        var state = event.getRenderState();
        var label = state.getRenderData(LABEL);
        if (label == null || state.distanceToCameraSq >= 4096) return;
        // Vanilla adds half a block above this attachment before billboarding the text.
        var attachment = Vec3.directionFromRotation(0, state.bodyRot).scale(0.46).add(0, state.eyeHeight - 0.45, 0);
        var camera = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        event.getSubmitNodeCollector().submitNameTag(event.getPoseStack(), attachment, 0, label, !state.isDiscrete,
                state.lightCoords, state.distanceToCameraSq, camera);
    }
}
