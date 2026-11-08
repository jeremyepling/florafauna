# FloraFauna

A Minecraft 26.3 mod for NeoForge 26.3.0.101. Players can bond with a symbiote, recover gear from a husk, mine with configurable tools and use living storage and mob transport systems.

Coding agents share the repository's worktree, implementation and testing instructions. Claude Code imports them through `CLAUDE.md`. If you start a coordinator in the parent workspace, point its workspace instructions to `florafauna/AGENTS.md`. [Shared agent guide](AGENTS.md)

## Development Setup

### Prerequisites

- Java 25
- Git
- PowerShell 7 for the Windows worktree and playtest scripts

### Build & Run

From the checkout you want to test, generate resources before packaging. The build writes the mod jar to `build/libs/`.

```powershell
.\gradlew.bat --max-workers=4 runData
.\gradlew.bat --max-workers=4 build runGameTestServer
.\gradlew.bat runClient
```

### IDE Setup

Open the checkout you want to test as an IntelliJ Gradle project using JDK 25. To open a worker from the main checkout, run `.\scripts\open_idea.ps1 -Path ..\florafauna-1` and choose a new window if prompted. Let Gradle sync so NeoForge can generate that checkout's run configurations. Use **Client** for playing or debugging the mod and **GameTestServer** for headless gameplay tests. Run the Gradle `runData` task before testing a fresh checkout or changes to generated content.

### Dev World

The client opens the save named `dev` in its worktree-specific run directory. The shared template lives in `dev/world-template/dev/` in the main checkout and is gitignored. The worktree creation helper seeds new slots from that template when present. On first launch, Gradle can also seed a missing save from a template present in the same checkout.

Joining the dev world applies these settings automatically.

- All recipes unlocked
- Daylight cycle stopped (time set to day)
- Weather cycle stopped
- Easy difficulty
- No advancement announcements

To update the shared dev world, follow these steps.

1. `.\scripts\edit_dev_world.ps1` — launches the client in your worktree
2. Make the changes in-game and exit the client.
3. To publish those world edits, run `.\scripts\save_dev_world.ps1` from the source checkout (add `-f` on a feature branch). This replaces the shared template.
4. To distribute that template, run `.\scripts\sync_dev_world.ps1` from the main checkout. This replaces the other worktrees' dev saves. For playtesting, use the disposable copies below.

### Automated client playtests

Close the source world, then run this from the checkout in PowerShell 7 with Java 25. The default source is that checkout's singleplayer `dev` save.

```powershell
.\scripts\test_dev_world.ps1
.\scripts\test_dev_world.ps1 -Scenario recovery -SourceWorld 'C:\path\to\a\closed\dev-save'
```

Each scenario launches a real Minecraft client in a fresh copy of the save. When the source is a 26.1 world, the runner selects Minecraft’s **Create Backup and Load** option for the copy and continues through the 26.3 file upgrade. Let the test control the client until it exits.

| Scenario | What the client and integrated server check |
| --- | --- |
| `recovery` | Drink stew, bind at a cocoon, walk, die, respawn, restart the game, and recover the original inventory exactly once. |
| `hammer` | Configure and mine 3×3 walls and floors, capture their outlines, strip horizontal driftwood while preserving its axis, and switch between flattening and tilling. |
| `barrier` | Place a configured mob barrier, verify its settings on both sides, and compare screenshots with the barrier item held and with an empty hand. |
| `pods` | Mine drops into storage, restart the game, move the packed pod, and count what a hopper delivers. |
| `transport` | Bond and capture a named cow, restart, release it once, and verify its identity and recapture cooldown. |
| `frenchie` | Observe sleep at night, waking in daylight, and the effects of sitting and water. |

The script prints the output directory under `.tmp/world-playtests`. Each run keeps separate worlds, per-phase logs and results, and screenshots from the recovery, hammer, barrier and Frenchie scenarios. A failed assertion produces a nonzero script exit. The ordinary mod jar excludes the playtest driver.

### Worktree Workflow

From the checkout on `main`, choose an unused slot number:

```powershell
.\scripts\wt_list.ps1                          # List existing worktrees
.\scripts\new_worktree.ps1 4 feature/new-mob  # Create a new branch and slot
.\scripts\new_worktree.ps1 5                  # Create an idle detached slot
```

The numbered folders are reusable checkouts. Give each independent feature its own branch. In an idle, clean detached slot, run `git switch -c feature/<name> main` before editing. Each slot has separate build outputs, generated resources and game saves. Gradle shares downloaded dependencies and Minecraft assets across slots.

From the main checkout, use PowerShell 7 to build and test the slots currently assigned to your batch:

```powershell
.\scripts\test_worktrees.ps1 -Slot 1,2,3
```

Logs are written under each slot's `.tmp/worktree-tests`. When ready to publish a committed feature, run `.\scripts\merge.ps1` manually from its worktree. It updates and pushes `main` via the checkout that owns it, removes the merged feature branches and leaves the worker detached. If the remote feature has additional commits, it keeps both feature branches. For merge conflicts, resolve and commit in the main checkout, then rerun the helper from the feature worktree.

## Project Structure

The Java entry point is `FloraFauna.java`; gameplay lives under `common/`, and registration and client setup under `setup/`, all in the `net.j40climb.florafauna` package. See [AGENTS.md](AGENTS.md#implement-a-feature) for implementation conventions.

- [Player symbiote](src/main/java/net/j40climb/florafauna/common/symbiote/README.md) and [husk recovery](src/main/java/net/j40climb/florafauna/common/block/husk/README.md)
- [Tool abilities](src/main/java/net/j40climb/florafauna/common/item/abilities/README.md), [mining anchors and pods](src/main/java/net/j40climb/florafauna/common/block/mininganchor/README.md), and [item intake](src/main/java/net/j40climb/florafauna/common/block/iteminput/README.md)
- [Mob barriers](src/main/java/net/j40climb/florafauna/common/block/mobbarrier/README.md) and [mob fear](src/main/java/net/j40climb/florafauna/common/mobsymbiote/fear/README.md)
