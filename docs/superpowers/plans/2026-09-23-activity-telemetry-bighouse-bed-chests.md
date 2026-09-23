# Activity Telemetry, BigHouse Road Level, and Vanilla Bed Chests

**Goal:** implement the approved structured activity telemetry, lower the
BigHouseMOD template by one base row, and create safe bed-adjacent chests only
for original vanilla-village structures when a colony is first adopted.

## Implementation Steps

1. Add focused analyzer tests for `VC_ACTIVITY` v1 parsing and schema
   migration; add a small Fabric logging adapter that emits bounded state
   transitions from existing idle and controlled-abandonment paths.
2. Add GameTests that describe the strict vanilla bed-chest geometry before
   changing placement: a valid wall/door layout and no creation from a worker
   cycle. Refactor `ChestPlacer` into a strict, structure-aware placement
   helper and invoke it only for the exact accepted candidate beds on first
   colony adoption.
3. Preserve bed positions in the accepted candidate so the Fabric bridge can
   stay within the candidate cluster. Exclude compliant bed chests from public
   village stock; make `ChestScanner` prefer an existing compliant bed chest
   and remove its late world-writing fallback.
4. Add the BigHouse template regression, rewrite the compressed NBT asset to
   remove its original base row and lower remaining entries, then update the
   blueprint and foundation expectations. The chest-placement rule must not
   touch the BigHouse's six private chests.
5. Update `STATE.md`, `TODO.md`, and the development log. Run focused
   tests, `./gradlew.bat test`, `./gradlew.bat runGametest --rerun-tasks`, and
   `./gradlew.bat build`; copy and hash-compare the release JAR, then commit
   one scoped task and push the current branch.

## Safety Gates

- No forced chunk loads, player-block replacement, new persistent telemetry
  store, or new mixin.
- Every placement is idempotent and one-time for a newly created colony.
- BigHouseMOD remains explicitly outside vanilla bed-chest placement.
- A failed test must demonstrate the intended pre-fix defect before its
  production change is written.
