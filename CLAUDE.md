# Flora Fauna Mod

NeoForge 1.21.11 Minecraft mod.

## Environment

**Windows 11** - User runs PowerShell 7, but Claude's Bash tool uses Git Bash internally.

Prefer Claude tools over shell commands:
- **Edit tool** instead of `sed`
- **Grep tool** instead of `grep`/`rg`
- **Read tool** instead of `cat`/`head`/`tail`
- **Glob tool** instead of `ls` for finding files

Scripts in `scripts/` are PowerShell (.ps1) for user to run directly.

## Build Commands

```bash
./gradlew build              # Build the mod
./gradlew runClient          # Run client
./gradlew runGameTestServer  # Run tests
./gradlew runData            # Data generation (runClientData)
```

## Project Location

```
C:\code\florafauna-workspace\florafauna
```

Worktrees are created in the parent directory via `scripts/new_worktree.ps1`.

## Architecture

**Feature-based organization with centralized registration:**

```
src/main/java/net/j40climb/florafauna/
├── FloraFauna.java                    # Main mod class
├── setup/
│   ├── FloraFaunaRegistry.java        # ALL DeferredRegisters (blocks, items, entities, etc.)
│   ├── FloraFaunaSetup.java           # Networking, creative tabs, commands
│   └── ClientSetup.java               # Renderers, screens, key bindings
├── client/                            # Client-only code (events, GUI)
├── common/
│   ├── block/{feature}/               # Block features (each in own folder)
│   ├── entity/{entity_name}/          # Entity features (each in own folder)
│   ├── item/{feature}/                # Item features
│   ├── symbiote/                      # Symbiote player system
│   └── datagen/                       # Data generation providers
└── test/                              # GameTest framework
```

**Key Principles:**
- **FloraFaunaRegistry** = defines WHAT exists (DeferredRegisters)
- **FloraFaunaSetup** = wires things together (networking, tabs, commands)
- **ClientSetup** = client-only rendering/input
- Each feature gets its own subdirectory with all related code
- Network packets go in `networking/` subdirectory within their feature

## Adding a New Entity

1. Create feature directory: `common/entity/myentity/`
2. Add entity class: `MyEntityEntity.java`
3. Add model: `MyEntityModel.java`
4. Add renderer: `MyEntityRenderer.java`
5. Add render state: `MyEntityRenderState.java`
6. Add animations: `MyEntityAnimations.java`
7. Register in `FloraFaunaRegistry.java` (ENTITY_TYPES)
8. Register renderer in `ClientSetup.java`
9. Add spawn egg in `FloraFaunaRegistry.java`
10. **Add spawn egg to creative tab** in `FloraFaunaSetup.java`
11. Add to `RegisterEntityEvents.java` (spawn placement, attributes)
12. Add texture: `assets/florafauna/textures/entity/myentity/`
13. Add lang entry: `entity.florafauna.myentity`

## Adding a New Block

1. Create feature directory: `common/block/myblock/`
2. Add block class: `MyBlock.java`
3. Add block entity (if needed): `MyBlockEntity.java`
4. Register in `FloraFaunaRegistry.java` (BLOCKS, BLOCK_ENTITIES)
5. **Add to creative tab** in `FloraFaunaSetup.java` (FLORAFAUNA_ITEMS_TAB)
6. Add datagen in providers (model, tags, loot table)
7. Add texture: `assets/florafauna/textures/block/`
8. Add lang entry: `block.florafauna.myblock`
9. Run `./gradlew runData`

## Wood Blocks (Enum-Driven)

Add new wood type in `common/block/wood/WoodType.java`:
```java
MAPLE("maple");  // Just add enum entry!
```
Then add textures and lang entries. Registration is automatic.

## Translatable Text

**NEVER hardcode strings.** Always use:
```java
Component.translatable("gui.florafauna.my_key")
```
Add to `assets/florafauna/lang/en_us.json`.

## Data Generation

Providers in `common/datagen/`:
- `FloraFaunaModelProvider.java` - Block/item models
- `FloraFaunaBlockTagsProvider.java` - Block tags
- `FloraFaunaItemTagsProvider.java` - Item tags
- `FloraFaunaBlockLootTableProvider.java` - Loot tables
- `FloraFaunaRecipeProvider.java` - Recipes

Run: `./gradlew runData`

## GameTest

```bash
./gradlew runGameTestServer  # Run all tests
```

Tests in `test/FloraFaunaGameTests.java`. Register via `registerTest()`.

## Scripts (Worktree Workflow)

From main repo (`C:\code\florafauna-workspace\florafauna`):

```powershell
.\scripts\new_worktree.ps1 3        # Create florafauna-3 worktree
.\scripts\sync_dev_world.ps1        # Push template to all worktrees
.\scripts\wt_list.ps1               # List worktrees
.\scripts\open_idea.ps1 <path>      # Open IntelliJ for a path
```

From any worktree:

```powershell
.\scripts\merge.ps1                 # Merge current branch to main
.\scripts\edit_dev_world.ps1        # Run datagen + launch client to edit dev world
.\scripts\save_dev_world.ps1        # Save dev world to main repo's template
.\scripts\save_dev_world.ps1 -f     # Save from non-main branch (force)
```

## Dev World Template

The master dev world template lives in the main repo at `dev/world-template/dev/` (gitignored). It is automatically copied to new worktrees and to clean run directories on first `runClient`.

- **`save_dev_world.ps1`** — copies your worktree's world back to the master template in the main repo
- **`sync_dev_world.ps1`** — pushes the master template to all worktrees
- **`edit_dev_world.ps1`** — launches the client in your worktree (run `save` afterward to update the master)

### Dev World Workflow

1. `edit_dev_world.ps1` — launch client, make changes in-game, exit
2. `save_dev_world.ps1 -f` — save your worktree's world back to the master template
3. `sync_dev_world.ps1` — (optional) push master template to other worktrees

## Git Workflow

**IMPORTANT: Never push directly to main.**

When working in a worktree (branch name differs from `main`):

1. **Commit** to the current feature branch
2. **Push to the feature branch**, not main:
   ```bash
   git push -u origin HEAD  # Pushes to branch matching current branch name
   ```
3. **User merges** via `scripts/merge.ps1` or PR

Do NOT use `git push origin HEAD:main` - this bypasses code review and the worktree workflow.

## Minecraft Source Reference

Decompiled source in:
```
build/moddev/artifacts/neoforge-*.jar
```
