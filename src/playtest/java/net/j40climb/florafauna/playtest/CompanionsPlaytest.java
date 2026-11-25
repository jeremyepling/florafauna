package net.j40climb.florafauna.playtest;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.debug.DebugOverlay;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.neoforged.fml.ModList;
//?} else {
/*import net.fabricmc.loader.api.FabricLoader;
*///?}

import java.util.Set;
import java.util.UUID;

/** Verifies the separately packaged mods through Minecraft's registries and the saved world. */
final class CompanionsPlaytest {
    private static final Identifier HAMMER = Identifier.fromNamespaceAndPath("florafauna", "hammer");
    private static final Identifier FRENCHIE = Identifier.fromNamespaceAndPath("florafauna", "frenchie");
    private static final Identifier CORE_ITEM = Identifier.fromNamespaceAndPath("florafauna", "dormant_symbiote");
    private static final Identifier HAMMER_STAGE = Identifier.fromNamespaceAndPath("florafauna", "hammer_stage");
    private static final UUID DOG = UUID.fromString("fec76d21-ce62-468f-b351-4dabe610f64a");
    private static final Component TOOL_NAME = Component.literal("Quarry Hammer");
    private static final BlockPos STONE = new BlockPos(0, 201, 2);

    static void steps(Playtest test, int phase) {
        test.client("Load all three separate mods", mc -> {
            for (String id : new String[]{"florafauna", "florafauna_hammer", "florafauna_frenchies"}) {
                //? if neoforge {
                Playtest.require(ModList.get().isLoaded(id), "Missing mod " + id);
                //?} else {
                /*Playtest.require(FabricLoader.getInstance().isModLoaded(id), "Missing mod " + id);
                *///?}
            }
        });
        if (phase == 1) {
            test.server("Equip the Hammer and a FloraFauna item and tame a Frenchie", player -> {
                var level = player.level().getServer().overworld();
                for (BlockPos pos : BlockPos.betweenClosed(-3, 199, -2, 3, 204, 4)) {
                    level.setBlockAndUpdate(pos, pos.getY() == 199 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(STONE, Blocks.STONE.defaultBlockState());
                player.teleportTo(level, 0.5, 200, 0.5, Set.of(), 0, 0, true);
                player.setGameMode(GameType.CREATIVE);
                var hammer = new ItemStack(BuiltInRegistries.ITEM.getOptional(HAMMER).orElseThrow());
                hammer.set(DataComponents.CUSTOM_NAME, TOOL_NAME);
                player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BuiltInRegistries.ITEM.getOptional(CORE_ITEM).orElseThrow()));
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(FRENCHIE).orElseThrow();
                var frenchie = (TamableAnimal) type.create(level, EntitySpawnReason.COMMAND);
                Playtest.require(frenchie != null, "The Frenchie could not spawn");
                frenchie.setUUID(DOG);
                frenchie.tame(player);
                frenchie.setOrderedToSit(true);
                frenchie.snapTo(1.5, 200, 2.5, 180, 0);
                Playtest.require(level.addFreshEntity(frenchie), "The Frenchie did not enter the world");
            });
        }
        test.awaitClient("See the named Hammer, core item and the same tamed Frenchie", mc ->
                TOOL_NAME.equals(mc.player.getMainHandItem().get(DataComponents.CUSTOM_NAME))
                && mc.player.getMainHandItem().is(BuiltInRegistries.ITEM.getOptional(HAMMER).orElseThrow())
                && mc.player.getMainHandItem().getCount() == 1
                && mc.player.getOffhandItem().is(BuiltInRegistries.ITEM.getOptional(CORE_ITEM).orElseThrow())
                && mc.player.getOffhandItem().getCount() == 1
                && mc.level.getEntity(DOG) instanceof TamableAnimal dog && dog.isOwnedBy(mc.player));
        if (phase == 1) {
            test.awaitClient("Receive the stone in the shared world", mc -> mc.level.getBlockState(STONE).is(Blocks.STONE));
            test.client("Strike the stone with the standalone Hammer", mc ->
                    Playtest.require(mc.gameMode.startDestroyBlock(STONE, Direction.NORTH), "Client refused to mine"));
        }
        test.awaitServer("Confirm the mined block, Hammer name and Frenchie owner on the server", p ->
                p.level().isEmptyBlock(STONE) && TOOL_NAME.equals(p.getMainHandItem().get(DataComponents.CUSTOM_NAME))
                && p.level().getEntity(DOG) instanceof TamableAnimal dog && dog.isOwnedBy(p));
        if (phase == 1) {
            test.client("Verify Forged Hammer diagnostics hide the locked abilities", mc -> {
                Playtest.require(hasStage(mc.player.getMainHandItem(), stageComponent(), "forged"), "Fresh Hammer was not Forged");
                var lines = DebugOverlay.getModDataComponentLines(mc.player.getMainHandItem());
                requireDiagnostics(lines, "Mining:", "Tool Config:", "Throw:", "  return=");
                for (var locked : new String[]{"Lightning:", "Teleport:"}) {
                    Playtest.require(lines.stream().noneMatch(line -> line.startsWith(locked)), "Forged Hammer exposed locked detail: " + locked);
                }
            });
            test.server("Provide a Tempest-stage fixture on the same named tool for unlocked diagnostics", p -> {
                var hammer = p.getMainHandItem();
                Playtest.require(hammer.getCount() == 1 && TOOL_NAME.equals(hammer.get(DataComponents.CUSTOM_NAME)),
                        "Stage fixture would replace or change the named Hammer");
                setStage(hammer, stageComponent(), "tempest");
                p.inventoryMenu.broadcastChanges();
            });
        }
        test.awaitServer("Verify the one named Tempest Hammer on the server, including after restart", p ->
                p.getMainHandItem().getCount() == 1 && TOOL_NAME.equals(p.getMainHandItem().get(DataComponents.CUSTOM_NAME))
                        && hasStage(p.getMainHandItem(), stageComponent(), "tempest"));
        test.awaitClient("Receive the Tempest stage with the same named tool, including after restart", mc ->
                mc.player.getMainHandItem().getCount() == 1 && TOOL_NAME.equals(mc.player.getMainHandItem().get(DataComponents.CUSTOM_NAME))
                        && hasStage(mc.player.getMainHandItem(), stageComponent(), "tempest"));
        test.client("Inspect all unlocked Tempest Hammer details from the core debug overlay", mc -> {
            var lines = DebugOverlay.getModDataComponentLines(mc.player.getMainHandItem());
            requireDiagnostics(lines, "Mining:", "Tool Config:", "Lightning:", "Teleport:", "Throw:", "  return=");
        });
        if (phase == 1) {
            var wood = WoodType.DRIFTWOOD.getBlockSet();
            checkStripping(test, STONE.west(), wood.log().get().defaultBlockState(), wood.strippedLog().get().defaultBlockState(), Direction.Axis.X);
            checkStripping(test, STONE.east(), wood.wood().get().defaultBlockState(), wood.strippedWood().get().defaultBlockState(), Direction.Axis.Z);
        }
        if (phase == 2) {
            test.client("Face the Frenchie with both mods' items equipped", mc ->
                    mc.player.lookAt(Anchor.EYES, mc.level.getEntity(DOG).getEyePosition()));
            test.screenshot("companions-rejoined");
        }
    }

