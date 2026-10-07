# Water Mine Access and Wide Shafts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give water-founded colonies one glass-sealed, unmineable `3 x 3` access stair to a real underground mine, and widen every normal mine stair to the same internal dimensions.

**Architecture:** The ordinary dry mine remains authoritative. When that mine exhausts every level, a Fabric integration component first qualifies a loaded, water-founded route; on refusal, the existing opposite dry-mouth recovery remains. The world itself contains the completed access. `MineShaft` changes to three lanes, and `MineSave` invalidates old cut cursors because their coordinate order changes.

**Tech Stack:** Java 21, Fabric 1.21.1, JUnit 5, Fabric GameTest, Minecraft server-world block APIs.

**Spec:** `docs/superpowers/specs/2026-09-27-water-mine-access-design.md`

## Global Constraints

- Java 21 and fixed Fabric/Minecraft 1.21.1 dependencies only.
- `core/` does not import Minecraft, Fabric or data packages.
- Never force-load a chunk; every examined or modified candidate chunk must already be loaded.
- Never modify village-original blocks, colony-built blocks, open projects or player-protected leaves.
- Normal dry mine selection is unchanged; water access is considered only when
  that mine later exhausts every level.
- Water access is exclusive to water-founded villages with a validated 64-block natural stone exit.
- Both normal and water-access stair corridors are three blocks wide and three blocks high.
- Mine cursors from an older geometry are reset, not translated; the arch state remains preserved.
- A miner must never target, break, reroute through or dig the access stairs, glass shell, floor, walls or ceiling.

## Review Focus

- A land village beside one pond must not qualify as water-founded; cover in Task 3 GameTest.
- A water route crossing an unloaded chunk must make no world change; cover in Task 3 GameTest.
- A 64-block region containing sand, player structures or village blocks must not count as a stone exit; cover in Task 2 unit test and Task 3 GameTest.
- A pre-existing mine saved with shape version 6 must preserve entrance orientation and arch while resetting cuts; cover in Task 1 `MineSaveTest`.
- A miner recovering, rerouting or detouring beside the lower exit must not return a glass or stair coordinate; cover in Task 4 GameTest.

---

## File Map

- Modify: `src/main/java/com/villagecolony/core/construction/model/MineShaft.java` -- exposes three lanes in the shared and branch stair coordinate order.
- Modify: `src/main/java/com/villagecolony/data/save/MineSave.java` -- increments shape version and restores old cursors empty.
- Modify: `src/test/java/com/villagecolony/core/construction/model/MineShaftTest.java` -- locks the three-lane coordinate and count contract.
- Modify: `src/test/java/com/villagecolony/data/save/MineSaveTest.java` -- locks the shape-6 migration behavior.
- Create: `src/main/java/com/villagecolony/fabric/work/WaterMineAccess.java` -- qualifies loaded water-founded routes, describes the protected `3 x 3` corridor and places it once.
- Modify: `src/main/java/com/villagecolony/fabric/work/MineDigging.java` -- asks water access only after ordinary mouth search fails, then opens the existing Mine at its exit.
- Modify: `src/main/java/com/villagecolony/fabric/work/MineRock.java` and `src/main/java/com/villagecolony/fabric/work/MineFrontier.java` -- apply the shared no-access-mining boundary to target validation and frontier recovery.
- Modify: `src/gametest/java/com/villagecolony/gametest/MinerGameTest.java` -- covers the live normal-mine width and miner target behavior.
- Create: `src/gametest/java/com/villagecolony/gametest/WaterMineAccessGameTest.java` -- builds controlled water fixtures and proves eligibility, sealing and refusal cases.
- Create: `docs/decisions/ADR-026-water-mine-access.md` -- records the water-only exception and save/world-truth decision.
- Modify: `STATE.md`, `TODO.md`, `docs/archive/technical/Development-Log.md` -- record implementation and the remaining author playtest.

### Task 1: Widen the normal MineShaft and migrate saved cursors

**Files:**
- Modify: `src/test/java/com/villagecolony/core/construction/model/MineShaftTest.java`
- Modify: `src/test/java/com/villagecolony/data/save/MineSaveTest.java`
- Modify: `src/main/java/com/villagecolony/core/construction/model/MineShaft.java`
- Modify: `src/main/java/com/villagecolony/data/save/MineSave.java`

**Interfaces:**
- Produces: `MineShaft.STAIR_LANES == 3`, `STAIR_STEP_BLOCKS == 9`, and a shape-7 save reader that resets only geometry cursors.

- [ ] **Step 1: Write failing geometry and migration tests**

Add a `MineShaftTest` that iterates the first stair step and asserts exactly nine cells: three lateral lanes times three vertical cells. Assert that the fourth lane coordinate is absent. In `MineSaveTest`, write a shape-6 entry with nonzero `cut` and `cuts`, load it, and assert the restored mine has all-zero cuts but the same entry, descent, gallery and `archRaised` state.

