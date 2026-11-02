# Village work, supply and roads implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make new villages keep workers productive within their professions, build one functional large pen, supply smelting/mining needs, and use the approved road graph.

**Architecture:** Keep `Task` as the work request and add a narrow progress watchdog around it. Persist only daily demand-failure counters; all locations, inventories, animals and roads remain derived from the Minecraft world or existing stores. Reuse `SmelterFallback`, mine claims, `RoadGraph`, and existing GameTest fixtures instead of creating parallel planners.

**Tech Stack:** Java 21, Fabric 1.21.1, Fabric GameTest, JUnit, Gradle, Python 3 Sentinela Local.

**Spec:** `docs/superpowers/specs/2026-11-02-village-work-supply-and-roads-design.md`

## Global Constraints

- Fabric 1.21.1 and Java 21; dependencies remain pinned and no dependency is added.
- `core/` never imports Minecraft, Fabric or `data/`; persistence stays in `data/`.
- World state is authoritative. Persist only the daily retry key required across reload.
- New physical rules apply only to villages with the ADR-043 new-generation marker.
- Do not run JUnit, GameTest, Sentinela profiles or `gradlew build` while temporary no-test mode is active. Create each proof now; execute only after the author says `liberar testes`.
- Each corrected feature commit increments the patch version, updates documentation, and copies the built JAR to the local TLauncher `mods` directory after an approved build.
- Preserve untracked `.serena/` and `.superpowers/`.

## Review Focus

- A worker walks toward changing targets but produces nothing; Task 1 proves the 300-second budget still expires.
- Restarting on the same world day must retain a failed demand; Task 1 proves save/load keeps the count and next day clears it.
- A smelter with a real urgent job must not make reserve ingots; Task 3 proves urgent work wins.
- An autonomous miner must not damage stairs, portals, supports, corridors or ceilings; Task 4 proves all protected positions are rejected.
- A discovered preexisting village must not receive pen, miner home or graph paving; Tasks 6–8 prove the origin guard.

---

### Task 1: Demand progress watchdog and daily retry ledger

**Files:**
- Create: `src/main/java/com/villagecolony/core/coordination/DemandKey.java`
- Create: `src/main/java/com/villagecolony/core/coordination/DemandRetries.java`
- Create: `src/main/java/com/villagecolony/data/save/DemandRetrySave.java`
- Create: `src/main/java/com/villagecolony/fabric/work/TaskProgressWatch.java`
- Modify: `src/main/java/com/villagecolony/data/save/ColonySavedData.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/IdleWatch.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/WorkTime.java`
- Create: `src/gametest/java/com/villagecolony/fabric/work/TaskProgressWatchGameTest.java`

**Interfaces:**
- Produces `DemandKey(UUID colonyId, TaskType type, ResourceType resource)` and `DemandRetries.recordFailure(DemandKey, long day): int`.
- Produces `TaskProgressWatch.sample(ServerWorld, UUID, WorkTime.State)` and `TaskProgressWatch.progressed(ServerWorld, Task)`.
- Consumes existing `WorkerJobs.dropAll`, `SupplyException.WAITED_TOO_LONG`, and `TaskService.assignedTo`.

- [ ] **Step 1: Write failing GameTests**

Add tests for: walking without `progressed` expires at 300 seconds; a valid `progressed` resets the deadline; a second failed `DemandKey` deposits only the missing amount; save/load retains the counter during the same day; advancing one world day clears it.

- [ ] **Step 2: Verify the tests fail**

Run after `liberar testes`: `./gradlew.bat runGametest --tests TaskProgressWatchGameTest`.

Expected: old `IdleWatch` ignores `WALK`, and no daily persisted counter exists.

- [ ] **Step 3: Implement core key and persistence**

Implement immutable `DemandKey`. Make `DemandRetrySave` store only `day -> DemandKey -> failures`; discard prior-day entries on read/use. Expose it through `ColonySavedData` without adding Minecraft types to `core/`.

- [ ] **Step 4: Implement `TaskProgressWatch`**

Track last useful progress per held task in server memory. At 300 seconds, release the task, log target/reason, apply per-target recency, and increment its persisted `DemandKey`. On failure two, route the exact outstanding amount through the existing supply exception and serving chest. `IdleWatch` remains the stationary diagnostic, not the mobile deadline owner.

- [ ] **Step 5: Verify focused proof**

Run after release: `./gradlew.bat runGametest --tests TaskProgressWatchGameTest`.

Expected: all new cases pass and no material exceeds requested deficit.

- [ ] **Step 6: Commit**

Commit task code, test and minimum documentation with the next patch version: `P0.x: limitar tarefa sem progresso útil (TaskProgressWatchGameTest)`.

