package net.j40climb.florafauna.platform.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** The shared config schema backed by TOML on Fabric. Keeps existing keys and bounds. */
public final class ModConfigSpec {
    private final List<Value<?>> values;
    private CommentedFileConfig watchedConfig;

    private ModConfigSpec(List<Value<?>> values) { this.values = List.copyOf(values); }

    public synchronized void watch(Path path, Runnable onReload) {
        load(path);
        if (watchedConfig != null) watchedConfig.close();
        watchedConfig = CommentedFileConfig.builder(path).sync().autoreload().onAutoReload(() -> {
            try {
                load(path);
                onReload.run();
            } catch (RuntimeException error) {
                LogUtils.getLogger().error("Cannot reload FloraFauna configuration {}", path, error);
            }
        }).build();
        watchedConfig.load();
    }

    public void load(Path path) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (var config = CommentedFileConfig.builder(path).sync().build()) {
                config.load();
                boolean changed = !Files.exists(path);
                for (var value : values) {
                    Object stored = config.get(value.path);
                    if (!value.accept(stored)) {
                        if (stored != null) LogUtils.getLogger().warn("Invalid FloraFauna config {}={}, using {}",
                                String.join(".", value.path), stored, value.defaultValue);
                        config.set(value.path, value.defaultValue);
                        changed = true;
                    }
                    if (!value.comment.isEmpty()) config.setComment(value.path, value.comment);
                }
                if (changed) config.save();
            }
        } catch (RuntimeException | java.io.IOException error) {
            throw new IllegalStateException("Cannot load FloraFauna configuration " + path, error);
        }
    }

    private abstract static class Value<T> {
        final List<String> path;
        final T defaultValue;
        final String comment;
        final Function<Object, T> parser;
        private volatile T current;

        Value(List<String> path, T defaultValue, String comment, Function<Object, T> parser) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.current = defaultValue;
            this.comment = comment;
            this.parser = parser;
        }

        boolean accept(Object stored) {
            T parsed = parser.apply(stored);
            current = parsed == null ? defaultValue : parsed;
            return parsed != null;
        }

        public T get() { return current; }
    }

    public static final class IntValue extends Value<Integer> {
        IntValue(List<String> path, int value, String comment, int minimum, int maximum) {
            super(path, value, comment, stored -> {
                if (!(stored instanceof Number number)) return null;
                double numeric = number.doubleValue();
                return Double.isFinite(numeric) && numeric >= minimum && numeric <= maximum
                        && numeric == Math.rint(numeric) ? (int) numeric : null;
            });
        }
    }

    public static final class DoubleValue extends Value<Double> {
        DoubleValue(List<String> path, double value, String comment, double minimum, double maximum) {
            super(path, value, comment, stored -> {
                if (!(stored instanceof Number number)) return null;
                double numeric = number.doubleValue();
                return Double.isFinite(numeric) && numeric >= minimum && numeric <= maximum ? numeric : null;
            });
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        BooleanValue(List<String> path, boolean value, String comment) {
            super(path, value, comment, stored -> stored instanceof Boolean bool ? bool : null);
        }
    }

    public static final class Builder {
        private final List<String> sections = new ArrayList<>();
        private final List<Value<?>> values = new ArrayList<>();
        private String comment = "";

        public Builder comment(String... lines) { comment = String.join("\n", lines); return this; }
        public Builder push(String section) { sections.add(section); comment = ""; return this; }
        public Builder pop() { sections.removeLast(); return this; }

        private List<String> path(String key) {
            var path = new ArrayList<>(sections);
            path.add(key);
            return List.copyOf(path);
        }

        private <T extends Value<?>> T add(T value) { values.add(value); comment = ""; return value; }
        public IntValue defineInRange(String key, int value, int min, int max) {
            return add(new IntValue(path(key), value, comment, min, max));
        }
        public DoubleValue defineInRange(String key, double value, double min, double max) {
            return add(new DoubleValue(path(key), value, comment, min, max));
        }
        public BooleanValue define(String key, boolean value) {
            return add(new BooleanValue(path(key), value, comment));
        }
        public ModConfigSpec build() { return new ModConfigSpec(values); }
    }
}
