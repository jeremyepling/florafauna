package net.j40climb.florafauna.test.fabric;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.j40climb.florafauna.test.SimpleGameTestInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Registers the same shared functions in Fabric's built-in and dynamic test registries. */
public final class RegisterGameTestsEvent {
    private final Registry<TestEnvironmentDefinition<?>> environments;
    private final Registry<GameTestInstance> tests;

    private RegisterGameTestsEvent(Registry<TestEnvironmentDefinition<?>> environments, Registry<GameTestInstance> tests) {
        this.environments = environments;
        this.tests = tests;
    }

    public static void initialize(Consumer<RegisterGameTestsEvent> register) {
        register.accept(new RegisterGameTestsEvent(null, null));
        DynamicRegistrySetupCallback.EVENT.register(registries ->
                registries.getOptional(Registries.TEST_INSTANCE).ifPresent(instances ->
                        register.accept(new RegisterGameTestsEvent(
                                registries.getOptional(Registries.TEST_ENVIRONMENT).orElseThrow(), instances))));
    }

    public Holder<TestEnvironmentDefinition<?>> registerEnvironment(Identifier id, TestEnvironmentDefinition<?> environment) {
        return environments == null ? Holder.direct(environment) : Registry.registerForHolder(environments, id, environment);
    }

    public void registerTest(Identifier id, SimpleGameTestInstance test) {
        if (tests == null) {
            Registry.register(BuiltInRegistries.TEST_FUNCTION, id, test::run);
            return;
        }
        var definition = test.definition();
        var structure = definition.structure().equals(Identifier.withDefaultNamespace("empty"))
                ? Identifier.fromNamespaceAndPath("fabric-gametest-api-v1", "empty") : definition.structure();
        Registry.register(tests, id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                new TestData<>(definition.environment(), structure, definition.maxTicks(), definition.setupTicks(), definition.required())));
    }
}
