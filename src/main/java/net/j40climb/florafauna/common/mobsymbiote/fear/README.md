# Fear System

Stress-based ecosystem for farming resources from hostile mobs without killing them.

## Overview

The Fear System creates a renewable resource loop where mobs with MobSymbiote can be scared by natural predators or environmental factors, causing them to "leak" resources instead of attacking.

**Supported Mobs:**
- **Creepers** - Fear cats/ocelots, leak gunpowder
- **Endermen** - Fear staring faces and reflections, leak ender pearls
- **Blazes** - Fear snow golems and cold environments, leak blaze rods

## Fear States

| State | Description | Duration |
|-------|-------------|----------|
| CALM | Normal AI, no output | Until fear source detected |
| PANICKED | Shaking, particles, special behavior | Config: `panicDurationForLeak` (default 10s) |
| LEAK | Drops resources, remains alive | Instant transition |
| EXHAUSTED | Cooldown, immune to fear | Config: `exhaustedCooldownTicks` (default 3.75m) |
| OVERSTRESS | Terminal - mob dies | Immediate |

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
                                        (death)
```

## Fear Sources by Mob Type

| Mob Type | Fear Sources | Line of Sight Required |
|----------|--------------|------------------------|
| Creeper | Cats, Ocelots | Yes |
| Enderman | Armor stands with player heads or carved pumpkins, reflective blocks | Yes |
| Blaze | Snow golems + Cold blocks (configurable) | Yes (for golems) |

## Creeper Fear

### Fear Sources
- **Cats** (tamed or wild)
- **Ocelots**

### Behavior
| State | Visual | Audio | Fuse |
|-------|--------|-------|------|
| CALM | Normal | Normal | Active |
| PANICKED | Smoke particles | - | Suppressed |
| LEAK | Smoke burst | Hurt sound (high pitch) | Suppressed |
| OVERSTRESS | Large smoke burst | Explosion sound | Explodes |

### Drops
| Item | Amount |
|------|--------|
| Gunpowder | 10-14 (configurable) |

## Enderman Fear

### Fear Sources
1. **Armor Stands with Staring Faces**
   - Player heads
   - Carved pumpkins

2. **Reflective Blocks** (within stare distance)
   - Polished stones (andesite, diorite, granite, deepslate, blackstone, basalt)
   - Glass blocks (all colors, tinted)
   - Ice blocks (ice, packed ice, blue ice)
   - Obsidian, crying obsidian

### Behavior
| State | Visual | Audio |
|-------|--------|-------|
| CALM | Normal | Normal |
| PANICKED | Portal particles | Stare sound |
| LEAK | Particle burst | Hurt sound |
| OVERSTRESS | Large particle burst | Death sound |

### Drops
| Item | Amount |
|------|--------|
| Ender Pearl | 2-4 (configurable) |

### Configuration
| Setting | Default | Description |
|---------|---------|-------------|
| `endermanStareDistance` | 4.0 | Max distance to detect reflective blocks |
| `enderPearlDropMin` | 2 | Minimum pearls per LEAK |
| `enderPearlDropMax` | 4 | Maximum pearls per LEAK |

## Blaze Fear

### Fear Sources
Blazes can be scared by either condition (configurable):

1. **Snow Golems** - Minimum count required (default: 1)
2. **Cold Blocks** - At least 2 blocks in an 8-block radius by default.
   - Snow, snow blocks, powder snow
   - Ice, packed ice, blue ice, frosted ice

By default, **EITHER** condition triggers fear. Set `blazeRequireBothConditions=true` to require both.

### Behavior
| State | Visual | Audio | Attacks |
|-------|--------|-------|---------|
| CALM | Normal | Normal | Active |
| PANICKED | Steam/smoke particles | Distressed sound | Suppressed (configurable) |
| LEAK | Steam burst | Extinguish sound | Suppressed |
| OVERSTRESS | Large cloud burst | Death sound (low pitch) | Dies (hypothermia) |

### Drops
| Item | Amount |
|------|--------|
| Blaze Rod | 2-4 (configurable) |

### Configuration
| Setting | Default | Description |
|---------|---------|-------------|
| `blazeMinSnowGolems` | 1 | Minimum snow golems to trigger fear |
| `blazeColdScanRadius` | 8 | Radius to scan for cold blocks |
| `blazeMinColdBlocks` | 2 | Minimum cold blocks needed. A value of 0 always satisfies the cold-block condition. |
| `blazeRequireBothConditions` | false | If true, require BOTH golems AND cold blocks |
| `blazeRodDropMin` | 2 | Minimum blaze rods per LEAK |
| `blazeRodDropMax` | 4 | Maximum blaze rods per LEAK |
| `blazeSuppressAttacks` | true | Stop blaze attacks while panicked |

## Overstress Mechanic

Consecutive LEAK events without returning to CALM naturally trigger OVERSTRESS:

| Leak Count | Result |
|------------|--------|
| 1-2 | Safe |
| 3+ | OVERSTRESS (mob dies) |

The counter resets only when the mob naturally returns to CALM (fear source leaves).

**Death Types:**
- **Creeper**: Explodes
- **Enderman**: Dies with particle effects
- **Blaze**: Dies from hypothermia with steam effects

## Requirements

- Mob must have **MobSymbiote Level 1+** attached
- Mob must be in `florafauna:fearful_mobs` tag
- Fear source must be within detection range (default 16 blocks)
- Line of sight required for most fear sources

## General Configuration

These keys are in the `[fear]` section of the mod configuration.

| Setting | Default | Description |
|---------|---------|-------------|
| `checkIntervalTicks` | 10 | Ticks between fear updates |
| `panicDurationForLeak` | 200 (10s) | Ticks in PANICKED before LEAK |
| `exhaustedCooldownTicks` | 4500 (3.75m) | Cooldown before CALM |
| `maxLeaksBeforeOverstress` | 3 | Consecutive leaks before death |
| `detectionRange` | 16.0 | Range to detect fear sources |

## Entity Attachment

Fear state stored as entity attachment (`FEAR_DATA`):

| Field | Type | Description |
|-------|------|-------------|
| `fearState` | FearState | Current state |
| `stateEnteredTick` | long | When state was entered |
| `leakCountSinceCooldown` | int | Consecutive leaks |
| `fearSourceX/Y/Z` | int | Position of fear source |
| `hasFearSourcePos` | boolean | Whether position is set |

## Block Tags

| Tag | Purpose | Contents |
|-----|---------|----------|
| `florafauna:reflective_blocks` | Enderman reflection fear | Polished stones, glass, ice, obsidian |
| `florafauna:cold_blocks` | Blaze cold fear | Snow, ice variants |

## File Structure

| File | Purpose |
|------|---------|
| `FearState.java` | State enum with helper methods |
| `FearData.java` | Entity attachment record |
| `FearHelper.java` | State machine logic |
| `FearSourceDetector.java` | Finds nearby fear sources |
| `FearStateEvents.java` | NeoForge event integration |
| `goals/FearAvoidanceGoal.java` | AI goal to flee from fear sources |
| `creeper/CreeperFearHandler.java` | Creeper-specific behavior |
| `enderman/EndermanFearHandler.java` | Enderman-specific behavior |
| `blaze/BlazeFearHandler.java` | Blaze-specific behavior |

## Debug Overlay

When looking at a fearful mob with debug overlay enabled:

- Current fear state (color-coded)
- Time in current state
- Countdown to next state transition
- Leak count with warning colors
- Fear source position

## Farm Design Tips

### Creeper Farm
1. Lure creepers with MobSymbiote to collection area
2. Place cats/ocelots nearby (within 16 blocks)
3. Hopper system collects gunpowder drops
4. Allow EXHAUSTED cooldown to prevent OVERSTRESS

### Enderman Farm
1. Build containment area with reflective walls (polished stone, glass)
2. Place armor stands with carved pumpkins facing inward
3. Ensure endermen can see the "faces" (line of sight)
4. Endermen with MobSymbiote cannot teleport, making containment easier
5. Hopper system collects ender pearl drops

### Blaze Farm
1. With default settings, place two cold blocks in range or put one snow golem within sight of the blaze.
2. If `blazeRequireBothConditions` is enabled, supply both sources.
3. Protect the golem from fireballs when the blaze is outside its panic state.
4. Collect blaze rod drops with hoppers.

## Testing

The headless GameTests include `florafauna:fear_data_state_transitions`, `florafauna:fear_source_creeper_sources` and `florafauna:area_scanner_count_blocks`. Use the IntelliJ Client to check a bonded mob's visible panic behavior, particles and sounds.
