# FloraFauna agent guide

Use this guide for work in any FloraFauna checkout. The mod targets Minecraft 26.1 and 26.3 with NeoForge and Java 25. Keep the editing target on 26.1 for commits. Check `versions/<target>/gradle.properties`, `build.gradle` and the Gradle wrapper for the versions in the assigned branch. Keep implementation and assets on the current Minecraft APIs.

## Coordinate work across agents

- When the developer describes independent features, delegate each complete feature to one worker. On this machine the coordinator can manage up to three concurrent workers in the numbered worktrees. Keep small or dependent changes together.
- The coordinator works from the parent workspace containing `florafauna` and its numbered siblings. Run `git -C florafauna worktree list --porcelain` there. Check each candidate's branch, status, unmerged commits and active assignment before using it. Preserve local edits, detached commits and saves.
- Base new features on a known commit of current `main` unless another base is requested. In a clean, available slot, use `git -C <slot> switch --no-track -c feature/<name> <base-sha>`. The [worktree instructions](README.md#worktree-workflow) cover adding a new slot.
- Give the worker its absolute checkout path, branch, base SHA, scope and observable acceptance checks. Use the same handoff for built-in subagents and agents running in separate tools. Each checkout has one writer. Workers verify their checkout with `git rev-parse --show-toplevel` and `git status --short --branch` before editing, and run commands and patches inside their assigned checkout.
- The worker owns code, registration, resources and tests for its feature. Agree on ownership when work touches the same registry ID, data component, packet, datagen provider, language file or GameTest registration. The coordinator reviews the combined behavior.

## Implement a feature

Java sources live under `src/main/java/net/j40climb/florafauna/`. Start with the nearest feature README and a working neighboring feature. The [root README](README.md#project-structure) indexes the feature guides.

| Area | Where to work |
| --- | --- |
| Gameplay | Keep blocks, entities and items with their feature under `common/`. Player symbiotes live in `common/symbiote/` and mob symbiotes in `common/mobsymbiote/`. Keep feature-specific network payloads with the feature. |
| Registration | `setup/FloraFaunaRegistry.java` owns deferred registers. `setup/FloraFaunaSetup.java` wires packets, creative tabs and commands. Add discoverable items and spawn eggs to the appropriate creative tab. |
| Client | `setup/ClientSetup.java` registers screens, entity renderers, keys and HUD layers on the client. Follow the current screen and render-state extraction patterns. |
| Data and assets | `common/datagen/` contains shared providers. Version-specific recipe and tag providers live under `versions/<target>/target/java/`. Put authored resources under `src/main/resources/`; generated output lives under ignored `versions/<target>/build/generated/resources/datagen/`. Use translatable player-facing text and add keys to `assets/florafauna/lang/en_us.json` under the resources root. |

For a block, follow its block item, block entity (if needed), creative tab, model, texture, loot, tags and translations through registration. Wood sets are driven by `common/block/wood/WoodType.java`. For a mob, follow a neighboring entity's attributes, spawn rules and model layers in `common/entity/RegisterEntityEvents.java`, renderer in `ClientSetup`, and applicable spawn egg and resources. The patched Minecraft source jar is in `versions/<target>/build/moddev/artifacts/` after Gradle prepares the development artifacts.

Any agent can use the detailed [block checklist](.claude/skills/block-wizard.md), [entity checklist](.claude/skills/entity-wizard.md), or [Blockbench import guide](.claude/agents/blockbench-integration.md) when a feature needs them.

## Verify from the assigned checkout

Use PowerShell 7 and the checkout's Gradle wrapper on Windows. During iteration, run the smallest check that observes the behavior being changed. A follow-up edit or a worker handoff usually needs only the affected check.

| What changed | Check during iteration |
| --- | --- |
| Documentation or instructions | Inspect the diff and validate any changed commands. |
| Java configuration, codecs, cycling or other pure logic | Run the relevant JUnit class or method, e.g. `.\gradlew.bat :26.1-neoforge:test --tests '*HammerConfigTest'`. |
| Server gameplay, loot, placement or permissions | Run one headless GameTest by ID, e.g. `.\gradlew.bat '-PgameTestFilter=florafauna:hammer_tunnel_gaps' :26.1-neoforge:runGameTestServer`. Use a namespaced wildcard for related cases such as `florafauna:hammer_*`. Check the log for the expected test names and count. |
| Input, rendering, walking or a save/rejoin interaction | When the behavior needs a real client, run the affected scenario, e.g. `.\scripts\test_dev_world.ps1 -Scenario hammer`. Each scenario manages its own launch phases. |
| Generated data or assets | Run `.\gradlew.bat :26.1-neoforge:runData` on a fresh checkout or after changing generation inputs, then inspect the affected files and exercise the consuming feature. |

At the end of a substantial or cross-feature change, the coordinator runs the [two-target headless checks](README.md#build--run) on the combined revision, or uses the equivalent CI result. Group the affected client scenarios in one run. Use `.\scripts\test_dev_world.ps1 -All` for a release, a shared client change that affects every scenario, or an explicit full pass. After fixing a failure, rerun the failed or affected checks. Report what actually ran.

GitHub Actions runs compile and unit checks for draft PRs. Ready-for-review PRs (including later pushes to them), pushes to main or numbered version branches, tags, and manual Build workflow runs get data generation, packaging and the full headless suite. Keep an active iteration local or in a draft PR. Ordinary feature branch pushes without a PR use the local results.

- Register headless gameplay checks in `test/FloraFaunaGameTests.java`. For player input, screens, persistence across restarts or rendering, use the applicable [client playtest](README.md#automated-client-playtests). The scenarios live in `src/playtest/java/`. Run them against a closed dev save; the runner creates its own copies and writes logs and screenshots under `versions/<target>/.tmp/world-playtests/`.
- Use IntelliJ IDEA for interactive Run/Debug. Open the individual checkout as a Gradle project with JDK 25 and sync it. When the IntelliJ integration is available, select the matching open project and use its named `Client` or `GameTestServer` configuration. For unattended checks use that checkout's wrapper. Keep the developer's other open projects available.
- Run one Gradle invocation at a time per checkout. The Gradle user home is shared for dependencies and Minecraft assets. Each checkout owns its build outputs and saves. The worktree batch script runs the full build and headless suite in every selected slot. Use it for deliberate multi-branch verification. Simultaneous dedicated servers need distinct ports in their own `server.properties`.
- Keep the shared dev-world template for deliberate template edits. The save and sync helpers replace worlds; use the disposable client playtests for automated verification.

## Integrate and report

Commit scoped changes on the assigned feature branch and report its commit, observable behavior, checks and any remaining manual verification. Normally push that branch for review. When features need to run together, merge their tested commits on a local integration branch, resolve shared registrations, and verify the combined code. When the developer requests publication to `main`, use the checkout that owns `main`, reconcile with the remote and push the verified history with a normal Git push.
