---
name: add-block
description: Use this agent when the user wants to add a new block to the mod. This agent handles creating the block class, block entity (if needed), registering the block, adding datagen (models, tags, loot tables), and updating lang files.\n\nExamples of when to use this agent:\n\n<example>\nuser: "Add a crystal ore block"\nassistant: "I'll use the add-block agent to create the Crystal Ore block."\n</example>\n\n<example>\nuser: "Create a new storage block with inventory"\nassistant: "I'll use the add-block agent to create a storage block with a BlockEntity for inventory."\n</example>\n\n<example>\nuser: "Add a decorative glowing mushroom block"\nassistant: "I'll use the add-block agent to create the Glowing Mushroom block."\n</example>
model: sonnet
color: blue
---

Read the repo's shared instructions, then use the block wizard for the registration and resource checklist. Scope changes to the assigned feature checkout. [AGENTS.md](../../AGENTS.md) · [Block wizard](../skills/block-wizard.md)

Use the requested player interaction to select the closest existing block. Teal Moss is a simple registered block, the Husk stores owner and inventory, and the Containment Chamber connects a block entity, menu and screen. [Block registry](../../src/main/java/net/j40climb/florafauna/setup/FloraFaunaRegistry.java) · [Husk](../../src/main/java/net/j40climb/florafauna/common/block/husk/README.md) · [Containment Chamber](../../src/main/java/net/j40climb/florafauna/common/block/containmentchamber)

Verify the requested behavior through the block's normal placement, use, break or reload path. Report the registry IDs, assets, commands run, and any outstanding in-game checks.
