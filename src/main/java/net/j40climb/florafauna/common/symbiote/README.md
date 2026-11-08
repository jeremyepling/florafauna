# Symbiote System

AI-driven companion system with abilities, voice output, dreams, and death recovery.

## Overview

The Symbiote is a permanent player companion that provides abilities, observational commentary, dream insights, and death recovery through the Husk system. The symbiote has personality - it speaks rarely, with deadpan wit, and tracks player progress.

## Binding Process

1. **Consume Symbiote Stew** - Grants "Symbiote Prepared" effect, sets `symbioteBindable = true`
2. **Open the Cocoon Chamber** with a Dormant Symbiote in your inventory.
3. **Select Bind** to transfer the symbiote to the player attachment. **Set Spawn** is a separate chamber action.
4. **Active** - Abilities enabled, voice system active

## Symbiote States

| State | Description | Abilities |
|-------|-------------|-----------|
| UNBOUND | No symbiote | None |
| READY_TO_BIND | Defined in the enum. Stew records readiness in the separate `symbioteBindable` field. | None |
| BONDED_ACTIVE | Full symbiote bond | Enabled |
| BONDED_WEAKENED | After death, before recovery | Disabled |

## State Transitions

```
UNBOUND ──(consume stew)──> UNBOUND + symbioteBindable
UNBOUND + symbioteBindable ──(bind in cocoon)──> BONDED_ACTIVE
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
| UNSEEN | Never encountered |
| SEEN | Observed once |
| TOUCHED | Interacted with multiple times |
| STABILIZED | Consistent engagement |
| INTEGRATED | Completed |
| NEGLECTED | Progress has stalled |

### Stall Detection

If progress stalls (same state for extended time), dreams become more direct.

## Items

| Item | Purpose |
|------|---------|
| **Dormant Symbiote** | Carries symbiote data, used for binding |
| **Symbiote Stew** | Consumable that prepares player for binding |

## Related Systems

- **Cocoon Chamber** - Block where binding occurs, sets spawn point
- The [Symbiotic Husk guide](../block/husk/README.md) covers death recovery and the copied-world recovery playtest.
- The [Mining Anchor guide](../block/mininganchor/README.md) covers storage, binding and waypoint controls.

## Commands

Use these commands as a creative player or with game master permission.

| Command | Effect |
|---------|--------|
| `/florafauna symbiote check` | Show the player's symbiote data. |
| `/florafauna symbiote bond` or `unbond` | Apply or remove the bond for testing. |
| `/florafauna symbiote toggle <ability>` | Toggle `dash`, `featherFalling` or `speed`. |
| `/florafauna symbiote dream` | Request a dream under normal eligibility rules. |
| `/florafauna symbiote dream force <level>` | Trigger a dream at level 1, 2 or 3. |
| `/florafauna symbiote cooldown reset` | Clear voice cooldowns. |
| `/florafauna symbiote reset` | Remove the bond and reset abilities, progress signals and voice cooldowns. |
