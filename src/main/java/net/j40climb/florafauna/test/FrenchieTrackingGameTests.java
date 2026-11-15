package net.j40climb.florafauna.test;

import java.util.Optional;
import net.j40climb.florafauna.common.entity.frenchie.DeathTrackingGoal;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

final class FrenchieTrackingGameTests {
    static void trackDeath(GameTestHelper helper) {
        var dog = helper.spawnWithNoFreeWill(FloraFaunaRegistry.FRENCHIE.get(), 1, 1, 1);
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setLastDeathLocation(Optional.empty());
        try {
            dog.tame(owner);
            dog.sitDown();
            var goal = new DeathTrackingGoal(dog);
            helper.assertTrue(!goal.startTracking(owner) && dog.isOrderedToSit(), "Missing scent should preserve sit order");
            var spot = dog.blockPosition().east(4);
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(Level.NETHER, spot)));
            helper.assertTrue(!goal.startTracking(owner) && !goal.active(), "Dog accepted another dimension's scent");
            owner.setLastDeathLocation(Optional.of(GlobalPos.of(helper.getLevel().dimension(), spot)));
            helper.assertTrue(goal.startTracking(owner) && !dog.isOrderedToSit(), "Valid tracking order should release sit");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            goal.save(output);
            goal.cancel();
            goal.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
            helper.assertTrue(goal.active(), "Saving and loading lost the trail");
            owner.snapTo(spot.east(14), 0, 0);
            dog.snapTo(spot, 0, 0);
            goal.tick();
            helper.assertTrue(goal.active() && dog.getNavigation().isDone(), "Dog should wait for its owner at the trail end");
            owner.snapTo(spot, 0, 0);
            goal.tick();
            helper.assertTrue(!goal.active(), "Dog should mark arrival when owner catches up");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(owner);
        }
        helper.succeed();
    }
}
