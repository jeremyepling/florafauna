# FloraFauna

A Fabric and NeoForge mod for Minecraft 26.1 and 26.3, with a Fabric preview for 26.4 Pre-Release 2. Players can bond with a symbiote, recover gear from a husk, grow a living storage network and transport bonded mobs. Hammer mining and Frenchie companions are available as separate mods.

Coding agents share the repository's worktree, implementation and testing instructions. If you start a coordinator in the parent workspace, point its workspace instructions to `florafauna/AGENTS.md`. [Shared agent guide](AGENTS.md)

## Development Setup

### Prerequisites

- Java 25
- Git
- PowerShell 7 for the Windows worktree and playtest scripts

### Build & Run

Maintain Minecraft 26.1 and 26.3 together on `main`, with Fabric and NeoForge builds for each. Start feature branches from `main`, edit shared gameplay once, and test all four targets before merging. Stonecutter defaults to editing 26.1 NeoForge. Use target-specific code or resources where Minecraft or loader APIs differ. All targets use Java 25; Fabric uses Loom and NeoForge uses ModDevGradle. Exact dependencies live in `versions/<minecraft-version>-<loader>/gradle.properties` and the loader build scripts.

To build one JAR, generate that target's data first, then package it in a second invocation.

```powershell
.\gradlew.bat :26.1-neoforge:runData
.\gradlew.bat :26.1-neoforge:build
```

The result is `versions/26.1-neoforge/build/libs/florafauna-26.1-neoforge-<mod version>.jar`. Substitute `26.3-neoforge`, `26.1-fabric` or `26.3-fabric` for the other JARs. Install the matching loader and, for Fabric, Fabric API. Fabric builds generate and consume shared data from the matching NeoForge target automatically. Each subproject owns its compiled classes, launch files and JARs. To run the full headless checks for all targets from a clean checkout, use PowerShell 7.

```powershell
foreach ($target in '26.1-neoforge', '26.3-neoforge', '26.1-fabric', '26.3-fabric') {
    .\gradlew.bat ":${target}:runData"
    if ($LASTEXITCODE) { throw "Datagen failed for $target" }
    .\gradlew.bat ":${target}:build" ":${target}:runGameTestServer"
    if ($LASTEXITCODE) { throw "Verification failed for $target" }
}
```

For a shorter iteration, `:<target>:check` runs unit and compilation checks. Use `.\gradlew.bat '-PgameTestFilter=florafauna:saved_barrier_pen_filters' :26.1-fabric:runGameTestServer` to check the saved barrier pen on Fabric. Ready PRs and release builds run the full headless checks and upload one JAR per Minecraft version and loader. Draft PRs run `check` on all four targets.

### Minecraft 26.4 preview

For Fabric, use Minecraft `26.4-pre-2`, Fabric Loader `0.20.1`, and Fabric API `0.167.1+26.4`. The Fabric preview packages shared resources from the 26.3 data generator. To collect matching jars for all three mods, run the following from this checkout with Hammer and Frenchies checkouts that contain the preview target.

```powershell
.\gradlew.bat -PdevMods=all "-PhammerDir=$hammerDir" "-PfrenchiesDir=$frenchiesDir" :26.4-fabric:assembleDevMods
```

The three jars are written to `versions/26.4-fabric/build/dev-mods/26.4-pre-2/`. Use `:26.4-fabric:runGameTestServer` with the same properties to exercise the combined server.