- [ ] **Step 2: Run the focused tests and verify the intended red state**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.model.MineShaftTest --tests com.villagecolony.data.save.MineSaveTest`

Expected: FAIL because the current stair has two lanes and shape 6 still accepts its cursor.

- [ ] **Step 3: Implement the smallest geometry and save change**

Set `MineShaft.STAIR_LANES` to `3`; retain the existing lateral coordinate formula so lane indexes `0`, `1`, and `2` occupy three adjacent cells. Let existing count constants derive from it. Increment `MineSave.SHAPE_VERSION` from `6` to `7` and add the corresponding Javadoc history. Do not translate a prior cursor; keep the existing mismatch branch that uses `new int[0]`.

- [ ] **Step 4: Verify focused green tests and compile**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.model.MineShaftTest --tests com.villagecolony.data.save.MineSaveTest`

Expected: PASS; all shape-6 cuts reset and new shape-7 saves round-trip.

Run: `./gradlew.bat build --console=plain`

Expected: PASS before introducing the water-access component.

- [ ] **Step 5: Commit the independently valid migration**

```powershell
git add src/main/java/com/villagecolony/core/construction/model/MineShaft.java src/main/java/com/villagecolony/data/save/MineSave.java src/test/java/com/villagecolony/core/construction/model/MineShaftTest.java src/test/java/com/villagecolony/data/save/MineSaveTest.java
git commit -m "P0.8: widen mine shafts and reset old cursors (MineShaftTest)"
```

### Task 2: Define and test water-access qualification and protected geometry

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/work/WaterMineAccess.java`
- Create: `src/test/java/com/villagecolony/fabric/work/WaterMineAccessTest.java`

**Interfaces:**
- Produces: `WaterMineAccess.find(ServerWorld world, BlockPos center, Side descent): Optional<WaterMineAccess.Route>`.
- Produces: `Route.entry()`, `Route.contains(BlockPos)`, `Route.stairs()`, `Route.shell()` and `Route.place(ServerWorld)`.
- Consumes: `BlockProtection`, loaded-chunk checks and `MineRock.isDiggableRock` without changing those contracts.

- [ ] **Step 1: Write failing pure-contract tests around Route geometry**

Construct a route with a known surface point, `Side.NORTH` and four descending steps. Assert: each step has three stair-floor positions; every coordinate in the three-by-three internal cross-section is clear; the shell has glass on floor, ceiling and both walls; `Route.contains` includes all of those positions; and a coordinate immediately outside the shell is not included. Add tests that a candidate with 63 natural rock positions fails and exactly 64 succeeds.

- [ ] **Step 2: Run the new unit test and verify red**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.work.WaterMineAccessTest`

Expected: FAIL because `WaterMineAccess` does not exist.

- [ ] **Step 3: Implement qualification without chunk loading**

Create the focused class in `fabric.work`. Use a bounded water sample around the colony center to distinguish water-founded villages from dry or lakeside terrain. Evaluate only already-loaded chunks. Search fixed, bounded outward directions; reject positions that `BlockProtection` identifies as village-original or colony-built. At the lower exit, count a contiguous `8 x 8` region of natural `MineRock.isDiggableRock` positions and require all 64. Build `Route` from the winning candidate; do not set blocks during qualification.

- [ ] **Step 4: Implement placement as an idempotent sealed structure**

In `Route.place`, replace only positions already validated as route volume. Fill the shell with `Blocks.GLASS`, use correctly faced stair states for the three floor lanes, and clear only the internal three-by-three volume. Recheck chunks and protection immediately before each write. Return false and leave the Mine unopened if any recheck fails.

- [ ] **Step 5: Verify focused green tests**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.work.WaterMineAccessTest`

Expected: PASS, including the 63/64 stone boundary and all shell/stair membership checks.

- [ ] **Step 6: Commit the isolated access model**

```powershell
git add src/main/java/com/villagecolony/fabric/work/WaterMineAccess.java src/test/java/com/villagecolony/fabric/work/WaterMineAccessTest.java
git commit -m "P0.8: qualify sealed water mine access (WaterMineAccessTest)"
```

### Task 3: Integrate the route when the normal mine exhausts

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/MineTrouble.java`
- Create: `src/gametest/java/com/villagecolony/gametest/WaterMineAccessGameTest.java`

**Interfaces:**
- Consumes: `WaterMineAccess.find(...)` and `Route.place(...)` from Task 2.
- Produces: a replacement `Mine` opened at `Route.entry()` only when the
  prior mine has exhausted every level and the water route is valid.

- [ ] **Step 1: Write failing GameTests for selection and world safety**

Add three fixtures:

1. water around a colony center with an `8 x 8` natural stone platform at the lower exit; exhaust a Mine and assert its replacement opens at the lower entry, stairs exist in all three lanes, shell cells are glass and every interior cell has empty fluid;
2. the same water fixture with only 63 natural stone cells; assert the existing opposite dry-mouth recovery is used and every water-route candidate block is unchanged;
3. a dry fixture; exhaust a Mine and assert the ordinary opposite-mouth recovery remains in effect and no glass appears.

- [ ] **Step 2: Run the specific GameTest class and verify red**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL because exhausted mines currently recover only through the dry mouth.

- [ ] **Step 3: Route exhausted-mine recovery through the qualified access**

