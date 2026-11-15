# FloraFauna

A Minecraft 26.3 mod for NeoForge 26.3.0.101. Players can bond with a symbiote, recover gear from a husk, tend mob farms and use living storage and mob transport systems.

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

During iteration, run the affected check. [Test scope](AGENTS.md#verify-from-the-assigned-checkout)

### Play with the Hammer and Frenchies

The Hammer and Frenchies are independent NeoForge mods. Their source, feature tests and installable jars live in [Hammer](https://github.com/jeremyepling/hammer) and [Frenchies](https://github.com/jeremyepling/frenchies). Match the Minecraft version in all checkouts: use the companions' **main** branches for 26.1 or their **26.3** branches for 26.3.

Clone those two repositories beside this checkout. From FloraFauna, use the same Gradle wrapper you normally use:

~~~powershell
.\gradlew.bat runData
.\gradlew.bat -PdevMods=all runClient
.\gradlew.bat -PdevMods=hammer runClient
.\gradlew.bat -PdevMods=all assembleDevMods
~~~

The first command generates FloraFauna's assets on a fresh checkout. The client commands build the selected companions from source and launch Minecraft. Use **all**, **hammer**, **frenchies**, or **none**. The default is **none**. To make all three your usual setup, save the `-PdevMods=all runClient` command as an IntelliJ Gradle run configuration. Each selection has its own dev client directory and saves. With a fresh profile and no template, create a world named `dev` on the first launch.

A sibling named `hammer-26.3` or `frenchies-26.3` takes precedence when running 26.3. For worktrees elsewhere, add `-PhammerDir=../path-to-hammer` and `-PfrenchiesDir=../path-to-frenchies`. Gradle checks the mod ID and Minecraft version of each selected checkout. The combined build uses Gradle composite substitution, so editing a companion updates the next run without publishing or copying a jar. [Gradle composite builds](https://docs.gradle.org/current/userguide/composite_builds.html)

**Playing outside Gradle.** The collection task writes the selected jars to `build/dev-mods/<minecraft-version>/`. Copy the desired jars for that Minecraft version to the client and server `mods` directories. For worlds created with the bundled mod, back up the world, replace the old FloraFauna jar with the split core (0.0.2 or later), and install the companion jars for content used by the save before opening it. The separate mods use the original `florafauna:*` item, entity and component IDs.

**Testing together.** `.\gradlew.bat -PdevMods=all runGameTestServer` loads tests from all three mods. Use `-PgameTestFilter=<namespace>:<test>` for a single case. For a real client, `.\scripts\test_dev_world.ps1 -Scenario companions -SourceWorld <closed-save>` equips the Hammer, tames a Frenchie, mines with both present and verifies the same dog and gear after a restart. Use `-DevMods all` with another core scenario to test it with both companions loaded. The client test runner also accepts `-HammerDir` and `-FrenchiesDir` for alternate checkouts.

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

### Test plots

Use the shared dev world for a walkthrough. The managed plots start at X 96, Z -48. Each setup comes from a checked-in Minecraft structure and can also run in its own world. In a development client with commands enabled, run these in chat.

```mcfunction
/ffplot build all
/ffplot list
/ffplot visit recovery
```

The `hammer` and `tool_actions` plots require the Hammer companion. The `frenchie` plot requires Frenchies. Each plot has a named sign and a lectern with instructions, supplies to collect and expected results. The second walkthrough begins on page four in both the recovery and Frenchie guides. The supply barrel holds the starting kit. At `item_input`, bring one diamond from Creative so the test chest is the only nearby storage destination.

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
| `barrier_pens` | Try the gecko filter in the west pen (Frenchies also match when installed) and the copper-golem ID and friendly-mob tag in the east pen. |
| `creeper_fear` | Bring a cat and a bonded creeper to the facing sides of the two pens. Watch for gunpowder and cooldown. |
| `blaze_fear` | Use the snow and tall barrier walls to observe a bonded blaze drop rods. |
| `enderman_fear` | Use the original diorite surfaces or pumpkin-headed stands to trigger pearl drops. |
| `long_route` | Pair the saved transport station's endpoints, offset by 13 blocks east and 6 south, and release a bonded cow. |
| `tool_actions` | Compare fresh hammer use targets with the original strip of leaves, stripped logs, farmland and path. |
| `iron_garden` | Bond an iron golem and watch it plant, convert and harvest poppies. Interrupt it and time its return to gardening. |

For **one plot in its own world**, create a Creative superflat save with commands enabled, run `/ffplot build recovery` and then `/ffplot visit recovery`. Reopen that save directly with `.\gradlew.bat "-PdevSave=recovery-lab" runClient`, using its actual folder name under `run/client-<checkout>-<profile>/saves/`. You can also place the same structure at a chosen position with vanilla `/place template florafauna:plots/recovery ~ ~ ~` in a clear area. Use `/ffplot build` for plots you want to visit by name.

Run `/ffplot build all` again to add newly registered plots. Existing installations keep your changes. In an older save, `/ffplot reset recovery` and `/ffplot reset frenchie` install their expanded books and restock their kits. Resetting a plot replaces its blocks and storage. Mobs, player inventories, bonds, time and gamerules belong to the world or player. Use an isolated save for tests that need fresh player state or change the time of day.

#### What came from the original saves

The shared template supplied both anchor tiers and their hoppers near X 21, Z -43, the two barrier pens near X 33, Z -54, the paired cat and creeper pens near X 36, Z -66, the blaze and enderman enclosures near Z -81 and -93, and the long transport route. The older playable save supplied the tool-action strip near X 22, Z -66. The original cocoon and its supply chest are in `recovery`. The intake, anchor and chest keep their original relative layout in `item_input`. The current GameTest and the walkthrough use that plot. The older `florafauna:test_item_input_to_chest` identifier remains available to saved structure blocks.

The captured pens retain their wall filters, heights and sight lines. Portable fixtures start with fresh actors from the kits, empty mob queues and unpaired machines. The anchor bench leaves space for attached pods to regrow. Its detached pod starts with a counted sample. Signs and lecterns replace the original labels and authoring blocks. A walkthrough can be rebuilt from the structures on fresh terrain with `/ffplot build all`.

#### Coverage and next checks

The headless suite runs the three fear pens through panic, a counted species-specific drop and cooldown. It also checks the captured barrier collision filters, extraction from the bench pod, and blocked intake retry after space opens in a full chest. The `plots` client scenario visits every entrance, pairs and resets the long route, restarts the world and opens a field guide. The gameplay client scenarios below exercise inputs, rendering and save persistence.

The iron garden GameTests use the same soil bed, flowers and chest as the walkthrough. For husk ownership, run `.\scripts\test_husk_multiplayer.ps1 -Offline` to start a local dedicated server with two clients. The owner dies with 17 shards. A visitor tries to claim them, then the owner makes room for one shard and reconnects to recover the other 16.

The recovery scenario already opens the cocoon screen and uses its spawn and bind buttons. A remaining visual check is to render the containment screen and the station item models and inspect their textures.

#### Adding a plot

1. Build the setup in a scratch world. An existing plot makes a convenient starting point. Face south from the entrance at relative `(2, 1, 0)`. Place the sign at `(0, 1, 1)`, lectern at `(2, 1, 1)` and supply barrel at `(4, 1, 1)`. Put the plot ID on the third sign line and give the book a setup page, action page and expected-result page.
2. Use a structure block in Save mode with the name `florafauna:plots/<id>`. Include the floor, clear air and the entrance. Exclude entities and the authoring block. Start with empty test inventories and unpaired machines so the fixture can move between worlds. Copy the exported `.nbt` from the save's `generated/florafauna/structure/plots/` into [the structure directory](src/main/resources/data/florafauna/structure/plots/).
3. Add an entry with a new permanent slot to [DevPlot.java](src/main/java/net/j40climb/florafauna/dev/DevPlot.java). The lowercase enum name is the ID. Slots form four columns on 32-block centers. Each template can occupy up to 28×48×28 blocks (X×Y×Z). The manager finds the surface on first placement and saves the elevation.
4. A headless GameTest can reference `DevPlot.<NAME>.structure()` and assert positions relative to the structure origin. A client scenario can call `DevPlot.<NAME>.place(level, origin)` once during setup and drive the player through the interaction. Leave restart phases to read the saved state. The item-input GameTest and plots client scenario demonstrate both paths.

### Automated client playtests

Close the source world, then run this from the checkout in PowerShell 7 with Java 25. The default source is that checkout's singleplayer `dev` save.

```powershell
.\scripts\test_dev_world.ps1 -Scenario plots
.\scripts\test_dev_world.ps1 -Scenario recovery -SourceWorld 'C:\path\to\a\closed\26.1\save'
.\scripts\test_dev_world.ps1 -All  # Full gameplay pass
```

Each scenario launches a real Minecraft client in a fresh copy of the save. When the source is a 26.1 world, the runner selects Minecraft’s **Create Backup and Load** option for the copy and continues through the 26.3 file upgrade. Let the test control the client until it exits.

| Scenario | What the client and integrated server check |
| --- | --- |
| `plots` | Build one fixture and the combined layout, visit all guides, preserve an edit across rebuild and restart, reset one plot and open its book. |
| `recovery` | Drink stew, bind at a cocoon, walk, die, respawn, restart the game, and recover the original inventory exactly once. |
| `companions` | Load both standalone mods, use a Hammer and keep the same tamed Frenchie and equipment through a restart. |
| `barrier` | Place a configured mob barrier, verify its settings on both sides, and compare screenshots with the barrier item held and with an empty hand. |
| `pods` | Mine drops into storage, restart the game, move the packed pod, and count what a hopper delivers. |
| `transport` | Bond and capture a named cow, restart, release it once, and verify its identity and recapture cooldown. |
| `slime` | Play a note, watch the bonded slime split and rejoin at the grate, and check the hopper and bond after restarting. |
| `spider` | Bond a spider, see it hanging upside down, shear its line, and collect one string. |

The script prints the output directory under `.tmp/world-playtests`. Each scenario has its own world and per-phase results. The `companions/screenshots` folder includes the view after rejoining with the Hammer and Frenchie. A failed assertion produces a nonzero script exit. The ordinary mod jar excludes the playtest driver.

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
- [Mining anchors and pods](src/main/java/net/j40climb/florafauna/common/block/mininganchor/README.md), and [item intake](src/main/java/net/j40climb/florafauna/common/block/iteminput/README.md)
- [Slime grate farm](src/main/java/net/j40climb/florafauna/common/block/slimegrate/README.md)
- [Mob barriers](src/main/java/net/j40climb/florafauna/common/block/mobbarrier/README.md) and [mob fear](src/main/java/net/j40climb/florafauna/common/mobsymbiote/fear/README.md)
- [Spider silk habitat](src/main/java/net/j40climb/florafauna/common/mobsymbiote/silk/README.md)
