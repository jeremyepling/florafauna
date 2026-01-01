# Symbiote Voice & Progression System

The symbiote is a bonded companion that observes the player's experiences and occasionally comments on them. It uses a tiered voice system with cooldowns to ensure messages feel rare and impactful, plus a progression system that tracks what concepts the symbiote has learned.

## Architecture Overview

```
symbiote/
├── event/                     # Game event handlers
│   └── SymbioteObservationEvents.java
├── observation/               # Event routing & classification
│   ├── ObservationArbiter.java
│   ├── ObservationCategory.java
│   └── ChaosSuppressor.java
├── voice/                     # Voice output & cooldowns
│   ├── SymbioteVoiceService.java
│   ├── VoiceTier.java
│   └── VoiceCooldownState.java
├── progress/                  # Concept state machine
│   ├── ProgressSignalTracker.java
│   ├── ProgressSignalUpdater.java
│   ├── ConceptSignal.java
│   └── SignalState.java
├── dream/                     # Dream insight system
│   ├── DreamInsightEngine.java
│   ├── DreamLevel.java
│   └── DreamContext.java
├── dialogue/                  # Data-driven dialogue
│   ├── SymbioteDialogueRepository.java
│   ├── SymbioteDialogueEntry.java
│   ├── SymbioteDialogueLoader.java
│   └── DialogueSelectionContext.java
├── SymbioteItem.java          # Consumable item for bonding
├── SymbioteData.java          # Player attachment data
└── SymbioteCommand.java       # Debug/admin commands
```

---

## Data Flow: Event → Voice Output

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GAME EVENT OCCURS                                │
│                    (damage, sleep, cold water, etc.)                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    SymbioteObservationEvents.java                        │
│  ────────────────────────────────────────────────────────────────────── │
│  • Listens to NeoForge events (LivingDamageEvent, PlayerWakeUpEvent)    │
│  • Checks if player has bonded symbiote                                  │
│  • Determines ObservationCategory (COMBAT_DAMAGE, FALL_DAMAGE, etc.)    │
│  • Calculates severity (0-100 based on damage amount)                   │
│  • Records damage for chaos suppression                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      ObservationArbiter.observe()                        │
│  ────────────────────────────────────────────────────────────────────── │
│  • Determines VoiceTier (TIER_1_AMBIENT or TIER_2_BREAKTHROUGH)         │
│  • Selects dialogue from SymbioteDialogueRepository                     │
│  • Calls SymbioteVoiceService.trySpeak()                                │
│  • Updates ProgressSignalTracker with concept progression               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌───────────────────────────────┐   ┌───────────────────────────────────┐
│   SymbioteVoiceService        │   │   ProgressSignalTracker           │
│  ───────────────────────────  │   │  ───────────────────────────────  │
│  • Check bond status          │   │  • Create/update concept signal   │
│  • Check chaos suppression    │   │  • Track interaction count        │
│  • Check tier cooldowns       │   │  • Auto-progression:              │
│  • Check category dampening   │   │    SEEN → TOUCHED → STABILIZED   │
│  • If allowed: send message   │   │    → INTEGRATED (or NEGLECTED)   │
│  • Update cooldown state      │   │  • Store in player attachment    │
└───────────────────────────────┘   └───────────────────────────────────┘
            │
            ▼
┌───────────────────────────────┐
│      Player sees message      │
│  (subtle italic, purple-gray) │
└───────────────────────────────┘
```

---

## Observation Categories

Defined in `ObservationCategory.java`:

| Category | Key | Triggered By |
|----------|-----|--------------|
| `COMBAT_DAMAGE` | `combat_damage` | Mob attacks, PvP damage |
| `FALL_DAMAGE` | `fall_damage` | Fall damage |
| `ENVIRONMENTAL_HAZARD` | `environmental_hazard` | Freezing, drowning, cold water |
| `PLAYER_STATE` | `player_state` | Sleep, other player states |
| `BONDING_MILESTONE` | `bonding_milestone` | Initial bonding, evolution |

Each category auto-creates a concept like `first_combat_damage` when first observed.

---

## Voice Tiers & Cooldowns

Defined in `VoiceTier.java`:

| Tier | When Used | Global Cooldown | Post-Tier-2 Lockout |
|------|-----------|-----------------|---------------------|
| **TIER_1_AMBIENT** | Normal observations | 5 min (6000 ticks) | Blocked 10 min after Tier 2 |
| **TIER_2_BREAKTHROUGH** | First-time high-severity events, bonding milestones | 30 min (36000 ticks) | N/A |

### Tier Determination Logic (in ObservationArbiter):

```java
if (category == BONDING_MILESTONE) → TIER_2_BREAKTHROUGH
if (severity >= 80 && first time for this category) → TIER_2_BREAKTHROUGH
else → TIER_1_AMBIENT
```

### Chaos Suppression

`ChaosSuppressor.java` blocks Tier 1 voice during rapid damage (5+ damage events in 3 seconds). This prevents spam during intense combat.

---

## Concept Progression State Machine

Defined in `SignalState.java`:

```
    UNSEEN (never encountered)
        │
        ▼ (first observation)
      SEEN
        │
        ▼ (3+ interactions)
     TOUCHED
        │
        ▼ (continued engagement)
   STABILIZED
        │
        ├──────────────────────┐
        ▼                      ▼
   INTEGRATED              NEGLECTED
  (fully learned)        (abandoned/stalled)
