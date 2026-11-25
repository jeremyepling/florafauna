package net.j40climb.florafauna.platform.registry;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Keeps attachment definitions shared while NeoForge owns its native builder. */
public final class AttachmentBuilder {
    private AttachmentBuilder() {}
    public static <T> AttachmentType.Builder<T> builder(Supplier<T> initializer) {
        return AttachmentType.builder(initializer);
    }
}
