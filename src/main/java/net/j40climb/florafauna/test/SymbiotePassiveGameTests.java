package net.j40climb.florafauna.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.j40climb.florafauna.common.symbiote.abilities.SymbioteAbilityEvents;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.platform.AttachmentAccess;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
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
        ServerPlayer player = StorageRecoveryGameTests.connectSurvivalPlayer(helper);
        try {
            putOnTestFloor(helper, player);
            checkPotionPrecedence(helper, player);
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void checkPotionPrecedence(GameTestHelper helper, ServerPlayer player) {
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
    }

    static void potionExpiry(GameTestHelper helper) {
        ServerPlayer player = StorageRecoveryGameTests.connectSurvivalPlayer(helper);
        try {
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

            // Drive the normal effect lifetime directly; mock connections do not tick uniformly on both loaders.
            for (int tick = 0; tick < 10; tick++) {
                player.doTick();
            }
            SymbioteAbilityEvents.onPlayerTick(player);
            assertNoPotions(helper, player);
            assertBoosts(helper, player, baseSpeed * 1.4, baseJump + 0.2, baseSafeFall + 2, "short potions expired");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    static void survivalProgression(GameTestHelper helper) {
        ServerPlayer player = StorageRecoveryGameTests.connectSurvivalPlayer(helper);
        try {
            putOnTestFloor(helper, player);
            player.setHealth(20);
            double baseSpeed = speed(player);
            sprintDistance(player, 50_000);
            player.setSprinting(true);
            jump(player);
            player.setSprinting(false);
            takeFall(helper, player, true);

            setBond(player, PlayerSymbioteData.DEFAULT.withSymbioteState(SymbioteState.BONDED_ACTIVE));
            sprintDistance(player, 9_999);
            assertNear(helper, 0, bond(player).speedLevel(), "speed tier below 100 sprinted blocks");
            assertNear(helper, baseSpeed, speed(player), "movement speed before first adaptation");
            sprintDistance(player, 1);
            assertNear(helper, 1, bond(player).speedLevel(), "speed tier at 100 sprinted blocks");
            assertNear(helper, baseSpeed * 1.2, speed(player), "first speed adaptation");
            sprintDistance(player, 39_999);
            assertNear(helper, 1, bond(player).speedLevel(), "speed tier below 500 sprinted blocks");
            sprintDistance(player, 1);
            assertNear(helper, 2, bond(player).speedLevel(), "speed tier at 500 sprinted blocks");
            assertNear(helper, baseSpeed * 1.4, speed(player), "second speed adaptation");
            assertNoPotions(helper, player);

            for (int jump = 0; jump < 20; jump++) {
                jump(player);
            }
            helper.assertTrue(!bond(player).dash(), "Non-sprinting jumps unlocked Dash");
            player.setSprinting(true);
            for (int jump = 0; jump < 19; jump++) {
                jump(player);
            }
            helper.assertTrue(!bond(player).dash(), "Fewer than 20 bonded sprint jumps unlocked Dash");
            jump(player);
            helper.assertTrue(bond(player).dash(), "The twentieth bonded sprint jump did not unlock Dash");
            helper.assertTrue(bond(player).jumpBoost() == 0, "Jump Boost unlocked without a second biome");
            player.setSprinting(false);
            player.setDeltaMovement(Vec3.ZERO);

            StorageRecoveryGameTests.clearDamageCooldown(player);
            player.hurtServer(helper.getLevel(), player.damageSources().generic(), 1);
            for (int landing = 1; landing <= 4; landing++) {
                takeFall(helper, player, true);
                helper.assertTrue(!bond(player).featherFalling(), "Feather Falling unlocked on landing " + landing);
            }
            takeFall(helper, player, true);
            helper.assertTrue(bond(player).featherFalling(), "The fifth surviving damaging landing did not unlock Feather Falling");
            takeFall(helper, player, false);
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    static void speedSaveCompatibility(GameTestHelper helper) {
        for (boolean oldSpeed : new boolean[]{false, true}) {
            int expectedLevel = oldSpeed ? 2 : 0;
            var legacyBond = JsonParser.parseString("""
                    {"symbioteState":"bonded_active","bondTime":0,"tier":1,"dash":false,
                     "featherFalling":false,"speed":%s,"jumpBoost":0,"symbioteBindable":false,
                     "pollenConsumedOnce":false,"cocoonSpawnSetOnce":false,"restorationHuskActive":false}
                    """.formatted(oldSpeed));
            var legacyDormant = JsonParser.parseString("""
                    {"bonded":false,"bondTime":0,"tier":1,"dash":false,
                     "featherFalling":false,"speed":%s,"jumpBoost":0}
                    """.formatted(oldSpeed));
            var decodedBond = PlayerSymbioteData.CODEC.parse(JsonOps.INSTANCE, legacyBond).getOrThrow();
            var decodedDormant = SymbioteData.CODEC.parse(JsonOps.INSTANCE, legacyDormant).getOrThrow();
            helper.assertTrue(decodedBond.speedLevel() == expectedLevel && decodedDormant.speedLevel() == expectedLevel,
                    "A legacy saved speed flag no longer maps to its prior bonus: " + oldSpeed);
        }

        var intermediate = BOND.withSpeedLevel(1);
        var serializedBond = PlayerSymbioteData.CODEC.encodeStart(JsonOps.INSTANCE, intermediate).getOrThrow();
        var restoredBond = PlayerSymbioteData.CODEC.parse(JsonOps.INSTANCE, serializedBond).getOrThrow();
        helper.assertTrue(restoredBond.speedLevel() == 1, "Save/load changed the intermediate speed tier");
        var serializedDormant = SymbioteData.CODEC.encodeStart(JsonOps.INSTANCE, intermediate.toItemData()).getOrThrow();
        var restoredDormant = SymbioteData.CODEC.parse(JsonOps.INSTANCE, serializedDormant).getOrThrow();
        helper.assertTrue(restoredDormant.speedLevel() == 1, "A dormant symbiote lost its intermediate speed tier");
        helper.succeed();
    }

    private static void sprintDistance(ServerPlayer player, int centimeters) {
        player.awardStat(Stats.SPRINT_ONE_CM, centimeters);
        SymbioteAbilityEvents.onPlayerTick(player);
    }

    private static void takeFall(GameTestHelper helper, ServerPlayer player, boolean expectDamage) {
        float previousHealth = player.getHealth();
        StorageRecoveryGameTests.clearDamageCooldown(player);
        player.hurtServer(helper.getLevel(), player.damageSources().fall(), 1);
        assertNear(helper, previousHealth - (expectDamage ? 1 : 0), player.getHealth(),
                expectDamage ? "damaging landing before adaptation" : "landing after Feather Falling");
    }

    private static PlayerSymbioteData bond(ServerPlayer player) {
        return AttachmentAccess.get(player, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
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
