# Mining Anchor System

Mobile storage system that automatically collects block drops during mining.

## Overview

Mining Anchors collect nearby block drops and store them directly in Storage Pods. The anchor itself has no storage - all items go to pods. Pods spawn automatically as needed when items are collected. The anchor tracks fill state and notifies the player's symbiote when storage is running low.

## How It Works

1. **Collection** - Anchor scans for dropped items from broken blocks (not player drops)
2. **Pod Spawning** - When items are collected, pods spawn as needed (up to max pods)
3. **Direct Storage** - Items are stored directly in pods, not in the anchor
4. **Capacity Limit** - Each pod has a capacity limit (total capacity / max pods)
5. **Waypoint** - Players can bind an anchor to their symbiote for HUD tracking

## Tiers

| Feature | Tier 1 (Feral) | Tier 2 (Hardened) |
|---------|----------------|-------------------|
| Display Name | Feral Mining Anchor | Hardened Mining Anchor |
| Pod Type | Feral Pod | Hardened Pod |
| Pod Break Behavior | Spills all items | Drops as item (like shulker box) |
| Pod Placeable | No | Yes |
| Total Capacity | 256 items | 256 items |
| Max Pods | 4 | 4 |
| Capacity Per Pod | 64 items | 64 items |

## Configuration Values

| Setting | Default | Description |
|---------|---------|-------------|
| `collectRadius` | 8 blocks | Scan radius for dropped items |
| `collectIntervalTicks` | 10 ticks | Time between collection scans |
| `miningAnchorBaseCapacity` | 256 items | Total capacity across all pods |
| `maxPods` | 4 | Maximum pods per anchor |
| `blockDropsOnly` | true | Only collect items from block breaking |

## Capacity Calculation

```
Total Capacity = Base Capacity (configured)
Pod Capacity = Base Capacity / Max Pods
             = 256 / 4
             = 64 items per pod
```

When all pods are full, the anchor stops collecting items.

## Fill States

| State | Fill % | Visual | Symbiote Dialogue |
|-------|--------|--------|-------------------|
| NORMAL | 0-74% | Normal | - |
| WARNING | 75-99% | Working | "Storage is filling up..." |
| FULL | 100% | Blocked | "No more room..." |

## Pod Spawn Positions

Pods spawn in cardinal directions from the anchor:
```
    [N]
[W] [A] [E]
    [S]

A = Anchor
N/E/S/W = Pod spawn positions (in order of priority)
```

## Player Interaction

- **Shift + Right Click** - Bind/unbind anchor to symbiote (requires bonded symbiote)
- **Right Click** - Show status message (storage count, pod count, state)

## Automation

- **Hoppers** - Can extract items from pods (uses NeoForge Transfer API)
- Items are distributed across pods, filling each to capacity before using the next
