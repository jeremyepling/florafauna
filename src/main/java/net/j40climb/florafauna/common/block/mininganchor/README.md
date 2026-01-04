# Mining Anchor System

Mobile storage system that automatically collects block drops during mining.

## Overview

Mining Anchors are placed in the world and vacuum up nearby block drops into their internal buffer. When the buffer fills, they spawn Storage Pods to expand capacity. The anchor tracks fill state and notifies the player's symbiote when storage is running low.

## How It Works

1. **Collection** - Anchor scans for dropped items from broken blocks (not player drops)
2. **Buffering** - Items are stored in the anchor's internal buffer
3. **Pod Growth** - When buffer reaches 80% capacity, a pod spawns adjacent to the anchor
4. **Overflow** - Excess items automatically transfer to connected pods
5. **Waypoint** - Players can bind an anchor to their symbiote for HUD tracking

## Tiers

| Feature | Tier 1 (Feral) | Tier 2 (Hardened) |
|---------|----------------|-------------------|
| Display Name | Feral Mining Anchor | Hardened Mining Anchor |
| Pod Type | Feral Pod | Hardened Pod |
| Pod Break Behavior | Spills all items | Drops as item (like shulker box) |
| Pod Placeable | No | Yes |
| Base Capacity | 256 items | 256 items |
| Max Pods | 4 | 4 |
| Max Total Capacity | 33,024 items | 33,024 items |

## Configuration Values

| Setting | Default | Description |
|---------|---------|-------------|
| `collectRadius` | 8 blocks | Scan radius for dropped items |
| `collectIntervalTicks` | 10 ticks | Time between collection scans |
| `miningAnchorBaseCapacity` | 256 items | Anchor buffer size |
| `podCapacityStacks` | 128 stacks | Capacity per pod (×64 = 8,192 items) |
| `maxPods` | 4 | Maximum pods per anchor |
| `podGrowthThreshold` | 0.8 (80%) | Buffer fill % that triggers pod spawn |
| `blockDropsOnly` | true | Only collect items from block breaking |

## Capacity Calculation

```
Total Capacity = Base Capacity + (Pod Count × Pod Capacity)
              = 256 + (4 × 8,192)
              = 256 + 32,768
              = 33,024 items
```

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

## File Structure

```
mininganchor/
├── AbstractMiningAnchorBlockEntity.java  # Base anchor logic
├── Tier1MiningAnchorBlock.java           # Feral anchor block
├── Tier1MiningAnchorBlockEntity.java     # Feral anchor entity
├── Tier2MiningAnchorBlock.java           # Hardened anchor block
├── Tier2MiningAnchorBlockEntity.java     # Hardened anchor entity
├── AnchorFillState.java                  # Fill state enum
├── MiningAnchorDialogueEvents.java       # Symbiote dialogue triggers
├── MiningAnchorInteractions.java         # Player interaction handlers
├── networking/
│   └── AnchorFillStatePayload.java       # Client sync for fill state
└── pod/
    ├── AbstractStoragePodBlockEntity.java # Base pod logic
    ├── PodContents.java                   # Pod item storage
    ├── Tier1PodBlock.java                 # Feral pod (spills items)
    ├── Tier1PodBlockEntity.java
    ├── Tier2PodBlock.java                 # Hardened pod (keeps items)
    └── Tier2PodBlockEntity.java
```
