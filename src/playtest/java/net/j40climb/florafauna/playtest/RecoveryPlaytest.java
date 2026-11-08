package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.block.husk.HuskBlock;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskType;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

final class RecoveryPlaytest {
    private static final BlockPos CHAMBER = new BlockPos(0, 200, 0);
    private static final int SHARD_SLOT = 4;
    private static final int SHARDS = 17;
    private static final int COPPER = 3;

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            bindAndDie(t);
        } else {
            recoverAfterRestart(t);
        }
    }

    private static void bindAndDie(Playtest t) {
        t.server("Arrange a Survival player with stew, a dormant symbiote and counted gear", player -> {
            ServerLevel level = player.level().getServer().overworld();
            for (int x = -3; x <= 3; x++) {
                for (int z = -2; z <= 16; z++) {
                    level.setBlockAndUpdate(CHAMBER.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                    for (int y = 0; y < 4; y++) {
                        level.setBlockAndUpdate(CHAMBER.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            level.setBlockAndUpdate(CHAMBER, FloraFaunaRegistry.COCOON_CHAMBER.get().defaultBlockState());
            level.getGameRules().set(GameRules.KEEP_INVENTORY, false, level.getServer());
            level.getGameRules().set(GameRules.IMMEDIATE_RESPAWN, false, level.getServer());
            Playtest.require(player.teleportTo(level, 0.5, 200, 2.5, Set.of(), 0, 0, true), "Failed to move to test platform");
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(16);
            player.removeAllEffects();
            player.setData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, PlayerSymbioteData.DEFAULT);
            player.getInventory().clearContent();
            player.getInventory().setSelectedSlot(0);
            player.getInventory().setItem(0, new ItemStack(FloraFaunaRegistry.SYMBIOTE_STEW.get()));
            player.getInventory().setItem(1, new ItemStack(FloraFaunaRegistry.DORMANT_SYMBIOTE.get()));
            player.getInventory().setItem(SHARD_SLOT, new ItemStack(Items.AMETHYST_SHARD, SHARDS));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.COPPER_INGOT, COPPER));
            player.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Wait for the test platform and stew to synchronize", mc ->
                mc.screen == null && mc.level.getBlockState(CHAMBER).is(FloraFaunaRegistry.COCOON_CHAMBER.get())
                        && mc.player.getInventory().getItem(0).is(FloraFaunaRegistry.SYMBIOTE_STEW.get())
                        && mc.player.distanceToSqr(0.5, 200, 2.5) < 1);
        t.client("Hold use and drink the stew", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.options.keyUse.setDown(true);
            Playtest.require(mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND).consumesAction(),
                    "The stew rejected the player's use action");
        });
        t.awaitServer("Verify the stew was consumed and enabled binding", p ->
                data(p).symbioteBindable() && p.getInventory().getItem(0).isEmpty());
        t.client("Release use", mc -> mc.options.keyUse.setDown(false));
        t.awaitClient("Wait for bindable state on the client", mc -> data(mc.player).symbioteBindable());
        useChamberButton(t, "set_spawn");
        t.awaitServer("Verify the chamber became the actual respawn point", p ->
                CHAMBER.equals(data(p).cocoonSpawnPos()) && p.getRespawnConfig() != null
                        && CHAMBER.equals(p.getRespawnConfig().respawnData().pos()));
        useChamberButton(t, "bind");
        t.awaitServer("Verify binding consumed the dormant symbiote", p ->
                data(p).symbioteState() == SymbioteState.BONDED_ACTIVE && p.getInventory().getItem(1).isEmpty());

        t.client("Walk forward away from the cocoon", mc -> {
            mc.player.setYRot(0);
            mc.options.keyUp.setDown(true);
        });
        t.awaitServer("Verify the server saw the player walk at least five blocks", p -> p.getZ() >= 7.5);
        t.client("Release forward", mc -> mc.options.keyUp.setDown(false));
        t.server("Kill the real player and create the restoration husk", p -> p.kill(p.level()));
        t.awaitClient("Wait for the vanilla Respawn button", mc ->
                mc.screen instanceof DeathScreen && enabledButton(mc.screen, "deathScreen.respawn") != null);
        t.screenshot("recovery-death");
        t.client("Click Respawn", mc -> click(mc, "deathScreen.respawn"));
        t.awaitServer("Verify respawn at the cocoon with an empty inventory", p -> p.isAlive()
                && CHAMBER.distSqr(p.blockPosition()) <= 4 && p.getInventory().isEmpty() && hasWaypoint(p));
        t.server("Verify the husk owner, all saved slots and absence of duplicate drops", RecoveryPlaytest::checkHusk);
        t.awaitClient("Verify the restoration waypoint synchronized before quitting", mc -> hasWaypoint(mc.player));
    }

    private static void recoverAfterRestart(Playtest t) {
        t.server("Verify weakened state and husk inventory survived a full restart", p -> {
            Playtest.require(hasWaypoint(p) && p.getInventory().isEmpty(), "Player lost the saved recovery state");
            checkHusk(p);
        });
        t.awaitClient("Wait for the saved waypoint to reach the rejoined client", mc -> mc.screen == null && hasWaypoint(mc.player));
        t.screenshot("recovery-after-restart");
        t.client("Walk toward the persisted husk", mc -> {
            Vec3 target = Vec3.atCenterOf(data(mc.player).restorationHuskPos());
            Vec3 delta = target.subtract(mc.player.position());
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
            mc.options.keyUp.setDown(true);
        });
        t.awaitServer("Reach the husk using client movement", p -> p.distanceToSqr(Vec3.atCenterOf(data(p).restorationHuskPos())) < 9);
        BlockPos[] recovered = new BlockPos[1];
        t.client("Release forward and right-click the restoration husk", mc -> {
            mc.options.keyUp.setDown(false);
            recovered[0] = data(mc.player).restorationHuskPos();
            useBlock(mc, recovered[0], Direction.NORTH);
        });
        t.awaitServer("Verify the husk restores the bond and clears the waypoint", p ->
                data(p).symbioteState() == SymbioteState.BONDED_ACTIVE && !data(p).restorationHuskActive());
        t.server("Verify each item returned to its exact slot", p -> checkRecovered(p, recovered[0]));
        t.awaitClient("Verify the recovered inventory and cleared waypoint synchronized", mc ->
                data(mc.player).symbioteState() == SymbioteState.BONDED_ACTIVE && data(mc.player).restorationHuskPos() == null
                        && mc.player.getInventory().getItem(SHARD_SLOT).getCount() == SHARDS);
        t.client("Right-click the emptied husk again", mc -> useBlock(mc, recovered[0], Direction.NORTH));
        long[] checkAt = new long[1];
        t.server("Allow the second interaction to reach the server", p -> checkAt[0] = p.level().getGameTime() + 5);
        t.awaitServer("Wait five world ticks", p -> p.level().getGameTime() >= checkAt[0]);
        t.server("Verify recovery cannot duplicate the items", p -> checkRecovered(p, recovered[0]));
        t.screenshot("recovery-complete");
    }

    private static PlayerSymbioteData data(Player player) {
        return player.getData(FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
    }

    private static boolean hasWaypoint(Player player) {
        PlayerSymbioteData state = data(player);
        return state.symbioteState() == SymbioteState.BONDED_WEAKENED && state.restorationHuskActive()
                && state.restorationHuskPos() != null && state.restorationHuskDim() == Level.OVERWORLD;
    }

    private static void checkHusk(ServerPlayer p) {
        BlockPos pos = data(p).restorationHuskPos();
        Playtest.require(pos != null && pos.getZ() >= 7 && pos.getZ() <= 16, "Husk is not at the walking player's death location: " + pos);
        Playtest.require(p.level().getBlockState(pos).getValue(HuskBlock.HUSK_TYPE) == HuskType.RESTORATION, "Expected a restoration husk at " + pos);
        HuskBlockEntity husk = (HuskBlockEntity) p.level().getBlockEntity(pos);
        Playtest.require(husk != null && husk.isOwner(p), "The husk lost its owner");
        checkStack(husk.handler.getResource(SHARD_SLOT).toStack(husk.handler.getAmountAsInt(SHARD_SLOT)), Items.AMETHYST_SHARD, SHARDS);
        checkStack(husk.handler.getResource(HuskBlockEntity.ARMOR_START).toStack(husk.handler.getAmountAsInt(HuskBlockEntity.ARMOR_START)), Items.IRON_BOOTS, 1);
        checkStack(husk.handler.getResource(HuskBlockEntity.OFFHAND_SLOT).toStack(husk.handler.getAmountAsInt(HuskBlockEntity.OFFHAND_SLOT)), Items.COPPER_INGOT, COPPER);
        Playtest.require(p.level().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).isEmpty(),
                "Items dropped on the ground as well as entering the husk");
    }

    private static void checkRecovered(ServerPlayer p, BlockPos pos) {
        checkStack(p.getInventory().getItem(SHARD_SLOT), Items.AMETHYST_SHARD, SHARDS);
        checkStack(p.getItemBySlot(EquipmentSlot.FEET), Items.IRON_BOOTS, 1);
        checkStack(p.getOffhandItem(), Items.COPPER_INGOT, COPPER);
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            count += p.getInventory().getItem(i).getCount();
        }
        Playtest.require(count == SHARDS + COPPER + 1, "Expected exactly the original inventory, found " + count + " items");
        Playtest.require(p.level().getBlockState(pos).getValue(HuskBlock.HUSK_TYPE) == HuskType.BROKEN,
                "Empty husk failed to transition to broken");
    }

    private static void checkStack(ItemStack stack, Item item, int count) {
        Playtest.require(stack.is(item) && stack.getCount() == count, "Expected " + count + " " + item + ", found " + stack);
    }

    private static void useChamberButton(Playtest t, String action) {
        String key = "gui.florafauna.cocoon." + action;
        t.client("Right-click the cocoon to open its screen", mc -> useBlock(mc, CHAMBER, Direction.SOUTH));
        t.awaitClient("Wait for the cocoon " + action + " button", mc ->
                mc.screen instanceof CocoonChamberScreen && enabledButton(mc.screen, key) != null);
        t.client("Click the cocoon " + action + " button", mc -> click(mc, key));
    }

    private static Button enabledButton(Screen screen, String key) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.active && b.visible && b.getMessage().equals(Component.translatable(key))).findFirst().orElse(null);
    }

    private static void click(Minecraft mc, String key) {
        Button button = enabledButton(mc.screen, key);
        Playtest.require(button != null, "Missing enabled button " + key);
        MouseButtonEvent leftClick = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0));
        Playtest.require(mc.screen.mouseClicked(leftClick, false), "Screen did not handle click on " + key);
    }

    private static void useBlock(Minecraft mc, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }
}
