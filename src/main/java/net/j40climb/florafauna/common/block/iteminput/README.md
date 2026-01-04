# Item Input System

Living storage intake system that collects dropped items and transfers them to vanilla storage.

## Overview

Item Inputs collect dropped items from the world, buffer them internally, and transfer them to paired storage containers (chests, barrels). The system prioritizes safety - items are never voided, and full storage causes visible blockage rather than loss.

## How It Works

1. **Collection** - Item Input scans for nearby dropped items
2. **Claiming** - Items are marked as claimed (cannot be picked up by players)
3. **Animation** - Items visually drift toward the input over ~10-20 ticks
4. **Absorption** - Item entity removed, stack added to internal buffer
5. **Transfer** - Buffered items are pushed to paired storage containers

## Item Lifecycle

```
FREE → CLAIMED → ABSORBED → BUFFERED → STORED
```

| State | Description |
|-------|-------------|
| FREE | Vanilla item on ground |
| CLAIMED | Cannot be picked up, animating toward input |
| ABSORBED | Animation complete, entity removed |
| BUFFERED | Stack stored in input's internal buffer |
| STORED | Inserted into chest/barrel |

## Blocks

| Block | Purpose |
|-------|---------|
| **Storage Anchor** | Scans for nearby containers, provides storage destinations |
| **Item Input** | Collects items, buffers, transfers to anchor's storage |
| **Field Relay** | Large-scale collection for farms and mob grinders |

## Configuration Values

### Collection

| Setting | Default | Description |
|---------|---------|-------------|
| `collectRadius` | 8 blocks | Scan radius for dropped items |
| `collectIntervalTicks` | 10 ticks | Time between collection scans |
| `maxItemEntitiesPerCollect` | 8 | Max item entities claimed per cycle |
| `maxItemsPerCollect` | 64 | Max total items claimed per cycle |
| `animationDurationTicks` | 15 ticks | Time for item to animate to input |

### Buffer

| Setting | Default | Description |
|---------|---------|-------------|
| `maxBufferedStacks` | 27 stacks | Buffer capacity (like a chest) |

### Transfer

| Setting | Default | Description |
|---------|---------|-------------|
| `transferIntervalTicks` | 20 ticks | Time between transfer attempts |
| `maxStacksPerTransferTick` | 4 stacks | Max unique stacks transferred per tick |
| `maxItemsPerTransferTick` | 256 items | Max total items transferred per tick |

### Blocked State

| Setting | Default | Description |
|---------|---------|-------------|
| `blockedRetryBaseTicks` | 40 ticks | Initial retry delay when blocked |
| `blockedRetryMaxTicks` | 600 ticks | Maximum retry delay (exponential backoff) |

### Storage Anchor

| Setting | Value | Description |
|---------|-------|-------------|
| Scan radius | 8 blocks | Range for auto-detecting containers |
| Scan interval | 100 ticks (5s) | Time between container scans |

## Visual States

| State | Condition | Behavior |
|-------|-----------|----------|
| NORMAL | Buffer empty | Idle |
| WORKING | Transferring items | Active animation |
| BLOCKED | No storage or storage full | Warning visuals, exponential backoff |

## Storage Destinations

### Priority Order
1. Explicitly linked containers (highest)
2. Auto-detected containers within radius

### Valid Destinations
- Chests
- Barrels
- Any block with IItemHandler capability

## Pairing

Item Inputs must be paired to a Storage Anchor to transfer items.

| Action | Result |
|--------|--------|
| Right-click Item Input | Show status (paired/unpaired, buffer count) |
| Shift + Right-click Item Input near Anchor | Pair to nearest anchor |
| Shift + Right-click paired Item Input | Unpair from anchor |

## Transfer API

Uses NeoForge Transfer API for safe item insertion:
- `ResourceHandler<ItemResource>` for container access
- `Transaction.openRoot()` for atomic operations
- Partial inserts committed, remainder kept in buffer

## Safety Rules

- Items are **never voided**
- Full buffer → stop claiming new items (items stay on ground)
- Full storage → exponential backoff, items remain buffered
- Block broken → buffer contents drop to ground
- Claimed items released if input removed
