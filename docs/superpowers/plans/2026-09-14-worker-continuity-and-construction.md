# Worker Continuity and Construction Discovery Plan

> **For agentic workers:** Execute one batch at a time and stop for author review between batches. Each batch requires a red/green test cycle and its own documentation update.

**Goal:** Keep eligible miners productively cycling through work and make construction discovery diverse, adaptive, and self-revalidating after repeated failures.

**Architecture:** Preserve the existing Fabric/Core boundaries and world-as-source-of-truth rule. First amend ADR-012 and make player edits reconcile local road-index data without throwing away incremental scan progress. Then investigate miner and builder state machines independently; add bounded retry/revalidation policies in the owning subsystem rather than a cross-domain global scheduler.

**Tech Stack:** Fabric 1.21.1, Java 21, Gradle, JUnit, Fabric GameTests.

**Spec:** `docs/decisions/ADR-012-Player-World-Changes.md`, `docs/decisions/ADR-016-construction-economy-and-site-discovery.md`, `docs/PATTERNS.md`, `docs/RULES.md`.

## Global Constraints

- Core does not import Minecraft/Fabric/data packages; domain packages remain independent.
- The world remains the source of truth; do not force-load chunks or invent resources.
- Preserve the work schedule, resource demand/reserves, danger checks, and protected-structure rules.
- Keep per-tick scan/work budgets bounded; a failed attempt must not become a permanent conclusion.
- Each batch is independently testable and ends at an author review checkpoint; no commit/push or JAR replacement is implied.

---

### Batch 1: Reconcile player edits without restarting site discovery

**Problem:** ADR-012 currently clears every scanner cursor and index after each nearby player block edit. Repeated terrain edits can repeatedly discard incremental progress.

**Files:**
- Modify: `docs/decisions/ADR-012-Player-World-Changes.md`
- Modify: `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/PlayerWorldChangeHandler.java`
- Test: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`

**Behavior:** Keep the active sweep and its accumulated findings. Reconcile the changed road column in both the completed and in-progress index; add it again if the resulting block is paving. Reset the bounded road-query cursor so nearby lot candidates are checked again against the live world. Never report a cached lot decision: terrain/access checks continue to read current block states.

**Verification:** GameTest first builds an index, edits a relevant column, and proves the existing index remains available while a newly valid lot is discovered. Also retain coverage for adding/removing paving and resuming a paused sweep.

### Batch 2: Miner continuity under recoverable failures

**Status em 2026-09-15: parcialmente integrado.** A limpeza de claim de job
encerrado foi entregue com `MinerWorkLifecycleTest`; ela remove o bloqueio
invalido no mesmo tique, sem fingir que resolve as demais transicoes desta
matriz. Alvo inalcançavel, ramo bloqueado, veio exaurido, fluido e retomada
apos o prazo continuam pendentes de testes red/green e de revisao do autor.

**Problem:** Miners can abandon a target or branch without visibly selecting useful follow-up work.

**Files to inspect before editing:** `MinerWork.java`, `MineDigging.java`, `MineFrontier.java`, `MineReach.java`, `MineClaims.java`, `MineMarks.java`, miner GameTests, `WorkHours` and the task reservation lifecycle.

**Behavior target:** During work hours and while a real mineral deficit exists, every miner transitions among mining, selecting another reachable frontier, reopening/rerouting a blocked branch, hauling, or an explicit timed retry. Rejected targets release claims/tasks; escalating backoff is per target/branch and expires. No promise overrides empty demand, full storage, off-hours, danger, unloaded chunks, or absent physical resources.

**Verification:** Red/green tests for unreachable target, blocked branch, exhausted vein, fluid hazard, claim cleanup, and resumption after the retry window. GameTests verify physical block removal and stored drops.

### Batch 3: Construction diversity across available builders

**Problem:** Repeated planner passes may keep selecting the same structure or leave other eligible builders without distinct useful work.

**Files to inspect before editing:** `ConstructionPlanner.java`, `BuilderWork.java`, `HousePlans.java`, `FarmPlans.java`, project/task reservation models, structure-priority rules, and related tests.

**Behavior target:** Keep one authoritative project per colony unless existing decisions permit otherwise. Allocate different eligible structure goals across available builders/selection cycles using current village need plus a deterministic rotation or least-recently-attempted choice. Existing projects continue first; failed/ineligible structures must not starve houses or professional needs.

**Verification:** Unit/GameTests prove two builders do not repeatedly choose the same eligible blueprint, priorities remain respected, and a saved/open project resumes unchanged.

### Batch 4: Multiple bounded site-evaluation strategies

**Problem:** One strict lot predicate can reject all candidate areas even when a safe, accessible site could work.

**Files to inspect before editing:** `BuildSiteScanner.java`, `BuilderApproach.java`, `VillageRoad.java`, `BlockProtection.java`, ADR-016, Rules 3/6/16/19/22 and current refusal diagnostics.

**Behavior target:** Separate candidate evaluation methods with explicit outcomes and diagnostics: strict natural/flat lot, smaller eligible blueprint, and a bounded terrain-adjustable candidate only where existing author rules permit. All strategies must preserve road access, volume clearance, protected structures/player blocks, loaded-chunk-only access, and bounded work. Do not broaden rocky-ground acceptance or add automatic terraforming without a separate author decision if it changes Rules 3/19.

**Verification:** Per-strategy tests for accepted/rejected terrain, accessibility, protected blocks, blueprint fit, and budget. Compare refusal counts and cost before changing strategy order.

### Batch 5: Revalidation after repeated unsuccessful attempts

**Problem:** Repeated failures can be reported indefinitely without refreshing the assumptions that made a target/site fail.

**Files to inspect before editing:** miner marks/claims, planner refusal state, construction project recovery, scanner cursors, existing retry/backoff patterns, and `docs/PATTERNS.md`.

**Behavior target:** Add bounded attempt counters and escalating retry delays in each owning subsystem. At a threshold, re-read live world state, ownership/claims, accessible resources, route and site eligibility; clear only stale derived state, then try a different target/strategy. Logs state the actual attempt count, reason, and next retry time. No unbounded work per tick or permanent blacklist.

**Verification:** Red/green tests establish the exact failure threshold, backoff progression, successful reset, stale-state cleanup, and continued progress on unrelated work.
