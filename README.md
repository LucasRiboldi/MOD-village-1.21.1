# Village Colony

## A Living Vanilla Village Expansion Mod for Minecraft 1.21.1

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Language-Java-orange)
![Status](https://img.shields.io/badge/Development-MVP-yellow)

---

# 1. Overview

**Village Colony** is a Minecraft Fabric mod that transforms vanilla villages into autonomous living colonies.

The objective is not to replace Minecraft's village system, but to extend it.

Villagers keep their natural behavior and professions while gaining the ability to:

* organize themselves;
* manage resources;
* produce materials;
* construct new buildings;
* expand their settlement naturally.

The colony should continue operating without player intervention.

---

# 2. Project Vision

The core idea:

> "A vanilla village that learned how to grow by itself."

The player is not the manager of the colony.

The player does not assign jobs.

The villagers are responsible for the daily life of the settlement.

---

# 3. Design Philosophy

## Vanilla First

Minecraft Vanilla remains the foundation.

The mod uses existing:

* villagers;
* blocks;
* recipes;
* structures;
* inventories;
* world generation.

The mod adds organization, not replacement.

---

## No Artificial Simulation

The project does not create:

* virtual inventories;
* abstract resources;
* external databases;
* artificial needs;
* independent NPC entities.

Resources exist physically inside the Minecraft world.

Example:

```
Villager

↓

Personal Chest

↓

Stored Resources

↓

Construction
```

---

# 4. Core Gameplay Loop

The colony follows this cycle:

```
Village Exists

↓

Villagers Work

↓

Resources Accumulate

↓

Materials Are Produced

↓

Builder Creates Structures

↓

Village Expands

↓

New Villagers Continue The Cycle
```

---

# 5. Current Development Goal

The first milestone is the MVP.

The MVP creates:

* autonomous village detection;
* villager registration;
* worker professions;
* resource storage;
* basic production;
* construction of new vanilla-style houses.

---

# 6. MVP Features

## Colony System

The mod detects vanilla villages and creates a colony representation.

The colony stores:

* location;
* villagers;
* buildings;
* tasks;
* resources.

---

## Worker System

Villagers receive responsibilities based on colony needs.

Initial professions:

* Lumberjack;
* Manufacturer;
* Farmer;
* Builder.

---

## Storage System

Each worker owns a personal storage.

Rules:

* storage is inside the worker's house;
* storage is near the worker's bed;
* resources are physically stored;
* no global inventory exists.

---

## Resource System

The colony tracks available resources.

Initial resources:

* Oak Log;
* Oak Planks;
* Cobblestone.

Resources are collected, stored and transformed.

---

## Construction System

Builders expand the village.

Rules:

* original vanilla village blocks are protected;
* builders do not destroy generated structures;
* new blocks created by the colony are registered;
* expansion follows existing village roads.

---

# 7. Player Role

The player is not required.

The colony must:

* survive;
* produce;
* organize;
* expand;

without commands or supervision.

The player may observe, interact and assist, but the colony is autonomous.

---

# 8. Technology

## Minecraft

Version:

```
1.21.1
```

---

## Mod Loader

```
Fabric
```

---

## Language

```
Java
```

---

## Dependencies

Required:

* Fabric Loader
* Fabric API

No external services are required.

---

# 9. Installation

## Player Installation

Requirements:

* Minecraft Java Edition;
* Fabric Loader 1.21.1;
* Fabric API.

Installation:

1. Install Fabric Loader.
2. Place the Village Colony jar inside:

```
.minecraft/mods
```

3. Start Minecraft using Fabric.

---

# 10. Development Setup

Requirements:

* Java Development Kit compatible with Minecraft 1.21.1;
* Gradle;
* IntelliJ IDEA recommended.

Clone project:

```
git clone <repository>
```

Build:

```
./gradlew build
```

The generated jar will be located in:

```
build/libs/
```

---

# 11. Project Structure

```
Village Colony

├── core        colony logic, no Minecraft types
│
├── fabric      integration with Minecraft
│
├── data        persistence
│
└── docs        specs and decisions
```

The authoritative package layout is:

```
docs/decisions/ADR-006-Package-Layout.md
```

Packages are grouped by domain inside each layer, and named in the
singular.

---

# 12. Architecture Principles

The project follows:

```
Minecraft/Fabric

↓

Adapter Layer

↓

Service Layer

↓

Core Models
```

---

Rules:

* Models contain data.
* Services contain logic.
* Fabric adapters communicate with Minecraft.
* Core systems do not depend on Minecraft classes.

---

# 13. Documentation

The project documentation is divided into:

## Design

```
PROJECT_CONSTITUTION.md
MVP.md
Development-Roadmap.md
```

---

## Architecture

```
Architecture-Foundation.md
Data-Model.md
Class-Architecture.md
Simulation-Loop.md
```

---

## Systems

```
Profession-System.md
Resource-System.md
Storage-System.md
Construction-System.md
Save-Data-System.md
```

---

## Development Control

```
claude/

CLAUDE.md
DEVELOPMENT-RULES.md
IMPLEMENTATION-ORDER.md
CODE-STANDARDS.md
```

---

# 14. Development Status

Current status:

```
Documentation Phase
```

Completed:

✅ Project vision
✅ Architecture design
✅ Data models
✅ Development roadmap
✅ Claude Code instructions

Next phase:

```
Fabric Project Initialization
```

---

# 15. Roadmap

## Phase 1

Foundation:

* Fabric setup;
* project structure;
* core models.

---

## Phase 2

Colony:

* village detection;
* persistence.

---

## Phase 3

Workers:

* professions;
* assignments.

---

## Phase 4

Resources:

* storage;
* collection;
* production.

---

## Phase 5

Construction:

* blueprints;
* builders;
* expansion.

---

# 16. Performance Goals

Village Colony must:

* run locally;
* require no external server;
* avoid unnecessary world scanning;
* preserve Minecraft performance.

The mod should scale naturally from:

```
One village

↓

Multiple autonomous colonies
```

---

# 17. Contribution Rules

Before adding features:

1. Read the architecture documents.
2. Confirm compatibility with the project vision.
3. Avoid unnecessary complexity.
4. Keep systems modular.

---

# 18. Final Objective

Village Colony aims to create the feeling that:

```
Minecraft villages are not static.

They are living communities waiting to evolve.
```

---

# License

MIT License.

Ver:

```
LICENSE
```
