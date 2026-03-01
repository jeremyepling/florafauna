package net.j40climb.florafauna.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state for ThrownItemEntity.
 * Stores the item appearance and rotation for rendering.
 */
public class ThrownItemRenderState extends EntityRenderState {
    public float xRot;
    public float yRot;
    public final ItemStackRenderState item = new ItemStackRenderState();
    public boolean isReturning;
}