### Task 2: Operational task report and Sentinela proof manifest

**Files:**
- Create: `docs/testing/sentinela-local-coverage.md`
- Modify: `scripts/action_report.py`
- Modify: `scripts/test_lab.py`
- Test: `tests/test_action_report.py`
- Test: `tests/test_test_lab.py`
- Modify: `README.md`

**Interfaces:**
- Produces a versioned Markdown manifest with `feature`, `rule`, `test`, and `minimum profile` columns.
- Produces action-report aggregates for open task duration, last useful progress, release reason, stock and production.
- Consumes current action journal JSONL; it does not create a second world-state file.

- [ ] **Step 1: Write failing Python tests**

Add fixtures with a long-running walking task and a completed task. Assert the report distinguishes elapsed task time from time without progress. Assert the coverage manifest rejects a feature row without test and profile.

- [ ] **Step 2: Verify the tests fail**

Run after release: `python -m unittest tests.test_action_report tests.test_test_lab`.

Expected: current report lacks progress-age and manifest validation.

- [ ] **Step 3: Implement report fields and manifest validation**

Extend only current journal parsing and summary rendering. Add manifest entries for every test introduced in Tasks 1–8. Keep `test_lab.py` manual; add manifest validation to its report preparation, never automatic execution or CI scheduling.

- [ ] **Step 4: Verify focused proof**

Run after release: `python -m unittest tests.test_action_report tests.test_test_lab`.

Expected: report names stalled demand and coverage rows validate.

- [ ] **Step 5: Commit**

Commit report, manifest and tests: `P0.x: registrar provas de recursos e profissões no Sentinela`.

### Task 3: Smelter priority, planned-work intermediates and ingot reserve

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/SmelterWork.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/SmelterFallback.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/SmelterGathers.java`
- Test: `src/gametest/java/com/villagecolony/fabric/work/SmelterFallbackGameTest.java`
- Test: `src/gametest/java/com/villagecolony/fabric/work/SmelterGathersGameTest.java`

**Interfaces:**
- Extend `SmelterFallback.wanted(ServerWorld, Colony)` to return only smelted intermediates required by open and queued plans before reserve work.
- Add `SmelterFallback.reserveIngots(ServerWorld, Colony): List<Item>`; it returns minerally available ingots below 64.
- `SmelterWork.run(ServerWorld, Colony)` consumes existing task requests first, then wanted intermediates, then reserve ingots.

- [ ] **Step 1: Write failing GameTests**

Assert order: active smelt task, queued-build intermediate, then ingot reserve. Assert no non-ingot reserve product appears. Assert each available ore ingot stops at 64. Assert missing sand creates one bounded route and expires through Task 1 instead of reacquiring indefinitely.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests SmelterFallbackGameTest --tests SmelterGathersGameTest`.

Expected: current fallback only responds to misses and uses the generic crafted stock ceiling.

- [ ] **Step 3: Implement smallest dispatch change**

Reuse recipe discovery in `SmelterFallback`; do not add a recipe cache or new inventory. Limit reserve items to ingots with a physical available ore route and count stock in existing colony chests. Give open tasks strict precedence.

- [ ] **Step 4: Verify focused proof**

Run after release: same command as Step 2.

Expected: priority and 64 cap tests pass.

- [ ] **Step 5: Commit**

Commit code and proofs: `P0.x: ocupar fundidor com demanda e lingotes (SmelterFallbackGameTest)`.

### Task 4: Daily autonomous mining without structural damage

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/work/MineSupply.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MinerWork.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineDigging.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineCuts.java`
- Test: `src/gametest/java/com/villagecolony/gametest/MinerGameTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/MineLeaseGameTest.java`

**Interfaces:**
- Produces `MineSupply.next(ServerWorld, Mine, WorkerStorage, long day): Optional<MiningTarget>`.
- Consumes existing mine arms, claims, reach and `MineMouth.isPortalBlock`.
- `MinerWork` invokes it only when no construction/material task is available.

- [ ] **Step 1: Write failing GameTests**

Build a mine with planned front blocks plus protected stair, portal, support, corridor and ceiling blocks. Assert idle mining chooses a legal front in deterministic rotation, deposits at most 64 stone and each discovered ore per day, and never selects protected blocks. Assert day rollover permits the next quota.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests MinerGameTest --tests MineLeaseGameTest`.

Expected: current miner opens no autonomous task and has no daily quota.

- [ ] **Step 3: Implement `MineSupply`**

Use existing planned fronts and claims; choose from eligible targets by a persisted/deterministic cursor. Count deposits already made during the current day through current storage, not a duplicate inventory. Reject all structural coordinates before assignment. Do not alter mine shape or scan arbitrary terrain.

