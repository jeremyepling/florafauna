# Symbiotic Husk System

Death recovery system that preserves player inventory and symbiote progression.

## Overview

When a player with a bonded symbiote dies, their inventory is stored in a Symbiotic Husk placed near the death location. The husk type depends on the player's symbiote state at death, determining whether abilities are restored upon retrieval.

## How It Works

1. **Death** - Player dies with bonded symbiote
2. **Husk Creation** - Inventory stored in husk at valid position near death
3. **Weakened State** - Player respawns with symbiote abilities disabled
4. **Locator** - HUD bar guides player to Restoration Husk
5. **Recovery** - Right-click husk to retrieve items and restore abilities

## Symbiote State Machine

| State | Description | On Death |
|-------|-------------|----------|
| UNBOUND | No symbiote | Normal death (no husk) |
| READY_TO_BIND | Consumed stew, not yet bound | Normal death (no husk) |
| BONDED_ACTIVE | Full symbiote abilities | Creates Restoration Husk, transitions to WEAKENED |
| BONDED_WEAKENED | Abilities disabled | Creates Container Husk, stays WEAKENED |

## Husk Types

| Type | Created When | Stores Items | Restores Abilities | Locator Bar | Particles |
|------|--------------|--------------|-------------------|-------------|-----------|
| RESTORATION | Die while BONDED_ACTIVE | Yes | Yes | Yes | Yes |
| CONTAINER | Die while BONDED_WEAKENED | Yes | No | No | No |
| BROKEN | After all items retrieved | No | No | No | No |

## Storage Layout

| Slots | Contents |
|-------|----------|
| 0-8 | Hotbar |
| 9-35 | Main inventory |
| 36-39 | Armor (feet, legs, chest, head) |
| 40 | Offhand |

**Total: 41 slots** (full player inventory)

## Placement Rules

| Death Scenario | Husk Placement |
|----------------|----------------|
| In fluid | Highest air block above fluid column |
| In air (falling) | First solid block below |
| In void (below world) | Cocoon spawn, else world spawn |
| Blocked position | Search upward (10 blocks), then radial (5 blocks) |
| All searches fail | Cocoon spawn, else world spawn |

**Valid Placement**: Air or replaceable block with solid support below

## Search Parameters

| Parameter | Value |
|-----------|-------|
| Max upward search | 10 blocks |
| Max radial search | 5 blocks |
| Radial Y variance | +/- 2 blocks |

## Interaction Rules

| Action | Result |
|--------|--------|
| Right-click (owner) | Transfer items to inventory |
| Right-click (non-owner) | "This husk does not recognize you" |
| Inventory full | Remaining items stay in husk |
| All items retrieved | Husk transitions to BROKEN |
| Break husk (owner) | Items drop to ground |
| Break husk (non-owner) | Prevented |

## Item Transfer Priority

1. Try original slot first (armor back to armor slot, etc.)
2. If original slot occupied, add to any available slot
3. If inventory full, item remains in husk

## Restoration Husk Features

- **Locator Bar** - HUD element pointing to husk location
- **Particles** - Firefly-style ambient particles (visible to all players)
- **Ability Restore** - Symbiote transitions WEAKENED → ACTIVE on interaction
- **Dimension Tracked** - Works across dimensions
