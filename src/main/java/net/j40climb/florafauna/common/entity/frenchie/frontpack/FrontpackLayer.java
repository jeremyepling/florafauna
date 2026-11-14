package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieVariant;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * Renders the Frenchie frontpack on players who are carrying a Frenchie.
 * This layer is added to the player renderer and checks the FrenchFrontpackData attachment.
 * Uses variant-specific textures for fawn and brindle Frenchies.
 * The texture for this model is the same as the frenchie entity
 */
public class FrontpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    static final ContextKey<FrenchieVariant> CARRIED_COAT = new ContextKey<>(
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frontpack_coat"));

    private final FrontpackModel frontpackModel;

    // Variant-specific textures for the entire frontpack model
    private static final Identifier FAWN_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID,
                    "textures/entity/frenchie/frenchie_fawn_texture.png");

    private static final Identifier BRINDLE_TEXTURE =
            Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID,
                    "textures/entity/frenchie/frenchie_brindle_texture.png");

    public FrontpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);

        // Bake the frontpack model from registered layer definition
        this.frontpackModel = new FrontpackModel(
                modelSet.bakeLayer(FrontpackModel.LAYER_LOCATION)
        );
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                      int packedLight, AvatarRenderState renderState, float yRot, float xRot) {

        FrenchieVariant coat = renderState.getRenderData(CARRIED_COAT);
        if (coat == null || renderState.isInvisible) return;

        Identifier texture = coat == FrenchieVariant.BRINDLE ? BRINDLE_TEXTURE : FAWN_TEXTURE;

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
