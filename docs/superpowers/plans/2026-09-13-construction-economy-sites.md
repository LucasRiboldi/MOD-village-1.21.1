# Economy and Construction Site Implementation Plan

> **For agentic workers:** Execute each lot independently and stop for author review between lots. Tasks use checkbox syntax.

**Goal:** Make construction materials discoverable, prioritize structures the village needs, reject inaccessible lots, expose candidate diagnostics, and periodically reconsider eligible terrain.

**Architecture:** Keep Minecraft types at the Fabric boundary and use `ResourceId` in the Core for arbitrary registered material identities. Keep `ResourceType` and its proven production chains intact as a compatibility path. Construction selection remains server-owned and world-authoritative; scans are bounded, loaded-chunk-only, and never modify terrain.

**Tech Stack:** Java 21, Fabric, Minecraft 1.21.1, Yarn mappings, Gradle wrapper, JUnit, Fabric GameTests.

**Spec:** `docs/decisions/ADR-016-construction-economy-and-site-discovery.md`, extending accepted material policy in ADR-015 without changing the pending P0.7 terrain decision.

## Global Constraints

- `core/` must not import Minecraft, Fabric, `fabric.*`, or `data.*` packages.
- No inventory or terrain snapshots are persisted when the world already owns that truth.
- Never force-load chunks for resource reads, site scans, or diagnostics.
- Preserve player/structure protection, road-level rules, and the unresolved P0.7 natural-ground rule.
- A construction substitution is valid only when both demand accounting and block placement accept it.
- Bound per-tick scan and diagnostic work; do not change terrain or leave persistent markers.
- After each lot, run focused tests and `./gradlew.bat build`; run `./gradlew.bat runGametest` for Fabric/world integration changes. Report in-world TPS and sustained villager progress as pending unless observed in a development world.

---

### Lot 1: Dynamic materials and profession routing

**Files:**
- Modify: `src/main/java/com/villagecolony/core/resource/model/ResourceTally.java`
- Modify: `src/main/java/com/villagecolony/core/resource/model/ColonyResources.java`
- Modify: `src/main/java/com/villagecolony/fabric/adapter/MinecraftTypeAdapter.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ChestInventoryReader.java`
- Modify: `src/main/java/com/villagecolony/core/task/model/Task.java`
- Modify: `src/main/java/com/villagecolony/core/task/service/TaskService.java`
- Modify: `src/main/java/com/villagecolony/core/coordination/ColonyCycle.java`
- Modify: the Fabric material-demand adapter currently built in `VillageDetectionHandler`
- Test: `src/test/java/com/villagecolony/core/resource/model/ResourceTallyTest.java`
- Test: `src/test/java/com/villagecolony/core/coordination/ColonyCycleTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/StorageGameTest.java`

**Interfaces:**
- Add `ResourceTally.ofIds(Map<ResourceId, Integer>)`, `amountOf(ResourceId)`, and `idCounts()` while retaining all `ResourceType` APIs.
- Add `ColonyResources.amountOf(ResourceId)` and `locationsOf(ResourceId)`.
- Read every non-empty chest stack by its registered item ID, while retaining typed counts for the existing specialist work handlers.
- Route a generic request only when a registered, executable production chain identifies its capability; keep the approved smelter/breeder fallback as labor coverage, never as permission to invent a recipe or output.
- Keep task resource identity as `ResourceId` and preserve typed access for all existing `ResourceType` consumers.

- [x] Prove generic tally aggregation, immutable snapshots, zero omission, and chest-location lookup in unit tests.
- [ ] Prove an untyped blueprint demand is visible in a real registered chest GameTest and that a typed resource still follows its existing profession route. Chest counting is covered; task routing remains pending.
- [ ] Run the focused tests, then full `build` and `runGametest` before requesting review.

### Lot 2: Construction priorities

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/HousePlans.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/VillagerScanner.java` only if its existing population/bed observations are insufficient
- Test: `src/test/java/com/villagecolony/fabric/work/HousePlansTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/ConstructionResumeGameTest.java`

**Contract:** Select a needed house when observed adult residents exceed available village beds; otherwise prefer an absent profession-supporting structure from the available catalog, then other eligible structures. Never replace an open project. Use observed Vanilla POIs/world structures rather than persisted duplicate counts.

- [ ] Add failing tests for bed deficit, missing profession structure, fallback structure, and preservation of an open project.
- [ ] Run the focused tests red, implement stable priority ordering, then verify green and run `build` plus `runGametest`.

### Lot 3: Accessible site selection

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/ConstructionResumeGameTest.java`

**Contract:** Before a candidate can be returned/reserved, require a standable approach position that is reachable from the colony-side road using loaded-world navigation data; do not teleport, force chunks, flatten ground, or weaken existing lot protections. A rejected inaccessible candidate must allow the existing bounded search to continue.

- [ ] Add GameTests for an accessible road-side approach, an enclosed/inaccessible candidate, and bounded rejection without chunk loading.
- [ ] Run the red test first; implement the least invasive reachability check; run focused GameTests, `build`, and `runGametest`.

### Lot 4: In-game site diagnostic command

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/command/BuildSiteCommand.java`
- Modify: `src/main/java/com/villagecolony/VillageColonyMod.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`
- Test: a command-focused GameTest under `src/gametest/java/com/villagecolony/gametest/`

**Contract:** Register a permission-appropriate server command scoped to the player's current dimension and nearby colony. It emits a bounded, temporary particle preview for evaluated candidates and a text summary of accepted/rejected counts and refusal reasons. It is read-only, does not force chunks, and returns control without synchronous area-wide scans.

- [ ] Test command registration, bounded output, no-world-mutation behavior, and readable refusal summary.
- [ ] Implement the preview through server particle packets; run the command GameTest and `build` plus `runGametest`.

### Lot 5: Incremental periodic rescan

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/VillageDetectionHandler.java`
- Modify: `src/main/java/com/villagecolony/data/save/SweepCursorSave.java` only if current cursor lifecycle cannot express a periodic pass without persisting world-derived candidates
- Test: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`
- Test: focused unit tests under `src/test/java/com/villagecolony/fabric/integration/`

**Contract:** Advance at most a fixed per-tick column budget across active colonies, inspect loaded chunks only, restart a completed search after a cooldown, and let ADR-012 player edits invalidate nearby work immediately. Candidate validity is always rechecked against the live world before planning.

- [ ] Test cursor fairness across colonies, eventual coverage, cooldown reset, player invalidation, and unloaded-chunk exclusion.
- [ ] Run focused tests, `build`, and `runGametest`; record measured per-tick scan counts. In-world TPS and sustained construction progress remain a required author playtest.
