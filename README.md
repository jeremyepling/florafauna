# FloraFauna

A NeoForge 1.21.1 Minecraft mod.

## Development Setup

### Prerequisites
- Java 21
- Git

### Build & Run

```bash
./gradlew build          # Build the mod
./gradlew runClient      # Run Minecraft client with mod
./gradlew runData        # Generate data files (models, recipes, tags)
```

### IDE Setup

**IntelliJ IDEA:** Open the project folder. Run configs are in `.run/`.

**VS Code / Cursor:** Open the project folder. Debug configs are in `.vscode/launch.json`.

### Dev World

The dev environment auto-loads into a "dev" world. The master template lives in the main repo at `dev/world-template/dev/` (gitignored). It is automatically copied on first `./gradlew runClient`.

**Dev world features** (applied automatically on join):
- All recipes unlocked
- Daylight cycle stopped (time set to day)
- Weather cycle stopped
- Peaceful difficulty
- No advancement announcements

**Editing the dev world:**
1. `.\scripts\edit_dev_world.ps1` — launches the client in your worktree
2. Make changes in-game, exit
3. `.\scripts\save_dev_world.ps1 -f` — saves your worktree's world back to the master template
4. `.\scripts\sync_dev_world.ps1` — (optional) pushes master template to other worktrees

Delete `run/client-*/saves/dev` to reset from template.

### Worktree Workflow

This repo uses git worktrees for parallel development:

```powershell
.\scripts\new_worktree.ps1 3        # Create florafauna-3 worktree
.\scripts\merge.ps1                 # Merge current branch to main
.\scripts\wt_list.ps1               # List worktrees
```

Each worktree gets isolated run directories and build caches.

## Project Structure

See [CLAUDE.md](CLAUDE.md) for detailed codebase documentation.
