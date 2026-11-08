package net.j40climb.florafauna.playtest;

import net.j40climb.florafauna.common.block.mininganchor.Tier2MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.PodContents;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

import static net.j40climb.florafauna.playtest.Playtest.require;

final class PodPlaytest {
    private static final BlockPos ANCHOR = new BlockPos(0, 200, 0);
    private static final List<BlockPos> STONES = List.of(ANCHOR.offset(0, 0, 1), ANCHOR.offset(-1, 0, 1), ANCHOR.offset(-1, 0, 2));
    private static final BlockPos REPLACED = ANCHOR.offset(3, 0, 1);
    private static final int STOCK = STONES.size();

    static void steps(Playtest t, int phase) {
        if (phase == 1) {
            t.server("Prepare a survival mining platform and three stone blocks", p -> {
                for (BlockPos pos : BlockPos.betweenClosed(ANCHOR.offset(-4, -1, -4), ANCHOR.offset(6, 3, 6))) {
                    p.level().setBlockAndUpdate(pos, pos.getY() < ANCHOR.getY()
                            ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                for (BlockPos stone : STONES) {
                    p.level().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
                }
                p.teleportTo(2.5, 200, 3.5);
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                p.getInventory().setItem(0, new ItemStack(FloraFaunaRegistry.TIER2_MINING_ANCHOR.get()));
                p.getInventory().setItem(1, new ItemStack(Items.DIAMOND_PICKAXE));
                p.inventoryMenu.broadcastChanges();
            });
            t.awaitClient("Receive platform, anchor item and survival inventory", mc ->
                    mc.level.getBlockState(ANCHOR.below()).is(Blocks.SMOOTH_STONE)
                            && mc.player.getInventory().getItem(0).is(FloraFaunaRegistry.TIER2_MINING_ANCHOR.get().asItem())
                            && mc.player.getInventory().getItem(1).is(Items.DIAMOND_PICKAXE));
            t.client("Place the hardened anchor from the hotbar", mc -> {
                select(mc, 0);
                place(mc, ANCHOR);
            });
            t.awaitServer("See the player's placed anchor", p -> p.level().getBlockEntity(ANCHOR) instanceof Tier2MiningAnchorBlockEntity);
            t.client("Select the diamond pickaxe", mc -> select(mc, 1));
            t.awaitServer("Hold the pickaxe in survival", p -> !p.isCreative() && p.getMainHandItem().is(Items.DIAMOND_PICKAXE));
            for (BlockPos stone : STONES) {
                mine(t, () -> stone, "stone " + stone.toShortString());
            }
            t.awaitServer("Collect exactly three block drops into one spawned hardened pod", p -> {
                Tier2MiningAnchorBlockEntity anchor = anchor(p);
                return anchor.getPodCount() == 1 && anchor.getStoredCount() == STOCK;
            });
            t.server("Check the stock and parent link before saving", p -> verifyStock(p, spawnedPod(p)));
            return;
        }

        BlockPos[] original = new BlockPos[1];
        t.server("Read retained anchor, pod link and exact stock after relaunch", p -> {
            original[0] = spawnedPod(p);
            verifyStock(p, original[0]);
            require(p.getMainHandItem().is(Items.DIAMOND_PICKAXE), "Saved player lost the pickaxe");
        });
        mine(t, () -> original[0], "hardened pod");
        t.server("Walk into pickup range of the surviving packed pod", p -> {
            require(anchor(p).getPodCount() == 0, "Broken pod is still attached to its anchor");
            List<ItemEntity> drops = looseItems(p).stream().filter(e -> e.getItem().is(podItem())).toList();
            require(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                    "Expected one packed pod drop, found " + drops.stream().map(ItemEntity::getItem).toList());
            ItemEntity drop = drops.getFirst();
            p.teleportTo(drop.getX(), ANCHOR.getY(), drop.getZ());
        });
        t.awaitServer("Pick the dropped pod up through player collision", p -> p.getInventory().countItem(podItem()) == 1);
        t.server("Check the packed item that the player picked up", p -> {
            ItemStack stack = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(podItem())).findFirst().orElseThrow();
            PodContents contents = stack.get(FloraFaunaRegistry.POD_CONTENTS.get());
            require(contents != null && contents.getItemCount() == STOCK && contents.items().stream().allMatch(s -> s.is(Items.COBBLESTONE)),
                    "Packed pod lost or changed its contents");
        });
        t.awaitClient("Receive the packed pod in the hotbar", mc -> {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(podItem())) return true;
            }
            return false;
        });
        t.client("Place the packed pod at a different position", mc -> {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(podItem())) {
                    select(mc, i);
                    place(mc, REPLACED);
                    return;
                }
            }
            throw new IllegalStateException("Packed pod disappeared from the client hotbar");
        });
        t.awaitServer("Receive the manually placed pod", p -> p.level().getBlockEntity(REPLACED) instanceof Tier2PodBlockEntity);
        t.server("Verify restored stock and place a hopper over a chest", p -> {
            verifyContents(p, REPLACED);
            require(p.getInventory().countItem(podItem()) == 0, "Placing a pod must consume its item");
            p.level().setBlockAndUpdate(REPLACED.below(2), Blocks.CHEST.defaultBlockState());
            p.level().setBlockAndUpdate(REPLACED.below(), Blocks.HOPPER.defaultBlockState());
        });
        t.awaitServer("Wait for the real hopper to deliver all three cobblestones", p ->
                ((ChestBlockEntity) p.level().getBlockEntity(REPLACED.below(2))).countItem(Items.COBBLESTONE) >= STOCK);
        long[] settleUntil = new long[1];
        t.server("Allow two more hopper cycles for a possible second payout", p -> settleUntil[0] = p.level().getGameTime() + 24);
        t.awaitServer("Settle the hopper", p -> p.level().getGameTime() >= settleUntil[0]);
        t.server("Verify a single payout across chest, hopper, pod, anchor, player and loose items", p -> {
            ChestBlockEntity chest = (ChestBlockEntity) p.level().getBlockEntity(REPLACED.below(2));
            HopperBlockEntity hopper = (HopperBlockEntity) p.level().getBlockEntity(REPLACED.below());
            Tier2PodBlockEntity pod = (Tier2PodBlockEntity) p.level().getBlockEntity(REPLACED);
            require(chest.countItem(Items.COBBLESTONE) == STOCK && hopper.isEmpty() && pod.isEmpty(), "Hopper payout duplicated or left stock");
            require(anchor(p).getPodCount() == 0 && anchor(p).getStoredCount() == 0, "Original anchor received a duplicate drop");
            require(p.getInventory().countItem(Items.COBBLESTONE) == 0 && p.getInventory().countItem(podItem()) == 0,
                    "Player received an unexpected second payout");
            require(looseItems(p).stream().noneMatch(e -> e.getItem().is(podItem()) || e.getItem().is(Items.COBBLESTONE)),
                    "Loose items remain from a second payout");
        });
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    private static void place(Minecraft mc, BlockPos destination) {
        BlockPos support = destination.below();
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(destination), Direction.UP, support, false);
        require(mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit).consumesAction(), "Placement rejected at " + destination);
    }

    private static void mine(Playtest t, Supplier<BlockPos> target, String description) {
        // Send the survival client's press/release actions. The server measures break progress,
        // fires block and loot events, and broadcasts the resulting block change to the client.
        t.client("Start mining " + description, mc -> mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target.get(), Direction.UP)));
        long[] release = new long[1];
        t.server("Hold the pickaxe on " + description, p -> release[0] = p.level().getGameTime() + 20);
        t.awaitServer("Mine " + description + " for 20 ticks", p -> p.level().getGameTime() >= release[0]);
        t.client("Release the pickaxe on " + description, mc -> mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target.get(), Direction.UP)));
        t.awaitServer("Check that " + description + " was broken on the server", p -> p.level().isEmptyBlock(target.get()));
    }

    private static Tier2MiningAnchorBlockEntity anchor(ServerPlayer p) {
        require(p.level().getBlockEntity(ANCHOR) instanceof Tier2MiningAnchorBlockEntity, "Hardened anchor missing from test platform");
        return (Tier2MiningAnchorBlockEntity) p.level().getBlockEntity(ANCHOR);
    }

    private static BlockPos spawnedPod(ServerPlayer p) {
        Tier2MiningAnchorBlockEntity anchor = anchor(p);
        require(anchor.getPodCount() == 1 && anchor.getStoredCount() == STOCK, "Expected one pod holding exactly " + STOCK);
        return anchor.getPodPositions().getFirst();
    }

    private static void verifyStock(ServerPlayer p, BlockPos pos) {
        verifyContents(p, pos);
        require(ANCHOR.equals(((Tier2PodBlockEntity) p.level().getBlockEntity(pos)).getParentAnchorPos()), "Pod lost its anchor link");
    }

    private static void verifyContents(ServerPlayer p, BlockPos pos) {
        require(p.level().getBlockEntity(pos) instanceof Tier2PodBlockEntity, "Missing hardened pod at " + pos);
        Tier2PodBlockEntity pod = (Tier2PodBlockEntity) p.level().getBlockEntity(pos);
        require(pod.getStoredCount() == STOCK && pod.getBuffer().getStacks().stream()
                .allMatch(s -> s.isEmpty() || s.is(Items.COBBLESTONE)), "Hardened pod has incorrect mining drops");
    }

    private static Item podItem() {
        return FloraFaunaRegistry.TIER2_POD.get().asItem();
    }

    private static List<ItemEntity> looseItems(ServerPlayer p) {
        return p.level().getEntitiesOfClass(ItemEntity.class, new AABB(ANCHOR).inflate(12), e -> e.isAlive());
    }
}
