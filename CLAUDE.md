# Flora Fauna Mod

NeoForge 1.21.10 Minecraft mod.

## Build & Run

### Command Line (Gradle)
```bash
./gradlew build          # Build the mod
./gradlew runClient      # Run Minecraft client with mod
./gradlew runServer      # Run Minecraft server with mod
./gradlew runData        # Run data generation (client + server)
```

### IDE Debugging

The project is configured to work in both **IntelliJ IDEA** and **Cursor (VS Code)**:

**IntelliJ IDEA:**
- Run configurations are in `.run/` directory
- Use the Run/Debug dropdown to select: Client, Server, Build, or Data Generation
- Click the debug button (🐞) to start debugging
- All configurations use Gradle tasks for compatibility

**Cursor / VS Code:**
- Debug configurations are in `.vscode/launch.json`
- Press F5 or click "Run and Debug" in the sidebar
- Select from: Client, ClientData, Server, ServerData, or GameTestServer
- All configs auto-build before launching (`preLaunchTask: gradle-build`)

**Important:** Both IDEs use the same output directory: `build/classes/java/main`

## Project Structure

The mod is organized using **feature-based architecture** where related code is grouped together:

```
src/main/java/net/j40climb/florafauna/
├── FloraFauna.java              # Main mod class
├── Config.java                  # Mod configuration
├── client/                      # Client-only code
│   ├── events/                  # Client event handlers (KeyInputEvents, RenderEvents, etc.)
│   ├── CustomBlockOutlineRenderer.java
│   ├── KeyMappings.java
│   └── ...
└── common/                      # Shared/server code
    ├── Register*.java           # Top-level registries (RegisterAttachmentTypes, RegisterDataComponentTypes, etc.)
    ├── block/                   # Block feature
    │   ├── RegisterBlocks.java  # Block registry
    │   ├── RegisterBlockEntities.java
    │   ├── wood/                # Wood block system
    │   │   ├── ModWoodType.java
    │   │   ├── WoodBlockSet.java
    │   │   ├── WoodBlockRegistration.java
    │   │   └── WoodStripping.java
    │   └── containmentchamber/  # Feature directory (all related files together)
    │       ├── ContainmentChamberBlock.java
    │       ├── ContainmentChamberBlockEntity.java
    │       ├── ContainmentChamberMenu.java
    │       └── ContainmentChamberScreen.java
    ├── entity/                  # Entity feature
    │   ├── RegisterEntities.java
    │   ├── RegisterEntityEvents.java
    │   ├── frenchie/            # Each entity has its own directory
    │   │   ├── FrenchieEntity.java
    │   │   ├── FrenchieModel.java
    │   │   ├── FrenchieRenderer.java
    │   │   ├── FrenchieAnimations.java
    │   │   ├── FrenchieRenderState.java
    │   │   └── FrenchieVariant.java
    │   ├── gecko/
    │   ├── lizard/
    │   └── frontpack/           # Feature with nested subdirectories
    │       ├── FrenchFrontpackLayer.java
    │       └── networking/      # Feature-specific networking
    ├── item/                    # Item feature
    │   ├── RegisterItems.java
    │   ├── RegisterCreativeModeTabs.java
    │   ├── energyhammer/        # Custom item feature directory
    │   │   ├── EnergyHammerItem.java
    │   │   └── networking/
    │   └── symbiote/            # Complex item with multiple subsystems
    │       ├── SymbioteItem.java
    │       ├── SymbioteCommand.java
    │       ├── SymbioteData.java
    │       ├── dialogue/
    │       ├── event/
    │       ├── networking/
    │       └── tracking/
    ├── datagen/                 # Data generation providers
    │   ├── RegisterDataGenerators.java
    │   ├── RegisterModelProvider.java
    │   ├── RegisterBlockTagsProvider.java
    │   ├── RegisterItemTagsProvider.java
    │   ├── RegisterBlockLootTableProvider.java
    │   └── RegisterRecipeProvider.java
    └── util/                    # Utility classes
```

