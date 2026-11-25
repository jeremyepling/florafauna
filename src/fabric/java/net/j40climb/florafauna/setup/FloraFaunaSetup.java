package net.j40climb.florafauna.setup;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.FloraFaunaCommands;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.CocoonActionPayload;
import net.j40climb.florafauna.common.block.cocoonchamber.networking.OpenCocoonScreenPayload;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.networking.ItemInputAnimationPayload;
import net.j40climb.florafauna.common.block.mininganchor.networking.AnchorFillStatePayload;
import net.j40climb.florafauna.common.block.mobbarrier.networking.UpdateMobBarrierConfigPayload;
import net.j40climb.florafauna.common.block.wood.WoodBlockSet;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.symbiote.abilities.DashPayload;
import net.j40climb.florafauna.common.symbiote.dialogue.SymbioteDialogueLoader;
import net.j40climb.florafauna.dev.DevPlotCommands;
import net.j40climb.florafauna.mixin.SpawnPlacementsInvoker;
import net.j40climb.florafauna.noclip.NoClipPayload;
import net.j40climb.florafauna.platform.StorageAccess;
import net.j40climb.florafauna.platform.network.IPayloadContext;
import net.j40climb.florafauna.platform.registry.FabricRegistries.Entries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;

/** Fabric setup for the same content and behavior as NeoForge. */
public final class FloraFaunaSetup {
    private FloraFaunaSetup() {}
    public static final Entries<CreativeModeTab> CREATIVE_TABS = new Entries<>(BuiltInRegistries.CREATIVE_MODE_TAB);

    public static final Supplier<CreativeModeTab> FLORAFAUNA_ITEMS_TAB =
            CREATIVE_TABS.register("florafauna_items_tab", () -> FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.florafauna.florafauna_items_tab"))
                    .icon(() -> new ItemStack(FloraFaunaRegistry.DORMANT_SYMBIOTE.get()))
                    .displayItems((parameters, output) -> FloraFaunaCreativeItems.accept(output)).build());

    public static void init() {
        CREATIVE_TABS.register();
        registerNetworking();
        FabricDefaultAttributeRegistry.register(FloraFaunaRegistry.GECKO.get(), GeckoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FloraFaunaRegistry.LIZARD.get(), LizardEntity.createAttributes());
        SpawnPlacementsInvoker.florafauna$register(FloraFaunaRegistry.GECKO.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacementsInvoker.florafauna$register(FloraFaunaRegistry.LIZARD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.FOREST),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "resonant_bloom")));
        ItemStorage.SIDED.registerForBlockEntity((pod, side) -> StorageAccess.expose(pod.getItemHandler()),
                FloraFaunaRegistry.TIER2_POD_BE.get());
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) -> {
            FloraFaunaCommands.register(dispatcher);
            DevPlotCommands.register(dispatcher);
        });
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "symbiote_dialogue"), new SymbioteDialogueLoader());
    }

    private static void registerNetworking() {
        serverbound(DashPayload.TYPE, DashPayload.STREAM_CODEC, DashPayload::onServerReceived);
        serverbound(CocoonActionPayload.TYPE, CocoonActionPayload.STREAM_CODEC, CocoonActionPayload::onServerReceived);
        serverbound(UpdateMobBarrierConfigPayload.TYPE, UpdateMobBarrierConfigPayload.STREAM_CODEC, UpdateMobBarrierConfigPayload::onServerReceived);
        serverbound(NoClipPayload.TYPE, NoClipPayload.STREAM_CODEC, NoClipPayload::onServerReceived);
        PayloadTypeRegistry.clientboundPlay().register(ItemInputAnimationPayload.TYPE, ItemInputAnimationPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenCocoonScreenPayload.TYPE, OpenCocoonScreenPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AnchorFillStatePayload.TYPE, AnchorFillStatePayload.STREAM_CODEC);
    }

    private static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, IPayloadContext> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.accept(payload, new IPayloadContext(context.player(), context.server())));
    }
}
