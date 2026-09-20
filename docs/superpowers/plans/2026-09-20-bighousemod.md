# BigHouseMOD Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with verification at each checkpoint.

**Goal:** Add an idempotent mod-owned `BigHouseMOD` foundation house to every adopted village without changing Vanilla structures.

**Architecture:** Copy the Vanilla plains big-house template into the mod namespace, clean its furniture/decorations, and add a fixed interior layout for eight beds and eight chests. A Fabric integration service will place the complete house only on a safe, empty footprint, register it as colony infrastructure, then bind the existing foundation workers to its beds and chests.

**Tech Stack:** Fabric 1.21.1, Java 21, Minecraft structure NBT, existing `StructureBlueprintReader`, `BuildingRegistry`, `WorkerService`, Fabric GameTests.

**Spec:** `docs/superpowers/specs/2026-09-20-bighousemod-design.md`

## Global Constraints

- Vanilla structure files remain unchanged.
- No existing player or Vanilla blocks may be overwritten.
- The world remains the source of truth for blocks, beds and chests.
- Repeated detection must not create a second `BigHouseMOD`.
- Fabric changes require GameTests.

---

### Task 1: Blueprint asset and reader contract

**Files:**
- Create: `src/main/resources/data/villagecolony/structure/houses/big_house_mod.nbt`
- Modify: `src/main/java/com/villagecolony/fabric/integration/StructureBlueprintReader.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BigHouseModBlueprintGameTest.java`

- [ ] Copy the Vanilla plains big-house template into the mod namespace and
  remove its furniture/decorative blocks while preserving shell, floor,
  walls, roof, entrance and windows.
- [ ] Add the `BIG_HOUSE_MOD` resource id and a reader path that exposes the
  cleaned blueprint without changing Vanilla ids.
- [ ] Add a failing GameTest for dimensions, eight beds, eight chests, a clear
  entrance and a clear central corridor.
- [ ] Run the focused GameTest and confirm the red phase before implementation.
- [ ] Implement the minimal reader/blueprint augmentation and rerun it green.

### Task 2: Safe placement and persistence

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/BigHouseFoundation.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/VillageDetectionHandler.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BigHouseFoundationGameTest.java`

- [ ] Add a failing GameTest with one protected block beside a candidate lot.
- [ ] Find a full-footprint lot using existing scanner/protection rules.
- [ ] Place the complete `BigHouseMOD` atomically only when every target block
  is replaceable or part of the approved terrain preparation.
- [ ] Register the resulting bounds with `BuildingRegistry` using the mod
  blueprint id and skip placement when a registered house already exists.
- [ ] Invoke the service during village adoption before foundation villagers
  are assigned, and retry safely on later detection.
- [ ] Rerun the focused GameTest and verify no overlap or duplicate house.

### Task 3: Bind foundation villagers to the house

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/integration/VillageFoundation.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ChestPlacer.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/VillagerScanner.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BigHouseFoundationGameTest.java`

- [ ] Allocate the eight interior bed positions deterministically, one per
  foundation role, and place exactly eight white beds.
- [ ] Place exactly eight single chests without adjacent chest merging and
  register each through the existing storage scanner.
- [ ] Spawn or relocate the eight adult foundation villagers inside the house,
  write unique `HOME` memories, and preserve the clear corridor/door.
- [ ] Assert every role has one live villager, one bed and one chest and that
  no ninth foundation object is added on a second pass.

### Task 4: Documentation, release and verification

**Files:**
- Modify: `STATE.md`, `TODO.md`, `docs/proxima-sessao.md`, `docs/technical/Development-Log.md`
- Modify: `docs/decisions/ADR-018-village-foundation-contract.md`
- Update: `downloads/village-colony-0.3.0.jar`

- [ ] Document the new `BigHouseMOD` invariant and the remaining live
  playtest requirement.
- [ ] Run `./gradlew.bat build`, `./gradlew.bat test`, and
  `./gradlew.bat clean runGametest`.
- [ ] Copy the rebuilt JAR to `downloads/` and `%APPDATA%/.minecraft/mods/`.
- [ ] Compare SHA-256 hashes across all three JAR copies.
- [ ] Run `git diff --check`, commit with the project P0 format, and push.
