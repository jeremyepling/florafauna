package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlock;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlockEntity;
import net.j40climb.florafauna.common.block.mobbarrier.MobBarrierBlockItem;
import net.j40climb.florafauna.common.block.mobbarrier.data.MobBarrierConfig;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlock;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlockEntity;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlock;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberMenu;
import net.j40climb.florafauna.common.block.husk.HuskBlock;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskType;
import net.j40climb.florafauna.common.block.mininganchor.Tier1MiningAnchorBlock;
import net.j40climb.florafauna.common.block.mininganchor.Tier1MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.Tier2MiningAnchorBlock;
import net.j40climb.florafauna.common.block.mininganchor.Tier2MiningAnchorBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier1PodBlock;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier1PodBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlock;
import net.j40climb.florafauna.common.block.mininganchor.pod.Tier2PodBlockEntity;
import net.j40climb.florafauna.common.block.mininganchor.pod.PodContents;
import net.j40climb.florafauna.common.block.mobtransport.MobInputBlock;
import net.j40climb.florafauna.common.block.mobtransport.MobInputBlockEntity;
import net.j40climb.florafauna.common.block.mobtransport.MobOutputBlock;
import net.j40climb.florafauna.common.block.mobtransport.MobOutputBlockEntity;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteData;
import net.j40climb.florafauna.common.entity.mobsymbiote.MobSymbioteItem;
import net.j40climb.florafauna.common.block.vacuum.BlockDropData;
import net.j40climb.florafauna.common.block.vacuum.ClaimedItemData;
import net.j40climb.florafauna.common.block.iteminput.fieldrelay.FieldRelayBlock;
import net.j40climb.florafauna.common.block.iteminput.fieldrelay.FieldRelayBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlock;
import net.j40climb.florafauna.common.block.iteminput.rootiteminput.ItemInputBlockEntity;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlock;
import net.j40climb.florafauna.common.block.iteminput.storageanchor.StorageAnchorBlockEntity;
import net.j40climb.florafauna.common.block.wood.WoodType;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frontpack.FrontpackData;
import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.entity.projectile.ThrownItemEntity;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.hammer.HammerItem;
import net.j40climb.florafauna.common.symbiote.binding.SymbiotePreparedEffect;
import net.j40climb.florafauna.common.symbiote.data.PlayerSymbioteData;
import net.j40climb.florafauna.common.symbiote.data.SymbioteData;
import net.j40climb.florafauna.common.symbiote.item.DormantSymbioteItem;
import net.j40climb.florafauna.common.symbiote.item.SymbioteStewItem;
import net.j40climb.florafauna.common.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.symbiote.voice.VoiceCooldownState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.*;

import java.util.function.Function;
import java.util.function.Supplier;

import static net.j40climb.florafauna.common.item.hammer.HammerItem.HAMMER_MATERIAL;

/**
 * Central registration class for all mod content.
 * All DeferredRegisters are consolidated here for easy overview.
 * Defines WHAT exists. No behavior, just registration.
 */
public class FloraFaunaRegistry {