**Key Principles:**
- **`Register*` naming**: All registration classes use `Register` prefix (e.g., `RegisterBlocks`, `RegisterItems`)
- **Feature directories**: Complex features get their own subdirectory with all related code (entity, renderer, model, animations, etc.)
- **Client code in features**: Client-side rendering code (Models, Renderers, Screens) can live within feature directories in `common/`
- **Separate client directory**: Pure client code (key bindings, client events, utilities) goes in `client/`
- **Networking per feature**: Network packets live in `networking/` subdirectories within their feature

## Minecraft/NeoForge Source Reference

The decompiled Minecraft and NeoForge source code is available in:
```
C:\Users\jerem\IdeaProjects\florafauna\build\moddev\artifacts\neoforge-21.10.64-merged.jar
```

This JAR contains:
- `net/minecraft/` - Minecraft classes (entities, blocks, items, world, etc.)
- `net/neoforged/` - NeoForge API and hooks

**Use this to:**
- Reference vanilla Minecraft implementations
- See how existing features work (entities, blocks, items, etc.)
- Find available methods and classes
- Understand NeoForge event systems and hooks

**Example usage:**
```bash
# List all entity classes
jar tf build/moddev/artifacts/neoforge-21.10.64-merged.jar | grep "net/minecraft/world/entity/"

# Extract a specific class for reference
jar xf build/moddev/artifacts/neoforge-21.10.64-merged.jar net/minecraft/world/entity/Entity.class
```

## Block Registration

### Regular Blocks
Defined in `common/block/RegisterBlocks.java`. Each block is a `public static final` field:
```java
public static final DeferredBlock<Block> TEAL_MOSS_BLOCK = registerBlock("teal_moss_block",
    props -> new Block(props.strength(4f).requiresCorrectToolForDrops()));
```

**Access:**
- `RegisterBlocks.TEAL_MOSS_BLOCK.get()` - Get the Block instance
- `RegisterBlocks.TEAL_MOSS_BLOCK` - Get DeferredBlock (for creative tabs, recipes, etc.)

### Wood Blocks (Enum-driven)
Wood blocks use a special pattern where the `ModWoodType` enum handles registration automatically.

**To add a new wood type:**
1. Add enum entry in `common/block/wood/ModWoodType.java`:
   ```java
   DRIFTWOOD("driftwood"),
   MAPLE("maple");  // Just add this!
   ```
2. Add lang entries in `en_us.json`
3. Add textures (see Textures section)

**Files involved** (`common/block/wood/`):
- `ModWoodType.java` - Enum that triggers registration (add new wood types here)
- `WoodBlockSet.java` - Record holding all blocks for a wood type
- `WoodBlockRegistration.java` - Creates log, stripped_log, wood, stripped_wood, planks
- `WoodStripping.java` - Event handler for axe stripping behavior

**Access wood blocks:**
```java
ModWoodType.DRIFTWOOD.getBlockSet().log()         // DeferredBlock
ModWoodType.DRIFTWOOD.getBlockSet().log().get()   // Block instance
ModWoodType.DRIFTWOOD.getBlockSet().planks()      // Different block type
```

**Iterate all wood types:**
```java
for (ModWoodType woodType : ModWoodType.values()) {
    WoodBlockSet wood = woodType.getBlockSet();
    // wood.log(), wood.planks(), etc.
}
```

## Item Registration

Items are registered in `common/item/RegisterItems.java`. BlockItems are automatically registered by `RegisterBlocks.registerBlock()`.

```java
public static final DeferredItem<Item> TOMATO = ITEMS.registerItem("tomato",
    properties -> new Item(properties));
```

**Access:**
- `RegisterItems.TOMATO.get()` - Get the Item instance
- `RegisterItems.TOMATO` - Get DeferredItem

## Entity Registration

Entities are registered in `common/entity/RegisterEntities.java`:
```java
public static final DeferredHolder<EntityType<?>, EntityType<GeckoEntity>> GECKO =
    ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(...).build(...));
```

