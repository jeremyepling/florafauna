# Flora Fauna Mod

NeoForge 1.21.11 Minecraft mod.

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
- Click the debug button to start debugging
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
│   ├── KeyMappings.java         # Key bindings (R, M, P, Mouse4)
│   ├── ClientUtils.java         # Client utilities
│   ├── events/                  # Client event handlers
│   │   ├── KeyInputEvents.java  # Input event handlers
│   │   └── RenderEvents.java    # Rendering event handlers
│   └── gui/                     # GUI base classes
│       ├── BaseContainerScreen.java
│       ├── BaseInventoryScreen.java
│       └── InventoryRenderHelper.java
├── common/                      # Shared/server code
│   ├── Register*.java           # Top-level registries
│   │   ├── RegisterAttachmentTypes.java   # Player attachment data (4 types)
│   │   ├── RegisterDataComponentTypes.java # Item data components (5 types)
│   │   ├── RegisterMenus.java             # Container menus
│   │   └── RegisterNetworking.java        # Network payload registration
│   ├── block/                   # Block feature
│   │   ├── RegisterBlocks.java            # Block registry (3 misc blocks + wood)
│   │   ├── RegisterBlockEntities.java     # Block entity registry
│   │   ├── CopperGolemBarrierBlock.java
│   │   └── containmentchamber/            # Feature directory
│   │       ├── ContainmentChamberBlock.java
│   │       ├── ContainmentChamberBlockEntity.java
│   │       ├── ContainmentChamberMenu.java
│   │       └── ContainmentChamberScreen.java
│   ├── wood/                    # Wood block system
│   │   ├── ModWoodType.java               # Enum (DRIFTWOOD)
│   │   ├── WoodBlockSet.java              # Record holding 8 block types
│   │   ├── WoodBlockRegistration.java     # Auto-registers all wood blocks
│   │   └── WoodStripping.java             # Axe stripping behavior
│   ├── entity/                  # Entity feature
│   │   ├── RegisterEntities.java          # Entity registry (3 entities)
│   │   ├── RegisterEntityEvents.java      # Spawn placement & attributes
│   │   ├── frenchie/                      # Frenchie entity (6 files)
│   │   ├── gecko/                         # Gecko entity (6 files)
│   │   ├── lizard/                        # Lizard entity (5 files)
│   │   └── frontpack/                     # Frontpack carrying system (8 files)
│   │       ├── FrontpackData.java
│   │       ├── FrontpackEvents.java
│   │       ├── FrontpackLayer.java
│   │       ├── FrontpackModel.java
│   │       ├── FrontpackAnimations.java
│   │       ├── FrontpackRendererEvents.java
│   │       └── networking/
│   │           └── PutDownFrenchiePayload.java
│   ├── item/                    # Item feature
│   │   ├── RegisterItems.java             # Item registry (6 items)
│   │   ├── RegisterCreativeModeTabs.java  # Creative tab
│   │   ├── hammer/                        # Hammer tool (13 files)
│   │   │   ├── HammerItem.java
│   │   │   ├── HammerEventHandlers.java
│   │   │   ├── MiningModeBlockInteractions.java
│   │   │   ├── MiningModeBlockOutlineRenderer.java
│   │   │   ├── abilities/                 # Network payloads
│   │   │   ├── data/                      # Data classes
│   │   │   └── menu/                      # Config screen
│   │   └── symbiote/                      # Symbiote item (22 files)
│   │       ├── SymbioteItem.java
│   │       ├── SymbioteData.java
│   │       ├── SymbioteCommand.java
│   │       ├── abilities/                 # Dash ability
│   │       ├── dialogue/                  # Dialogue system (5 files)
│   │       ├── dream/                     # Dream system (3 files)
│   │       ├── observation/               # Observation tracking (4 files)
│   │       ├── progress/                  # Progress tracking (4 files)
│   │       └── voice/                     # Voice cooldowns (3 files)
│   ├── datagen/                 # Data generation providers (7 files)
│   │   ├── RegisterDataGenerators.java
│   │   ├── RegisterModelProvider.java
│   │   ├── RegisterBlockTagsProvider.java
│   │   ├── RegisterItemTagsProvider.java
│   │   ├── RegisterBlockLootTableProvider.java
│   │   ├── RegisterRecipeProvider.java
│   │   └── TestStructureProvider.java
│   └── util/                    # Utility classes
└── test/                        # GameTest framework
    ├── FloraFaunaGameTests.java           # Main test class (14 tests)
    ├── ColoredTestReporter.java
    └── SimpleGameTestInstance.java
