# ADR-001-Core-Principles.md

# Architecture Decision Record 001

# Village Colony Core Principles

**Status:** Accepted
**Date:** Project Foundation Phase
**Decision Type:** Architecture / Design Philosophy

---

# 1. Context

Village Colony aims to transform Minecraft Vanilla villages into autonomous living colonies.

During development, many possible approaches could be considered:

* replacing villagers with custom NPCs;
* creating artificial economies;
* adding virtual inventories;
* implementing independent simulation systems;
* creating new survival mechanics.

These approaches can increase complexity and move the project away from the original objective.

This ADR defines the fundamental principles that guide all future technical decisions.

---

# 2. Decision Summary

Village Colony will extend Minecraft Vanilla systems instead of replacing them.

The mod will:

* use existing villagers;
* use existing Minecraft resources;
* use existing recipes;
* use existing structures as reference;
* store only additional colony knowledge.

The world itself remains the source of truth.

---

# 3. Principle: Vanilla First

## Decision

Minecraft Vanilla behavior is the foundation of the project.

The mod must integrate with:

* VillagerEntity;
* Vanilla professions;
* Vanilla blocks;
* Vanilla inventories;
* Vanilla recipes;
* Vanilla structures.

---

## Reason

Minecraft already contains a stable ecosystem.

Replacing it would create:

* compatibility problems;
* unnecessary complexity;
* duplicated systems.

---

## Consequence

The mod adds intelligence around existing mechanics.

Example:

Correct:

```text
Villager

↓

Village Colony System

↓

Organized Work
```

Incorrect:

```text
Custom NPC

↓

Separate Civilization System
```

---

# 4. Principle: No Player Dependency

## Decision

The colony must function without player interaction.

---

## Reason

Minecraft villages already exist independently.

The mod should enhance that behavior.

---

## Consequence

The player:

Can:

* observe;
* assist;
* trade;
* interact.

Cannot:

* assign every task;
* control every villager;
* become required for survival.

---

# 5. Principle: No Artificial Needs

## Decision

The mod will not create new survival needs.

The existing Minecraft villager behavior remains responsible for:

* sleeping;
* food;
* breeding;
* schedules.

---

## Reason

The objective is not to create a new survival simulation.

The objective is to create a colony organization layer.

---

## Consequence

The mod will not add:

* hunger systems;
* morale;
* happiness;
* energy;
* custom health.

---

# 6. Principle: Physical Resources Only

## Decision

Resources must exist physically inside Minecraft.

---

## Reason

The world should remain understandable.

A player should be able to inspect the colony and see:

* where resources are stored;
* who collected them;
* how they are used.

---

## Correct Model

```text
Tree

↓

Lumberjack

↓

Worker Chest

↓

Manufacturer

↓

Construction
```

---

## Forbidden Model

```text
Village Resource Counter

+

500 Virtual Wood
```

---

# 7. Principle: Personal Worker Storage

## Decision

Each worker has a personal storage location.

The storage belongs to the worker.

---

## Rules

Storage:

* is located near the worker's bed;
* is represented by a real Minecraft chest;
* contains real items;
* can be inspected by the player.

---

## Reason

This creates a visible and believable economy.

---

# 8. Principle: Construction Must Respect Vanilla

## Decision

Builders expand villages without destroying original structures.

---

## Rules

Builders:

Can:

* place new blocks;
* create new houses;
* expand roads.

Cannot:

* destroy original generated village blocks;
* modify protected structures.

---

# 9. Principle: Colony Ownership of New Blocks

## Decision

Every block placed by the colony must receive internal ownership information.

---

## Reason

The system must distinguish:

```text
Original Minecraft Village

versus

Colony Expansion
```

---

## Consequence

Future systems can understand:

* what was generated;
* what was built;
* what can be modified.

---

# 10. Principle: Modular Architecture

## Decision

Systems must remain independent.

---

Architecture:

```text
Core Models

↓

Services

↓

Fabric Adapters
```

---

## Reason

Future expansion requires independent systems.

Examples:

Possible future additions:

* mining;
* blacksmith;
* defense;
* trade.

They should not require rewriting the foundation.

---

# 11. Principle: No Global Colony Inventory

## Decision

The colony will not have a central virtual inventory.

---

## Reason

Central inventories remove physical realism.

---

## Alternative

Use:

* worker storage;
* real containers;
* transportation systems in future versions.

---

# 12. Principle: Save Only Knowledge

## Decision

Persistence stores colony information, not the Minecraft world.

---

## Save:

* colony identity;
* workers;
* tasks;
* buildings;
* relationships.

---

## Do Not Save:

* block data already existing in Minecraft;
* complete inventories;
* entity copies.

---

# 13. Principle: Performance Before Complexity

## Decision

The mod must prioritize server performance.

---

## Rules

Avoid:

* full world scans;
* expensive tick operations;
* unnecessary entity searches.

Prefer:

* scheduled updates;
* cached information;
* event-driven systems.

---

# 14. Principle: Incremental Development

## Decision

Features are developed in controlled phases.

---

Required process:

```text
Documentation

↓

Implementation

↓

Testing

↓

Review

↓

Expansion
```

---

# 15. Rejected Alternatives

## Custom NPC System

Rejected.

Reason:

Minecraft already provides villagers.

---

## Complete Civilization Simulation

Rejected.

Reason:

Outside project scope.

---

## External Database

Rejected.

Reason:

The mod must work locally with zero maintenance.

---

## Player-Controlled Colony

Rejected.

Reason:

Conflicts with autonomous village philosophy.

---

# 16. Long-Term Impact

These decisions ensure Village Colony remains:

* modular;
* lightweight;
* compatible;
* maintainable;
* faithful to Minecraft.

---

# 17. Final Statement

All future systems must answer:

> "Does this make the vanilla village feel more alive without turning it into a different game?"

If the answer is no, the feature should not be added.
