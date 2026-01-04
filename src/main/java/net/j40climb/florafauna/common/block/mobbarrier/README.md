# Mob Barrier System

Invisible barrier block that selectively blocks configured entities while allowing others to pass through.

## Overview

The Mob Barrier is an invisible, unbreakable block that creates solid collision only for specified entity types. Players and unconfigured entities pass through freely. Useful for mob farms, animal pens, and area protection.

## Features

- Completely invisible when placed
- Solid collision only for configured entities
- All other entities pass through freely
- Blocks pathfinding for configured entities
- Unbreakable in survival mode
- Config stored on item (portable presets)

## Entity Matching

Entities can be blocked by:

| Type | Format | Example |
|------|--------|---------|
| Entity ID | `namespace:entity_name` | `minecraft:zombie` |
| Entity Tag | `#namespace:tag_name` | `#minecraft:undead` |

## Common Entity Tags

| Tag | Description |
|-----|-------------|
| `#minecraft:undead` | Zombies, skeletons, phantoms, etc. |
| `#minecraft:arthropod` | Spiders, silverfish, endermites |
| `#minecraft:raiders` | Pillagers, vindicators, evokers, etc. |
| `#minecraft:skeletons` | All skeleton variants |
| `#minecraft:zombies` | All zombie variants |
| `#minecraft:illager` | Illager mob types |
| `#minecraft:followable_friendly_mobs` | 25 passive mobs (cow, pig, sheep, etc.) |
| `#minecraft:fall_damage_immune` | Entities immune to fall damage |
| `#minecraft:can_breathe_under_water` | Aquatic entities |
| `#minecraft:powder_snow_walkable_mobs` | Light mobs that walk on powder snow |

## Interactions

| Action | Result |
|--------|--------|
| **Place block** | Barrier placed with config from held item |
| **B key** (holding barrier) | Open config screen |
| **Shift + Right-click** (on placed barrier, holding barrier item) | Copy config from placed block to held item |

## Config Screen

The config screen allows adding/removing blocked entities:

- **Text input** - Enter entity ID or tag (with `#` prefix)
- **Add button** - Add entry to block list
- **Entry list** - Shows all blocked entities/tags
- **Click entry** - Remove from list

## Block Properties

| Property | Value |
|----------|-------|
| Render shape | Invisible |
| Hardness | Unbreakable (survival) |
| Collision | Selective (configured entities only) |
| Pathfinding | Blocked for configured entities |
| Interaction | Pass-through (can interact with blocks behind) |

## Use Cases

| Scenario | Configuration |
|----------|---------------|
| Mob farm containment | `#minecraft:undead` |
| Animal pen | `#minecraft:followable_friendly_mobs` |
| Spider-proof area | `#minecraft:arthropod` |
| Raid protection | `#minecraft:raiders` |
| Single mob type | `minecraft:creeper` |

## Config Persistence

- Config is stored as a **data component** on the item
- Placing a barrier copies the item's config to the block
- Shift-right-clicking copies the block's config back to the item
- Allows creating and sharing barrier presets