**Entity organization:**
- Each entity gets its own directory under `common/entity/{entity_name}/`
- Directory contains: Entity class, Model, Renderer, Animations, RenderState, Variant (if applicable)
- Renderers are registered in `FloraFauna.java` ClientModEvents

## Creative Tabs

Creative tabs are defined in `common/item/RegisterCreativeModeTabs.java`. Wood blocks iterate via `ModWoodType.values()`.

## Registration Order

In `FloraFauna.java`, registries are initialized in this order:
1. `RegisterCreativeModeTabs.register(modEventBus)`
2. `RegisterBlocks.register(modEventBus)` - Must be before RegisterItems
3. `RegisterItems.register(modEventBus)`
4. `RegisterDataComponentTypes.register(modEventBus)`
5. `RegisterAttachmentTypes.register(modEventBus)`
6. `RegisterEntities.register(modEventBus)`
7. `RegisterBlockEntities.register(modEventBus)`
8. `RegisterMenus.register(modEventBus)`

BlockItems are added during `RegisterBlocks` initialization, which is why blocks must be registered before items.

## Data Generation

Run `./gradlew runData` to generate JSON files.

**Data providers** (in `common/datagen/`):
- `RegisterModelProvider.java` - Block/item models (wood blocks iterate via enum)
- `RegisterBlockTagsProvider.java` - Block tags (logs, planks, mineable)
- `RegisterItemTagsProvider.java` - Item tags
- `RegisterBlockLootTableProvider.java` - Loot tables (drop self)
- `RegisterRecipeProvider.java` - Crafting recipes (planks from logs, wood from logs)
- `RegisterDataGenerators.java` - Event handlers for data generation

Data providers are registered in `FloraFauna.java` via:
```java
modEventBus.addListener(RegisterDataGenerators::gatherClientData);
modEventBus.addListener(RegisterDataGenerators::gatherServerData);
```

## Assets & Resources

### Textures
- Block textures: `assets/florafauna/textures/block/`
- Item textures: `assets/florafauna/textures/item/`
- Entity textures: `assets/florafauna/textures/entity/`

**Wood block textures:**
- `{name}_log.png`, `{name}_log_top.png` - Log side and top
- `stripped_{name}_log.png`, `stripped_{name}_log_top.png` - Stripped log
- `{name}_planks.png` - Planks

### Models
- Block models: `assets/florafauna/models/block/`
- Item models: `assets/florafauna/models/item/`

### Lang
- `assets/florafauna/lang/en_us.json` - English translations

**IMPORTANT: Always use translatable text**
- **Never** hardcode user-facing strings in Java code (e.g., `Component.literal("Enchantment:")`)
- **Always** use translation keys with `Component.translatable()`
- Add translation keys to `assets/florafauna/lang/en_us.json`

**Example:**
```java
// BAD - hardcoded string
guiGraphics.drawString(this.font, "Enchantment:", x, y, color);

// GOOD - translatable
guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.energy_hammer_config.enchantment"), x, y, color);
```

Then add to `en_us.json`:
```json
"gui.florafauna.energy_hammer_config.enchantment": "Enchantment:"
```

## Event Handlers

Events use `@EventBusSubscriber` for auto-discovery:

**Common/Server events:**
- Registered on `NeoForge.EVENT_BUS`
- Located in feature directories (e.g., `common/block/wood/WoodStripping.java`)
- Located in `common/entity/RegisterEntityEvents.java`

**Client events:**
- Registered on `NeoForge.EVENT_BUS` with `Dist.CLIENT`
- Located in `client/events/` directory (KeyInputEvents, RenderEvents, PlayerEvents)
- Located in feature directories (e.g., `common/entity/frontpack/FrontpackRendererEvents.java`)

**Mod bus events:**
- Client setup events in `FloraFauna.ClientModEvents` static class
- Used for entity renderer registration, screen registration, etc.

**Important:** Avoid static initializers that access blocks - use lazy initialization instead, since static blocks run before registries are populated.

## Adding a New Feature

To add a new feature (e.g., a custom entity):

