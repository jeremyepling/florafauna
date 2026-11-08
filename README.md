# FloraFauna

A Minecraft 26.1 mod for NeoForge 26.1.0.19-beta.

## Development Setup

### Prerequisites
- Java 25
- Git

### Build & Run

```bash
./gradlew build          # Build the mod
./gradlew runClient      # Run Minecraft client with mod
./gradlew runData        # Generate data files (models, recipes, tags)
```

### IDE Setup

**IntelliJ IDEA:** Open the checkout you want to test as a Gradle project using JDK 25. To open a worker from the main checkout, run `.\scripts\open_idea.ps1 -Path ..\florafauna-1` and choose a new window if prompted. Let Gradle sync so NeoForge can generate that checkout's run configurations. Use **Client** for playing or debugging the mod and **GameTestServer** for headless gameplay tests. Run the Gradle `runData` task before testing a fresh checkout or changes to generated content.

**VS Code / Cursor:** Open the project folder. Debug configs are in `.vscode/launch.json`.

### Dev World

The dev environment auto-loads into a "dev" world. The master template lives in the main repo at `dev/world-template/dev/` (gitignored). It is automatically copied on first `./gradlew runClient`.

**Dev world features** (applied automatically on join):
- All recipes unlocked
- Daylight cycle stopped (time set to day)
- Weather cycle stopped
- Easy difficulty
- No advancement announcements

**Editing the dev world:**
1. `.\scripts\edit_dev_world.ps1` — launches the client in your worktree
2. Make changes in-game, exit
3. `.\scripts\save_dev_world.ps1 -f` — saves your worktree's world back to the master template
4. `.\scripts\sync_dev_world.ps1` — (optional) pushes master template to other worktrees

Delete `run/client-*/saves/dev` to reset from template.

### Worktree Workflow

From the checkout on `main`, choose an unused slot number:

```powershell
.\scripts\wt_list.ps1                          # List existing worktrees
.\scripts\new_worktree.ps1 4 feature/new-mob  # Create a new branch and slot
.\scripts\new_worktree.ps1 5                  # Create an idle detached slot
```

Each worktree gets isolated run directories and build caches. To use a detached slot, run `git switch -c feature/<name> main` there before editing. From the main checkout, use PowerShell 7 to build and test slots concurrently:

```powershell
.\scripts\test_worktrees.ps1 -Slot 1,2,3
```

Logs are written under each slot's `.tmp/worktree-tests`. When ready to publish a committed feature, run `.\scripts\merge.ps1` manually from its worktree. It updates and pushes `main` via the checkout that owns it, removes the merged feature branches and leaves the worker detached. If the remote feature has additional commits, it keeps both feature branches. For merge conflicts, resolve and commit in the main checkout, then rerun the helper from the feature worktree.

## Project Structure

See [CLAUDE.md](CLAUDE.md) for detailed codebase documentation.
