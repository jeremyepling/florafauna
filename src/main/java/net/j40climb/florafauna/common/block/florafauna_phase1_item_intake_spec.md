# Florafauna — Phase 1 Storage & Item Intake (Claude Code Implementation Spec)
_Target: Minecraft 1.21.11 · NeoForge (post-21.9 Transfer API)_
_Mod id: `FloraFauna.MOD_ID` (`"florafauna"`)_
_Package root: `net.j40climb.florafauna`_

This document is **authoritative for Phase 1**. Claude Code should implement *exactly* what is described here.

---

## Phase 1 Goal

Ship a **living storage intake system** that:
- Removes items from player interaction
- Visually absorbs them into the world
- Buffers safely (no voiding, no spam)
- Transfers them later into paired vanilla storage
- Is fully performance-tunable

No GUIs. No backpacks. No pipes.

---

# Core Mental Model (LOCKED)

- **Items are claimed by the world, not the player**
- **Intakes eat items**
- **Buffers decouple visuals from storage**
- **Storage insertion is delayed, throttled, and safe**

---

# Item Intake Lifecycle (MANDATORY)

## State Machine (per ItemEntity)

```
FREE → CLAIMED → ABSORBED → BUFFERED → STORED
```

- `FREE`: vanilla item on ground
- `CLAIMED`: visually present, cannot be picked up
- `ABSORBED`: animation complete, entity removed
- `BUFFERED`: stack lives in intake memory
- `STORED`: inserted into chest/barrel

---

## Step 1 — Visual Capture (World → Intake)

When an `ItemEntity` enters an intake radius:

### Server actions
- Mark item as claimed:
  - `item.setPickUpDelay(Integer.MAX_VALUE)`
  - Tag entity with `florafauna:claimed`
- Record `claimedAtTick`
- Do **not** add to buffer yet

### Client-visible behavior
For ~10–20 ticks:
- Item drifts toward intake center
- Slight downward motion (sinking)
- Root/funnel pulses outward
- Item remains rendered
- Player **cannot** pick it up
- Item **cannot** despawn

### Completion
After animation duration:
- Server removes `ItemEntity`
- Extract `ItemStack`
- Pass stack to intake buffer

---

## Step 2 — Internal Buffer (DECOUPLING MECHANISM)

Each **Item Intake BlockEntity** owns:

```java
NonNullList<ItemStack> buffer;
```

### Rules (LOCKED)
- Buffer is **private**
- Not an inventory
- Not a capability
- Never exposed to player or other blocks
- Stacks are merged by item + components
- Hard cap: `maxBufferedStacks`

### Buffer full behavior
- Intake stops claiming new items
- New items remain on ground (safe fallback)
- Intake enters **BLOCKED / CHOKING** visual state

---

## Step 3 — Buffered Transfer (Intake → Storage)

Buffered stacks are transferred **later**, on a separate timer.

### Transfer cadence
- Every `transferIntervalTicks`
- Limited by:
  - `maxStacksPerTransferTick`
  - `maxItemsPerTransferTick`

### Transfer API usage (MANDATORY)
- Use NeoForge **Transfer API**
- `ResourceHandler<ItemResource>`
- `Transaction.openRoot()`
- Commit partial inserts

### If destination is full
- Keep items in buffer
- Apply exponential backoff:
  - `blockedRetryBaseTicks`
  - up to `blockedRetryMaxTicks`
- Continue blocked visuals

**Never void items. Ever.**

---

## Storage Destination (Phase 1)

### Valid destinations
- Paired **Chest**
- Paired **Barrel**

### Resolution order
1. Paired container within `localDestRadius`
2. Any paired container from anchor network
3. Otherwise: remain buffered

---

# Blocks to Implement (Phase 1)

## 1) Storage Anchor
- Scans for nearby chests/barrels
- Marks them as paired
- Provides valid destinations for intakes

## 2) Root Intake Node (Surface)
- Claims nearby items
- Plays absorption animation
- Buffers stacks
- Pushes into paired storage

## 3) Fungal Funnel (Cave Variant)
- Same logic as Root Intake
- Different visuals/sounds

## 4) Field Relay (Optional Phase 1C)
- Does **not** collect items
- Links nearby intakes to a Storage Anchor
- Enables remote mining

---

# Performance Controls (CONFIG)

Claude must expose these as config values:

### Collection
- `collectRadius`
- `collectIntervalTicks`
- `maxItemEntitiesPerCollect`
- `maxItemsPerCollect`

### Buffer
- `maxBufferedStacks`

### Transfer
- `transferIntervalTicks`
- `maxStacksPerTransferTick`
- `maxItemsPerTransferTick`

### Overflow
- `blockedRetryBaseTicks`
- `blockedRetryMaxTicks`

---

# Acceptance Criteria (Phase 1)

- Dropped items near intake are **never picked up by player**
- Items visibly slide and sink before disappearing
- No items enter player inventory
- Storage fills gradually over time
- Full storage causes visible blockage, not loss
- TPS stable under stress

---

# Explicit Non-Goals (DO NOT IMPLEMENT)

- GUIs
- Player inventory hooks
- Item magnet behavior
- Item teleportation
- Cross-dimension transport

---

## Final Note to Claude Code

If any ambiguity exists, prefer:
- **world-visible behavior**
- **player clarity**
- **safety over speed**

This system should feel *alive*, not mechanical.
