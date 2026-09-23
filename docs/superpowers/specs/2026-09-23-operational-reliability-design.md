# Operational Reliability Delivery Design

**Date:** 2026-09-23
**Status:** Approved design awaiting written-spec review
**Scope:** Decisions 1A, 2B, 3B, 4B, 5A, 6A, 7B, 8A, 9A then 9B, 10A, and 11A.

## Goal

Make colony activity observable and recoverable without creating virtual
resources, modifying player changes, duplicating structures, or allowing a
single blocked workflow to stall growth.

The world remains the source of truth for blocks, inventories, workers, beds,
and structures. Persisted data records only mod-owned identity, migration
version, bounded diagnostics, and project state that cannot be reconstructed
from the world.

## Non-negotiable invariants

1. A capability in rest cannot receive a task in any assignment pass.
2. An impossible dependent block is skipped with an explicit partial result;
   it is never counted as placed and is not retried until the blueprint or its
   supporting world changes.
3. A portal, arch, lantern, stair, or branch removed by the player is never
   recreated by mine recovery.
4. A new mine mouth can be furnished only while opening a newly registered
   mine. Replacing an exhausted mine requires a valid mouth on the opposite
   side of the colony.
5. No request, inventory index, or diagnostic record may create a resource,
   force-load a chunk, or treat an unread chest as empty.
6. Player-private chests remain excluded from colony supply.
7. Save migration is one-time and idempotent. Reopening a migrated world must
   not duplicate a chest, construction, or historical telemetry.
8. Core classes do not import Minecraft or Fabric types.

## Delivery order

### Phase 1: Operational contracts

Implement the local behavior and deterministic evidence first.

- **1A:** use one task eligibility rule in both `WorkAssignment` passes. A
  worker resting a capability is ineligible for that capability throughout the
  cycle.
- **2B:** introduce a partial construction result for a piece that cannot be
  physically supported. The project releases the construction slot and keeps a
  reasoned skipped-piece record. It is reconsidered only after a matching
  world or blueprint change.
- **5A:** keep lot, bed, road, and terrain policies independent. Scanner
  refusals expose controlled reason codes and counters.
- **6A:** schedule colony scans with a per-colony budget and deterministic
  round-robin order. Unit tests prove fairness and bounded work, while save
  playtests report actual latency.
- **8A:** add idempotent migration fixtures for prior and current save shapes.
- **9A:** add a small deterministic GameTest matrix for shepherd, smelter,
  E4, E21, blocked work, and construction continuation.
- **10A:** add a release-manifest dry run and audit `ConstructionService.forget`
  before any deletion.

### Phase 2: Technical mine recovery and persistent trace

Implement the selected mine recovery only as a recovery of mod-owned planning
state.

- `MineRecovery` can release stale claims, clear an impossible plan cursor,
  select another valid arm, and end or replace a mine at its finite boundary.
- It has no block placement dependency and cannot call `MineMouth.furnish` for
  an already known mine. `Mine.archRaised` remains the durable proof that an
  earlier arch must not be reconsidered.
- The finite geometry remains one shared stair and shared area before four
  branches, then a lower level. Vein following retains priority over geometric
  ordering, with coal included in the requested-vein priority policy.
- `ActivityTrace` is a bounded, append-only ring buffer per colony. It records
  every simulated worker tick, including idle and waiting states, as worker id,
  profession, activity, state, controlled reason, logical target kind, and
  progress counter. It stores no raw player coordinates.
- The initial bound is 16,384 events per colony. When full, the oldest event is
  removed and an overflow counter is retained. Serialization occurs during the
  normal world save, never as disk I/O on each tick.
- Trace schema version 1 is forward-safe: unknown reason or activity values are
  preserved as `UNKNOWN` rather than failing world load.

### Phase 3: Physical global warehouse

Implement a global view of existing public and worker chests without adding a
virtual inventory.

- `WarehouseIndex` is a per-cycle snapshot built from the same recognized
  chest set used by `ColonyChests` and `ChestInventoryReader`.
- `SupplyRequest` is derived from an active work or construction need and has
  resource id, amount, consumer, priority, status, and controlled block reason.
  It is not a saved source of truth for stock.
- Request statuses are `OPEN`, `RESERVED`, `FULFILLED`, and `BLOCKED`.
  Reservations prevent same-cycle double consumption and expire when the
  cycle's world snapshot is invalidated.
- Priority is active construction, active tool or profession work, capacity
  relief for produced residue, then stock objectives.
- Sticks, apples, and saplings are routed as physical resources. With no
  reachable capacity, the producer pauses with `NO_CAPACITY`; no item is
  destroyed or dropped as an automatic overflow.
- ADR-022 remains the sole construction exception: when no local physical or
  recipe route exists, one final construction piece may be supplied to the
  builder-serving chest if that chest has space.

### Phase 4: Inventory observation, endurance, and release evidence

Deliver observation before changing construction choice.

- `VillageInventory` is an immutable core snapshot of adults, beds,
  professions, completed structures, active projects, chest coverage, and
  observed resources.
- A Fabric observer builds this snapshot from the world and existing registries
  without persisting a second copy of Vanilla state.
- `NEED_SCORE` is explicitly deferred. It may consume `VillageInventory` only
  after the house/infrastructure alternation and activity-trace playtests pass.
- The generic endurance suite runs only after the deterministic matrix. It
  records a reproducible seed, duration, cycle, task count, latency summary,
  and trace excerpt on failure.
- The release manifest contains commit id, JAR SHA-256, test outcomes, save
  shape versions, and the expected artifact destinations. A mismatched hash
  fails the dry run and blocks release publication.

## Ownership and package boundaries

- `core.coordination` owns eligibility and scheduling contracts.
- `core.construction` owns mine geometry, recovery decisions, construction
  outcomes, and `VillageInventory` value objects.
- `core.storage` owns request and reservation models, never Minecraft chest
  access.
- `data.save` owns only serialization and migration of mod-owned values.
- `fabric.integration` reads/writes physical chests and world blocks.
- `fabric.work` maps core outcomes to worker activity and telemetry events.

## Error handling

All operational failures use controlled reasons. A missing chunk, absent chest,
full chest, stale claim, unsupported block, invalid blueprint, or exhausted
mine is observable but cannot be silently converted into completion. Recovery
is bounded by the relevant cycle and has no world-edit side effect except
normal work that is independently eligible and physically possible.

## Verification and acceptance

Every behavior change follows red-green-refactor: a focused test must fail for
the intended missing behavior before production code is added. Fabric changes
also require a GameTest. Full verification before release is:

1. `./gradlew.bat build`
2. focused unit tests for each changed core contract
3. `./gradlew.bat runGametest`
4. release-manifest dry run and SHA-256 comparison
5. save playtests for mine recovery, chest routing, BigHouse migration,
   house/infrastructure alternation, and activity traces

Automated tests prove contracts and migrations. They do not replace save
playtests or real-world latency measurement.
