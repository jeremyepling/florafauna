package net.j40climb.florafauna.test;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.block.mobbarrier.networking.UpdateMobBarrierConfigPayload;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.VarInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

final class MobBarrierGameTests {
    static void oldPreset(GameTestHelper helper) {
        var saved = new CompoundTag();
        var ids = new ListTag();
        ids.add(StringTag.valueOf("cow"));
        saved.put("entityIds", ids);
        var tags = new ListTag();
        tags.add(StringTag.valueOf("minecraft:undead")); // Old presets also allow unprefixed tags.
        saved.put("entityTags", tags);
        var config = MobBarrierConfig.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(!config.blockVision() && config.entityTags().equals(List.of("minecraft:undead"))
                && config.entityIds().equals(List.of("cow")), "Changed legacy preset spelling or vision default");
        helper.assertTrue(config.shouldBlockEntity(helper.spawnWithNoFreeWill(EntityTypes.COW, 0, 1, 0))
                && config.shouldBlockEntity(helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 1, 1, 0))
                && !config.shouldBlockEntity(helper.spawnWithNoFreeWill(EntityTypes.PIG, 2, 1, 0)), "Saved filters matched the wrong mobs");
        var wire = Unpooled.buffer();
        try {
            // Existing clients send the item component directly as the edit snapshot.
            MobBarrierConfig.STREAM_CODEC.encode(wire, config);
            var decoded = UpdateMobBarrierConfigPayload.STREAM_CODEC.decode(wire).config();
            helper.assertTrue(config.equals(decoded) && config.hashCode() == decoded.hashCode(), "Lost component value equality");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    static void boundedEdits(GameTestHelper helper) {
        var wire = Unpooled.buffer();
        try {
            // 1 list count + 3 length bytes + 32761 bytes + 1 empty list + 1 flag = 32767 bytes.
            var atLimit = new MobBarrierConfig(List.of("a".repeat(32761)), List.of(), false);
            MobBarrierConfig.STREAM_CODEC.encode(wire, atLimit);
            helper.assertTrue(UpdateMobBarrierConfigPayload.fitsEditPacket(atLimit)
                    && UpdateMobBarrierConfigPayload.STREAM_CODEC.decode(wire).config().equals(atLimit), "Rejected last valid edit size");
            wire.clear();
            var oversized = atLimit.withAddedEntityTag("#a:b");
            MobBarrierConfig.STREAM_CODEC.encode(wire, oversized);
            helper.assertTrue(!UpdateMobBarrierConfigPayload.fitsEditPacket(oversized), "Editor accepted an oversized snapshot");
            try {
                UpdateMobBarrierConfigPayload.STREAM_CODEC.decode(wire);
                throw helper.assertionException("Decoded an oversized edit");
            } catch (DecoderException expected) {
                // The item component itself must still sync regardless of the edit budget.
                wire.readerIndex(0);
                helper.assertTrue(oversized.equals(MobBarrierConfig.STREAM_CODEC.decode(wire)), "Could not sync a large saved item");
            }
            wire.clear();
            VarInt.write(wire, Integer.MAX_VALUE);
            try {
                UpdateMobBarrierConfigPayload.STREAM_CODEC.decode(wire);
                throw helper.assertionException("Accepted an oversized count in a tiny edit packet");
            } catch (DecoderException expected) {
                helper.succeed();
            }
        } finally {
            wire.release();
        }
    }

    static void heldPreset(GameTestHelper helper) {
        var editor = helper.makeMockPlayer(GameType.SURVIVAL);
        var component = FloraFaunaRegistry.MOB_BARRIER_CONFIG.get();
        var config = new MobBarrierConfig(List.of("cow"), List.of("#custom:animals"), true);
        var update = new UpdateMobBarrierConfigPayload(config);
        editor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        update.applyTo(editor);
        helper.assertTrue(!editor.getMainHandItem().has(component), "Edited a non-barrier item");
        editor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FloraFaunaRegistry.MOB_BARRIER.asItem()));
        update.applyTo(editor);
        helper.assertTrue(config.equals(editor.getMainHandItem().get(component)), "Rejected a valid data pack tag");
        for (var invalid : List.of(config.withAddedEntityId("example:unknown_mob"), config.withAddedEntityTag("#"))) {
            new UpdateMobBarrierConfigPayload(invalid).applyTo(editor);
            helper.assertTrue(config.equals(editor.getMainHandItem().get(component)), "Invalid edit replaced the valid preset");
        }
        helper.succeed();
    }
}
