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

### Dev World Setup

The dev environment auto-loads into a "dev" world. First-time setup:

1. Run client: `./gradlew runClient`
2. Create new world named exactly **"dev"**:
   - Game Mode: **Creative**
   - World Type: **Superflat** → Customize → **Tunneler's Dream**
3. Exit Minecraft
4. Copy to template (from main repo):
   ```bash
   cp -r run/client-*/saves/dev dev/world-template/
   ```

The world template auto-copies to new worktrees and clean run directories.

**Dev world features** (applied automatically on join):
- All recipes unlocked
- Daylight cycle stopped (time set to day)
- Weather cycle stopped
- Peaceful difficulty
- No advancement announcements

Delete `run/client-*/saves/dev` to reset from template.

### Worktree Workflow

This repo uses git worktrees for parallel development:

```powershell
# From main repo (florafauna/)
scripts/new-worktree.ps1 -Name feature-name    # Create worktree
scripts/open-idea.ps1 ..\feature-name          # Open in IntelliJ
```

Each worktree gets isolated run directories and gradle state.

## Project Structure

See [CLAUDE.md](CLAUDE.md) for detailed codebase documentation.