    private static DataComponentType<?> stageComponent() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(HAMMER_STAGE).orElseThrow();
    }

    private static <T> void setStage(ItemStack stack, DataComponentType<T> type, String stage) {
        var codec = type.codec();
        Playtest.require(codec != null, "Hammer stage component has no persistent codec");
        stack.set(type, codec.parse(JsonOps.INSTANCE, new JsonPrimitive(stage)).getOrThrow());
    }

    private static <T> boolean hasStage(ItemStack stack, DataComponentType<T> type, String stage) {
        var value = stack.get(type);
        var codec = type.codec();
        return value != null && codec != null && codec.encodeStart(JsonOps.INSTANCE, value).result()
                .map(encoded -> encoded.equals(new JsonPrimitive(stage))).orElse(false);
    }

    private static void requireDiagnostics(java.util.List<String> lines, String... prefixes) {
        for (var prefix : prefixes) {
            Playtest.require(lines.stream().anyMatch(line -> line.startsWith(prefix)), "Missing Hammer detail: " + prefix);
        }
    }

    private static void checkStripping(Playtest test, BlockPos pos, BlockState input, BlockState stripped, Direction.Axis axis) {
        var before = input.setValue(RotatedPillarBlock.AXIS, axis);
        var after = stripped.setValue(RotatedPillarBlock.AXIS, axis);
        test.server("Place core driftwood along " + axis, p -> p.level().setBlockAndUpdate(pos, before));
        test.awaitClient("Receive the driftwood fixture", mc -> mc.level.getBlockState(pos).equals(before));
        test.client("Use the standalone Hammer on core driftwood", mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)));
        test.awaitServer("Preserve the driftwood axis after stripping", p -> p.level().getBlockState(pos).equals(after));
    }
}
