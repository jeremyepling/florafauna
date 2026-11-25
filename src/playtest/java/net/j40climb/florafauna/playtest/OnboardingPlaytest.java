package net.j40climb.florafauna.playtest;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberScreen;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteState;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** A disposable Survival path from a starter bloom to a first earned adaptation. */
final class OnboardingPlaytest {
    private static final BlockPos FIRST_BLOOM = new BlockPos(0, 120, 4);
    private static final BlockPos PERCEPTION_BLOOM = new BlockPos(2, 120, 2);
    private static final BlockPos CHAMBER = new BlockPos(0, 120, 0);
    private static final String FIRST_CONTACT = "symbiote.onboarding.first_contact";
    private static final String POLLEN_HINT = "symbiote.onboarding.pollen_hint";
    private static final String POLLEN_CONSUMED = "symbiote.onboarding.pollen_consumed";
    private static final String DREAM_REFLECTIVE = "symbiote.dream.jump.reflective";
    private static final String DREAM_DIRECTIONAL = "symbiote.dream.jump.directional";

    static void steps(Playtest t, int phase) {
        Playtest.require(phase == 1, "Onboarding runs in one disposable session");
        t.server("Prepare adjacent plains and forest paths with two starter blooms", p -> {
            ServerLevel level = p.level().getServer().overworld();
            level.getGameRules().set(GameRules.BLOCK_DROPS, true, level.getServer());
            for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-3, 119, -3), new BlockPos(29, 123, 6))) {
                level.setBlockAndUpdate(pos, pos.getY() == 119
                        ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            // fillbiome requires the affected chunks to be loaded. These are the two
            // loaded, neighboring chunks that contain the entire walking path.
            level.getChunk(0, 0);
            level.getChunk(1, 0);
            var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
            var plainsFill = FillBiomeCommand.fill(level, new BlockPos(0, 112, 0), new BlockPos(12, 132, 6),
                    biomes.getOrThrow(Biomes.PLAINS));
            Playtest.require(level.getBiome(new BlockPos(0, 120, 2)).is(Biomes.PLAINS),
                    "Could not establish the plains start: " + plainsFill.right().map(Exception::getMessage).orElse("no change"));
            var forestFill = FillBiomeCommand.fill(level, new BlockPos(20, 112, 0), new BlockPos(28, 132, 6),
                    biomes.getOrThrow(Biomes.FOREST));
            Playtest.require(level.getBiome(new BlockPos(24, 120, 2)).is(Biomes.FOREST),
                    "Could not establish the forest destination: " + forestFill.right().map(Exception::getMessage).orElse("no change"));
            level.setBlockAndUpdate(FIRST_BLOOM, FloraFaunaRegistry.RESONANT_BLOOM.get().defaultBlockState());
            Playtest.require(p.teleportTo(level, 0.5, 120, 2.5, Set.of(), 0, 25, true), "Could not reach starter path");
            p.setGameMode(GameType.SURVIVAL);
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(16);
            p.removeAllEffects();
            net.j40climb.florafauna.platform.AttachmentAccess.set(p, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA, PlayerSymbioteData.DEFAULT);
            net.j40climb.florafauna.platform.AttachmentAccess.set(p, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT, ProgressSignalTracker.DEFAULT);
            p.getInventory().clearContent();
            p.getInventory().setSelectedSlot(0);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("See the starter bloom in Survival before touching it", mc -> mc.screen == null
                && mc.gameMode.getPlayerMode() == GameType.SURVIVAL
                && mc.level.getBlockState(FIRST_BLOOM).is(FloraFaunaRegistry.RESONANT_BLOOM.get())
                && mc.player.distanceToSqr(0.5, 120, 2.5) < 1);
        long[] renderedAt = new long[1];
        t.client("Aim down at the starter bloom", mc -> {
            mc.player.setYRot(0);
            mc.player.setXRot(42);
            renderedAt[0] = mc.level.getGameTime() + 15;
        });
        t.awaitClient("Let the bloom path render before the screenshot", mc ->
                mc.level.getGameTime() >= renderedAt[0] && mc.player.getXRot() >= 35);
        t.screenshot("onboarding-bloom-before-contact");
        t.client("Break the first bloom with the Survival player's hand", mc -> {
            mc.particleEngine.clearParticles();
            Playtest.require(mc.gameMode.startDestroyBlock(FIRST_BLOOM, Direction.NORTH), "Bloom did not accept breaking");
        });
        t.awaitClient("See first-contact chat and spore particles", mc ->
                hasChatKey(mc, FIRST_CONTACT)
                        && particlesNear(mc, Vec3.atBottomCenterOf(FIRST_BLOOM).add(0, 0.55, 0)));
        t.screenshot("onboarding-spore-contact");
        t.awaitServer("Record first contact without granting an item symbiote", p ->
                p.level().isEmptyBlock(FIRST_BLOOM)
                        && progress(p).getSignal("resonant_bloom_contact").isPresent()
                        && data(p).symbioteState() == SymbioteState.UNBOUND
                        && p.getInventory().countItem(FloraFaunaRegistry.DORMANT_SYMBIOTE.get()) == 0);
        t.server("Inspect the two-pollen drop and move into pickup range", p ->
                inspectPollenDropAndMove(p, FIRST_BLOOM, 2));
        t.awaitServer("Collect exactly two pollen from one bloom", p -> pollen(p) == 2);
        t.awaitClient("Read the pollen guidance", mc -> hasChatKey(mc, POLLEN_HINT));
        t.client("Consume one of the two earned pollen", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.options.keyUse.setDown(true);
            Playtest.require(mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND).consumesAction(),
                    "The symbiotic pollen rejected use");
        });
        t.awaitServer("One consumed pollen prepares the spore", p ->
                pollen(p) == 1 && inventoryCount(p) == 1
                        && data(p).symbioteBindable() && data(p).pollenConsumedOnce());
        t.client("Release the pollen use key", mc -> mc.options.keyUse.setDown(false));
        t.awaitClient("Hear the pollen consumption feedback", mc -> hasChatKey(mc, POLLEN_CONSUMED));