```

**Key Principles:**
- **`Register*` naming**: All registration classes use `Register` prefix (e.g., `RegisterBlocks`, `RegisterItems`)
- **Feature directories**: Complex features get their own subdirectory with all related code
- **Client code in features**: Client-side rendering code (Models, Renderers, Screens) can live within feature directories
- **Separate client directory**: Pure client code (key bindings, client events, utilities) goes in `client/`
- **Networking per feature**: Network packets live in `networking/` subdirectories within their feature

## Minecraft/NeoForge Source Reference

The decompiled Minecraft and NeoForge source code is available in:
```
C:\Users\jerem\IdeaProjects\florafauna\build\moddev\artifacts\neoforge-21.11.13-beta-merged.jar
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
jar tf build/moddev/artifacts/neoforge-21.11.13-beta-merged.jar | grep "net/minecraft/world/entity/"

# Extract a specific class for reference
jar xf build/moddev/artifacts/neoforge-21.11.13-beta-merged.jar net/minecraft/world/entity/Entity.class
```

## Block Registration

### Regular Blocks
Defined in `common/block/RegisterBlocks.java`. Each block is a `public static final` field:
```java
public static final DeferredBlock<Block> TEAL_MOSS_BLOCK = registerBlock("teal_moss_block",
    props -> new Block(props.strength(4f).requiresCorrectToolForDrops()));
```

**Current blocks:** TEAL_MOSS_BLOCK, SYMBIOTE_CONTAINMENT_CHAMBER, COPPER_GOLEM_BARRIER

**Access:**
- `RegisterBlocks.TEAL_MOSS_BLOCK.get()` - Get the Block instance
- `RegisterBlocks.TEAL_MOSS_BLOCK` - Get DeferredBlock (for creative tabs, recipes, etc.)

### Wood Blocks (Enum-driven)
Wood blocks use a special pattern where the `ModWoodType` enum handles registration automatically.

**To add a new wood type:**
1. Add enum entry in `common/wood/ModWoodType.java`:
   ```java
   DRIFTWOOD("driftwood"),
   MAPLE("maple");  // Just add this!
   ```
2. Add lang entries in `en_us.json`
3. Add textures (see Textures section)

**Files involved** (`common/wood/`):
- `ModWoodType.java` - Enum that triggers registration (add new wood types here)
- `WoodBlockSet.java` - Record holding all blocks for a wood type
- `WoodBlockRegistration.java` - Creates all wood block variants
- `WoodStripping.java` - Event handler for axe stripping behavior

**Blocks per wood type (8 total):**
- Log, Stripped Log, Wood, Stripped Wood, Planks, Slab, Fence, Fence Gate

**Access wood blocks:**
```java
ModWoodType.DRIFTWOOD.getBlockSet().log()         // DeferredBlock
ModWoodType.DRIFTWOOD.getBlockSet().log().get()   // Block instance
ModWoodType.DRIFTWOOD.getBlockSet().planks()      // Planks block
ModWoodType.DRIFTWOOD.getBlockSet().slab()        // Slab block
ModWoodType.DRIFTWOOD.getBlockSet().fence()       // Fence block
ModWoodType.DRIFTWOOD.getBlockSet().fenceGate()   // Fence Gate block
```

**Iterate all wood types:**
```java
for (ModWoodType woodType : ModWoodType.values()) {
    WoodBlockSet wood = woodType.getBlockSet();
    // wood.log(), wood.planks(), wood.slab(), etc.
}
```

## Item Registration

Items are registered in `common/item/RegisterItems.java`. BlockItems are automatically registered by `RegisterBlocks.registerBlock()`.

```java
public static final DeferredItem<Item> TOMATO = ITEMS.registerItem("tomato",
    properties -> new Item(properties));
