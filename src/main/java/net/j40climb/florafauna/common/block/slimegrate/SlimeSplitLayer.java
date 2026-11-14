package net.j40climb.florafauna.common.block.slimegrate;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Both lobes belong to the same animal. Use the normal slime models, including the translucent skin. */
public final class SlimeSplitLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
    private static final ContextKey<Float> PROGRESS = new ContextKey<>(Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "slime_split"));
    private final SlimeOuterLayer skin;

    private SlimeSplitLayer(SlimeRenderer renderer, EntityModelSet models) {
        super(renderer);
        skin = new SlimeOuterLayer(renderer, models);
    }

    public static void addLayer(EntityRenderersEvent.AddLayers event) {
        if (event.getRenderer(EntityType.SLIME) instanceof SlimeRenderer renderer) {
            renderer.addLayer(new SlimeSplitLayer(renderer, event.getEntityModels()));
        }
    }

    public static void registerState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(SlimeRenderer.class, (Slime slime, SlimeRenderState state) -> {
            if (!slime.hasData(FloraFaunaRegistry.SLIME_LAST_SHED)) return;
            long shed = slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED);
            double age = slime.level().getGameTime() - shed + state.ageInTicks - slime.tickCount;
            if (shed < 0 || age < 0 || age >= SlimeGrateFarm.SPLIT_TICKS) return;
            float progress = (float) (age / SlimeGrateFarm.SPLIT_TICKS);
            float opening = opening(progress);
            state.scale *= 1f - 0.37f * opening;
            state.setRenderData(PROGRESS, progress);
        });
    }

    // Open quickly, hang for a beat, then draw the two faces back together.
    private static float opening(float progress) {
        return Math.clamp(Math.min(progress * 6, (1 - progress) * 4), 0, 1);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SlimeRenderState state, float yRot, float xRot) {
        Float progress = state.getRenderData(PROGRESS);
        if (progress == null || state.isInvisible) return;
        float spread = opening(progress);
        pose.pushPose();
        pose.translate(0.83 * spread, -0.17 * Math.abs(Math.sin(2 * Math.PI * progress)), 0.1 * spread);
        collector.submitModel(getParentModel(), state, pose, RenderTypes.entityCutout(SlimeRenderer.SLIME_LOCATION),
                light, LivingEntityRenderer.getOverlayCoords(state, 0), state.outlineColor, null);
        skin.submit(pose, collector, light, state, yRot, xRot);
        pose.popPose();
    }
}
