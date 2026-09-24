# Operational Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make colony work observable, bounded, and recoverable while preserving physical inventories and every player world change.

**Architecture:** Implement local contracts before shared state: assignment, partial construction, bounded scanning, and migration are independently testable. Mine recovery and activity tracing persist only mod-owned facts. The warehouse is a derived snapshot over the recognized physical chest set. `VillageInventory` observes only; `NEED_SCORE` remains out of this delivery.

**Tech Stack:** Java 21, Fabric 1.21.1, Yarn/Fabric GameTest, Gradle, JUnit 5, Minecraft NBT `PersistentState`, PowerShell.

**Spec:** `docs/superpowers/specs/2026-09-23-operational-reliability-design.md`

## Global Constraints

- Fabric 1.21.1 and Java 21; dependencies use fixed versions, never `latest`, `+`, or `SNAPSHOT`.
- `core/` cannot import Minecraft, Fabric, `data/`, or another core domain except `core/coordination`.
- The world is authoritative for blocks, inventories, beds, workers, and structures. Never create a resource, force-load a chunk, or read an unloaded chest as empty.
- Private player chests never enter colony supply.
- Do not add a Mixin or use `@Overwrite`.
- A player-removed portal, arch, lantern, stair, or mine branch is never reconstructed.
- Save migrations are one-time and idempotent; traces are bounded at exactly 16,384 events per colony.
- Every production change begins with a focused failing test. Every Fabric change adds a GameTest.
- Update `STATE.md`, `TODO.md`, and `docs/technical/Development-Log.md` with verified evidence only.

## Review Focus

- Resting workers cannot reserve through a hidden fallback pass. Covered by Task 1.
- An unsupported dependent block is skipped, not counted as placed, and not retried before its support changes. Covered by Task 2.
- A destroyed mine arch remains absent after technical recovery and reload. Covered by Task 6.
- An unloaded chest blocks supply resolution instead of appearing empty. Covered by Task 9.
- Trace overflow retains the newest 16,384 records and unknown serialized enums become `UNKNOWN`. Covered by Task 7.

---

## File Structure

| Path | Responsibility |
|---|---|
| `core/coordination/WorkEligibility.java` | One pure task-reservation rule. |
| `core/construction/model/ConstructionOutcome.java` | Placed, skipped, and blocked construction results. |
| `core/construction/service/MineRecovery.java` | Pure mine-plan recovery decisions, never world edits. |
| `core/construction/service/RemovalAudit.java` | Explicit authorization and evidence for construction-record removal. |
| `core/storage/model/{WarehouseIndex,SupplyRequest}.java` | Derived physical-stock snapshot, requests, and reservations. |
| `core/telemetry/model/{ActivityTrace,ActivityTraceEvent,ActivityKind,ActivityState,ControlledReason,TargetKind}.java` | Bounded per-colony trace buffer and forward-compatible event vocabularies. |
| `core/colony/model/VillageInventory.java` | Immutable, read-only village observation. |
| `fabric/integration/{ColonyScanScheduler,WarehouseObserver,VillageInventoryObserver}.java` | World adapters for core contracts. |
| `data/save/{ActivityTraceSave,SaveMigration}.java` | Schema-safe persistence and migrations. |
| `scripts/release_manifest.py` | Fails dry-run when release evidence or hashes differ. |

## Task 1: Unify Work Eligibility (Decision 1A)

**Files:**
- Create: `src/main/java/com/villagecolony/core/coordination/WorkEligibility.java`
- Modify: `src/main/java/com/villagecolony/core/coordination/WorkAssignment.java`
- Test: `src/test/java/com/villagecolony/core/coordination/WorkAssignmentTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/ColonyCycleGameTest.java`

**Interfaces:**
- Produces: `static boolean WorkEligibility.canReserve(Worker, Capability, Predicate<UUID>, Task)`.
- Consumes: `Worker.isResting`, `TaskType.needsOwnStorage`, and `Task.reserveFor`.

- [ ] **Step 1: Write the failing unit and GameTest.**

```java
@Test
void restingCapabilityIsNeverReserved() {
    worker.rest(Capability.MINE);
    tasks.open(colonyId, Capability.MINE);
    assertThat(WorkAssignment.assign(colonyId, workers, tasks)).isZero();
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void restingMinerDoesNotReserveMineTask(TestContext context) {
    // One rested miner, one mine task, one cycle: assert no reservation.
}
```

- [ ] **Step 2: Run the tests before code.**

Run: `./gradlew.bat test --tests com.villagecolony.core.coordination.WorkAssignmentTest.restingCapabilityIsNeverReserved`

Expected: FAIL because `WorkAssignment.takeOneTask` currently reserves resting capability in its second pass.

- [ ] **Step 3: Implement one reservation predicate and remove the fallback.**

```java
public static boolean canReserve(Worker worker, Capability capability,
        Predicate<UUID> hasStorage, Task task) {
    return !worker.isResting(capability)
            && (!task.type().needsOwnStorage() || hasStorage.test(worker.villagerId()));
}
```

Call this predicate from `reserveOne`; delete the second loop in `takeOneTask`.

