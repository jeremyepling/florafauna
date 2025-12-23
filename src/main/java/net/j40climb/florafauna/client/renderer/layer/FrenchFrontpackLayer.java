package net.j40climb.florafauna.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.client.model.FrenchFrontpackModel;
import net.j40climb.florafauna.client.renderer.FrontpackRenderStateManager;
import net.j40climb.florafauna.common.entity.client.frenchie.FrenchieVariant;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Frenchie frontpack on players who are carrying a Frenchie.
 * This layer is added to the player renderer and checks the FrenchFrontpackData attachment.
 * Uses variant-specific textures for fawn and brindle Frenchies.
 * The texture for this model is the same as the frenchie entity
 */
public class FrenchFrontpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final FrenchFrontpackModel frontpackModel;

    // Variant-specific textures for the entire frontpack model
    private static final ResourceLocation FAWN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID,
                    "textures/entity/frenchie/frenchie_fawn_texture.png");

    private static final ResourceLocation BRINDLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FloraFauna.MOD_ID,
                    "textures/entity/frenchie/frenchie_brindle_texture.png");

    public FrenchFrontpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);

        // Bake the frontpack model from registered layer definition
        this.frontpackModel = new FrenchFrontpackModel(
                modelSet.bakeLayer(FrenchFrontpackModel.LAYER_LOCATION)
        );
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                      int packedLight, AvatarRenderState renderState, float yRot, float xRot) {

        // Get the cached frontpack state for this player (using entity ID)
        FrontpackRenderStateManager.FrontpackRenderState frontpackState =
                FrontpackRenderStateManager.getState(renderState.id);

        // Don't render if player isn't carrying a Frenchie
        if (!frontpackState.hasFrontpack) {
            return;
        }

        // Don't render on invisible players
        if (renderState.isInvisible) {
            return;
        }

        // Select the appropriate texture based on the Frenchie variant
        ResourceLocation texture = frontpackState.variant == FrenchieVariant.BRINDLE
                ? BRINDLE_TEXTURE
                : FAWN_TEXTURE; // Default to fawn for any other variant

        // Position the frontpack model on the player's front
        poseStack.pushPose();

        // Align with the player's body part (this positions us at the player's torso)
        this.getParentModel().body.translateAndRotate(poseStack);

        // Adjust position: Y negative = up, X negative = left, Z positive = toward back
        poseStack.translate(0.0, -0.8, 0.0);

        // Render the frontpack model with the variant-specific texture
        renderColoredCutoutModel(
                this.frontpackModel,
                texture,
                poseStack,
                nodeCollector,
                packedLight,
                renderState,
                0xFFFFFFFF, // White (no color tint)
                0 // No outline
        );

        poseStack.popPose();
    }
}