- [ ] **Step 4: Verify focused proof**

Run after release: same command as Step 2.

Expected: quota, rotation and structural protection pass.

- [ ] **Step 5: Commit**

Commit code and tests: `P0.x: manter mineiro produtivo sem cortar escadas (MinerGameTest)`.

### Task 5: Fixed professions and own-role secondary work

**Files:**
- Modify: `src/main/java/com/villagecolony/core/worker/model/Worker.java`
- Modify: `src/main/java/com/villagecolony/core/worker/service/ProfessionAssigner.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/WorkerStrikes.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/BuildHelper.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/ShepherdWork.java`
- Test: `src/gametest/java/com/villagecolony/fabric/work/BuildHelperGameTest.java`
- Test: `src/gametest/java/com/villagecolony/fabric/work/VillageHelpersGameTest.java`

**Interfaces:**
- `Worker.giveUpProfession()` no longer clears an assigned profession for operational failure.
- `ProfessionAssigner` assigns only workers with no profession.
- `BuildHelper.lendAHand` accepts only the builder path or is removed where no caller remains.

- [ ] **Step 1: Write failing GameTests**

Assert a failed miner/smelter/shepherd retains its profession. Assert carpenter, mason and shepherd never reserve `BUILD`. Assert an unassigned villager can still receive a missing profession.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests BuildHelperGameTest --tests VillageHelpersGameTest`.

Expected: helper and reassignment paths permit cross-role construction or profession loss.

- [ ] **Step 3: Remove cross-role paths narrowly**

Keep role-specific collection and recovery helpers. Delete only the construction lending calls and failure-based reassignment, preserving lifecycle reassignment for a worker that genuinely has no profession.

- [ ] **Step 4: Verify focused proof**

Run after release: same command as Step 2.

Expected: stable professions and own-role task selection pass.

- [ ] **Step 5: Commit**

Commit code and tests: `P0.x: fixar profissões e remover ajuda de obra indevida (VillageHelpersGameTest)`.

### Task 6: One large pen for each new village

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/PenPlans.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/ShepherdPens.java`
- Modify: `src/main/resources/data/villagecolony/structure/colony/animal_pen.nbt`
- Test: `src/gametest/java/com/villagecolony/fabric/work/PenModelGameTest.java`
- Test: `src/gametest/java/com/villagecolony/fabric/work/ShepherdPensGameTest.java`

**Interfaces:**
- `PenPlans.forColony` returns the large model only for a new-origin colony lacking a completed or open large-pen building.
- `ShepherdPens` keeps geometric recognition for small/legacy pens and returns capacity 20 for the registered large model.

- [ ] **Step 1: Write failing GameTests**

Assert one scheduled large pen despite multiple animal species; assert a queued pen blocks a duplicate; assert model interior has one safe water block, twelve planted farmland blocks, grass elsewhere, torches and no obstructing stone/dirt; assert capacity is 20; assert an old geometric small pen remains usable; assert preexisting-origin colony receives no model change.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests PenModelGameTest --tests ShepherdPensGameTest`.

Expected: current planner counts species and model has no water/crops.

- [ ] **Step 3: Implement model and scheduling guard**

Use existing building metadata to identify the large model. Do not infer it from arbitrary fences. Update the single NBT model and preserve small pen handling. Keep animal handling physical; no artificial POI or teleport.

- [ ] **Step 4: Verify focused proof**

Run after release: same command as Step 2.

Expected: composition, uniqueness, capacity and compatibility pass.

- [ ] **Step 5: Commit**

Commit NBT, code and tests: `P0.x: construir um curral grande funcional por vila nova (PenModelGameTest)`.

### Task 7: Miner home beside the new mine portal

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/ConstructionOrder.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/HousePlans.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineFurnishing.java`
- Test: `src/gametest/java/com/villagecolony/gametest/MinersRoomGameTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/VillageFoundationGameTest.java`

**Interfaces:**
- Add a mine-home plan selector that returns a smallest biome-compatible dwelling adjacent to the portal footprint.
- Consume `MineMouth` position and existing lot/structure protections; return empty rather than force a lot.

- [ ] **Step 1: Write failing GameTests**

