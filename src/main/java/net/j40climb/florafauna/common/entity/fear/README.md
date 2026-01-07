# Fear System

Stress-based ecosystem for farming resources from hostile mobs without killing them.

## Overview

The Fear System creates a renewable resource loop where mobs with MobSymbiote can be scared by natural predators, causing them to "leak" resources instead of attacking. Currently implemented for creepers (gunpowder farming), with planned expansion to endermen and blazes.

## Fear States

| State | Description | Duration                                         |
|-------|-------------|--------------------------------------------------|
| CALM | Normal AI, no output | Until fear source detected                       |
| PANICKED | Shaking, particles, fuse suppressed | Config: `pan/<br/>icDurationForLeak` (default 10s)    |
| LEAK | Drops resources, remains alive | Instant transition                               |
| EXHAUSTED | Cooldown, immune to fear | Config: `exhaustedCooldownTicks` (default 3.75m) |
| OVERSTRESS | Terminal - triggers explosion | Immediate                                        |

## State Machine

```
         ┌──────────────────────────────────┐
         │                                  │
         ▼                                  │
      CALM ──(fear source)──> PANICKED     │
         ▲                       │          │
         │                       │          │
         │                  (duration)      │
         │                       │          │
         │                       ▼          │
         │                     LEAK ────────┤
         │                       │          │
         │                       │   (too many leaks)
         │                       ▼          │
         └──(cooldown)────── EXHAUSTED      │
                                            │
                                            ▼
                                      OVERSTRESS
                                       (explode)
```

## Fear Sources

| Mob Type | Fear Sources |
|----------|--------------|
| Creeper | Cats, Ocelots |
| Enderman | (planned) |
| Blaze | (planned) |

## Creeper Behavior

| State | Visual | Audio | Fuse |
|-------|--------|-------|------|
| CALM | Normal | Normal | Active |
| PANICKED | Smoke particles | - | Suppressed |
| LEAK | Smoke burst, poof | Hurt sound (high pitch) | Suppressed |
| EXHAUSTED | Normal | Normal | Active |

## Resource Output

### Creeper Leak Event

| Drop | Amount |
|------|--------|
| Gunpowder | 10-14 (configurable) |

## Overstress Mechanic

Consecutive LEAK events without returning to CALM naturally trigger OVERSTRESS:

| Leak Count | Result |
|------------|--------|
| 1-2 | Safe |
| 3 | OVERSTRESS (explosion) |

The counter resets only when the mob naturally returns to CALM (fear source leaves). Being scared again during EXHAUSTED preserves the count.

## Requirements

- Mob must have **MobSymbiote Level 1+** attached
- Mob must be in `florafauna:fearful_mobs` tag
- Fear source must be within detection range (default 16 blocks)

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `fearCheckIntervalTicks` | 10 | Ticks between fear updates |
| `panicDurationForLeak` | 200 (10s) | Ticks in PANICKED before LEAK |
| `exhaustedCooldownTicks` | 4500 (3.75m) | Cooldown before CALM |
| `maxLeaksBeforeOverstress` | 3 | Consecutive leaks to trigger explosion |
| `fearSourceDetectionRange` | 16.0 | Range to detect fear sources |
| `gunpowderDropMin` | 10 | Minimum gunpowder per LEAK |
| `gunpowderDropMax` | 14 | Maximum gunpowder per LEAK |

## Entity Attachment

Fear state stored as entity attachment (`FEAR_DATA`):

| Field | Type | Description |
|-------|------|-------------|
| `fearState` | FearState | Current state |
| `stateEnteredTick` | long | When state was entered |
| `leakCountSinceCooldown` | int | Consecutive leaks |
| `fearSourceX/Y/Z` | int | Position of fear source |
| `hasFearSourcePos` | boolean | Whether position is set |

## File Structure

| File | Purpose |
|------|---------|
| `FearState.java` | State enum with helper methods |
| `FearData.java` | Entity attachment record |
| `FearHelper.java` | State machine logic |
| `FearSourceDetector.java` | Finds nearby fear sources |
| `FearStateEvents.java` | Forge event integration |
| `FearAvoidanceGoal.java` | AI goal to flee from fear sources |
| `creeper/CreeperFearHandler.java` | Creeper-specific behavior |

## Debug Overlay

When looking at a fearful mob with debug overlay enabled:

- Current fear state (color-coded)
- Time in current state
- Countdown to next state transition
- Leak count with warning colors
- Fear source position

## Farm Design Tips

1. **Lure creepers** with MobSymbiote to collection area
2. **Place cats/ocelots** nearby (within 16 blocks)
3. **Hopper system** collects gunpowder drops
4. **Timing**: Allow EXHAUSTED cooldown to prevent OVERSTRESS
5. **Cat spacing**: One cat can scare multiple creepers

## Planned Expansions

- **Enderman**: Fear from water/rain, drops ender pearls
- **Blaze**: Fear from snow/ice, drops blaze rods
- **Wither Skeleton**: Fear from wolves(?), drops coal/skulls
