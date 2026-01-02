# Florafauna — Symbiotic Husk Implementation Spec

> Purpose: Hand-off implementation document for Claude Code  
> Target: NeoForge 1.21.11  
> Mod ID: FloraFauna.MOD_ID (strings use `florafauna`)  
> Package: net.j40climb.florafauna

---

## 1. High-level intent

The Symbiotic Husk is a biological death-recovery system that:
- Prevents item loss
- Prevents symbiote progression loss
- Provides clear, diegetic recovery guidance
- Separates spawn (cocoon) from recovery (husk)

PlayerSymbioteData is never destroyed — death only weakens it.

---

## 2. Player symbiote state machine

States:
- UNBOUND
- READY_TO_BIND
- BONDED_ACTIVE
- BONDED_WEAKENED

Transitions:
- Drink symbiote_stew → UNBOUND → READY_TO_BIND
- Bind in Cocoon → READY_TO_BIND → BONDED_ACTIVE
- Die while active → BONDED_ACTIVE → BONDED_WEAKENED + RESTORATION_HUSK
- Die while weakened → remain BONDED_WEAKENED + CONTAINER_HUSK
- Interact with Restoration Husk → BONDED_WEAKENED → BONDED_ACTIVE

---

## 3. PlayerSymbioteData (authoritative)

- Persisted permanently
- Never synced into items

Required fields:
- symbiote_state
- unlocked_abilities
- restoration_husk_pos
- restoration_husk_dimension
- restoration_husk_active

---

## 4. Symbiotic Husk

- Block + BlockEntity
- Visible to all players
- Only owner can interact
- Mobs cannot interact or damage
- Never used as spawn

Each death creates a husk storing the player inventory at that moment.

---

## 5. Husk logical types

RESTORATION_HUSK:
- Created when dying while BONDED_ACTIVE
- Stores items
- Restores symbiote
- Tracked + locator bar
- Emits firefly-style particles

CONTAINER_HUSK:
- Created when dying while BONDED_WEAKENED
- Stores items only
- No locator, no restore

BROKEN_HUSK:
- Inert visual remnant

---

## 6. Husk blockstates

- restoration_husk
- container_husk
- broken_husk

---

## 7. Interaction rules

Right-click FULL husk (owner):
- Transfer items to inventory
- If inventory fills: remaining items stay in husk

When all items transferred:
- Transition to BROKEN_HUSK

If Restoration Husk:
- Restore symbiote
- Re-enable abilities
- Clear restoration pointer

---

## 8. Locator Bar

- Active only while BONDED_WEAKENED
- Points to Restoration Husk only
- Removed after restoration

---

## 9. Particles

- Firefly bush–style particles
- Active only for Restoration Husk while weakened
- Ambient, no directional trails

---

## 10. Husk placement rules (LOCKED)

- Death in fluid → highest air block above column
- Death in air → first solid block below

Edge cases:
- Void → cocoon spawn, else world spawn
- Blocked → upward then radial search
- Suffocation → radial search
- Height limits → clamp + search

Valid placement:
- Air or replaceable
- Solid support below

---

End of spec
