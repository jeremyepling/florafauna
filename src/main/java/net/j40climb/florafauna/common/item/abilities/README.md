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

## Adding Abilities to Items

```java
// In item registration
ItemStack stack = new ItemStack(myItem);
stack.set(FloraFaunaRegistry.MULTI_BLOCK_MINING, MiningModeData.DEFAULT);
stack.set(FloraFaunaRegistry.TOOL_CONFIG, ToolConfig.DEFAULT);
stack.set(FloraFaunaRegistry.LIGHTNING_ABILITY, Unit.INSTANCE);
```

## Event Handlers

All abilities use `@EventBusSubscriber` for automatic registration:

| Event | Handler |
|-------|---------|
| `BlockEvent.BreakEvent` | Multi-block breaking |
| `PlayerEvent.BreakSpeed` | Mining speed modification |
| `PlayerInteractEvent.LeftClickBlock` | Visual feedback |
| `ExtractBlockOutlineRenderStateEvent` | Block outline rendering |
