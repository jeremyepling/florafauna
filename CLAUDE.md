# Flora Fauna Mod

NeoForge 1.21.10 Minecraft mod.

## Build & Run
```bash
./gradlew build          # Build the mod
./gradlew runClient      # Run Minecraft client with mod
./gradlew runData        # Run data generation
```

## Project Structure
- `src/main/java/net/j40climb/florafauna/` - Main source code
- `src/main/resources/assets/florafauna/` - Assets (textures, lang, models)
- `src/main/resources/data/florafauna/` - Data (recipes, loot tables, tags)

## Block Registration

### Regular Blocks
Defined in `ModBlocks.java`. Each block is a `public static final` field:
```java
public static final DeferredBlock<Block> MY_BLOCK = registerBlock("my_block", props -> new Block(props...));
```
Access: `ModBlocks.MY_BLOCK.get()` or `ModBlocks.MY_BLOCK` for DeferredBlock

### Wood Blocks (Enum-driven)
Wood blocks use a special pattern where the `ModWoodType` enum handles registration automatically.

**To add a new wood type:**
1. Add enum entry in `ModWoodType.java`:
   ```java
   DRIFTWOOD("driftwood"),
   MAPLE("maple");  // Just add this!
   ```
2. Add lang entries in `en_us.json`

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
Items in `ModItems.java`. BlockItems are auto-registered by `ModBlocks.registerBlock()`.

## Creative Tabs
`ModCreativeModeTabs.java` - Wood blocks iterate via `ModWoodType.values()`.

## Registration Order
In `FloraFauna.java`, `ModBlocks.register()` must be called before `ModItems.register()`
so BlockItems are added to the item registry before it's finalized.

## Data Generation
Run `./gradlew runData` to generate JSON files. Providers in `common/datagen/`:
- `ModModelProvider.java` - Block/item models (wood blocks iterate via enum)
- `ModBlockTagsProvider.java` - Block tags (logs, planks, mineable)
- `ModItemTagsProvider.java` - Item tags
- `BlockLootTableProvider.java` - Loot tables (drop self)
- `ModRecipeProvider.java` - Crafting recipes (planks from logs, wood from logs)

## Textures
Wood block textures go in `assets/florafauna/textures/block/`:
- `{name}_log.png`, `{name}_log_top.png` - Log side and top
- `stripped_{name}_log.png`, `stripped_{name}_log_top.png` - Stripped log
- `{name}_planks.png` - Planks

## Event Handlers
Classes with `@EventBusSubscriber` are auto-discovered by NeoForge:
- `BlockEvents.java` - General block events (break events, etc.)
- `WoodStripping.java` - Axe stripping for wood blocks

**Important:** Avoid static initializers that access blocks - use lazy initialization
instead, since static blocks run before registries are populated.
