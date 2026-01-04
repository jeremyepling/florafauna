# Symbiote System

AI-driven companion system with abilities, voice output, dreams, and death recovery.

## Overview

The Symbiote is a permanent player companion that provides abilities, observational commentary, dream insights, and death recovery through the Husk system. The symbiote has personality - it speaks rarely, with deadpan wit, and tracks player progress.

## Binding Process

1. **Consume Symbiote Stew** - Grants "Symbiote Prepared" effect, sets `symbioteBindable = true`
2. **Enter Cocoon Chamber** - With Dormant Symbiote in inventory
3. **Bind** - Symbiote transfers from item to player attachment
4. **Active** - Abilities enabled, voice system active

## Symbiote States

| State | Description | Abilities |
|-------|-------------|-----------|
| UNBOUND | No symbiote | None |
| READY_TO_BIND | Consumed stew, not yet bound | None |
| BONDED_ACTIVE | Full symbiote bond | Enabled |
| BONDED_WEAKENED | After death, before recovery | Disabled |

## State Transitions

```
UNBOUND ──(consume stew)──> READY_TO_BIND
READY_TO_BIND ──(bind in cocoon)──> BONDED_ACTIVE
BONDED_ACTIVE ──(die)──> BONDED_WEAKENED + Restoration Husk
BONDED_WEAKENED ──(interact with husk)──> BONDED_ACTIVE
```

## Abilities

| Ability | Key | Description |
|---------|-----|-------------|
| Dash | Mouse 4 | Quick forward burst |
| Feather Falling | Passive | Reduced fall damage |
| Speed | Passive | Movement speed boost |
| Jump Boost | Passive | Higher jumps (configurable level) |

## Player Data

Stored as player attachment (`PLAYER_SYMBIOTE_DATA`):

| Field | Type | Description |
|-------|------|-------------|
| `symbioteState` | SymbioteState | Current bond state |
| `bondTime` | long | Game time of bonding |
| `tier` | int | Symbiote tier level |
| `dash` | boolean | Dash ability unlocked |
| `featherFalling` | boolean | Feather falling unlocked |
| `speed` | boolean | Speed boost unlocked |
| `jumpBoost` | int | Jump boost level (0 = off) |

## Voice System

The symbiote speaks through a tiered cooldown system:

### Voice Tiers

| Tier | Type | Global Cooldown | Category Dampening |
|------|------|-----------------|-------------------|
| Tier 1 | Ambient | 5 minutes | 1 minute |
| Tier 2 | Breakthrough | 30 minutes | 1 hour |

**Post-Tier-2 Lockout**: After a Tier 2 message, Tier 1 is silenced for 15 minutes.

### Observation Categories

| Category | Triggers |
|----------|----------|
| ENVIRONMENTAL_HAZARD | Cold water, lava, drowning |
| COMBAT_DAMAGE | Mob attacks |
| FALL_DAMAGE | Fall damage |
| PLAYER_STATE | Hunger, low health |
| SLEEP | Sleep events |
| BONDING_MILESTONE | Initial bond, tier up |
| MINING_ANCHOR | Fill warnings |

### Chaos Suppression

After 5+ damage events in quick succession, voice output is suppressed to avoid spam during intense combat.

## Dream System

Dreams provide guidance when the player is stuck:

### Dream Levels

| Level | Name | Color | Style |
|-------|------|-------|-------|
| L1 | Reflective | Gray | Vague, poetic |
| L2 | Directional | Purple | Clearer hints |
| L3 | Anchored | Light Purple | Specific guidance |

Dreams escalate with repeated sleep when no progress is made.

## Progress Tracking

The symbiote tracks player progress through concept signals:

### Signal States

| State | Description |
|-------|-------------|
| UNMET | Not encountered |
| ENCOUNTERED | Discovered |
| IN_PROGRESS | Actively working on |
| INTEGRATED | Completed |

### Stall Detection

If progress stalls (same state for extended time), dreams become more direct.

## Items

| Item | Purpose |
|------|---------|
| **Dormant Symbiote** | Carries symbiote data, used for binding |
| **Symbiote Stew** | Consumable that prepares player for binding |

## Related Systems

- **Cocoon Chamber** - Block where binding occurs, sets spawn point
- **Symbiotic Husk** - Death recovery system (see husk/README.md)
- **Mining Anchor** - Storage system with symbiote dialogue integration

## Commands

```
/florafauna symbiote status     - Show symbiote state
/florafauna symbiote bind       - Force bind (debug)
/florafauna symbiote unbind     - Force unbind (debug)
/florafauna symbiote toggle <ability> - Toggle ability
/florafauna symbiote dream      - Trigger dream
/florafauna symbiote reset      - Reset cooldowns
```
