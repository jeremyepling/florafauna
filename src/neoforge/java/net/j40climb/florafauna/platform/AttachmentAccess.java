package net.j40climb.florafauna.platform;

import java.util.Optional;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Loader bridge for persistent and synchronized entity attachments. */
public final class AttachmentAccess {
    private AttachmentAccess() {}
    public static <T> T get(Object target, Supplier<AttachmentType<T>> type) {
        return ((IAttachmentHolder) target).getData(type);
    }
    public static <T> void set(Object target, Supplier<AttachmentType<T>> type, T value) {
        ((IAttachmentHolder) target).setData(type, value);
    }
    public static boolean has(Object target, Supplier<? extends AttachmentType<?>> type) {
        return ((IAttachmentHolder) target).hasData(type.get());
    }
    public static <T> T remove(Object target, Supplier<AttachmentType<T>> type) {
        return ((IAttachmentHolder) target).removeData(type);
    }
    public static <T> Optional<T> existing(Object target, Supplier<AttachmentType<T>> type) {
        return has(target, type) ? Optional.of(get(target, type)) : Optional.empty();
    }
}
