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

## Mining Speed

| Speed | Break Speed | Description |
|-------|-------------|-------------|
| STANDARD | Original | Normal mining speed |
| EFFICIENCY | 35.0 | Fast mining |
| INSTABREAK | 100.0 | Instant block break |

## Tool Config

Stores enchantment and speed settings:

| Setting | Options | Default |
|---------|---------|---------|
| Enchantment | Fortune III / Silk Touch | Fortune III |
| Mining Speed | Standard / Efficiency / Instabreak | Efficiency |

**Note**: Fortune and Silk Touch are mutually exclusive - exactly one is always active.

## Key Bindings

| Key | Action | Component Required |
|-----|--------|-------------------|
| **V** | Cycle mining mode | `MULTI_BLOCK_MINING` |
| **P** | Open tool config screen | `TOOL_CONFIG` |
| **R** | Spawn lightning | `LIGHTNING_ABILITY` |
| **M** | Teleport to surface | `TELEPORT_SURFACE_ABILITY` |
| **G** | Throw item | `THROWABLE_ABILITY` |

## Multi-Block Mining

When breaking a block with `MULTI_BLOCK_MINING` component:

1. Calculate pattern based on current shape and facing direction
2. Render block outline preview on client
3. On break, destroy all blocks in pattern
4. Drops collected normally (with Fortune/Silk Touch applied)

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

**Note**: The multi-tool ability requires the item to override `canPerformAction()` to declare support for the tool actions. Currently only `HammerItem` implements this - other items with the component will not perform tool modifications until they add the override.

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

All abilities use `@EventBusSubscriber` for automatic registration:

| Event | Handler |
|-------|---------|
| `BlockEvent.BreakEvent` | Multi-block breaking |
| `PlayerEvent.BreakSpeed` | Mining speed modification |
| `PlayerInteractEvent.LeftClickBlock` | Visual feedback |
| `PlayerInteractEvent.RightClickBlock` | Right-click abilities and multi-tool |
| `PlayerInteractEvent.RightClickItem` | Right-click abilities (in air) |
| `ExtractBlockOutlineRenderStateEvent` | Block outline rendering |
