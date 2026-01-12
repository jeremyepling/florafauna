package net.j40climb.florafauna.noclip;

/**
 * Duck-typed interface that entities (specifically Players) implement via mixin
 * to support no-clip functionality.
 *
 * This interface is implemented by PlayerNoClipMixin which injects these methods
 * into the Player class at runtime.
 */
public interface ClippingEntity {

    /**
     * @return true if this entity is allowed to clip (creative mode check)
     */
    boolean canClip();

    /**
     * @return true if this entity is currently in clipping/noclip mode
     */
    boolean isClipping();

    /**
     * Sets the clipping state for this entity.
     * @param clipping true to enable noclip, false to disable
     */
    void setClipping(boolean clipping);

    /**
     * Utility method to cast any object to ClippingEntity.
     * Use with caution - only works on entities that have the mixin applied.
     */
    static ClippingEntity cast(Object entity) {
        return (ClippingEntity) entity;
    }
}
