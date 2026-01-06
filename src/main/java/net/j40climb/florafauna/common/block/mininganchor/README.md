# Mining Anchor System

Mobile storage system that automatically collects block drops during mining.

## Overview

Mining Anchors collect nearby block drops and store them directly in Storage Pods. The anchor itself has no storage - all items go to pods. Pods spawn automatically as needed when items are collected, spreading horizontally from the anchor. The anchor tracks fill state and notifies the player when storage is running low.

## How It Works

1. **Collection** - Anchor scans for dropped items from broken blocks (not player drops)
2. **Pod Spawning** - When items are collected, pods spawn as needed (up to max pods for tier)
3. **Direct Storage** - Items are stored directly in pods, not in the anchor
4. **Pod-Owned Capacity** - Each pod tier defines its own capacity (slots * 64 items)
5. **Horizontal Spreading** - Pods spawn outward from anchor, preferring same Y-level

## Tiers

| Feature | Tier 1 (Feral) | Tier 2 (Hardened) |
|---------|----------------|-------------------|
| Display Name | Feral Mining Anchor | Hardened Mining Anchor |
| Pod Type | Feral Pod | Hardened Pod |
| Pod Break Behavior | Spills all items | Drops as item (like shulker box) |
| Pod Placeable | No | Yes |
| Max Pods | 4 | 8 |
| Pod Slots | 9 | 27 (shulker-sized) |
| Pod Capacity | 576 items | 1,728 items |
| **Total Capacity** | **2,304 items** | **13,824 items** |

## Configuration Values

| Setting | Default | Description |
|---------|---------|-------------|
| `collectRadius` | 8 blocks | Scan radius for dropped items |
| `collectIntervalTicks` | 10 ticks | Time between collection scans |
| `tier1MaxPods` | 4 | Maximum pods for Tier 1 anchors |
| `tier2MaxPods` | 8 | Maximum pods for Tier 2 anchors |
| `podSpawnRadius` | 5 blocks | Maximum distance pods can spawn from anchor |
| `blockDropsOnly` | true | Only collect items from block breaking |

## Pod Capacity

Pods own their capacity - it's not derived from the anchor. Each tier has a fixed slot count:

| Pod Tier | Slots | Capacity (slots × 64) |
|----------|-------|----------------------|
| Tier 1 (Feral) | 9 | 576 items |
| Tier 2 (Hardened) | 27 | 1,728 items |

Total anchor capacity = Pod capacity × Max pods for tier.

## Fill States

| State | Fill % | Visual | Dialogue |
|-------|--------|--------|----------|
| NORMAL | 0-74% | Normal | - |
| WARNING | 75-99% | Working | "Storage is filling up..." |
| FULL | 100% | Blocked | "No more room..." |

## Pod Spawn Algorithm

Pods spawn using an expanding ring algorithm:

1. **Priority**: Same Y-level first, then Y+1 if blocked
2. **Order**: Cardinal directions → diagonals → next ring
3. **Valid Positions**: Air blocks or blocks tagged `florafauna:pod_replaceable`

```
Ring 1 (distance 1):
      [N]
   [W] A [E]
      [S]

Ring 2 (distance 2):
        [N]
     [.][.][.]
  [W][.] A [.][E]
     [.][.][.]
        [S]

Continues to ring 5 (max spawn radius)
```

### Pod Replaceable Blocks

The `florafauna:pod_replaceable` tag includes blocks that pods can replace when spawning:
- Grass (short, tall)
- Ferns (small, large)
- Dead bush
- Seagrass
- Flowers (dandelion, poppy, tulips, etc.)
- Snow layer

## Player Interaction

- **Right Click** - Show status message (storage count, pod count, state)

## Automation

- **Hoppers** - Can extract items from pods (uses NeoForge Transfer API)
- Items are distributed across pods, filling each before spawning the next