Assert a new-origin colony with a portal schedules one small biome dwelling within the defined portal-adjacent lot candidates, exactly once. Assert occupied/protected candidates do not overwrite terrain and preexisting-origin colonies receive no new home.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests MinersRoomGameTest --tests VillageFoundationGameTest`.

Expected: no portal-adjacent mine-home scheduling exists.

- [ ] **Step 3: Implement with existing house catalog**

Reuse `HouseCatalog`, `HousePlans` ordering and site checks. Do not add a custom structure or a second home registry. Place the request after portal furnishing succeeds and before distant mining work.

- [ ] **Step 4: Verify focused proof**

Run after release: same command as Step 2.

Expected: one protected biome-compatible plan is selected.

- [ ] **Step 5: Commit**

Commit code and tests: `P0.x: planejar casa do mineiro junto ao pórtico (MinersRoomGameTest)`.

### Task 8: Activate the existing road graph for new villages

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/integration/RoadGraphMigration.java`
- Modify: `src/main/java/com/villagecolony/core/construction/model/VillagePathPlanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/PathPaving.java`
- Modify: `src/main/java/com/villagecolony/data/save/RoadGraphStore.java`
- Test: `src/gametest/java/com/villagecolony/fabric/integration/RoadGraphMigrationGameTest.java`
- Test: `src/gametest/java/com/villagecolony/fabric/integration/RoadNetworkGameTest.java`
- Test: `src/gametest/java/com/villagecolony/fabric/integration/StreetRouteGameTest.java`

**Interfaces:**
- `RoadGraphMigration` remains idempotent and non-physical for old villages.
- `VillagePathPlanner` consumes validated `RoadGraphRoutes` only when the ADR-043 origin marker is valid.
- `PathPaving` receives cardinal, support-checked main/branch segments and reports actual changed columns.

- [ ] **Step 1: Write failing GameTests**

Assert a new-origin village gets a 3-wide main and 1-wide branch through graph routes; routes are cardinal, have support and use door-lower-block support elevation; protected water, farms, mine and structures reject a path; a preexisting village migrates read-only and writes no new blocks.

- [ ] **Step 2: Verify the tests fail**

Run after release: `./gradlew.bat runGametest --tests RoadGraphMigrationGameTest --tests RoadNetworkGameTest --tests StreetRouteGameTest`.

Expected: graph persistence exists but planner/paving does not consume it.

- [ ] **Step 3: Integrate graph at the boundary**

Keep `roads.columns` intact. Validate graph before every physical plan, defer unavailable chunks, and fall back to no new route rather than legacy paving for a marked new village with invalid graph. Reuse ADR-043 origin check; never mark older villages as new.

- [ ] **Step 4: Verify focused proof and water-path reproduction**

Run after release: same command as Step 2, plus the existing minimal water navigation GameTest identified by current diagnosis.

Expected: graph cases pass and water navigation has an explained result before activating broad playtest.

- [ ] **Step 5: Commit**

Commit code, tests and ADR status update: `P0.x: ativar ruas por grafo em vilas novas (StreetRouteGameTest)`.

### Task 9: Documentation, release evidence and local JAR

**Files:**
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/RULES.md`
- Modify: `docs/archive/technical/Development-Log.md`
- Modify: `docs/decisions/ADR-043-hierarchical-road-graph.md`
- Modify: `docs/testing/sentinela-local-coverage.md`

**Interfaces:**
- Consumes each committed feature’s test name, rule and version.
- Produces a documentation state matching the final committed code and local JAR checksum/version record.

- [ ] **Step 1: Update live state after each feature**

Record completed rules, deferred playtest evidence and exact Sentinela coverage. Mark no test result as passed while temporary no-test mode remains active.

- [ ] **Step 2: Execute verification only after author releases tests**

Run focused suites per tasks, then `python scripts/test_lab.py fast`, `./gradlew.bat clean build`, `./gradlew.bat test`, and `./gradlew.bat runGametest` in that order. Record command, exit code and reports.

- [ ] **Step 3: Build and deploy JAR after approved verification**

Run `./gradlew.bat assemble`, verify the versioned JAR hash, copy it to `%APPDATA%\\.minecraft\\mods`, and verify size/hash after copy. Do not add the JAR to Git.

- [ ] **Step 4: Commit and hand off**

Commit each documentation update with its feature. After all tasks, push only from a clean branch/PR flow and report test status, JAR version/hash and remaining playtest needs.

## Self-review

- Spec coverage: Tasks 1–2 cover timeout, exception persistence, reporting and Sentinela; Tasks 3–5 cover professions, smelting and mining; Tasks 6–7 cover new-village structures; Task 8 covers graph activation; Task 9 covers documentation, JAR and later verification.
- Step scan: each task starts with a named failing proof, has a narrow code boundary, defines a command deferred by temporary no-test mode, and has its own commit.
- Type consistency: `DemandKey` is core-only; `DemandRetrySave` is data-only; Fabric workers consume them. The graph uses existing `RoadGraphRoutes`/`RoadGraphStore`.
- Review focus: all five listed input classes have an owning GameTest task.
- Proportion: this plan specifies interfaces, values and proof names; it does not prescribe method bodies or duplicate existing road-plan algorithms.
