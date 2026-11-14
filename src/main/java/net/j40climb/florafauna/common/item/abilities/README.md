# Tool Abilities System

Component-based tool abilities that can be added to any item via data components.

## Overview

The abilities system provides reusable tool functionality through data components. Any item with the appropriate components gains the corresponding abilities - no inheritance required.

## Data Components

| Component | Purpose |
|-----------|---------|
| `MULTI_BLOCK_MINING` | Enables multi-block mining patterns |
| `TOOL_CONFIG` | Enchantment and mining speed settings |
| `LIGHTNING_ABILITY` | Spawn lightning at target |
| `TELEPORT_SURFACE_ABILITY` | Teleport to surface |
| `THROWABLE_ABILITY` | Throw item as projectile |
| `HAMMER_ACTION` | Selected right-click action |
| `HAMMER_CHARGE` | Available blocks for charged ore strikes |
| `RIGHT_CLICK_ACTION` | Configures which ability triggers on right-click |
| `MULTI_TOOL_ABILITY` | Enables axe/shovel/hoe actions (strip, path, till) |

## Mining Shapes

| Shape | Pattern | Radius |
|-------|---------|--------|
| SINGLE | 1×1 | 0 |
| FLAT_3X3 | 3×3 plane | 1 |
| FLAT_5X5 | 5×5 plane | 2 |
| FLAT_7X7 | 7×7 plane | 3 |
| SHAPELESS | Vein mining | 1 |
| TUNNEL_UP | Staircase up | 0 |
| TUNNEL_DOWN | Staircase down | 0 |
| WALKABLE_TUNNEL | 1 wide × 3 high × 5 deep | 0 |
| LARGE_TUNNEL | 3 wide × 3 high × 5 deep | 0 |
| BORE | 1 wide × 1 high × 7 deep | 0 |

## Mining Speed

| Speed | Break Speed | Description |
|-------|-------------|-------------|
| STANDARD | Original | Normal mining speed |
| EFFICIENCY | 35.0 | Fast mining |
| INSTABREAK | 100.0 | Instant block break |

## Tool Config

Each Hammer remembers its settings.

| Setting | Options | Default |
|---------|---------|---------|
| Enchantment | Fortune III / Silk Touch | Fortune III |
| Mining Speed | Standard / Efficiency / Instabreak | Efficiency |
| Vein matching | Same ore / Exact block | Same ore |

**Note**: Fortune and Silk Touch are mutually exclusive - exactly one is always active.

## Key Bindings

| Key | Action | Component Required |
|-----|--------|-------------------|
| **C** | Cycle mining mode | `MULTI_BLOCK_MINING` |
| **K** | Open the right-click action wheel and select Auto, Strip, Path, Till or Throw | `HAMMER_ACTION` |
| **J** (hold) | Mine only the targeted block. Release to resume the selected shape. | `MULTI_BLOCK_MINING` |
| **P** | Open tool config screen | `TOOL_CONFIG` |
| **R** | Spawn lightning | `LIGHTNING_ABILITY` |
| **M** | Teleport to surface | `TELEPORT_SURFACE_ABILITY` |
| **T** | Tap to throw. Hold for one second and release for a charged Hammer throw. | `THROWABLE_ABILITY` |

These are the registered defaults. The dev options template binds throw to **comma**. Check the current binding in Minecraft's Controls menu.

Each Hammer remembers its right-click selection. **Auto** applies enabled block transformations in their usual order and then tries the configured fallback. **Strip**, **Path** and **Till** use just the selected enabled transformation. A target that cannot be transformed keeps its usual block interaction. **Throw** throws the held Hammer immediately.

## Multi-Block Mining

When breaking a block with `MULTI_BLOCK_MINING` component:

1. Calculate pattern based on current shape and facing direction
2. Render block outline preview on client
3. On break, destroy all blocks in pattern
4. Collect drops through the normal player mining and loot callbacks. A canceled center break leaves all neighbors intact. Each extra block gets its own protection checks and break callback.

Flat planes follow the original hit face. Shapeless mining follows matching blocks through faces, edges and corners and defaults to 64 blocks, including the center. Existing component values are bounded to a radius of 0–3 and a vein limit of 1–64.

Instabreak sets mining speed to 100. Block hardness still determines how long a strike takes.