1. **Create feature directory**: `common/entity/myentity/`
2. **Add entity class**: `MyEntity.java`
3. **Add rendering (if needed)**: `MyEntityModel.java`, `MyEntityRenderer.java`, `MyEntityRenderState.java`
4. **Register entity**: Add to `RegisterEntities.java`
5. **Register renderer**: Add to `FloraFauna.ClientModEvents.onClientSetup()`
6. **Add spawn egg**: Add to `RegisterItems.java`
7. **Add datagen**: Add tags, loot tables, recipes in respective providers
8. **Add assets**: Textures, models, lang entries
9. **Add events (if needed)**: Create event handler in feature directory or `RegisterEntityEvents.java`
10. **Add networking (if needed)**: Create `networking/` subdirectory in feature

## Networking

Network packets are organized per-feature in `networking/` subdirectories:
- `common/entity/frontpack/networking/PutDownFrenchiePayload.java`
- `common/item/energyhammer/networking/`
- `common/item/symbiote/networking/`

Central networking registration is in `common/RegisterNetworking.java`.

## Utilities

Utility classes go in `common/util/`:
- General-purpose helper classes
- Shared utilities used across features

## GameTest Framework

The mod uses NeoForge's GameTest framework for automated testing of game logic.

### Running Tests

```bash
# Run all game tests (headless server, exits with test results)
./gradlew runGameTestServer

# In-game during regular gameplay
/test runall                    # Run all tests
/test run florafauna:<name>     # Run specific test
/test runfailed                 # Re-run failed tests
```

### Test Organization

Tests are in `test/FloraFaunaGameTests.java`:
- **Registration**: `FloraFaunaGameTests.register(modEventBus)` called from `FloraFauna.java`
- **Structure**: Uses `empty_1x1x1.nbt` minimal structure template
- **Environment**: Tests run in `florafauna:default` test environment

### Current Tests (14 total)

**VoiceCooldownState (5 tests)** - Cooldown logic for symbiote voice system:
- `voice_cooldown_initial_state` - Fresh state allows speaking
- `voice_cooldown_tier1_can_speak` - Cooldown blocks then expires
- `voice_cooldown_tier1_blocked_during_cooldown` - Blocked during cooldown window
- `voice_cooldown_tier2_lockout` - Tier 2 locks out Tier 1
- `voice_cooldown_category_dampening` - Category dampening works

**ProgressSignalTracker (4 tests)** - State machine for concept progress:
- `progress_signal_initial_state` - Empty tracker defaults
- `progress_signal_state_transitions` - Signal increments and state changes
- `progress_signal_stall_detection` - Stall score calculation
- `progress_signal_dream_state_tracking` - Dream state updates

**ChaosSuppressor (2 tests)** - Damage-based voice suppression:
- `chaos_suppressor_initial_state` - Fresh player not suppressed
- `chaos_suppressor_threshold` - 5 damage events triggers suppression

**SymbioteLineRepository (2 tests)** - Voice line selection:
- `line_repository_empty_returns_empty` - Empty repository behavior
- `line_repository_dream_selection` - Repository construction

### Adding New Tests

1. Add test method in `FloraFaunaGameTests.java`:
```java
private static void testMyFeature(GameTestHelper helper) {
    // Test logic here
    if (somethingWrong) {
        throw helper.assertionException("Description of failure");
    }
    helper.succeed();  // Must call this if test passes
}
```

2. Register the test in the appropriate `register*Tests` method:
```java
registerTest(event, env, "my_feature_test", FloraFaunaGameTests::testMyFeature);
```

### Key Test Utilities

- `helper.makeMockServerPlayerInLevel()` - Create a mock player for testing
- `helper.succeed()` - Mark test as passed
- `helper.assertionException("message")` - Create failure exception
- `helper.getLevel()` - Get the ServerLevel
- `helper.spawn(EntityType, BlockPos)` - Spawn entities

### Test Files

- `test/FloraFaunaGameTests.java` - Main test class with all tests
- `test/SimpleGameTestInstance.java` - Custom GameTestInstance wrapper
- `resources/data/florafauna/structure/empty_1x1x1.nbt` - Minimal structure template