```

**Current items:** TOMATO, HAMMER, SYMBIOTE, GECKO_SPAWN_EGG, LIZARD_SPAWN_EGG, FRENCHIE_SPAWN_EGG

**Access:**
- `RegisterItems.TOMATO.get()` - Get the Item instance
- `RegisterItems.TOMATO` - Get DeferredItem

### Hammer Item

The Hammer is a complex multi-mode mining tool located in `common/item/hammer/`:

**Structure:**
```
hammer/
├── HammerItem.java                     # Main item class
├── HammerEventHandlers.java            # Event handlers for mining
├── MiningModeBlockInteractions.java    # Block interaction logic
├── MiningModeBlockOutlineRenderer.java # Block selection rendering
├── abilities/                          # Network payloads
│   ├── SpawnLightningPayload.java
│   ├── TeleportToSurfacePayload.java
│   ├── SetMiningSpeedPayload.java
│   └── UpdateHammerConfigPayload.java
├── data/                               # Data classes
│   ├── MiningModeData.java
│   ├── MiningSpeed.java
│   └── MiningShape.java
└── menu/                               # Config screen
    ├── HammerConfig.java
    └── HammerConfigScreen.java
```

**Features:**
- Multi-mode mining tool (unbreakable)
- Configurable via GUI screen (P key)
- Spawn lightning (R key)
- Teleport to surface (M key)
- Data components: MINING_MODE_DATA, MINING_SPEED, HAMMER_CONFIG

### Symbiote Item

The Symbiote is a complex consumable item with AI-driven behavior, located in `common/item/symbiote/`:

**Structure:**
```
symbiote/
├── SymbioteItem.java          # Main item class (consumable)
├── SymbioteData.java          # Bonding status, tier, energy, health
├── SymbioteCommand.java       # /symbiote command
├── abilities/                 # Dash ability
│   └── DashPayload.java
├── dialogue/                  # Dialogue system (5 files)
│   ├── DialogueEntry.java
│   ├── DialogueLoader.java
│   ├── DialogueRepository.java
│   ├── SelectionContext.java
│   └── DialogueEvents.java
├── dream/                     # Dream system (3 files)
│   ├── DreamContext.java
│   ├── DreamInsightEngine.java
│   └── DreamLevel.java
├── observation/               # Observation tracking (4 files)
│   ├── ObservationArbiter.java
│   ├── ChaosSuppressor.java
│   ├── ObservationCategory.java
│   └── ObservationEvents.java
├── progress/                  # Progress tracking (4 files)
│   ├── ProgressSignalTracker.java
│   ├── ProgressSignalUpdater.java
│   ├── ConceptSignal.java
│   └── SignalState.java
└── voice/                     # Voice cooldowns (3 files)
    ├── SymbioteVoiceService.java
    ├── VoiceCooldownState.java
    └── VoiceTier.java
```

**Features:**
- Consumable item (2-second drink animation)
- Bonds to player on consumption
- Voice cooldown tiers (Tier 1: 5min, Tier 2: 30min)
- Dream escalation levels
- Progress signal tracking
- Observation categories (combat, bonding, etc.)
- Dialogue selection based on progress
- Dash ability (Mouse 4)

## Entity Registration

Entities are registered in `common/entity/RegisterEntities.java`:
```java
public static final DeferredHolder<EntityType<?>, EntityType<GeckoEntity>> GECKO =
    ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(...).build(...));