    // ==================== REGISTRIES ====================

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FloraFauna.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FloraFauna.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FloraFauna.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FloraFauna.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FloraFauna.MOD_ID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FloraFauna.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FloraFauna.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FloraFauna.MOD_ID);

    // ==================== BLOCKS ====================

    public static final DeferredBlock<Block> TEAL_MOSS_BLOCK = registerBlock("teal_moss_block",
            props -> new Block(props.strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<ContainmentChamberBlock> SYMBIOTE_CONTAINMENT_CHAMBER = registerBlock("containment_chamber",
            props -> new ContainmentChamberBlock(props
                    .strength(5f, 6f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 5)
                    .noOcclusion()
            ));

    public static final DeferredBlock<CocoonChamberBlock> COCOON_CHAMBER = registerBlock("cocoon_chamber",
            props -> new CocoonChamberBlock(props
                    .strength(5f, 6f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SCULK)
                    .lightLevel(state -> 7)
                    .noOcclusion()
            ));

    public static final DeferredBlock<MobBarrierBlock> MOB_BARRIER = BLOCKS.registerBlock("mob_barrier",
            props -> new MobBarrierBlock(props
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
            ));

    // Register custom MobBarrierBlockItem (carries config component)
    static {
        ITEMS.registerItem("mob_barrier", props -> new MobBarrierBlockItem(MOB_BARRIER.get(), props));
    }

    public static final DeferredBlock<HuskBlock> HUSK = registerBlock("husk",
            props -> new HuskBlock(props
                    .strength(50f, 1200f)
                    .sound(SoundType.SCULK)
                    .lightLevel(state -> state.getValue(HuskBlock.HUSK_TYPE) == HuskType.RESTORATION ? 7 : 0)
                    .noOcclusion()
            ));

    // Item Input System blocks
    public static final DeferredBlock<StorageAnchorBlock> STORAGE_ANCHOR = registerBlock("storage_anchor",
            props -> new StorageAnchorBlock(props
                    .strength(3f, 6f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()
            ));

    public static final DeferredBlock<ItemInputBlock> ITEM_INPUT = registerBlock("item_input",
            props -> new ItemInputBlock(props
                    .strength(2f, 4f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.WOOD)
                    .noOcclusion()
            ));

    public static final DeferredBlock<FieldRelayBlock> FIELD_RELAY = registerBlock("field_relay",
            props -> new FieldRelayBlock(props
                    .strength(2f, 4f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)
                    .noOcclusion()
            ));

    // Mining Anchor System blocks
    public static final DeferredBlock<Tier1MiningAnchorBlock> TIER1_MINING_ANCHOR = registerBlock("tier1_mining_anchor",
            props -> new Tier1MiningAnchorBlock(props
                    .strength(3f, 6f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SCULK)
                    .noOcclusion()
            ));

    public static final DeferredBlock<Tier2MiningAnchorBlock> TIER2_MINING_ANCHOR = registerBlock("tier2_mining_anchor",
            props -> new Tier2MiningAnchorBlock(props
                    .strength(5f, 10f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
            ));

    // Storage Pod blocks (for Mining Anchor system)
    // Feral Pod: spawned by anchor, no item (spills contents when broken)
    public static final DeferredBlock<Tier1PodBlock> TIER1_POD = registerBlockNoItem("tier1_pod",
            props -> new Tier1PodBlock(props
                    .strength(2f, 4f)
                    .sound(SoundType.SCULK)
                    .noOcclusion()
            ));

    // Hardened Pod: player-placeable, has item (keeps contents like shulker box)
    public static final DeferredBlock<Tier2PodBlock> TIER2_POD = registerBlock("tier2_pod",
            props -> new Tier2PodBlock(props
                    .strength(3f, 6f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
            ));

    // Mob Transport System blocks
    public static final DeferredBlock<MobInputBlock> MOB_INPUT = registerBlock("mob_input",
            props -> new MobInputBlock(props
                    .strength(2f, 4f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SCULK)
                    .noOcclusion()
            ));

    public static final DeferredBlock<MobOutputBlock> MOB_OUTPUT = registerBlock("mob_output",
            props -> new MobOutputBlock(props
                    .strength(2f, 4f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.SCULK)
                    .noOcclusion()
            ));

    // Wood blocks registered via ModWoodType enum
    static { registerWoodTypes(); }
    private static void registerWoodTypes() { WoodType.values(); }

    // ==================== ENTITIES ====================
    // NOTE: Entities must be declared before spawn eggs to avoid forward reference errors

    public static final ResourceKey<EntityType<?>> GECKO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "gecko"));
    public static final ResourceKey<EntityType<?>> LIZARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "lizard"));
    public static final ResourceKey<EntityType<?>> FRENCHIE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "frenchie"));

    public static final Supplier<EntityType<GeckoEntity>> GECKO = ENTITY_TYPES.register("gecko",
            () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.5f, 0.35f).build(GECKO_KEY));

    public static final Supplier<EntityType<LizardEntity>> LIZARD = ENTITY_TYPES.register("lizard",
            () -> EntityType.Builder.of(LizardEntity::new, MobCategory.CREATURE)
                    .sized(1.4f, 1.35f).build(LIZARD_KEY));

    public static final Supplier<EntityType<FrenchieEntity>> FRENCHIE = ENTITY_TYPES.register("frenchie",
            () -> EntityType.Builder.of(FrenchieEntity::new, MobCategory.CREATURE)
                    .sized(0.9f, 0.9f).build(FRENCHIE_KEY));

    // Projectile entity
    public static final ResourceKey<EntityType<?>> THROWN_ITEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FloraFauna.MOD_ID, "thrown_item"));
    public static final Supplier<EntityType<ThrownItemEntity>> THROWN_ITEM = ENTITY_TYPES.register("thrown_item",
            () -> EntityType.Builder.<ThrownItemEntity>of(ThrownItemEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(THROWN_ITEM_KEY));

    // ==================== ITEMS ====================

    // Tools
    public static final DeferredItem<Item> HAMMER = ITEMS.registerItem("hammer", properties ->
            new HammerItem(properties.tool(HAMMER_MATERIAL, BlockTags.MINEABLE_WITH_PICKAXE, 8, -2.4f, 0)));

    // Custom Items
    public static final DeferredItem<Item> DORMANT_SYMBIOTE = ITEMS.registerItem("dormant_symbiote",
            DormantSymbioteItem::new);

    public static final DeferredItem<Item> SYMBIOTE_STEW = ITEMS.registerItem("symbiote_stew", properties ->
            new SymbioteStewItem(properties.component(net.minecraft.core.component.DataComponents.CONSUMABLE,
                    Consumable.builder()
                            .consumeSeconds(2.0f)
                            .animation(ItemUseAnimation.DRINK)
                            .sound(SoundEvents.GENERIC_DRINK)
                            .hasConsumeParticles(true)
                            .build()
            )));

    public static final DeferredItem<Item> MOB_SYMBIOTE = ITEMS.registerItem("mob_symbiote",
            MobSymbioteItem::new);

    // Spawn Eggs (entities must be declared before these)
    public static final DeferredItem<Item> GECKO_SPAWN_EGG = ITEMS.registerItem("gecko_spawn_egg", properties ->
            new SpawnEggItem(properties.spawnEgg(GECKO.get())));
    public static final DeferredItem<Item> LIZARD_SPAWN_EGG = ITEMS.registerItem("lizard_spawn_egg", properties ->
            new SpawnEggItem(properties.spawnEgg(LIZARD.get())));
    public static final DeferredItem<Item> FRENCHIE_SPAWN_EGG = ITEMS.registerItem("frenchie_spawn_egg", properties ->
            new SpawnEggItem(properties.spawnEgg(FRENCHIE.get())));

    // ==================== BLOCK ENTITIES ====================

    public static final Supplier<BlockEntityType<ContainmentChamberBlockEntity>> CONTAINMENT_CHAMBER_BE = BLOCK_ENTITIES.register(
            "containment_chamber",
            () -> new BlockEntityType<>(ContainmentChamberBlockEntity::new, false, SYMBIOTE_CONTAINMENT_CHAMBER.get()));

    public static final Supplier<BlockEntityType<CocoonChamberBlockEntity>> COCOON_CHAMBER_BE = BLOCK_ENTITIES.register(
            "cocoon_chamber",
            () -> new BlockEntityType<>(CocoonChamberBlockEntity::new, false, COCOON_CHAMBER.get()));

    public static final Supplier<BlockEntityType<HuskBlockEntity>> HUSK_BE = BLOCK_ENTITIES.register(
            "husk",
            () -> new BlockEntityType<>(HuskBlockEntity::new, false, HUSK.get()));

    public static final Supplier<BlockEntityType<MobBarrierBlockEntity>> MOB_BARRIER_BE = BLOCK_ENTITIES.register(
            "mob_barrier",
            () -> new BlockEntityType<>(MobBarrierBlockEntity::new, false, MOB_BARRIER.get()));

    // Item Input System block entities
    public static final Supplier<BlockEntityType<StorageAnchorBlockEntity>> STORAGE_ANCHOR_BE = BLOCK_ENTITIES.register(
            "storage_anchor",
            () -> new BlockEntityType<>(StorageAnchorBlockEntity::new, false, STORAGE_ANCHOR.get()));

    public static final Supplier<BlockEntityType<ItemInputBlockEntity>> ITEM_INPUT_BE = BLOCK_ENTITIES.register(
            "item_input",
            () -> new BlockEntityType<>(ItemInputBlockEntity::new, false, ITEM_INPUT.get()));

    public static final Supplier<BlockEntityType<FieldRelayBlockEntity>> FIELD_RELAY_BE = BLOCK_ENTITIES.register(
            "field_relay",
            () -> new BlockEntityType<>(FieldRelayBlockEntity::new, false, FIELD_RELAY.get()));

    // Mining Anchor System block entities
    public static final Supplier<BlockEntityType<Tier1MiningAnchorBlockEntity>> TIER1_MINING_ANCHOR_BE = BLOCK_ENTITIES.register(
            "tier1_mining_anchor",
            () -> new BlockEntityType<>(Tier1MiningAnchorBlockEntity::new, false, TIER1_MINING_ANCHOR.get()));

    public static final Supplier<BlockEntityType<Tier2MiningAnchorBlockEntity>> TIER2_MINING_ANCHOR_BE = BLOCK_ENTITIES.register(
            "tier2_mining_anchor",
            () -> new BlockEntityType<>(Tier2MiningAnchorBlockEntity::new, false, TIER2_MINING_ANCHOR.get()));

    // Storage Pod block entities
    public static final Supplier<BlockEntityType<Tier1PodBlockEntity>> TIER1_POD_BE = BLOCK_ENTITIES.register(
            "tier1_pod",
            () -> new BlockEntityType<>(Tier1PodBlockEntity::new, false, TIER1_POD.get()));

    public static final Supplier<BlockEntityType<Tier2PodBlockEntity>> TIER2_POD_BE = BLOCK_ENTITIES.register(
            "tier2_pod",
            () -> new BlockEntityType<>(Tier2PodBlockEntity::new, false, TIER2_POD.get()));

    // Mob Transport System block entities
    public static final Supplier<BlockEntityType<MobInputBlockEntity>> MOB_INPUT_BE = BLOCK_ENTITIES.register(
            "mob_input",
            () -> new BlockEntityType<>(MobInputBlockEntity::new, false, MOB_INPUT.get()));

    public static final Supplier<BlockEntityType<MobOutputBlockEntity>> MOB_OUTPUT_BE = BLOCK_ENTITIES.register(
            "mob_output",
            () -> new BlockEntityType<>(MobOutputBlockEntity::new, false, MOB_OUTPUT.get()));

    // ==================== MENUS ====================

    public static final DeferredHolder<MenuType<?>, MenuType<ContainmentChamberMenu>> CONTAINMENT_CHAMBER_MENU =
            registerMenuType("containment_chamber", ContainmentChamberMenu::new);

    // ==================== MOB EFFECTS ====================

    public static final Holder<MobEffect> SYMBIOTE_PREPARED = MOB_EFFECTS.register(
            "symbiote_prepared",
            SymbiotePreparedEffect::new);

    // ==================== DATA COMPONENTS ====================

    // Tool ability data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MiningModeData>> MULTI_BLOCK_MINING =
            DATA_COMPONENTS.registerComponentType("multi_block_mining",
                    builder -> builder.persistent(MiningModeData.CODEC).networkSynchronized(MiningModeData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolConfig>> TOOL_CONFIG =
            DATA_COMPONENTS.registerComponentType("tool_config",
                    builder -> builder.persistent(ToolConfig.CODEC).networkSynchronized(ToolConfig.STREAM_CODEC));

    // Marker components for abilities
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LIGHTNING_ABILITY =
            DATA_COMPONENTS.registerComponentType("lightning_ability",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> TELEPORT_SURFACE_ABILITY =
            DATA_COMPONENTS.registerComponentType("teleport_surface_ability",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    // Symbiote item data components
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SymbioteData>> SYMBIOTE_DATA =
            DATA_COMPONENTS.registerComponentType("symbiote_data",
                    builder -> builder.persistent(SymbioteData.CODEC).networkSynchronized(SymbioteData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProgressSignalTracker>> SYMBIOTE_PROGRESS =
            DATA_COMPONENTS.registerComponentType("symbiote_progress",
                    builder -> builder.persistent(ProgressSignalTracker.CODEC).networkSynchronized(ProgressSignalTracker.STREAM_CODEC));

    // Throwable ability data component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ThrowableAbilityData>> THROWABLE_ABILITY =
            DATA_COMPONENTS.registerComponentType("throwable_ability",
                    builder -> builder.persistent(ThrowableAbilityData.CODEC).networkSynchronized(ThrowableAbilityData.STREAM_CODEC));

    // Mob barrier config data component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MobBarrierConfig>> MOB_BARRIER_CONFIG =
            DATA_COMPONENTS.registerComponentType("mob_barrier_config",
                    builder -> builder.persistent(MobBarrierConfig.CODEC).networkSynchronized(MobBarrierConfig.STREAM_CODEC));

    // Pod contents data component (for Hardened Pods that keep items when broken)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PodContents>> POD_CONTENTS =
            DATA_COMPONENTS.registerComponentType("pod_contents",
                    builder -> builder.persistent(PodContents.CODEC).networkSynchronized(PodContents.STREAM_CODEC));

    // ==================== ATTACHMENT TYPES ====================

    public static final Supplier<AttachmentType<PlayerSymbioteData>> PLAYER_SYMBIOTE_DATA =
            ATTACHMENT_TYPES.register("player_symbiote_data", () ->
                    AttachmentType.builder(() -> PlayerSymbioteData.DEFAULT)
                            .serialize(PlayerSymbioteData.CODEC.fieldOf("player_symbiote_data"))
                            .sync(PlayerSymbioteData.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<FrontpackData>> FRENCH_FRONTPACK_DATA =
            ATTACHMENT_TYPES.register("french_frontpack_data", () ->
                    AttachmentType.builder(() -> FrontpackData.DEFAULT)
                            .serialize(FrontpackData.CODEC.fieldOf("french_frontpack_data"))
                            .sync(FrontpackData.STREAM_CODEC)
                            .build());

    public static final Supplier<AttachmentType<ProgressSignalTracker>> SYMBIOTE_PROGRESS_ATTACHMENT =
            ATTACHMENT_TYPES.register("symbiote_progress", () ->
                    AttachmentType.builder(() -> ProgressSignalTracker.DEFAULT)
                            .serialize(ProgressSignalTracker.CODEC.fieldOf("symbiote_progress"))
                            .sync(ProgressSignalTracker.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<VoiceCooldownState>> VOICE_COOLDOWNS =
            ATTACHMENT_TYPES.register("voice_cooldowns", () ->
                    AttachmentType.builder(() -> VoiceCooldownState.DEFAULT)
                            .serialize(VoiceCooldownState.CODEC.fieldOf("voice_cooldowns"))
                            .sync(VoiceCooldownState.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    // Vacuum System entity attachments (for ItemEntity)
    public static final Supplier<AttachmentType<ClaimedItemData>> CLAIMED_ITEM_DATA =
            ATTACHMENT_TYPES.register("claimed_item_data", () ->
                    AttachmentType.builder(() -> ClaimedItemData.DEFAULT)
                            .serialize(ClaimedItemData.CODEC.fieldOf("claimed_item_data"))
                            .sync(ClaimedItemData.STREAM_CODEC)
                            .build());

    // Block drop tracking (marks items that came from block breaking)
    public static final Supplier<AttachmentType<BlockDropData>> BLOCK_DROP_DATA =
            ATTACHMENT_TYPES.register("block_drop_data", () ->
                    AttachmentType.builder(() -> BlockDropData.DEFAULT)
                            .serialize(BlockDropData.CODEC.fieldOf("block_drop_data"))
                            .sync(BlockDropData.STREAM_CODEC)
                            .build());

    // Mob symbiote attachment (for marking mobs as bonded for capture)
    public static final Supplier<AttachmentType<MobSymbioteData>> MOB_SYMBIOTE_DATA =
            ATTACHMENT_TYPES.register("mob_symbiote_data", () ->
                    AttachmentType.builder(() -> MobSymbioteData.DEFAULT)
                            .serialize(MobSymbioteData.CODEC.fieldOf("mob_symbiote_data"))
                            .sync(MobSymbioteData.STREAM_CODEC)
                            .build());

    // ==================== HELPER METHODS ====================

    /**
     * Registers a block and automatically creates its BlockItem.
     */
    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> blockToRegister = BLOCKS.registerBlock(name, function);
        ITEMS.registerItem(name, (properties) -> new BlockItem(blockToRegister.get(), properties.useBlockDescriptionPrefix()));
        return blockToRegister;
    }

    /**
     * Registers a block without creating a BlockItem.
     * Used for blocks that are spawned programmatically (e.g., feral pods).
     */
    public static <T extends Block> DeferredBlock<T> registerBlockNoItem(String name, Function<BlockBehaviour.Properties, T> function) {
        return BLOCKS.registerBlock(name, function);
    }

    /**
     * Helper to register menu types.
     */
    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(
            String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    // ==================== INITIALIZATION ====================

    /**
     * Registers all registries to the mod event bus.
     */
    public static void init(IEventBus modEventBus) {
        // DATA_COMPONENTS must be registered before ITEMS because MobBarrierBlockItem uses MOB_BARRIER_CONFIG
        DATA_COMPONENTS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