        long[] pollenTime = new long[1];
        t.server("Advance the disposable world's clock by more than 12000 ticks", p -> {
            pollenTime[0] = p.level().getGameTime();
            ((ServerLevelData) p.level().getLevelData()).setGameTime(pollenTime[0] + 12001);
        });
        t.awaitServer("Keep pollen readiness after more than 12000 game ticks", p ->
                p.level().getGameTime() - pollenTime[0] > 12000
                        && pollen(p) == 1 && data(p).symbioteBindable()
                        && data(p).pollenConsumedOnce());

        t.server("Craft the chamber with the remaining pollen", p -> {
            Playtest.require(pollen(p) == 1, "The chamber must use the bloom's remaining pollen");
            p.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS));
            p.getInventory().setItem(2, new ItemStack(whiteWool()));
            ItemStack chamber = assemble(p, Items.OAK_PLANKS, whiteWool(), FloraFaunaRegistry.SYMBIOTIC_POLLEN.get());
            Playtest.require(chamber.is(FloraFaunaRegistry.COCOON_CHAMBER.get().asItem()) && chamber.getCount() == 1,
                    "Wood, wool and pollen did not craft one Cocoon Chamber");
            consumePollen(p, 1);
            for (int slot = 1; slot <= 2; slot++) {
                Playtest.require(p.getInventory().getItem(slot).getCount() == 1, "Missing single recipe ingredient in slot " + slot);
                p.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            Playtest.require(pollen(p) == 0, "Crafting failed to consume the remaining pollen");
            p.getInventory().setItem(0, chamber);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive the crafted chamber", mc ->
                mc.player.getInventory().getItem(0).is(FloraFaunaRegistry.COCOON_CHAMBER.get().asItem()));
        t.client("Place the crafted chamber on the path", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            useBlock(mc, CHAMBER.below(), Direction.UP);
        });
        t.awaitServer("Verify the chamber was placed and its item consumed", p ->
                p.level().getBlockState(CHAMBER).is(FloraFaunaRegistry.COCOON_CHAMBER.get())
                        && p.getInventory().getItem(0).isEmpty() && inventoryCount(p) == 0);
        t.awaitClient("Receive bindable state", mc -> data(mc.player).symbioteBindable());
        useChamberButton(t, "bind");
        t.awaitServer("Bind the resident spore without a Dormant Symbiote item", p ->
                data(p).symbioteState() == SymbioteState.BONDED_ACTIVE
                        && p.getInventory().countItem(FloraFaunaRegistry.DORMANT_SYMBIOTE.get()) == 0);
        t.server("Leave a nearby bloom for bonded perception", p ->
                p.level().setBlockAndUpdate(PERCEPTION_BLOOM, FloraFaunaRegistry.RESONANT_BLOOM.get().defaultBlockState()));
        t.awaitClient("See the nearby bloom", mc ->
                mc.level.getBlockState(PERCEPTION_BLOOM).is(FloraFaunaRegistry.RESONANT_BLOOM.get()));
        t.client("Aim the camera at the perception bloom", mc -> {
            Vec3 target = Vec3.atCenterOf(PERCEPTION_BLOOM);
            Vec3 delta = target.subtract(mc.player.position());
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
            mc.player.setXRot(30);
        });
        t.client("Clear prior particles before checking perception", mc -> mc.particleEngine.clearParticles());
        t.awaitClient("See the bonded symbiote shimmer around the nearby bloom", mc ->
                particlesNear(mc, Vec3.atBottomCenterOf(PERCEPTION_BLOOM).add(0, 0.8, 0)));
        t.screenshot("onboarding-bonded-perception");

        t.client("Jump repeatedly in the starter plains", mc -> mc.options.keyJump.setDown(true));
        t.awaitServer("Accumulate partial jump experience in plains", p ->
                progress(p).getSignal("adaptive_jump").map(s -> s.interactionCount() >= 12).orElse(false)
                        && data(p).jumpBoost() == 0);
        t.client("Stop jumping before asking for guidance", mc -> mc.options.keyJump.setDown(false));
        t.awaitServer("Land after partial progress", ServerPlayer::onGround);
        useChamberButton(t, "dream");
        t.awaitServer("Record the first dream while jump adaptation remains unfinished", p ->
                progress(p).dreamLevel() == 1 && data(p).jumpBoost() == 0);
        t.awaitClient("Receive a relevant first jump dream", mc -> hasChatKey(mc, DREAM_REFLECTIVE));
        useChamberButton(t, "dream");
        t.awaitServer("Escalate the dream after another request without progress", p -> progress(p).dreamLevel() == 2);
        t.awaitClient("Receive clearer jump guidance", mc -> hasChatKey(mc, DREAM_DIRECTIONAL));
        t.screenshot("onboarding-dream-guidance");

        t.client("Walk east to a second vanilla biome", mc -> {
            mc.player.setYRot(-90);
            mc.options.keyUp.setDown(true);
        });
        t.awaitServer("Reach the forest through player movement", p -> p.getX() >= 24.5
                && p.level().getBiome(p.blockPosition()).is(Biomes.FOREST));
        t.client("Stop in the forest", mc -> mc.options.keyUp.setDown(false));
        t.client("Jump in the forest until the adaptation is earned", mc -> mc.options.keyJump.setDown(true));
        t.awaitServer("Earn Jump Boost I through jumps in two biomes", p -> data(p).jumpBoost() == 1);
        t.client("Stop jumping after the adaptation", mc -> mc.options.keyJump.setDown(false));
        t.awaitClient("Receive the earned ability on the client", mc -> data(mc.player).jumpBoost() == 1);
        t.screenshot("onboarding-first-adaptation");
    }

    private static ItemStack assemble(ServerPlayer p, Item first, Item second, Item third) {
        CraftingInput input = CraftingInput.of(3, 1,
                List.of(new ItemStack(first), new ItemStack(second), new ItemStack(third)));
        return p.level().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, p.level())
                .orElseThrow(() -> new AssertionError("No crafting recipe for " + first + ", " + second + ", " + third))
                .value().assemble(input);
    }

    private static Item whiteWool() {
        return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("minecraft", "white_wool"))
                .orElseThrow(() -> new AssertionError("Vanilla white wool item is missing"));
    }

    private static void consumePollen(ServerPlayer p, int count) {
        for (int i = 0; i < p.getInventory().getContainerSize() && count > 0; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(FloraFaunaRegistry.SYMBIOTIC_POLLEN.get())) {
                int consumed = Math.min(stack.getCount(), count);
                stack.shrink(consumed);
                count -= consumed;
            }
        }
        Playtest.require(count == 0, "The flower did not provide all recipe pollen");
    }

    private static int pollen(ServerPlayer p) {
        return p.getInventory().countItem(FloraFaunaRegistry.SYMBIOTIC_POLLEN.get());
    }

    private static int inventoryCount(ServerPlayer p) {
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            count += p.getInventory().getItem(i).getCount();
        }
        return count;
    }

    private static void inspectPollenDropAndMove(ServerPlayer p, BlockPos bloom, int expectedTotal) {
        List<ItemEntity> nearby = p.level().getEntitiesOfClass(ItemEntity.class, new AABB(bloom).inflate(1.5));
        List<ItemEntity> pollenDrops = nearby.stream()
                .filter(item -> item.getItem().is(FloraFaunaRegistry.SYMBIOTIC_POLLEN.get())).toList();
        int total = pollen(p) + pollenDrops.stream().mapToInt(item -> item.getItem().getCount()).sum();
        Playtest.require(total == expectedTotal, "Bloom at " + bloom.toShortString() + " yielded total "
                + total + " pollen (expected " + expectedTotal + ", inventory=" + pollen(p)
                + ", blockDrops=" + p.level().getGameRules().get(GameRules.BLOCK_DROPS)
                + ", nearby items=" + nearby.stream().map(item -> item.getItem().toString()).toList() + ")");
        if (pollen(p) == 0) {
            Playtest.require(pollenDrops.size() == 1 && pollenDrops.getFirst().getItem().getCount() == expectedTotal,
                    "One bloom should create one stack of " + expectedTotal + " pollen");
        }
        if (!pollenDrops.isEmpty()) {
            ItemEntity drop = pollenDrops.getFirst();
            Playtest.require(p.teleportTo((ServerLevel) p.level(), drop.getX(), 120, drop.getZ(),
                    Set.of(), 0, 25, true), "Could not move onto the pollen drop at " + bloom.toShortString());
        }
    }

    private static PlayerSymbioteData data(net.minecraft.world.entity.player.Player p) {
        return net.j40climb.florafauna.platform.AttachmentAccess.get(p, FloraFaunaRegistry.PLAYER_SYMBIOTE_DATA);
    }

    private static ProgressSignalTracker progress(ServerPlayer p) {
        return net.j40climb.florafauna.platform.AttachmentAccess.get(p, FloraFaunaRegistry.SYMBIOTE_PROGRESS_ATTACHMENT);
    }

    private static void useChamberButton(Playtest t, String action) {
        String key = "gui.florafauna.cocoon." + action;
        t.client("Open the cocoon for " + action, mc -> useBlock(mc, CHAMBER, Direction.SOUTH));
        t.awaitClient("Wait for enabled cocoon " + action, mc ->
                mc.screen instanceof CocoonChamberScreen && enabledButton(mc.screen, key) != null);
        t.client("Click cocoon " + action, mc -> {
            Button button = enabledButton(mc.screen, key);
            Playtest.require(button != null, "Missing enabled cocoon button " + key);
            MouseButtonEvent click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                    button.getY() + button.getHeight() / 2.0,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            Playtest.require(mc.screen.mouseClicked(click, false), "Cocoon ignored " + action);
        });
    }

    private static Button enabledButton(Screen screen, String key) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.active && button.visible
                        && button.getMessage().equals(Component.translatable(key))).findFirst().orElse(null);
    }

    private static void useBlock(Minecraft mc, BlockPos pos, Direction face) {
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    private static boolean hasChatKey(Minecraft mc, String key) {
        try {
            Field field = ChatComponent.class.getDeclaredField("allMessages");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            //? if <26.3 {
            List<GuiMessage> messages = (List<GuiMessage>) field.get(mc.gui.getChat());
            //?} else {
            /*List<GuiMessage> messages = (List<GuiMessage>) field.get(mc.gui.hud.getChat());
            *///?}
            return messages.stream().anyMatch(message ->
                    message.content().getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals(key) && !message.content().getString().equals(key));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect received chat messages", e);
        }
    }

    private static boolean particlesNear(Minecraft mc, Vec3 center) {
        try {
            Field groupsField = ParticleEngine.class.getDeclaredField("particles");
            Field queueField = ParticleGroup.class.getDeclaredField("particles");
            groupsField.setAccessible(true);
            queueField.setAccessible(true);
            Field xField = Particle.class.getDeclaredField("x");
            Field yField = Particle.class.getDeclaredField("y");
            Field zField = Particle.class.getDeclaredField("z");
            xField.setAccessible(true);
            yField.setAccessible(true);
            zField.setAccessible(true);
            Map<?, ?> groups = (Map<?, ?>) groupsField.get(mc.particleEngine);
            for (Object value : groups.values()) {
                if (!(value instanceof ParticleGroup<?> group)) continue;
                for (Object member : (Queue<?>) queueField.get(group)) {
                    if (!(member instanceof Particle particle)) continue;
                    if (Math.abs(xField.getDouble(particle) - center.x) < 0.8
                            && Math.abs(yField.getDouble(particle) - center.y) < 0.8
                            && Math.abs(zField.getDouble(particle) - center.z) < 0.8) {
                        return true;
                    }
                }
            }
            return false;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect client particles", e);
        }
    }
}
