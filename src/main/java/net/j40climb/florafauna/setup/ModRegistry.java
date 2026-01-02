package net.j40climb.florafauna.setup;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.block.CopperGolemBarrierBlock;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlock;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberBlockEntity;
import net.j40climb.florafauna.common.block.cocoonchamber.CocoonChamberMenu;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlock;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberBlockEntity;
import net.j40climb.florafauna.common.block.containmentchamber.ContainmentChamberMenu;
import net.j40climb.florafauna.common.block.husk.HuskBlock;
import net.j40climb.florafauna.common.block.husk.HuskBlockEntity;
import net.j40climb.florafauna.common.block.husk.HuskType;
import net.j40climb.florafauna.common.block.wood.ModWoodType;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieEntity;
import net.j40climb.florafauna.common.entity.frontpack.FrontpackData;
import net.j40climb.florafauna.common.entity.gecko.GeckoEntity;
import net.j40climb.florafauna.common.entity.lizard.LizardEntity;
import net.j40climb.florafauna.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna.common.item.hammer.HammerItem;
import net.j40climb.florafauna.common.item.symbiote.*;
import net.j40climb.florafauna.common.item.symbiote.progress.ProgressSignalTracker;
import net.j40climb.florafauna.common.item.symbiote.voice.VoiceCooldownState;
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
 */
public class ModRegistry {

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

    public static final DeferredBlock<Block> COPPER_GOLEM_BARRIER = registerBlock("copper_golem_barrier",
            props -> new CopperGolemBarrierBlock(props
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE)
            ));

    public static final DeferredBlock<HuskBlock> HUSK = registerBlock("husk",
            props -> new HuskBlock(props
                    .strength(50f, 1200f)
                    .sound(SoundType.SCULK)
                    .lightLevel(state -> state.getValue(HuskBlock.HUSK_TYPE) == HuskType.RESTORATION ? 7 : 0)
                    .noOcclusion()
            ));

    // Wood blocks registered via ModWoodType enum
    static { registerWoodTypes(); }
    private static void registerWoodTypes() { ModWoodType.values(); }

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

    // ==================== MENUS ====================

    public static final DeferredHolder<MenuType<?>, MenuType<ContainmentChamberMenu>> CONTAINMENT_CHAMBER_MENU =
            registerMenuType("containment_chamber", ContainmentChamberMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<CocoonChamberMenu>> COCOON_CHAMBER_MENU =
            registerMenuType("cocoon_chamber", CocoonChamberMenu::new);

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
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
