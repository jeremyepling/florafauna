package net.j40climb.florafauna.common.entity.frontpack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieVariant;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders the Frenchie frontpack on players who are carrying a Frenchie.
 * This layer is added to the player renderer and checks the FrenchFrontpackData attachment.
 * Uses variant-specific textures for fawn and brindle Frenchies.
 * The texture for this model is the same as the frenchie entity
 */
public class FrontpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

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
        Identifier texture = frontpackState.variant == FrenchieVariant.BRINDLE
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

    /**
     * Client-side manager for caching frontpack render state data extracted from player attachments.
     * This bridges the gap between the attachment system and the render layer,
     * since AvatarRenderState doesn't contain a direct player reference.
     */
    public static class FrontpackRenderStateManager {
    
        // Use entity ID as key since AvatarRenderState has 'id' field
        private static final Map<Integer, FrontpackRenderState> FRONTPACK_STATES = new HashMap<>();
    
        /**
         * Extracts frontpack data from a player's FrenchFrontpackData attachment
         * and caches it for rendering.
         */
        public static void extractFromPlayer(Player player) {
            FrontpackData data = player.getData(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
    
            FrontpackRenderState state = new FrontpackRenderState();
            state.hasFrontpack = data.hasCarriedFrenchie();
    
            if (state.hasFrontpack && data.frenchieNBT() != null) {
                // Extract variant ID from NBT (matches how FrenchieEntity stores it)
                // getInt returns Optional<Integer> in newer versions
                int variantId = data.frenchieNBT().getInt("Variant").orElse(0);
                state.variant = FrenchieVariant.byId(variantId);
            } else {
                // Default to FAWN if no valid data
                state.variant = FrenchieVariant.FAWN;
            }
    
            FRONTPACK_STATES.put(player.getId(), state);
        }
    
        /**
         * Gets the cached frontpack render state for a player by entity ID.
         * Returns a default empty state if no data is cached.
         */
        public static FrontpackRenderState getState(int entityId) {
            return FRONTPACK_STATES.getOrDefault(entityId, new FrontpackRenderState());
        }
    
        /**
         * Simple data holder for frontpack render state.
         */
        public static class FrontpackRenderState {
            public boolean hasFrontpack = false;
            public FrenchieVariant variant = FrenchieVariant.FAWN;
        }
    }
}