- [ ] **Step 4: Verify unit and Fabric behavior.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.coordination.WorkAssignmentTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; the rested worker remains unassigned.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/coordination/WorkEligibility.java src/main/java/com/villagecolony/core/coordination/WorkAssignment.java src/test/java/com/villagecolony/core/coordination/WorkAssignmentTest.java src/gametest/java/com/villagecolony/gametest/ColonyCycleGameTest.java
git commit -m "P1.1: unificar elegibilidade de trabalho (WorkAssignmentTest)"
```

## Task 2: Make Unsupported Pieces Explicitly Partial (Decision 2B)

**Files:**
- Create: `src/main/java/com/villagecolony/core/construction/model/ConstructionOutcome.java`
- Modify: `src/main/java/com/villagecolony/core/construction/model/ConstructionProject.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/BuilderWork.java`
- Test: `src/test/java/com/villagecolony/core/construction/model/ConstructionOutcomeTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BuildProgressGameTest.java`

**Interfaces:**
- Produces: `ConstructionOutcome.placed()`, `.skipped(ColonyPos, SkipReason)`, and `.blocked(SkipReason)`.
- Consumes: existing construction project ids and Fabric support validation.

- [ ] **Step 1: Write failure tests.**

```java
@Test
void skippedPieceIsNotPlacedAndReleasesOnlyItsSlot() {
    ConstructionOutcome outcome = ConstructionOutcome.skipped(pos, SkipReason.UNSUPPORTED);
    assertThat(outcome.placedCount()).isZero();
    assertThat(outcome.releasesProjectSlot()).isTrue();
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void unsupportedTorchLeavesPartialProject(TestContext context) {
    // Remove the support, execute twice, assert no torch and no repeated slot reservation.
}
```

- [ ] **Step 2: Run red.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.construction.model.ConstructionOutcomeTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because the result type does not exist and the world path has no partial outcome.

- [ ] **Step 3: Implement result mapping.**

```java
public sealed interface ConstructionOutcome permits Placed, Skipped, Blocked {
    int placedCount();
    boolean releasesProjectSlot();
    Optional<SkipReason> reason();
}
```

`BuilderWork.placeOne` must validate physical support before placement, return one `Skipped(UNSUPPORTED)`, and leave that `BlueprintBlock` in `ConstructionProject.remaining()`. Persist a support fingerprint only for the deferred piece; retry it only when its support changes or when the blueprint changes. A skipped piece must never flow through `markPlaced`.

- [ ] **Step 4: Verify continuation.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.construction`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; supported pieces may continue while the unsupported piece remains observable.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/construction src/main/java/com/villagecolony/fabric/work/BuilderWork.java src/test/java/com/villagecolony/core/construction src/gametest/java/com/villagecolony/gametest/BuildProgressGameTest.java
git commit -m "P1.2: registrar peca de construcao parcial (ConstructionOutcomeTest)"
```

## Task 3: Separate Scanner Policy and Bound Cost (Decisions 5A, 6A)

**Files:**
- Create: `src/main/java/com/villagecolony/core/coordination/ScanRefusalReason.java`
- Create: `src/main/java/com/villagecolony/fabric/integration/ColonyScanScheduler.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/ServerLifecycleHandler.java`
- Test: `src/test/java/com/villagecolony/fabric/integration/ColonyScanSchedulerTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`

**Interfaces:**
- Produces: `ScanReport(UUID, int, Map<ScanRefusalReason,Integer>, boolean)` and `int ColonyScanScheduler.runCycle(Collection<UUID>)`.
- Consumes: existing resumable `BuildSiteScanner` sweep cursors.

- [ ] **Step 1: Write fairness and refusal tests.**

```java
@Test
void schedulerVisitsColoniesRoundRobinWithinBudget() {
    scheduler.withBudget(2).runCycle(List.of(a, b, c));
    assertThat(probe.visited()).containsExactly(a, b);
    scheduler.runCycle(List.of(a, b, c));
    assertThat(probe.visited()).endsWith(c, a);
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void bedAndRoadRefusalsAreIndependent(TestContext context) {
    // Create one bed-policy failure and one road-policy failure; assert distinct reason codes.
}
```

- [ ] **Step 2: Run red.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.integration.ColonyScanSchedulerTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because no budgeted scheduler or distinct reason report exists.

- [ ] **Step 3: Implement bounded scheduling.**

```java
public int runCycle(Collection<UUID> colonyIds) {
    return roundRobin.take(budget).mapToInt(scanner::scanOneSlice).sum();
}
```

Map lot, bed, road, and terrain decisions to distinct `ScanRefusalReason` values. Reuse the persisted sweep cursor; do not add terrain edits, forced chunks, or a global scan cache.

- [ ] **Step 4: Verify limits.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.integration.ColonyScanSchedulerTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; one cycle visits at most the configured number of colonies.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/coordination/ScanRefusalReason.java src/main/java/com/villagecolony/fabric/integration/ColonyScanScheduler.java src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java src/main/java/com/villagecolony/fabric/event/ServerLifecycleHandler.java src/test/java/com/villagecolony/fabric/integration/ColonyScanSchedulerTest.java src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java
git commit -m "P1.3: limitar scanner por colonia (ColonyScanSchedulerTest)"
```

## Task 4: Add Idempotent Save Migrations (Decision 8A)

**Files:**
- Create: `src/main/java/com/villagecolony/data/save/SaveMigration.java`
- Modify: `src/main/java/com/villagecolony/data/save/ColonySavedData.java`
- Modify: `src/main/java/com/villagecolony/data/save/MineSave.java`
- Test: `src/test/java/com/villagecolony/data/save/ColonySavedDataTest.java`
- Test: `src/test/java/com/villagecolony/data/save/MineSaveTest.java`
- Create: `src/gametest/java/com/villagecolony/gametest/BigHouseModBlueprintGameTest.java`

**Interfaces:**
- Produces: `NbtCompound SaveMigration.migrate(NbtCompound source)` with monotonic `saveVersion`.
- Consumes: existing colony, mine, construction, and BigHouse NBT shapes.

- [ ] **Step 1: Write legacy/current/double-load fixtures.**

```java
@Test
void migrationIsIdempotentForLegacyMineAndBedChestData() {
    NbtCompound once = SaveMigration.migrate(legacyFixture());
    assertThat(SaveMigration.migrate(once)).isEqualTo(once);
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void reopeningMigratedBigHouseDoesNotDuplicateChest(TestContext context) {
    // Save, reload through production reader, then assert one eligible chest.
}
```

- [ ] **Step 2: Run red.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.data.save.ColonySavedDataTest --tests com.villagecolony.data.save.MineSaveTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because the root migration version and ordered transforms do not exist.

- [ ] **Step 3: Apply ordered NBT transforms before deserialize.**

```java
public static NbtCompound migrate(NbtCompound source) {
    NbtCompound target = source.copy();
    for (int v = target.getInt("saveVersion", 0); v < CURRENT; v++) apply(v, target);
    target.putInt("saveVersion", CURRENT);
    return target;
}
```

Each transform may add missing mod-owned tags or normalize invalid data only. It must not place a block, synthesize a chest, or call Fabric code.

- [ ] **Step 4: Verify round trips.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.data.save`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; legacy and current fixtures round-trip, and a second migration changes nothing.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/data/save src/test/java/com/villagecolony/data/save src/gametest/java/com/villagecolony/gametest/BigHouseModBlueprintGameTest.java
git commit -m "P1.4: migrar saves de forma idempotente (ColonySavedDataTest)"
```

## Task 5: Define Pure Mine Recovery (Decision 3B)

**Files:**
- Create: `src/main/java/com/villagecolony/core/construction/service/MineRecovery.java`
- Modify: `src/main/java/com/villagecolony/core/construction/model/Mine.java`
- Modify: `src/main/java/com/villagecolony/core/construction/service/MineRegistry.java`
- Test: `src/test/java/com/villagecolony/core/construction/service/MineRecoveryTest.java`
- Test: `src/test/java/com/villagecolony/core/construction/model/MineTest.java`

**Interfaces:**
- Produces: `MineRecovery.Decision recover(Mine, MineRecovery.Signal)` where decisions are `RELEASE_STALE_CLAIM`, `CLEAR_CURSOR`, `SELECT_NEXT_ARM`, `EXHAUST`, and `NO_ACTION`.
- Consumes: `Mine.firstArmStillOpen`, `Mine.advanceIfEveryOpenArmIsDone`, `Mine.archRaised`, and `MineRegistry`.

- [ ] **Step 1: Write core recovery failures.**

```java
@Test
void recoveryNeverResetsArchOrCreatesAnotherShaft() {
    Mine mine = restoredMineWithRaisedArch();
    assertThat(MineRecovery.recover(mine, STALE_CLAIM)).isEqualTo(RELEASE_STALE_CLAIM);
    assertThat(mine.archRaised()).isTrue();
    assertThat(mine.shaft()).isSameAs(originalShaft);
}
```

```java
@Test
void sharedShaftOpensOneArmThenExactlyFourBranches() {
    assertThat(mine.branchesOpenNow()).isEqualTo(1);
    advanceToSharedBoundary(mine);
    assertThat(mine.branchesOpenNow()).isEqualTo(Mine.ARMS);
}
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.service.MineRecoveryTest --tests com.villagecolony.core.construction.model.MineTest`

Expected: FAIL because recovery has no pure API.

- [ ] **Step 3: Implement decision-only recovery.**

```java
public static Decision recover(Mine mine, Signal signal) {
    return switch (signal) {
        case STALE_CLAIM -> Decision.RELEASE_STALE_CLAIM;
        case IMPOSSIBLE_CURSOR -> mine.firstArmStillOpen().isPresent()
                ? Decision.CLEAR_CURSOR : Decision.EXHAUST;
        case NONE -> Decision.NO_ACTION;
    };
}
```

No `ServerWorld`, `BlockPos`, or `MineMouth` is permitted in this class. Preserve one shared stair, then four branches, and finite lower-level progression.

- [ ] **Step 4: Verify core contracts.**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.service.MineRecoveryTest --tests com.villagecolony.core.construction.model.MineTest`

Expected: PASS; recovery only changes mod-owned planning state.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/construction/model/Mine.java src/main/java/com/villagecolony/core/construction/service/MineRecovery.java src/main/java/com/villagecolony/core/construction/service/MineRegistry.java src/test/java/com/villagecolony/core/construction
git commit -m "P1.5: recuperar plano da mina sem reconstruir mundo (MineRecoveryTest)"
```

## Task 6: Integrate Mine Recovery Without Re-furnishing (Decision 3B)

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/MineDigging.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/MineMouth.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/OreVein.java`
- Test: `src/test/java/com/villagecolony/fabric/work/MinerRequestedResourceTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/MinerGameTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/MineSettlingGameTest.java`

**Interfaces:**
- Consumes: `MineRecovery.Decision`; only new-mine creation may call `MineMouth.furnish`.
- Produces: controlled trace reasons for stale claim, exhaustion, and invalid opposite mouth.

- [ ] **Step 1: Write mine world regressions.**

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void destroyedMineArchRemainsGoneAfterRecoveryAndReload(TestContext context) {
    // Open mine, remove arch, trigger stale recovery, reload, assert air remains.
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void exhaustedMineRequiresValidOppositeMouth(TestContext context) {
    // Exhaust mine, invalidate opposite mouth, assert no replacement and explicit reason.
}
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL while an existing mine can reach `furnishAndLight` or lacks controlled recovery output.

- [ ] **Step 3: Restrict furnishing to new valid mines.**

```java
if (registry.hasMine(colonyId)) {
    applyRecovery(MineRecovery.recover(mine, signal));
} else {
    openNewMine(world, colonyId, center).ifPresent(opened -> furnishAndLight(world, opened));
}
```

Keep `furnishAndLight` private to new-mine creation and valid opposite-side replacement. Do not infer a missing player block as a construction request. Include coal in explicit requested-vein priority without interrupting an active reachable vein.

- [ ] **Step 4: Verify mine behavior.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.work.MinerRequestedResourceTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; arch and portal remain absent, geometry stays finite, and coal wins eligible priority ties.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/fabric/work/MineDigging.java src/main/java/com/villagecolony/fabric/integration/MineMouth.java src/main/java/com/villagecolony/fabric/integration/OreVein.java src/test/java/com/villagecolony/fabric/work/MinerRequestedResourceTest.java src/gametest/java/com/villagecolony/gametest/MinerGameTest.java src/gametest/java/com/villagecolony/gametest/MineSettlingGameTest.java
git commit -m "P1.6: recuperar mina sem recriar portal (MinerGameTest)"
```

## Task 7: Persist a Bounded Activity Trace (Decision 7B)

**Files:**
- Create: `src/main/java/com/villagecolony/core/telemetry/model/ActivityTrace.java`
- Create: `src/main/java/com/villagecolony/core/telemetry/model/ActivityTraceEvent.java`
- Create: `src/main/java/com/villagecolony/core/telemetry/model/ActivityKind.java`
- Create: `src/main/java/com/villagecolony/core/telemetry/model/ActivityState.java`
- Create: `src/main/java/com/villagecolony/core/telemetry/model/ControlledReason.java`
- Create: `src/main/java/com/villagecolony/core/telemetry/model/TargetKind.java`
- Create: `src/main/java/com/villagecolony/data/save/ActivityTraceSave.java`
- Modify: `src/main/java/com/villagecolony/data/save/ColonySavedData.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/ActivityLog.java`
- Test: `src/test/java/com/villagecolony/core/telemetry/model/ActivityTraceTest.java`
- Test: `src/test/java/com/villagecolony/data/save/ColonySavedDataTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/ColonyCycleGameTest.java`

**Interfaces:**
- Produces: `append(ActivityTraceEvent)`, `newestFirst(int)`, and `overflowCount()`.
- Event fields: worker UUID, profession, `ActivityKind`, `ActivityState`, `ControlledReason`, `TargetKind`, and a progress counter. Each enum declares `UNKNOWN`; it stores no raw coordinates.

- [ ] **Step 1: Write retention and schema failures.**

```java
@Test
void traceKeepsNewest16384AndCountsOverflow() {
    IntStream.range(0, 16_385).forEach(index -> trace.append(event(index)));
    assertThat(trace.newestFirst(16_384)).hasSize(16_384);
    assertThat(trace.overflowCount()).isEqualTo(1);
}
```

```java
@Test
void unknownSavedValuesBecomeUnknown() {
    assertThat(ActivityTraceSave.read(eventWith("FUTURE", "FUTURE")).activity())
            .isEqualTo(ActivityKind.UNKNOWN);
}
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.core.telemetry.model.ActivityTraceTest --tests com.villagecolony.data.save.ColonySavedDataTest`

Expected: FAIL because the trace model and schema adapter do not exist.

- [ ] **Step 3: Implement in-memory ring buffer and normal-save serialization.**

```java
public void append(ActivityTraceEvent event) {
    if (events.size() == CAPACITY) { events.removeFirst(); overflowCount++; }
    events.addLast(event);
}
```

`ActivityLog` normalizes every simulated worker tick, including idle and waiting, into a trace event. `ActivityTraceSave` writes schema version `1` only as part of the normal `PersistentState` save.

- [ ] **Step 4: Verify persistence and tick coverage.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.telemetry.model.ActivityTraceTest --tests com.villagecolony.data.save.ColonySavedDataTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; no disk I/O occurs per tick and unknown values load as `UNKNOWN`.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/telemetry src/main/java/com/villagecolony/data/save/ActivityTraceSave.java src/main/java/com/villagecolony/data/save/ColonySavedData.java src/main/java/com/villagecolony/fabric/work/ActivityLog.java src/test/java/com/villagecolony/core/telemetry src/test/java/com/villagecolony/data/save/ColonySavedDataTest.java src/gametest/java/com/villagecolony/gametest/ColonyCycleGameTest.java
git commit -m "P1.7: persistir traco circular de atividade (ActivityTraceTest)"
```

## Task 8: Establish Physical Warehouse Contract (Decision 4B)

**Files:**
- Create: `docs/decisions/ADR-023-physical-warehouse-index.md`
- Create: `src/main/java/com/villagecolony/core/storage/model/WarehouseIndex.java`
- Create: `src/main/java/com/villagecolony/core/storage/model/SupplyRequest.java`
- Create: `src/main/java/com/villagecolony/core/storage/model/SupplyRequestStatus.java`
- Create: `src/main/java/com/villagecolony/core/storage/model/SupplyBlockReason.java`
- Test: `src/test/java/com/villagecolony/core/storage/model/WarehouseIndexTest.java`

**Interfaces:**
- Produces: `WarehouseIndex.reserve(SupplyRequest)`, `invalidate()`, and `SupplyRequest.withStatus(SupplyRequestStatus, SupplyBlockReason)`.
- Consumes: resource id/amount only. No Minecraft Item, chest, or world class enters `core`.

- [ ] **Step 1: Write ADR and reservation failure.**

```java
@Test
void reservationPreventsSameCycleDoubleConsumption() {
    WarehouseIndex index = WarehouseIndex.complete(Map.of(iron, 1));
    assertThat(index.reserve(request(iron, 1))).isEqualTo(RESERVED);
    assertThat(index.reserve(request(iron, 1))).isEqualTo(BLOCKED);
}
```

The ADR must define a derived per-cycle snapshot, public/worker chest boundary, no virtual stock, and ADR-022 as the sole final-piece exception.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.core.storage.model.WarehouseIndexTest`

Expected: FAIL because warehouse/request classes do not exist.

- [ ] **Step 3: Implement immutable stock and cycle-local reservation.**

```java
public Reservation reserve(SupplyRequest request) {
    if (!complete) return Reservation.blocked(SupplyBlockReason.SNAPSHOT_INCOMPLETE);
    int available = quantities.getOrDefault(request.resourceId(), 0) - reserved(request.resourceId());
    return available >= request.amount() ? reserveNow(request)
            : Reservation.blocked(SupplyBlockReason.INSUFFICIENT_PHYSICAL_STOCK);
}
```

Define `OPEN`, `RESERVED`, `FULFILLED`, `BLOCKED`; priority is active construction, active tool/profession work, capacity relief, then stock objective.

- [ ] **Step 4: Verify invalidation and priority.**

Run: `./gradlew.bat test --tests com.villagecolony.core.storage.model.WarehouseIndexTest`

Expected: PASS; invalidation clears reservations without changing stock and lower priority cannot consume scarce stock first.

- [ ] **Step 5: Commit.**

```powershell
git add docs/decisions/ADR-023-physical-warehouse-index.md src/main/java/com/villagecolony/core/storage/model src/test/java/com/villagecolony/core/storage/model/WarehouseIndexTest.java
git commit -m "P1.8: definir armazem fisico por snapshot (WarehouseIndexTest)"
```

## Task 9: Observe Chests and Route Requests Physically (Decision 4B)

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/WarehouseObserver.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ChestInventoryReader.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ColonyChests.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ColonySupply.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MinerHaul.java`
- Test: `src/test/java/com/villagecolony/fabric/integration/WarehouseObserverTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/StorageGameTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/ChestReliefGameTest.java`

**Interfaces:**
- Produces: `WarehouseIndex WarehouseObserver.snapshot(ServerWorld, UUID, ResourceGroup...)`.
- Consumes: `ColonyChests.nearestFirst`, `ChestInventoryReader.survey`, physical withdraw/deposit methods.

- [ ] **Step 1: Write unread-chest and no-capacity tests.**

```java
@Test
void unloadedChestMakesWarehouseIncompleteInsteadOfEmpty() {
    assertThat(observer.snapshot(world, colonyId).isComplete()).isFalse();
    assertThat(index.reserve(request)).isEqualTo(BLOCKED_SNAPSHOT_INCOMPLETE);
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void fullChestsPauseProducerWithoutOverflowDrop(TestContext context) {
    // Fill recognized public chests; assert NO_CAPACITY and no item entity.
}
```

- [ ] **Step 2: Run red.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.integration.WarehouseObserverTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because no observer maps partial survey to a blocked request.

- [ ] **Step 3: Build from the existing recognized chest set.**

```java
List<ColonyPos> recognized = ColonyChests.recognized(world, colonyId, colony.center());
ChestSurvey survey = ChestInventoryReader.survey(world, recognized, groups);
return WarehouseIndex.fromSurvey(survey.resources(), !survey.isPartial(), survey.freeSpaceByGroup());
```

Factor the recognized-chest discovery now embedded in `ColonyChests.nearestFirst` into `recognized`; `nearestFirst` then sorts that exact set. The observer adapts `ChestSurvey` to core resource values without importing Fabric into `core/`. Route sticks, apples, and saplings through physical requests. With no reachable capacity, return `BLOCKED(NO_CAPACITY)` and pause the producer. Never delete or automatically drop overflow; never scan private chests.

- [ ] **Step 4: Verify physical routes.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.integration.WarehouseObserverTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; all request fulfillment corresponds to physical chest mutation.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/fabric/integration/WarehouseObserver.java src/main/java/com/villagecolony/fabric/integration/ChestInventoryReader.java src/main/java/com/villagecolony/fabric/integration/ColonyChests.java src/main/java/com/villagecolony/fabric/integration/ColonySupply.java src/main/java/com/villagecolony/fabric/work/MinerHaul.java src/test/java/com/villagecolony/fabric/integration/WarehouseObserverTest.java src/gametest/java/com/villagecolony/gametest/StorageGameTest.java src/gametest/java/com/villagecolony/gametest/ChestReliefGameTest.java
git commit -m "P1.9: atender pedidos pelo armazem fisico (WarehouseObserverTest)"
```

## Task 10: Build Deterministic Profession Matrix (Decision 9A)

**Files:**
- Create: `src/gametest/java/com/villagecolony/gametest/OperationalMatrixGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/ShepherdGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/SmelterGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/ConstructionResumeGameTest.java`
- Modify: `docs/behavioral-tests/README.md`

**Interfaces:**
- Produces: fixed UUID/seed scenarios for shepherd, smelter, E4, E21, blocked work, and construction continuation.
- Consumes: public fixture helpers only; no random UUID or unbounded world search.

- [ ] **Step 1: Write each critical scenario.**

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void e21FullChestIsNoCapacityNotWorkStalled(TestContext context) {
    // Fixed UUID, full recognized chest, one cycle, assert controlled NO_CAPACITY.
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void constructionResumesAfterPhysicalMaterialArrives(TestContext context) {
    // Start WAITING_RESOURCES, deposit required item, cycle, assert next piece is placed.
}
```

- [ ] **Step 2: Run matrix cases before behavior edits.**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: any E4/E21 failure is deterministic and includes a controlled reason.

- [ ] **Step 3: Repair only a reproduced cause.**

```java
assertThat(trace.newestFirst(1).getFirst().reason()).isEqualTo(ControlledReason.NO_CAPACITY);
```

If a case is already green, record it as covered and leave production behavior unchanged. Do not use endurance to diagnose an unproven E4/E21 cause.

- [ ] **Step 4: Verify matrix.**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS with fixed identities and enough assertion context for reproduction.

- [ ] **Step 5: Commit.**

```powershell
git add src/gametest/java/com/villagecolony/gametest/OperationalMatrixGameTest.java src/gametest/java/com/villagecolony/gametest/ShepherdGameTest.java src/gametest/java/com/villagecolony/gametest/SmelterGameTest.java src/gametest/java/com/villagecolony/gametest/ConstructionResumeGameTest.java docs/behavioral-tests/README.md
git commit -m "P1.10: fixar matriz critica de profissoes (OperationalMatrixGameTest)"
```

## Task 11: Observe Village Inventory Without Growth Mutation (Decision 11A)

**Files:**
- Create: `src/main/java/com/villagecolony/core/colony/model/VillageInventory.java`
- Create: `src/main/java/com/villagecolony/fabric/integration/VillageInventoryObserver.java`
- Test: `src/test/java/com/villagecolony/core/colony/model/VillageInventoryTest.java`
- Test: `src/test/java/com/villagecolony/fabric/integration/VillageInventoryObserverTest.java`
- Test: `src/gametest/java/com/villagecolony/gametest/FarmPlanGameTest.java`

**Interfaces:**
- Produces: `VillageInventory(UUID, int adults, int beds, Map<ProfessionType,Integer>, int completed, int active, ChestCoverage, ResourceTally)`.
- Consumes: registries and `ChestInventoryReader.ChestSurvey`; it never calls `ConstructionPlanner.plan`.

- [ ] **Step 1: Write immutable observation tests.**

```java
@Test
void inventoryKeepsPartialChestCoverageSeparateFromStock() {
    VillageInventory inventory = observer.observe(world, colonyId);
    assertThat(inventory.chestCoverage().isPartial()).isTrue();
    assertThat(inventory.observedResources()).isEqualTo(readChestResources);
}
```

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
public void observingInventoryDoesNotChangeHouseAlternation(TestContext context) {
    // Observe twice around planning; assert same next blueprint and no new project.
}
```

- [ ] **Step 2: Run red.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.colony.model.VillageInventoryTest --tests com.villagecolony.fabric.integration.VillageInventoryObserverTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because value and observer classes do not exist.

- [ ] **Step 3: Implement read-only snapshot.**

```java
public VillageInventory observe(ServerWorld world, UUID colonyId) {
    Colony colony = colonies.require(colonyId);
    List<ColonyPos> recognized = ColonyChests.recognized(world, colonyId, colony.center());
    ChestSurvey survey = ChestInventoryReader.survey(world, recognized);
    return new VillageInventory(colonyId, adults(), beds(), professions(), completed(),
            active(), ChestCoverage.from(survey), survey.resources().total());
}
```

Do not serialize the snapshot and do not add `NEED_SCORE` or modify `HousePlans`/ `ConstructionPlanner`.

- [ ] **Step 4: Verify no planning side effect.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.colony.model.VillageInventoryTest --tests com.villagecolony.fabric.integration.VillageInventoryObserverTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; identical world state gives equal snapshots and unchanged alternation.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/core/colony/model/VillageInventory.java src/main/java/com/villagecolony/fabric/integration/VillageInventoryObserver.java src/test/java/com/villagecolony/core/colony/model/VillageInventoryTest.java src/test/java/com/villagecolony/fabric/integration/VillageInventoryObserverTest.java src/gametest/java/com/villagecolony/gametest/FarmPlanGameTest.java
git commit -m "P1.11: observar inventario da vila sem planejar (VillageInventoryTest)"
```

## Task 12: Add Seeded Endurance After the Matrix (Decision 9B)

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/work/EnduranceReport.java`
- Create: `src/main/java/com/villagecolony/fabric/work/LatencySummary.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/ColonyEnduranceGameTest.java`
- Create: `src/test/java/com/villagecolony/fabric/work/EnduranceReportTest.java`
- Modify: `scripts/analyze_village_log.py`
- Modify: `docs/behavioral-tests/README.md`

**Interfaces:**
- Produces: `EnduranceReport(long seed, long durationTicks, long cycles, int taskCount, LatencySummary, List<ActivityTraceEvent>)`.
- Consumes: deterministic matrix helpers and Task 7 trace model.

- [ ] **Step 1: Write report failure.**

```java
@Test
void enduranceFailureIncludesSeedAndTraceExcerpt() {
    EnduranceReport report = EnduranceReport.failed(17L, trace);
    assertThat(report.seed()).isEqualTo(17L);
    assertThat(report.failureTrace()).isNotEmpty();
}
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.work.EnduranceReportTest`

Expected: FAIL because the report type does not exist.

- [ ] **Step 3: Implement reporting, not alternate scheduling.**

```java
public static EnduranceReport failed(long seed, ActivityTrace trace) {
    return new EnduranceReport(seed, elapsedTicks, cycles, taskCount, latency,
            trace.newestFirst(64));
}
```

Default to a fixed seed and accept one named system property for a reviewed alternate seed. Failure output must retain seed, duration, cycle, task count, latency summary, and 64 trace events.

- [ ] **Step 4: Verify deterministic reruns.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.fabric.work.EnduranceReportTest`
- `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS normally; an injected failure prints complete reproduction evidence.

- [ ] **Step 5: Commit.**

```powershell
git add src/main/java/com/villagecolony/fabric/work/EnduranceReport.java src/gametest/java/com/villagecolony/gametest/ColonyEnduranceGameTest.java scripts/analyze_village_log.py docs/behavioral-tests/README.md
git commit -m "P1.12: relatar endurance reproduzivel (EnduranceReportTest)"
```

## Task 13: Audit Deletion and Automate Release Evidence (Decision 10A)

**Files:**
- Create: `scripts/release_manifest.py`
- Create: `src/main/java/com/villagecolony/core/construction/service/RemovalAudit.java`
- Modify: `src/main/java/com/villagecolony/core/construction/service/ConstructionService.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/ConstructionCancellation.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/WaitingWork.java`
- Test: `src/test/java/com/villagecolony/core/construction/service/ConstructionServiceTest.java`
- Modify: `STATE.md`, `TODO.md`, `docs/technical/Development-Log.md`, and `docs/proxima-sessao.md`

**Interfaces:**
- Produces: `release_manifest.py --dry-run --jar <path> --downloads <path> --mods <path>`, nonzero on mismatch.
- Consumes: commit SHA, JAR SHA-256, test status, save schema versions, and artifact destinations.

- [ ] **Step 1: Prove deletion and hash-mismatch failures.**

```java
@Test
void forgetRejectsActiveOrUnauditedProject() {
    assertThat(service.forget(activeProjectId, RemovalAudit.absent())).isFalse();
}
```

```powershell
python scripts/release_manifest.py --dry-run --jar build/libs/village-colony-0.3.0.jar --downloads downloads/village-colony-0.3.0.jar --mods "$env:APPDATA\.minecraft\mods\village-colony-0.3.0.jar"
```

Expected: red unit test for unsafe deletion and nonzero exit when any artifact hash differs.

- [ ] **Step 2: Run focused checks.**

Run, in order:
- `./gradlew.bat test --tests com.villagecolony.core.construction.service.ConstructionServiceTest`
- `python scripts/release_manifest.py --help`

Expected: test exposes unguarded deletion or a missing caller audit; script help lists all evidence arguments.

- [ ] **Step 3: Gate deletion and manifest mismatches.**

```java
public boolean forget(UUID projectId, RemovalAudit audit) {
    return projectId != null
            && audit.allows(find(projectId).orElse(null))
            && projects.remove(projectId) != null;
}
```

```python
if len(set(hashes.values())) != 1:
    raise SystemExit("release manifest failed: artifact hash mismatch")
```

Create the core `RemovalAudit` value with explicit reasons for player cancellation, patience abandonment, and completed-project purge. Update every current `forget` caller to construct the matching audit record before deletion, including `ConstructionCancellation` and `WaitingWork`; direct removal without an audit is rejected. The manifest includes commit id, hashes, unit/GameTest result, save versions, and destinations. It does not copy or publish an artifact.

- [ ] **Step 4: Verify release evidence.**

Run, in order:
- `./gradlew.bat build`
- `./gradlew.bat runGametest --rerun-tasks`
- `python scripts/release_manifest.py --dry-run --jar build/libs/village-colony-0.3.0.jar --downloads downloads/village-colony-0.3.0.jar --mods "$env:APPDATA\.minecraft\mods\village-colony-0.3.0.jar"`

Expected: full automated tests pass; dry-run fails until all three intentionally match, then prints exact evidence.

- [ ] **Step 5: Commit.**

```powershell
git add scripts/release_manifest.py src/main/java/com/villagecolony/core/construction/service/RemovalAudit.java src/main/java/com/villagecolony/core/construction/service/ConstructionService.java src/main/java/com/villagecolony/fabric/integration/ConstructionCancellation.java src/main/java/com/villagecolony/fabric/work/WaitingWork.java src/test/java/com/villagecolony/core/construction/service/ConstructionServiceTest.java STATE.md TODO.md docs/technical/Development-Log.md docs/proxima-sessao.md
git commit -m "P1.13: auditar release e exclusao de obra (ConstructionServiceTest)"
```

## Task 14: Full Verification, Save Playtests, and Release Handoff

**Files:**
- Modify: `STATE.md`, `TODO.md`, `docs/technical/Development-Log.md`, and `docs/proxima-sessao.md`
- Modify: `downloads/village-colony-0.3.0.jar` only after every automated check is green

**Interfaces:**
- Consumes: all prior test evidence and `release_manifest.py`.
- Produces: one actual release hash and a separate confirmed/pending save-playtest list.

- [ ] **Step 1: Run all automated verification sequentially.**

Run, in order:
- `./gradlew.bat test`
- `./gradlew.bat build`
- `./gradlew.bat runGametest --rerun-tasks`
- `python scripts/analyze_village_log.py (Join-Path $env:APPDATA '.minecraft\logs\latest.log')`

Expected: record actual counts and failures. Do not infer save-playtest success from automated commands.

- [ ] **Step 2: Perform required save playtests with Minecraft closed during JAR installation.**

```text
1. Destroy mine arch/portal, trigger technical recovery, reload: it remains absent.
2. Exhaust finite mine: only a valid opposite mouth opens; coal is preferred when eligible.
3. Fill public chests: producer reports NO_CAPACITY with no item loss; free capacity resumes physical work.
4. Reopen a migrated BigHouse world twice: no duplicate chest or structure.
5. Exercise house/infrastructure alternation and inspect idle, waiting, progress, and controlled-error trace events.
```

- [ ] **Step 3: Copy the verified JAR and compare three hashes.**

```powershell
Copy-Item -LiteralPath build\libs\village-colony-0.3.0.jar -Destination downloads\village-colony-0.3.0.jar -Force
Copy-Item -LiteralPath build\libs\village-colony-0.3.0.jar -Destination (Join-Path $env:APPDATA '.minecraft\mods\village-colony-0.3.0.jar') -Force
Get-FileHash build\libs\village-colony-0.3.0.jar, downloads\village-colony-0.3.0.jar, (Join-Path $env:APPDATA '.minecraft\mods\village-colony-0.3.0.jar') -Algorithm SHA256
```

- [ ] **Step 4: Run final manifest.**

Run: `python scripts/release_manifest.py --dry-run --jar build/libs/village-colony-0.3.0.jar --downloads downloads/village-colony-0.3.0.jar --mods "$env:APPDATA\.minecraft\mods\village-colony-0.3.0.jar"`

Expected: zero exit and manifest values matching final commit and artifacts.

- [ ] **Step 5: Record verified facts, publish, and report evidence boundaries.**

```powershell
git add STATE.md TODO.md docs/technical/Development-Log.md docs/proxima-sessao.md downloads/village-colony-0.3.0.jar
git commit -m "P1.14: publicar confiabilidade operacional (build e runGametest)"
git push
```

Only close a save playtest after observed user confirmation. Automated results and in-game evidence must remain separate.

## Self-Review

### Spec coverage

| Approved decision | Task |
|---|---:|
| 1A unified eligibility | 1 |
| 2B partial construction | 2 |
| 3B technical mine recovery, finite geometry, coal, no player reconstruction | 5, 6 |
| 4B physical warehouse/capacity | 8, 9 |
| 5A policy reasons | 3 |
| 6A scan budget/round-robin | 3 |
| 7B persisted per-tick trace | 7 |
| 8A idempotent migration | 4 |
| 9A matrix then 9B endurance | 10, 12 |
| 10A release/deletion audit | 13, 14 |
| 11A VillageInventory before NEED_SCORE | 11 |

No approved requirement is uncovered. `NEED_SCORE` is intentionally deferred by the approved specification.

### Placeholder Scan

No placeholder markers or generic test instructions remain. Every task includes exact files, interfaces, a failing test, verification, and a commit.

### Type Consistency

`ActivityTrace` and `ActivityTraceEvent` precede their save/endurance consumers. `WarehouseIndex` and `SupplyRequest` precede `WarehouseObserver`. `MineRecovery` is core-only and is consumed by `MineDigging` after pure tests. `VillageInventory` has no planner entry point.

### Execution Order

Tasks 1-4 establish local contracts and migrations. Tasks 5-7 add mine recovery and persistent operational evidence. Tasks 8-11 add physical logistics, deterministic coverage, and observation. Task 12 depends on the matrix and trace. Tasks 13-14 gate release on complete evidence.
