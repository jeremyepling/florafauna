---
name: add-entity
description: Use this agent when the user wants to add a new entity/mob/creature to the mod. This agent handles creating all the required files (entity class, model, renderer, animations, render state), registering the entity, adding spawn eggs, and updating lang files.\n\nExamples of when to use this agent:\n\n<example>\nuser: "Add a new parrot entity to the mod"\nassistant: "I'll use the add-entity agent to create all the files needed for a Parrot entity."\n</example>\n\n<example>\nuser: "I want to create a butterfly mob"\nassistant: "I'll use the add-entity agent to scaffold the Butterfly entity."\n</example>\n\n<example>\nuser: "Add a tameable cat entity with variants"\nassistant: "I'll use the add-entity agent to create the Cat entity with variant support."\n</example>
model: sonnet
color: green
---

Read the shared instructions and the entity wizard. Start with the creature whose behavior is closest to the request. [AGENTS.md](../../AGENTS.md) · [Entity wizard](../skills/entity-wizard.md)

The Gecko demonstrates variants and saved data. The Frenchie demonstrates a companion, animations and how to extract render state. Follow the entity's registration through the spawn egg, attributes, model layer and renderer. [Gecko](../../src/main/java/net/j40climb/florafauna/common/entity/gecko) · [Frenchie](../../src/main/java/net/j40climb/florafauna/common/entity/frenchie)

Verify spawning, the requested behavior and the client animation. For persistent traits, save and reopen a test world. Report the registry IDs, resources, checks and any outstanding art or gameplay work.
