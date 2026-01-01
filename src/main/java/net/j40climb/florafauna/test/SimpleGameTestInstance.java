package net.j40climb.florafauna.test;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

/**
 * Simple implementation of GameTestInstance for function-based tests.
 * Wraps a Consumer<GameTestHelper> to create a runnable test.
 */
public class SimpleGameTestInstance extends GameTestInstance {

    private final Consumer<GameTestHelper> testFunction;

    public SimpleGameTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> testFunction) {
        super(info);
        this.testFunction = testFunction;
    }

    @Override
    public void run(GameTestHelper helper) {
        testFunction.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        //throw new UnsupportedOperationException("SimpleGameTestInstance is not codec-based");
        // Return a no-op codec so registry sync can serialize/deserialize without throwing
        return MapCodec.unit(this);
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Simple Function Test");
    }
}
