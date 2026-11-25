package net.j40climb.florafauna.platform;

import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Loader bridge for persistent and synchronized entity attachments. */
public final class AttachmentAccess {
    private AttachmentAccess() {}
    public static <T> T get(Object target, Supplier<AttachmentType<T>> type) {
        return ((AttachmentTarget) target).getAttachedOrCreate(type.get());
    }
    public static <T> void set(Object target, Supplier<AttachmentType<T>> type, T value) {
        ((AttachmentTarget) target).setAttached(type.get(), value);
    }
    public static boolean has(Object target, Supplier<? extends AttachmentType<?>> type) {
        return ((AttachmentTarget) target).hasAttached(type.get());
    }
    public static <T> T remove(Object target, Supplier<AttachmentType<T>> type) {
        return ((AttachmentTarget) target).removeAttached(type.get());
    }
    public static <T> Optional<T> existing(Object target, Supplier<AttachmentType<T>> type) {
        return Optional.ofNullable(((AttachmentTarget) target).getAttached(type.get()));
    }
}
