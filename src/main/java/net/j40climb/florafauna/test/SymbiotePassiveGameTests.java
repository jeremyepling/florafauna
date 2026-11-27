package net.j40climb.florafauna.test;

import net.j40climb.florafauna.common.symbiote.abilities.SymbioteAbilityEvents;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class SymbiotePassiveGameTests {
    private static final BlockPos FLOOR = new BlockPos(1, 0, 1);
    private static final PlayerSymbioteData BOND = PlayerSymbioteData.DEFAULT
            .withSymbioteState(SymbioteState.BONDED_ACTIVE).withSpeed(true).withJumpBoost(2);

    private SymbiotePassiveGameTests() {}

    static void potionPrecedence(GameTestHelper helper) {
        ServerPlayer player = FloraFaunaGameTests.createTestPlayer(helper);
        putOnTestFloor(helper, player);
        double baseSpeed = speed(player);
        double baseSafeFall = safeFall(player);
        double baseJump = jump(player);

        setBond(player, BOND.withJumpBoost(1));
        assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.1, baseSafeFall + 1, "active bond with Jump I");
        assertNoPotions(helper, player);
        setBond(player, BOND);
        assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.2, baseSafeFall + 2, "active bond with Jump II");
        assertNoPotions(helper, player);

        for (int level = 1; level <= 3; level++) {
            var speed = new MobEffectInstance(MobEffects.SPEED, 80, level - 1);
            var jump = new MobEffectInstance(MobEffects.JUMP_BOOST, 80, level - 1);
            player.addEffect(speed);
            player.addEffect(jump);
            SymbioteAbilityEvents.onPlayerTick(player);
            int effectiveLevel = Math.max(2, level);
            assertBoosts(helper, player, baseSpeed * (1 + 0.2 * effectiveLevel),
                    baseJump + 0.1 * effectiveLevel, baseSafeFall + effectiveLevel, "bond with level " + level + " potions");
            assertPotions(helper, player, speed, jump);
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.JUMP_BOOST);
            SymbioteAbilityEvents.onPlayerTick(player);
            assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.2, baseSafeFall + 2, "potions removed");
            assertNoPotions(helper, player);
        }

        var speed = new MobEffectInstance(MobEffects.SPEED, 80);
        var jump = new MobEffectInstance(MobEffects.JUMP_BOOST, 80);
        player.addEffect(speed);
        player.addEffect(jump);
        setBond(player, BOND.withSymbioteState(SymbioteState.BONDED_WEAKENED));
        assertBoosts(helper, player, baseSpeed * 1.2, baseJump + 0.1, baseSafeFall + 1, "weakened bond with potions");
        assertPotions(helper, player, speed, jump);

        setBond(player, BOND);
        assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.2, baseSafeFall + 2, "restored bond with potions");
        setBond(player, BOND.withSpeed(false).withJumpBoost(0));
        assertBoosts(helper, player, baseSpeed * 1.2, baseJump + 0.1, baseSafeFall + 1, "grants disabled with potions");
        assertPotions(helper, player, speed, jump);

        setBond(player, BOND);
        setBond(player, PlayerSymbioteData.DEFAULT);
        assertBoosts(helper, player, baseSpeed * 1.2, baseJump + 0.1, baseSafeFall + 1, "unbound with potions");
        assertPotions(helper, player, speed, jump);
        player.removeEffect(MobEffects.SPEED);
        player.removeEffect(MobEffects.JUMP_BOOST);
        SymbioteAbilityEvents.onPlayerTick(player);
        assertBoosts(helper, player, baseSpeed, baseJump, baseSafeFall, "unbound without potions");
        assertNoPotions(helper, player);

        helper.setBlock(FLOOR, Blocks.HONEY_BLOCK);
        double honeyJump = jump(player);
        helper.assertTrue(honeyJump < baseJump, "Fixture must apply the honey jump factor");
        setBond(player, BOND);
        assertNear(helper, honeyJump + 0.2, jump(player), "honey takeoff still receives the full Jump II boost");
        helper.succeed();
    }

    static void potionExpiry(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        putOnTestFloor(helper, player);
        double baseSpeed = speed(player);
        double baseSafeFall = safeFall(player);
        double baseJump = jump(player);
        setBond(player, BOND);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 10, 2));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 10, 2));
        SymbioteAbilityEvents.onPlayerTick(player);
        assertBoosts(helper, player, baseSpeed * 1.6, baseJump + 0.3, baseSafeFall + 3, "short level III potions");
        player.setDeltaMovement(Vec3.ZERO);

        helper.runAfterDelay(20, () -> {
            try {
                assertNoPotions(helper, player);
                assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.2, baseSafeFall + 2, "short potions expired");
                helper.succeed();
            } finally {
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }

    private static void putOnTestFloor(GameTestHelper helper, ServerPlayer player) {
        helper.setBlock(FLOOR, Blocks.STONE);
        player.snapTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)), 0, 0);
    }

    private static void setBond(ServerPlayer player, PlayerSymbioteData data) {
        AttachmentAccess.set(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, data);
        SymbioteAbilityEvents.onPlayerTick(player);
    }

    private static void assertBoosts(GameTestHelper helper, ServerPlayer player, double speed, double jump,
                                     double safeFall, String phase) {
        assertNear(helper, speed, speed(player), phase + " movement speed");
        assertNear(helper, jump, jump(player), phase + " jump takeoff");
        assertNear(helper, safeFall, safeFall(player), phase + " safe fall distance");
    }

    private static void assertNoPotions(GameTestHelper helper, ServerPlayer player) {
        helper.assertTrue(!player.hasEffect(MobEffects.SPEED) && !player.hasEffect(MobEffects.JUMP_BOOST),
                "The bond created or refreshed a timed status effect");
    }

    private static void assertPotions(GameTestHelper helper, ServerPlayer player, MobEffectInstance speed, MobEffectInstance jump) {
        helper.assertTrue(player.getEffect(MobEffects.SPEED) == speed && speed.getDuration() == 80 && speed.showIcon()
                        && player.getEffect(MobEffects.JUMP_BOOST) == jump && jump.getDuration() == 80 && jump.showIcon(),
                "The bond replaced, retimed, or hid an actual potion effect");
    }

    private static double speed(ServerPlayer player) {
        return player.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    private static double safeFall(ServerPlayer player) {
        return player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
    }

    private static double jump(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        return player.getDeltaMovement().y;
    }

    private static void assertNear(GameTestHelper helper, double expected, double actual, String description) {
        helper.assertTrue(Math.abs(expected - actual) < 1.0E-5, description + " expected " + expected + ", got " + actual);
    }
}
