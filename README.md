# FloraFauna

A Minecraft 26.1 mod for NeoForge 26.1.0.19-beta. Players can bond with a symbiote, recover gear from a husk, mine with configurable tools and use living storage and mob transport systems.

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

### Test plots

Use the shared dev world for a walkthrough. The managed plots start at X 96, Z -48. Each setup comes from a checked-in Minecraft structure and can also run in its own world. In a development client with commands enabled, run these in chat.

```mcfunction
/ffplot build all
/ffplot list
/ffplot visit hammer
```

Each plot has a named sign and a lectern with instructions, supplies to collect and expected results. The second walkthrough begins on page four in both the recovery and Frenchie guides. The supply barrel holds the starting kit. At `item_input`, bring one diamond from Creative so the test chest is the only nearby storage destination.

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
| `barrier_pens` | Try the Frenchie filter in the west pen and the copper-golem ID and friendly-mob tag in the east pen. |
| `creeper_fear` | Bring a cat and a bonded creeper to the facing sides of the two pens. Watch for gunpowder and cooldown. |
| `blaze_fear` | Use the snow and tall barrier walls to observe a bonded blaze drop rods. |
| `enderman_fear` | Use the original diorite surfaces or pumpkin-headed stands to trigger pearl drops. |
| `long_route` | Pair the saved transport station's endpoints, offset by 13 blocks east and 6 south, and release a bonded cow. |
| `tool_actions` | Compare fresh hammer use targets with the original strip of leaves, stripped logs, farmland and path. |
| `iron_garden` | Bond an iron golem and watch it plant, convert and harvest poppies. Interrupt it and time its return to gardening. |

For **one plot in its own world**, create a Creative superflat save with commands enabled, run `/ffplot build hammer` and then `/ffplot visit hammer`. Reopen that save directly with `.\gradlew.bat "-PdevSave=hammer-lab" runClient`, using its actual folder name under `run/client-<checkout>/saves/`. You can also place the same structure at a chosen position with vanilla `/place template florafauna:plots/hammer ~ ~ ~` in a clear area. Use `/ffplot build` for plots you want to visit by name.

Run `/ffplot build all` again to add newly registered plots. Existing installations keep your changes. In an older save, `/ffplot reset recovery` and `/ffplot reset frenchie` install their expanded books and restock their kits. Resetting a plot replaces its blocks and storage. Mobs, player inventories, bonds, time and gamerules belong to the world or player. Use an isolated save for tests that need fresh player state or change the time of day.

#### What came from the original saves

The shared template supplied both anchor tiers and their hoppers near X 21, Z -43, the two barrier pens near X 33, Z -54, the paired cat and creeper pens near X 36, Z -66, the blaze and enderman enclosures near Z -81 and -93, and the long transport route. The older playable save supplied the tool-action strip near X 22, Z -66. The original cocoon and its supply chest are in `recovery`. The intake, anchor and chest keep their original relative layout in `item_input`. The current GameTest and the walkthrough use that plot. The older `florafauna:test_item_input_to_chest` identifier remains available to saved structure blocks.

The captured pens retain their wall filters, heights and sight lines. Portable fixtures start with fresh actors from the kits, empty mob queues and unpaired machines. The anchor bench leaves space for attached pods to regrow. Its detached pod starts with a counted sample. Signs and lecterns replace the original labels and authoring blocks. A walkthrough can be rebuilt from the structures on fresh terrain with `/ffplot build all`.

#### Coverage and next checks

The headless suite runs the three fear pens through panic, a counted species-specific drop and cooldown. It also checks the captured barrier collision filters, extraction from the bench pod, and blocked intake retry after space opens in a full chest. The `plots` client scenario visits every entrance, pairs and resets the long route, restarts the world and opens a field guide. The gameplay client scenarios below exercise inputs, rendering and save persistence.

The iron garden GameTests use the same soil bed, flowers and chest as the walkthrough. For husk ownership, run `.\scripts\test_husk_multiplayer.ps1 -Offline` to start a local dedicated server with two clients. The owner dies with 17 shards. A visitor tries to claim them, then the owner makes room for one shard and reconnects to recover the other 16. The Frenchie frontpack client scenario checks the dog's identity, ownership and coat across a restart.

The recovery scenario already opens the cocoon screen and uses its spawn and bind buttons. A remaining visual check is to render the containment screen and the station item models and inspect their textures.

#### Adding a plot

1. Build the setup in a scratch world. An existing plot makes a convenient starting point. Face south from the entrance at relative `(2, 1, 0)`. Place the sign at `(0, 1, 1)`, lectern at `(2, 1, 1)` and supply barrel at `(4, 1, 1)`. Put the plot ID on the third sign line and give the book a setup page, action page and expected-result page.
2. Use a structure block in Save mode with the name `florafauna:plots/<id>`. Include the floor, clear air and the entrance. Exclude entities and the authoring block. Start with empty test inventories and unpaired machines so the fixture can move between worlds. Copy the exported `.nbt` from the save's `generated/florafauna/structure/plots/` into [the structure directory](src/main/resources/data/florafauna/structure/plots/).
3. Add an entry with a new permanent slot to [DevPlot.java](src/main/java/net/j40climb/florafauna/dev/DevPlot.java). The lowercase enum name is the ID. Slots form four columns on 32-block centers. Each template can occupy up to 28×48×28 blocks (X×Y×Z). The manager finds the surface on first placement and saves the elevation.
4. A headless GameTest can reference `DevPlot.<NAME>.structure()` and assert positions relative to the structure origin. A client scenario can call `DevPlot.<NAME>.place(level, origin)` once during setup and drive the player through the interaction. Leave restart phases to read the saved state. The item-input GameTest and hammer client scenario demonstrate both paths.

### Automated client playtests

Close the source world, then run this from the checkout in PowerShell 7 with Java 25. The default source is that checkout's singleplayer `dev` save.

```powershell
.\scripts\test_dev_world.ps1
.\scripts\test_dev_world.ps1 -Scenario recovery -SourceWorld 'C:\path\to\a\closed\26.1\save'
```

Each scenario launches a real Minecraft client in a fresh copy of the save. Let the test control the client until it exits.

| Scenario | What the client and integrated server check |
| --- | --- |
| `plots` | Build one fixture and the combined layout, visit all guides, preserve an edit across rebuild and restart, reset one plot and open its book. |
| `recovery` | Drink stew, bind at a cocoon, walk, die, respawn, restart the game, and recover the original inventory exactly once. |
| `hammer` | Change tool settings, capture wall and floor outlines, mine the matching 3×3 areas, and strip a log. |
| `pods` | Mine drops into storage, restart the game, move the packed pod, and count what a hopper delivers. |
| `transport` | Bond and capture a named cow, restart, release it once, and verify its identity and recapture cooldown. |
| `frenchie` | Observe sleep at night, waking in daylight, and the effects of sitting and water. |
| `frontpack` | Pick up a tamed Frenchie, check the pack before and after restarting, and put down the same dog exactly once. |

The script prints the output directory under `.tmp/world-playtests`. Each scenario has its own world and per-phase results. The `frontpack/screenshots` folder contains matched third-person views before and after putdown. The plots, recovery, hammer and sleep scenarios also save screenshots. A failed assertion produces a nonzero script exit. The ordinary mod jar excludes the playtest driver.

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