The opt-in `26.4-neoforge` target tracks **26.4 Snapshot 10**. On November 28, 2026, NeoForge Maven had no matching loader. Upstream setup completed at `1c14d83`, but Java compilation failed on NeoForge’s fluid API types. [Upstream port](https://github.com/neoforged/NeoForge/commit/1c14d8301132d6e4fe291e50c3262b718a0a5c63)

Once a matching development artifact is available in the NeoForge Maven repository or Maven Local, supply its version in both invocations. The normal 26.1 and 26.3 builds need no preview property.

```powershell
.\gradlew.bat "-PpreviewNeoVersion=$neoVersion" :26.4-neoforge:runData
.\gradlew.bat "-PpreviewNeoVersion=$neoVersion" :26.4-neoforge:build :26.4-neoforge:runGameTestServer
```

For the client scenarios, use `-Target 26.4 -Loader fabric` for Fabric or `-Target 26.4 -PreviewNeoVersion $neoVersion` for NeoForge. Choose companion checkouts that target the same Minecraft version and loader.

### Play with the Hammer and Frenchies

FloraFauna, Hammer and Frenchies can each run on their own. To play or develop with them together, choose the mods for a particular Minecraft version. A Stonecutter companion checkout supplies both targets. [Hammer source and tests](https://github.com/jeremyepling/hammer) · [Frenchies source and tests](https://github.com/jeremyepling/frenchies)

Use `florafauna/main`, `hammer/main` and `frenchies/main` in the grouped workspace, or place the three repositories side by side. Gradle selects the companion subproject matching the core task's Minecraft version and loader. For older NeoForge-only companion checkouts, the grouped layout also supports `hammer/26.3` and `frenchies/26.3`.

Select `none` (the default), `hammer`, `frenchies` or `all` with `-PdevMods`. Gradle builds the selected local companions and adds their jars to the run. On a fresh checkout, generate the core data for each target you plan to use.

```powershell
.\gradlew.bat :26.1-neoforge:runData
.\gradlew.bat -PdevMods=hammer :26.1-neoforge:runClient
.\gradlew.bat :26.3-neoforge:runData
.\gradlew.bat -PdevMods=all :26.3-neoforge:runClient
.\gradlew.bat -PdevMods=all :26.3-neoforge:assembleDevMods
.\gradlew.bat -PdevMods=all :26.3-fabric:assembleDevMods
```

For feature worktrees or other locations, pass `-PhammerDir=../path-to-hammer` and `-PfrenchiesDir=../path-to-frenchies`. Each selected checkout must declare the matching mod ID and Minecraft version. To make all three mods your usual setup, save a personal IntelliJ Gradle run configuration for each target with its qualified `runClient` task and `-PdevMods=all`.

**Installing the jars.** The collection task writes the core and selected companion jars to `versions/<target>/build/dev-mods/<minecraft-version>/`. The 26.3 example above writes to `versions/26.3-neoforge/build/dev-mods/26.3/`. Copy the chosen jars into the client and server `mods` directories for that Minecraft version.

**Opening a bundled-mod save.** Back up the closed world, replace the old FloraFauna jar with the split core and install the matching companion jars for any Hammer or Frenchie content present in the save before its first load. The companions retain the original `florafauna:*` registry IDs. In a 26.1 migration test, the split mods loaded and saved the named Hammer with its original vein settings and charge, and the sitting Frenchie retained its UUID and owner.

### IntelliJ editing target

Open the checkout as a Gradle project using JDK 25. To open a worker in a separate window, run `.\scripts\open_idea.ps1 -Path ..\florafauna-1`. In IntelliJ's Gradle tool window, run the root task **Set active project to 26.3-neoforge**, then reload the Gradle project. The shared `src` tree now exposes the 26.3 branches of Stonecutter conditionals. Use the selected subproject's `runClient` or `runGameTestServer` task. Run its `runData` before the first launch.

Before committing, run the root task **Reset active project**, reload Gradle, and verify `stonecutter.gradle` selects `26.1-neoforge`. The same switch and reset tasks are available from the wrapper.

```powershell
.\gradlew.bat 'Set active project to 26.3-neoforge'
.\gradlew.bat 'Reset active project'
```

Qualified build and test commands work from either editing target. Shared gameplay, tests and assets live under `src/`. Target-specific data providers, renderer hooks, mixins, access transformers and structure variants live under `versions/<target>/target/`.

### Dev World

The client opens the save named `dev` under `run/client-<checkout>-<profile>/saves/` for 26.1 and `versions/26.3-neoforge/run/client-<checkout>-<profile>/saves/` for 26.3. Profiles are named `core`, `hammer`, `frenchies` and `frenchies-hammer`. The shared template lives in `dev/world-template/dev/` in the main checkout and is gitignored. The worktree creation helper seeds new slots from that template when present. On first launch, Gradle can also seed a missing save from a template present in the same checkout. To reuse a pre-split dev save, close the game and copy the world folder from the older `client-<checkout>/saves/` into the chosen profile’s `saves/` directory. Select the companions used by that world before opening the copy.

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

### Test plots

Use the shared dev world for a walkthrough. The managed plots start at X 96, Z -48. Each setup comes from a checked-in Minecraft structure and can also run in its own world. In a development client with commands enabled, run these in chat.

```mcfunction
/ffplot build all
/ffplot list
/ffplot visit recovery
```

`/ffplot list` shows the plots available with the current mods. The `hammer` and `tool_actions` plots require Hammer. The `frenchie` plot requires Frenchies. Each plot has a named sign and a lectern with instructions, supplies to collect and expected results. The second walkthrough begins on page four in both the recovery and Frenchie guides. The supply barrel holds the starting kit. At `item_input`, bring one diamond from Creative so the test chest is the only nearby storage destination.

| Plot ID | What you can check |
| --- | --- |
| `item_input` | Pair an input to an anchor and follow a dropped diamond to its chest. |
| `recovery` | Recover an inventory after rejoining. With a second player, try another player's husk and count a partial stack through a restart. |
| `pods` | Mine three stones into a pod and count the hopper payout after moving it. |
| `frenchie` | Check sleep, sitting and waking in the pen. Carry a tamed dog in the frontpack through a restart and put it down once. |
| `hammer` | Mine a 3×3 wall and floor, check the backing and strip a log. |
| `transport` | Link an input and output, then capture and release a bonded cow. |
| `barrier` | Configure a barrier and check its outline against the dark backdrop. |
| `anchor_bench` | Compare the original feral and hardened anchor stations. Count three cobblestone delivered by the detached pod into its hopper. |
| `barrier_pens` | Test the west filter with a gecko or a Frenchie and the copper-golem ID and friendly-mob tag in the east pen. |
| `creeper_fear` | Bring a cat and a bonded creeper to the facing sides of the two pens. Watch for gunpowder and cooldown. |
| `blaze_fear` | Use the snow and tall barrier walls to observe a bonded blaze drop rods. |
| `enderman_fear` | Use the original diorite surfaces or pumpkin-headed stands to trigger pearl drops. |
| `long_route` | Pair the saved transport station's endpoints, offset by 13 blocks east and 6 south, and release a bonded cow. |
| `tool_actions` | Compare fresh hammer use targets with the original strip of leaves, stripped logs, farmland and path. |
| `iron_garden` | Bond an iron golem and watch it plant, convert and harvest poppies. Interrupt it and time its return to gardening. |

For **one plot in its own world**, create a Creative superflat save with commands enabled, run `/ffplot build recovery` and then `/ffplot visit recovery`. Reopen that save directly with `.\gradlew.bat "-PdevSave=recovery-lab" :26.1-neoforge:runClient`, using its actual folder name under `run/client-<checkout>-<profile>/saves/`. You can also place the same structure at a chosen position with vanilla `/place template florafauna:plots/recovery ~ ~ ~` in a clear area. Use `/ffplot build` for plots you want to visit by name.

Run `/ffplot build all` again to add newly registered plots. Existing installations keep your changes. In an older save, `/ffplot reset recovery` and `/ffplot reset frenchie` install their expanded books and restock their kits. Resetting a plot replaces its blocks and storage. Mobs, player inventories, bonds, time and gamerules belong to the world or player. Use an isolated save for tests that need fresh player state or change the time of day.

#### What came from the original saves

The shared template supplied both anchor tiers and their hoppers near X 21, Z -43, the two barrier pens near X 33, Z -54, the paired cat and creeper pens near X 36, Z -66, the blaze and enderman enclosures near Z -81 and -93, and the long transport route. The older playable save supplied the tool-action strip near X 22, Z -66. The original cocoon and its supply chest are in `recovery`. The intake, anchor and chest keep their original relative layout in `item_input`. The current GameTest and the walkthrough use that plot. The older `florafauna:test_item_input_to_chest` identifier remains available to saved structure blocks.

The captured pens retain their wall filters, heights and sight lines. Portable fixtures start with fresh actors from the kits, empty mob queues and unpaired machines. The anchor bench leaves space for attached pods to regrow. Its detached pod starts with a counted sample. Signs and lecterns replace the original labels and authoring blocks. A walkthrough can be rebuilt from the structures on fresh terrain with `/ffplot build all`.

#### Coverage and next checks

The headless suite runs the three fear pens through panic, a counted species-specific drop and cooldown. It also checks the captured barrier collision filters, extraction from the bench pod, and blocked intake retry after space opens in a full chest. The `plots` client scenario visits every available entrance, pairs and resets the long route, restarts the world and opens a field guide. The gameplay client scenarios below exercise inputs, rendering and save persistence.

The iron garden GameTests use the same soil bed, flowers and chest as the walkthrough. For husk ownership, run `.\scripts\test_husk_multiplayer.ps1 -Offline` to start a local dedicated server with two clients. The owner dies with 17 shards. A visitor tries to claim them, then the owner makes room for one shard and reconnects to recover the other 16. The Frenchies repository owns coat and frontpack checks. The core `companions` scenario checks the dog and equipment with all three mods loaded.

The recovery scenario already opens the cocoon screen and uses its spawn and bind buttons. A remaining visual check is to render the containment screen and the station item models and inspect their textures.

#### Adding a plot

1. Build the setup in a scratch world. An existing plot makes a convenient starting point. Face south from the entrance at relative `(2, 1, 0)`. Place the sign at `(0, 1, 1)`, lectern at `(2, 1, 1)` and supply barrel at `(4, 1, 1)`. Put the plot ID on the third sign line and give the book a setup page, action page and expected-result page.
2. Use a structure block in Save mode with the name `florafauna:plots/<id>`. Include the floor, clear air and the entrance. Exclude entities and the authoring block. Start with empty test inventories and unpaired machines so the fixture can move between worlds. Copy the exported `.nbt` from the save's `generated/florafauna/structure/plots/` into [the structure directory](src/main/resources/data/florafauna/structure/plots/).
3. Add an entry with a new permanent slot to [DevPlot.java](src/main/java/net/j40climb/florafauna/dev/DevPlot.java). The lowercase enum name is the ID. Slots form four columns on 32-block centers. Each template can occupy up to 28×48×28 blocks (X×Y×Z). The manager finds the surface on first placement and saves the elevation.
4. A headless GameTest can reference `DevPlot.<NAME>.structure()` and assert positions relative to the structure origin. A client scenario can call `DevPlot.<NAME>.place(level, origin)` once during setup and drive the player through the interaction. Leave restart phases to read the saved state. The item-input GameTest and the plots client scenario exercise the fixture through both paths.

### Automated client playtests

Close the source world, then run this from the checkout in PowerShell 7 with Java 25. The default source is the selected target's singleplayer `dev` save.

```powershell
.\scripts\test_dev_world.ps1 -Target 26.3 -Scenario barrier
.\scripts\test_dev_world.ps1 -Target 26.3 -Scenario companions -DevMods all -SourceWorld 'C:\path\to\a\closed\save'
.\scripts\test_dev_world.ps1 -Scenario recovery -SourceWorld 'C:\path\to\a\closed\26.1\save'
.\scripts\test_dev_world.ps1 -All  # Defaults to 26.1
.\scripts\test_dev_world.ps1 -Target 26.3 -All
.\scripts\test_dev_world.ps1 -Target 26.3 -Loader fabric -All -SourceWorld 'C:\path\to\a\closed\save'
.\scripts\test_husk_multiplayer.ps1 -Target 26.3
.\scripts\test_husk_multiplayer.ps1 -Target 26.3 -Loader fabric
```

For an integrated headless run, use `.\gradlew.bat -PdevMods=all :26.3-neoforge:runGameTestServer`. The `companions` client scenario exercises both companions. To exercise a core scenario with one companion, use `-Scenario recovery -DevMods hammer`. The script accepts `-HammerDir` and `-FrenchiesDir` for other checkout locations.

Each scenario launches a real Minecraft client in a fresh copy of the save. When 26.3 opens a 26.1 source world, the runner selects Minecraft’s **Create Backup and Load** option for the copy and continues through the 26.3 file upgrade. Let the test control the client until it exits.

| Scenario | What the client and integrated server check |
| --- | --- |
| `plots` | Build one fixture and the combined layout, visit all guides, preserve an edit across rebuild and restart, reset one plot and open its book. |
| `recovery` | Drink stew, bind at a cocoon, walk, die, respawn, restart the game, and recover the original inventory exactly once. |
| `companions` | Equip a named Hammer and dormant symbiote, tame a Frenchie, mine stone and verify the same dog and equipment after restarting. |
| `barrier` | Place a configured mob barrier, verify its settings on both sides, and compare screenshots with the barrier item held and with an empty hand. |
| `pods` | Mine drops into storage, restart the game, move the packed pod, and count what a hopper delivers. |
| `transport` | Bond and capture a named cow, restart, release it once, and verify its identity and recapture cooldown. |
| `slime` | Play a note, watch the bonded slime split and rejoin at the grate, and check the hopper and bond after restarting. |
| `spider` | Bond a spider, see it hanging upside down, shear its line, and collect one string. |

The script prints the output directory under `versions/<target>/.tmp/world-playtests`. Each scenario has its own world and per-phase results. The `companions/screenshots` folder contains the view after rejoining with the Hammer and the Frenchie. The plots, recovery and barrier scenarios also save screenshots. A failed assertion produces a nonzero script exit. The ordinary mod jar excludes the playtest driver.

### Worktree Workflow

Use a named worktree for each feature. In the grouped layout, run `git worktree add -b feature/new-mob ../worktrees/new-mob main` from `florafauna/main`. Independent features and checks can start together. For an existing numbered-slot layout, the following helpers are also available.

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

Shared gameplay, models, registry definitions and creative contents live in `src/main/java`, in the `net.j40climb.florafauna` package. Loader entrypoints, setup and native platform adapters live in `src/fabric/java` and `src/neoforge/java`. Stonecutter selects the `fabric` and `neoforge` conditionals and applies shared Minecraft API replacements from `gradle/version-compat.gradle`. Target-specific APIs and resources live under `versions/<target>/target`. See [AGENTS.md](AGENTS.md#implement-a-feature) for implementation conventions.

- [Player symbiote](src/main/java/net/j40climb/florafauna/common/symbiote/README.md) and [husk recovery](src/main/java/net/j40climb/florafauna/common/block/husk/README.md)
- [Mining anchors and pods](src/main/java/net/j40climb/florafauna/common/block/mininganchor/README.md) and [item intake](src/main/java/net/j40climb/florafauna/common/block/iteminput/README.md)
- [Slime grate farm](src/main/java/net/j40climb/florafauna/common/block/slimegrate/README.md)
- [Mob barriers](src/main/java/net/j40climb/florafauna/common/block/mobbarrier/README.md) and [mob fear](src/main/java/net/j40climb/florafauna/common/mobsymbiote/fear/README.md)
- [Spider silk habitat](src/main/java/net/j40climb/florafauna/common/mobsymbiote/silk/README.md)