Straight cuts advance into the face you strike. On a wall, strike the bottom of the passage for Walkable Tunnel, the center for Large Tunnel, or the entry point for Bore. On a floor or ceiling, the cut advances vertically. Walkable Tunnel centers its three-block slot along your horizontal heading. Air gaps count toward depth. All three shapes leave the floor as mined.

Stair modes cut five columns with five blocks of clearance per column before placing the tread. A side hit builds ascending stairs from one block below the center, or descending stairs from two below. A floor hit in descending mode follows the player's horizontal heading. Ceiling hits and ascending floor hits mine just the center. Teal stair outlines show planned placements through rock and across air gaps before you strike.

Orange outlines mark the faces of planned mining blocks that touch source or flowing lava. Check the marked side, ceiling, floor or end wall before striking. Holding the single-block key narrows this warning to the targeted block. The warning leaves the final decision to the player.

Shapeless mode groups matching stone and deepslate ore variants. An iron vein can cross the stone–deepslate boundary. Copper touching that vein stays in place. Select **Exact block** in the Hammer settings to stop at the variant boundary.

After a successful center hit, flat and tunnel swings skip air and continue through their usual footprint. Both stair directions place a cobblestone tread across an existing air gap at the planned floor height. The flight still ends at the fifth column. Vein mining follows matching neighbors within its configured radius.

## Charged Hammer Throw

Aim at an exposed ore block and hold the throw key until the ready message appears (20 server ticks), then release. On impact the Hammer mines connected ore using its own Fortune or Silk Touch, vein matching and block limit, then returns. Stone or a denied center hit spends no charge. Other ore types touching the vein stay in place. Each ore block actually removed costs one charge. Ordinary swings and quick throws are free.

A full Hammer stores 64 charge and recovers one charge per second while held in either hand. The inventory bar and tooltip show the remaining charge.

## Right-Click Action

The `RIGHT_CLICK_ACTION` component configures which ability triggers on right-click. It stores an Identifier referencing one of the ability components.

| Ability ID | Effect |
|------------|--------|
| `florafauna:throwable_ability` | Throw the item as a projectile |
| `florafauna:multi_block_mining` | Cycle mining mode |
| `florafauna:lightning_ability` | Spawn lightning at target block |
| `florafauna:teleport_surface_ability` | Teleport to surface |

## Multi-Tool Ability

The `MULTI_TOOL_ABILITY` component enables tool modifications on right-click:

| Setting | Effect |
|---------|--------|
| `strip` | Strip logs/wood (like axe) |
| `flatten` | Create grass paths (like shovel) |
| `till` | Till dirt to farmland (like hoe) |

**Priority**: Multi-tool actions take priority over `RIGHT_CLICK_ACTION`. If a block can be modified (stripped, pathed, or tilled), only that action occurs. Otherwise, the right-click ability executes.

Items with the component use the registered vanilla block transformers. The strip setting works on logs, wood and bamboo blocks. FloraFauna adds its driftwood log and wood to NeoForge's transformables data map so axes and the hammer both preserve the pillar axis when stripping driftwood.

## Adding Abilities to Items

```java
// In item registration
ItemStack stack = new ItemStack(myItem);
stack.set(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);
stack.set(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT);
stack.set(FloraFaunaRegistry.LIGHTNING_ABILITY, Unit.INSTANCE);
stack.set(FloraFaunaRegistry.RIGHT_CLICK_ACTION, new RightClickAction(Identifier.fromNamespaceAndPath("florafauna", "throwable_ability")));
stack.set(FloraFaunaRegistry.MULTI_TOOL_ABILITY, MultiToolAbilityData.DEFAULT);
```

## Event Handlers

Mining uses a server game-mode mixin for the completed center break. Other abilities use NeoForge events.

| Event | Handler |
|-------|---------|
| Server player mining action | Retain the hit face and complete multi-block mining once the center is removed |
| `PlayerEvent.BreakSpeed` | Mining speed modification |
| `PlayerInteractEvent.LeftClickBlock` | Visual feedback |
| `PlayerInteractEvent.RightClickBlock` | Right-click abilities and multi-tool |
| `PlayerInteractEvent.RightClickItem` | Right-click abilities (in air) |
| `ExtractBlockOutlineRenderStateEvent` | Block outline rendering |

## Verification

From the checkout root, run `.\scripts\test_dev_world.ps1 -Scenario hammer` using a closed dev save. The client changes tool settings, checks single-block input, captures the mining and placement outlines, compares the same tunnel face with and without a hidden lava pocket, and walks the stairs and straight tunnel in a generated quarry.