Keep `MineSite` unchanged. In `MineTrouble.abandonAtBottom`, call
`WaterMineAccess.find` before the existing opposite-mouth search. Call
`Route.place` before `MINES.open`; on success use `Route.entry()` for
`MineShaft.from`, log `opens a sealed water mine access`, and light the new
mine without raising a dry-mouth arch. On refusal, retain the existing
opposite-mouth behavior.

- [ ] **Step 4: Verify GameTests and full unit suite**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS for all three fixtures.

Run: `./gradlew.bat test`

Expected: PASS.

- [ ] **Step 5: Commit the integrated fallback**

```powershell
git add src/main/java/com/villagecolony/fabric/work/MineTrouble.java src/gametest/java/com/villagecolony/gametest/WaterMineAccessGameTest.java
git commit -m "P0.8: open water mines only through sealed access (WaterMineAccessGameTest)"
```

### Task 4: Enforce the no-mining boundary for the access route

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/MineRock.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineFrontier.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineDigging.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/WaterMineAccessGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/MinerGameTest.java`

**Interfaces:**
- Consumes: `WaterMineAccess.Route.contains(BlockPos)` and a lower Mine entry.
- Produces: one shared target predicate that excludes route coordinates before a claim, break, recovery or detour action is issued.

- [ ] **Step 1: Write failing rejection GameTests**

In the water fixture, place mineable stone adjacent to an access stair and force target selection through the ordinary frontier, blocked-frontier recovery and detour paths. Assert each returned target is outside `Route.contains`, every stair and glass position remains unchanged after the miner tick, and a mineable rock beyond the lower exit is eventually returned. In `MinerGameTest`, assert the normal widened mine returns a target beyond its entry, not a stair floor position.

- [ ] **Step 2: Run the focused GameTests and verify red**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: FAIL until every target source uses the shared exclusion.

- [ ] **Step 3: Add one shared exclusion at mining eligibility**

Add a `WaterMineAccess.protects(ServerWorld, Mine, BlockPos)` query that recognizes the connected glass-and-stair route from the saved lower entry using only a bounded local scan. Call it from the common mine-rock/target eligibility boundary, before `MineRock.isDiggableRock` can return true, and from frontier reopening. Do not rely on an in-memory global route cache. Keep ordinary glass or stairs outside a recognized connected route governed by existing natural-rock rules.

- [ ] **Step 4: Verify green behavior and regression suite**

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: PASS; protected access blocks survive and lower mine stone remains reachable.

Run: `./gradlew.bat test`

Expected: PASS.

- [ ] **Step 5: Commit protection enforcement**

```powershell
git add src/main/java/com/villagecolony/fabric/work/MineRock.java src/main/java/com/villagecolony/fabric/work/MineFrontier.java src/main/java/com/villagecolony/fabric/work/MineDigging.java src/gametest/java/com/villagecolony/gametest/WaterMineAccessGameTest.java src/gametest/java/com/villagecolony/gametest/MinerGameTest.java
git commit -m "P0.8: protect sealed mine access from miners (WaterMineAccessGameTest)"
```

### Task 5: Record the decision and complete release-level verification

**Files:**
- Create: `docs/decisions/ADR-026-water-mine-access.md`
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `docs/archive/technical/Development-Log.md`

**Interfaces:**
- Produces: the documented water-only exception, shape-7 migration and explicit playtest evidence to collect.

- [ ] **Step 1: Write ADR-026 before final verification**

Record: normal dry mouth precedence; water-founded and loaded-chunk eligibility; `8 x 8` natural stone exit; free initial infrastructure scope; physical-world truth with no duplicated route save; protected route contract; shape-7 cursor reset; rejected alternatives of generic terrain fallback and chunk loading.

- [ ] **Step 2: Update live project state**

In `STATE.md` and `TODO.md`, distinguish automated coverage from pending save playtest. Add the exact user-visible checks: a water village produces one glass-sealed three-wide stair, no water reaches the interior, miners descend it, no stair/glass is broken, and a land village still gets a normal three-wide mine.

- [ ] **Step 3: Run the complete verification sequence**

Run: `./gradlew.bat test`

Expected: PASS.

Run: `./gradlew.bat runGametest --rerun-tasks`

Expected: all registered GameTests PASS; record exact count from `build/gametest/logs/latest.log`.

Run: `./gradlew.bat build --console=plain`

Expected: PASS and produces the Fabric JAR in `build/libs`.

- [ ] **Step 4: Inspect release evidence without publishing**

Run: `Get-FileHash (Get-ChildItem build\\libs\\*.jar | Where-Object { $_.Name -notmatch 'sources|dev' } | Select-Object -First 1).FullName -Algorithm SHA256`

Expected: one recorded artifact hash. Do not copy to `downloads`, the Minecraft mods folder, commit, or push unless the author separately requests release publication.

- [ ] **Step 5: Commit documentation only after all checks are green**

```powershell
git add docs/decisions/ADR-026-water-mine-access.md STATE.md TODO.md docs/archive/technical/Development-Log.md
git commit -m "P0.8: document sealed water mine access (runGametest)"
```