```

**Current entities:** GECKO, LIZARD, FRENCHIE

**Entity organization:**
- Each entity gets its own directory under `common/entity/{entity_name}/`
- Directory contains: Entity class, Model, Renderer, Animations, RenderState, Variant (if applicable)
- Renderers are registered in `FloraFauna.java` ClientModEvents

### Frontpack System

The frontpack system allows players to carry Frenchie entities. Located in `common/entity/frontpack/`:

**Files:**
- `FrontpackData.java` - Attachment data holding carried Frenchie NBT
- `FrontpackEvents.java` - Pickup/put down event handlers
- `FrontpackLayer.java` - Renderer layer for displaying carried Frenchie
- `FrontpackModel.java` - Model for frontpack rendering
- `FrontpackAnimations.java` - Animation states
- `FrontpackRendererEvents.java` - Client rendering events
- `networking/PutDownFrenchiePayload.java` - Network packet

**Usage:** Shift-right-click a Frenchie to pick up; shift-right-click again to put down

## Key Bindings

Defined in `client/KeyMappings.java`:
- **R key** - Hammer: Spawn lightning at raycast position
- **M key** - Hammer: Teleport to surface
- **P key** - Hammer: Open config screen
- **Mouse 4** - Symbiote: Dash ability

Category: `florafauna:key-category`

## Data Components & Attachments

### Data Components (Item-specific state)
Registered in `common/RegisterDataComponentTypes.java`:
- `MINING_SPEED` - Hammer mining speed
- `MINING_MODE_DATA` - Hammer mining mode state
- `HAMMER_CONFIG` - Hammer configuration settings
- `SYMBIOTE_DATA` - Symbiote bonding and state data
- `SYMBIOTE_PROGRESS` - Symbiote progress tracking

### Attachments (Player-specific state)
Registered in `common/RegisterAttachmentTypes.java`:
- `SYMBIOTE_DATA` - Player's symbiote bond data
- `FRENCH_FRONTPACK_DATA` - Carried Frenchie data
- `SYMBIOTE_PROGRESS` - Player's symbiote progress
- `VOICE_COOLDOWNS` - Voice system cooldowns

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
9. `RegisterNetworking.register(modEventBus)`

BlockItems are added during `RegisterBlocks` initialization, which is why blocks must be registered before items.

## Networking

Network packets are organized per-feature in `networking/` subdirectories:

| Payload | Location | Direction | Purpose |
|---------|----------|-----------|---------|
| SpawnLightningPayload | `item/hammer/abilities/` | Client -> Server | Spawn lightning at raycast |
| TeleportToSurfacePayload | `item/hammer/abilities/` | Client -> Server | Teleport player to surface |
| SetMiningSpeedPayload | `item/hammer/abilities/` | Client -> Server | Set mining speed |
| UpdateHammerConfigPayload | `item/hammer/abilities/` | Client -> Server | Update hammer config |
| DashPayload | `item/symbiote/abilities/` | Client -> Server | Dash ability |
| PutDownFrenchiePayload | `entity/frontpack/networking/` | Client -> Server | Pick up/put down Frenchie |

Central registration: `common/RegisterNetworking.java`

## Data Generation

Run `./gradlew runData` to generate JSON files.

**Data providers** (in `common/datagen/`):
- `RegisterModelProvider.java` - Block/item models (wood blocks iterate via enum)
- `RegisterBlockTagsProvider.java` - Block tags (logs, planks, mineable)
- `RegisterItemTagsProvider.java` - Item tags
- `RegisterBlockLootTableProvider.java` - Loot tables (drop self)
- `RegisterRecipeProvider.java` - Crafting recipes (planks from logs, wood from logs)
- `TestStructureProvider.java` - Test structure generation for GameTest
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
guiGraphics.drawString(this.font, Component.translatable("gui.florafauna.hammer_config.enchantment"), x, y, color);
```

Then add to `en_us.json`:
```json
"gui.florafauna.hammer_config.enchantment": "Enchantment:"
```

## Event Handlers

Events use `@EventBusSubscriber` for auto-discovery:

**Common/Server events:**
- Registered on `NeoForge.EVENT_BUS`
- Located in feature directories (e.g., `common/wood/WoodStripping.java`)
- Located in `common/entity/RegisterEntityEvents.java`

**Client events:**
- Registered on `NeoForge.EVENT_BUS` with `Dist.CLIENT`
- Located in `client/events/` directory (KeyInputEvents, RenderEvents)
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
- `test/ColoredTestReporter.java` - Custom colored console reporter
- `test/SimpleGameTestInstance.java` - Custom GameTestInstance wrapper
- `resources/data/florafauna/structure/empty_1x1x1.nbt` - Minimal structure template
