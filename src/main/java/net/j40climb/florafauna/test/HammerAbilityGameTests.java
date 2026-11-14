package net.j40climb.florafauna.test;

import com.mojang.authlib.GameProfile;
import net.j40climb.florafauna.common.item.abilities.data.*;
import net.j40climb.florafauna.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna.common.item.projectile.ThrownItemEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.UUID;

final class HammerAbilityGameTests {
    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        FloraFaunaGameTests.registerTest(event, env, "hammer_fresh_and_configured_loot", HammerAbilityGameTests::checkConfigurations);
        FloraFaunaGameTests.registerTest(event, env, "hammer_throw_single_item_and_reload", HammerAbilityGameTests::checkThrow);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "hammer-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        player.setOnGround(true);
        return player;
    }

    private static void checkConfigurations(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack hammer = new ItemStack(FloraFaunaRegistry.HAMMER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        var fortune = helper.getLevel().registryAccess().holderOrThrow(Enchantments.FORTUNE);
        var silk = helper.getLevel().registryAccess().holderOrThrow(Enchantments.SILK_TOUCH);
        helper.assertTrue(hammer.getEnchantments().getLevel(fortune) == 3, "Fresh Hammer must already have Fortune III");
        var stone = Blocks.STONE.defaultBlockState();
        var pos = player.blockPosition();
        var speedWithoutConfig = hammer.copy();
        speedWithoutConfig.remove(FloraFaunaRegistry.TOOL_CONFIG);
        player.setItemInHand(InteractionHand.MAIN_HAND, speedWithoutConfig);
        float originalSpeed = player.getDestroySpeed(stone, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        for (MiningShape shape : MiningShape.values()) {
            var mode = new MiningModeData(shape, shape.getRadius(), 64, true);
            hammer.set(FloraFaunaRegistry.MULTI_BLOCK_MINING, mode);
            for (MiningSpeed speed : MiningSpeed.values()) {
                for (boolean useFortune : new boolean[]{false, true}) {
                    ToolConfig config = new ToolConfig(useFortune, !useFortune, speed);
                    UpdateToolConfigPayload.applyToHeldItem(player, config);
                    String description = shape + " / " + speed + " / " + config.getEnchantmentType();
                    helper.assertTrue(config.equals(hammer.get(FloraFaunaRegistry.TOOL_CONFIG)), description);
                    helper.assertTrue(mode.equals(hammer.get(FloraFaunaRegistry.MULTI_BLOCK_MINING)), "Config changed shape: " + description);
                    helper.assertTrue(hammer.getEnchantments().getLevel(fortune) == (useFortune ? 3 : 0)
                            && hammer.getEnchantments().getLevel(silk) == (useFortune ? 0 : 1), description);
                    float expected = switch (speed) {
                        case STANDARD -> originalSpeed;
                        case EFFICIENCY -> 35;
                        case INSTABREAK -> 100;
                    };
                    helper.assertTrue(player.getDestroySpeed(stone, pos) == expected, "Effective speed: " + description);
                }
            }
        }
        helper.succeed();
    }

    private static void checkThrow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack stack = new ItemStack(Items.STICK, 16);
        stack.set(FloraFaunaRegistry.THROWABLE_ABILITY, ThrowableAbilityData.DEFAULT);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Returning stick"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND);
        var projectiles = helper.getLevel().getEntities(FloraFaunaRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
        helper.assertTrue(projectiles.size() == 1 && stack.getCount() == 15, "Exactly one item must leave the hand");
        ThrownItemEntity thrown = projectiles.getFirst();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        thrown.saveWithoutId(output);
        thrown.discard();
        var restored = FloraFaunaRegistry.THROWN_ITEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(restored.getThrownItem().getCount() == 1
                && ItemStack.isSameItemSameComponents(stack, restored.getThrownItem()), "Reload lost item count or components");
        restored.setOwner(player);
        restored.setPos(player.position().add(40, 0, 0));
        restored.tick();
        helper.assertTrue(restored.isReturning() && restored.isNoPhysics(), "Range alone must start homing");
        restored.setPos(player.position());
        restored.tick();
        helper.assertTrue(restored.isRemoved() && stack.getCount() == 16, "Return must conserve the stack");
        helper.succeed();
    }
}