```

### How Concepts Progress

1. **Auto-created**: When `ObservationArbiter.observe()` is called, a concept like `first_combat_damage` is created if it doesn't exist
2. **Interaction counting**: Each observation increments the interaction count
3. **State transitions**: Handled by `ProgressSignalUpdater.checkAndAdvanceState()`
4. **Stall detection**: If a concept hasn't progressed in a long time, it may become `NEGLECTED`

### Concept Naming Convention

Concepts are dynamically created with the pattern: `first_<category_key>`

Examples:
- `first_combat_damage`
- `first_fall_damage`
- `first_environmental_hazard`
- `first_bonding_milestone`

---

## Data-Driven Dialogue

Dialogue is loaded from JSON files in `data/florafauna/symbiote_dialogue/`.

### Dialogue Entry Structure (`SymbioteDialogueEntry.java`):

```json
{
  "key": "symbiote.voice.combat.hit",
  "tier": "TIER_1_AMBIENT",
  "category": "combat_damage",
  "minSeverity": 0,
  "maxSeverity": 100,
  "weight": 10,
  "requiredConcepts": [],
  "excludedConcepts": []
}
```

### Dialogue Selection (`SymbioteDialogueRepository.selectDialogue()`):

1. Filter dialogue by tier, category, severity range
2. Check required/excluded concepts against player's progress
3. Weighted random selection from remaining dialogue

---

## Dream System

Dreams provide insight into stalled progression. Triggered via `/symbiote dream`.

### Dream Levels (`DreamLevel.java`):

| Level | When | Style |
|-------|------|-------|
| **L1_REFLECTIVE** | First dream or >5 days since last | Vague, poetic |
| **L2_DIRECTIONAL** | <2 days since last, no progress | Clearer hints |
| **L3_ANCHORED** | Repeated dreams, still stuck | Specific guidance |

### Dream Flow:

```
/symbiote dream
      │
      ▼
DreamInsightEngine.processDream()
      │
      ├─► Determine DreamLevel based on time since last dream
      │
      ├─► Build DreamContext (stalled signals, partially complete)
      │
      ├─► Select dream dialogue from repository
      │
      └─► Send styled message + update dream state
```

---

## Player Attachments

Stored on the player via NeoForge's attachment system:

| Attachment | Type | Purpose |
|------------|------|---------|
| `SYMBIOTE_DATA` | `SymbioteData` | Bond status, tier, abilities |
| `SYMBIOTE_PROGRESS` | `ProgressSignalTracker` | Concept states, dream level |
| `VOICE_COOLDOWNS` | `VoiceCooldownState` | Tier/category cooldown timestamps |

### Data Persistence

When the symbiote is **unbonded** (via command or future mechanic), progress is saved to the item as a data component (`SYMBIOTE_PROGRESS`). When re-bonded to any player, the memory is restored.

---

## Adding New Observations

### 1. Add Event Handler

In `SymbioteObservationEvents.java`:

```java
@SubscribeEvent
public static void onMyEvent(MyEventType event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;

    SymbioteData data = player.getData(RegisterAttachmentTypes.SYMBIOTE_DATA);
    if (!data.bonded()) return;

    ObservationArbiter.observe(player, ObservationCategory.PLAYER_STATE, 50, Map.of(
        "event", "my_event_name"
    ));
}
```

### 2. (Optional) Add New Category

In `ObservationCategory.java`:

```java
MY_NEW_CATEGORY("my_new_category");
```

### 3. Add Dialogue

In `data/florafauna/symbiote_dialogue/my_category.json` and `en_us.json`.

---

## Commands

| Command | Description |
|---------|-------------|
| `/symbiote check` | Show current symbiote data |
| `/symbiote bond` | Bond a symbiote (debug) |
| `/symbiote unbond` | Unbond and get item back |
| `/symbiote reset` | Full reset (no item) |
| `/symbiote dream` | Trigger a dream |
| `/symbiote dream force <1-3>` | Force specific dream level |
| `/symbiote cooldown reset` | Reset voice cooldowns |
| `/symbiote progress` | Show concept progress |
| `/symbiote progress set <concept> <state>` | Force concept state |

---

## Key Design Principles

1. **Rare and impactful**: Cooldowns ensure the symbiote doesn't spam. Messages should feel special.

2. **Subtle presentation**: No prefix, no emoji. Italic text in muted purple-gray (`#B39DDB`).

3. **Memory persists**: The symbiote remembers experiences across bond/unbond cycles.

4. **Data-driven**: Dialogue is loaded from JSON resources, not hardcoded.

5. **Progressive revelation**: The symbiote learns concepts over time (SEEN → INTEGRATED).

6. **Chaos awareness**: Voice is suppressed during intense combat to avoid distraction.
