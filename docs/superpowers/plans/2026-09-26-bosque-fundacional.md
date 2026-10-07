# Bosque Fundacional Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every new colony two safe mature trees and add one safe mature tree for every ten living adult villagers.

**Architecture:** `Colony` owns only the persisted population milestone; `VillageForest` owns all Minecraft-world selection and Vanilla tree generation. `VillageBiomes` supplies a biome-specific pair, `VillageAdoption` seeds once, and the active colony cycle retries population milestones without force-loading chunks.

**Tech Stack:** Java 21, Fabric 1.21.1, Vanilla `SaplingGenerator`, JUnit 5, Fabric GameTest.

**Spec:** `docs/superpowers/specs/2026-09-26-bosque-fundacional-design.md`

## Global Constraints

- Fabric 1.21.1 and Java 21; add no dependency and force no chunk load.
- `core/` must not import Minecraft, Fabric, `data/`, or Fabric integration.
- Generate only on natural ground in loaded chunks and never overwrite an occupied canopy, structure, chest, or persistent leaf.
- Persist only the completed population milestone; failed placements leave it unchanged for later retry.
- Update STATE.md, TODO.md and docs/archive/technical/Development-Log.md with actual verification evidence.
- Keep unrelated working-tree changes out of commits.

## Review Focus

- Save made before this feature: missing NBT must restore milestone zero; Task 1 covers it.
- A villa found again after reload: creation hook must not seed a second pair; Task 3 covers it by keeping the hook creation-only.
- Occupied or protected canopy: no placement may alter the existing block; Task 2 GameTest covers it.
- A population jump beyond one threshold: only one milestone can be served per active cycle; Task 3 covers the one-step API.
- Unloaded terrain: candidate enumeration must skip unloaded chunks; Task 2 reads chunks through the non-loading world-chunk lookup.

---

### Task 1: Persist the completed population milestone

**Files:**
- Modify: `src/main/java/com/villagecolony/core/colony/model/Colony.java`
- Modify: `src/main/java/com/villagecolony/data/save/ColonySavedData.java`
- Modify: `src/test/java/com/villagecolony/data/save/ColonySavedDataTest.java`

**Interfaces:**
- Produces: `int Colony.forestPopulationMilestone()` and `void Colony.markForestPopulationMilestone(int)`.
- Produces: NBT key `forestPopulationMilestone`, defaulting to zero when absent.

- [x] **Step 1: Write failing persistence tests**

Add tests which save a colony marked at 20, reload it and assert 20; and load NBT without the key and assert 0.

- [x] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew.bat test --tests com.villagecolony.data.save.ColonySavedDataTest`

Expected: FAIL because the milestone API/NBT field does not exist.

- [x] **Step 3: Implement the minimal core field and serialization**

Add a non-negative field and monotonic setter to `Colony`; write/read its integer beside `observedBeds`.

- [x] **Step 4: Run the focused test to verify it passes**

Run: `./gradlew.bat test --tests com.villagecolony.data.save.ColonySavedDataTest`

Expected: PASS.

### Task 2: Generate a safe mature tree in the village forest ring

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/VillageForest.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/VillageBiomes.java`
- Create: `src/gametest/java/com/villagecolony/gametest/VillageForestGameTest.java`

**Interfaces:**
- Consumes: `Colony` center and `TreeSpecies` pair from `VillageBiomes.forestSpeciesAt`.
- Produces: `VillageForest.seedInitial(ServerWorld, Colony)` and `VillageForest.plantForPopulation(ServerWorld, Colony, int)`.

- [x] **Step 1: Write failing GameTests**

Create one test that prepares a natural ring, calls `seedInitial`, and finds logs from two different forest species; create one that blocks every valid canopy and asserts the block survives and the method returns false.

- [x] **Step 2: Run GameTests to verify they fail**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: compilation/test failure because `VillageForest` does not exist.

- [x] **Step 3: Implement candidate scanning and Vanilla generation**

Use only loaded chunks; require a natural ground block, empty conservative canopy box and no known village/colony structure. Pass the selected `SaplingGenerator` an air restore state so a failed feature cannot leave a sapling. Pick positions with deterministic sector rotation and a spacing check.

- [x] **Step 4: Run GameTests to verify they pass**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: all registered GameTests pass.

### Task 3: Integrate creation and population retries

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/event/VillageAdoption.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/ColonyCycleRunner.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/VillagerScanner.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/VillageForestGameTest.java`

**Interfaces:**
- Consumes: `VillageForest.seedInitial`, `VillageForest.plantForPopulation`, `ScanResult.adultPopulation`.
- Produces: an initial seed only when `created`; one completed decade per active cycle.

- [x] **Step 1: Write the failing population-milestone GameTest**

Prepare a colony with milestone 10 and a safe ring, call the integration-facing population method for 20 adults, then assert exactly one new mature tree and milestone 20. Repeat with a blocked ring and assert it remains 10.

- [x] **Step 2: Run GameTests to verify the new test fails**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because no population integration advances the milestone.

- [x] **Step 3: Wire the two call sites**

Call seed only from `created` after village foundation. In the active cycle, obtain the scanner’s adult count, call the population method once, and record a concise success or pending log.

- [x] **Step 4: Run focused and full verification**

Run: `./gradlew.bat test --rerun-tasks` then `./gradlew.bat runGametest --rerun-tasks` then `./gradlew.bat build`

Expected: all commands pass; report GameTest count exactly.

### Task 4: Record the live state and implementation session

**Files:**
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `docs/archive/technical/Development-Log.md`

**Interfaces:**
- Consumes: actual final test output and playtest status.
- Produces: a concise backlog entry and explicit remaining save-playtest checklist.

- [x] **Step 1: Document factual evidence only**

Record source files, focused/unit/GameTest commands, exact final counts, and that a real save still needs to verify forest appearance and lumberjack routing.

- [x] **Step 2: Inspect the staged diff**

Run: `git diff --check` and `git diff --stat`

Expected: no whitespace errors and no unrelated log-statistics files staged.
