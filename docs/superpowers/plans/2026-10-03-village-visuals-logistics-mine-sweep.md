# Village visuals, logistics, mine, and sweep Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver pixel-art overlays, physical construction supply, village bounds particles, a 5x4/3x3 mine, and resumable resource scans that skip known fluids.

**Architecture:** Client overlays map stable payload identifiers to local textures and a measured panel layout. Server-side markers and surface gathering read the current `VillageBounds`; mine geometry has one source; the fluid index is memory-only and invalidated by bounds changes.

**Tech Stack:** Fabric 1.21.1, Java 21, Fabric GameTest, JUnit 5, Mod Menu.

**Spec:** `docs/superpowers/specs/2026-10-03-village-visuals-logistics-mine-sweep-design.md`

## Implementation Status — 2026-10-03

Tasks 1–7 are implemented and covered by unit tests or Fabric GameTests. The
existing `SiteMarker` owns both particle outlines, the established builder
supply route was retained and regression-tested, and the already-three-wide
mine geometry required only the new 5x4 mouth. ADRs and live project status
were updated in Task 8. The later visual refinement replaced the opaque panel
with a transparent nine-slice border and moved the icon above the text. The
approved profession-chest follow-up is also implemented: a full chest exports
its final ten slots only to community chests in the same village (ADR-032).
The step checkboxes below preserve the originally approved execution script;
the only remaining evidence is the real-save checklist, not production
implementation.

## Global Constraints

- Fabric 1.21.1 and Java 21; no mixins or new external dependencies.
- World state is authoritative; the fluid index is rebuildable and never authorizes edits or inventory changes.
- Unmodded clients retain vanilla signs and receive no unsupported payload.
- Mine migration retains mouth/orientation/arch and resets only geometry-dependent cursors.
- Cleanup removes only `minecraft:grass_block` at planned non-farm footprint positions.
- Preserve player-owned, invalid, and out-of-bounds chests; remove a real item before construction placement.
- Run `./gradlew.bat build` before production edits. Every changed Fabric behavior has a GameTest.

## Review Focus

- Unknown role/status identifiers choose a neutral panel rather than another role icon; Task 1.
- Hiding a profession label is client-local and leaves payload identifiers intact; Task 2.
- An empty valid chest does not fabricate material or wake the project; Task 3.
- A budgeted bounds scan resumes at a unique next column before entering the outer ring; Task 6.
- The fluid index only suppresses known fluid columns; all non-indexed columns
  still consult the world before a block action; Task 7.

---

### Task 1: Explicit pixel-art panel model

**Files:**
- Create: `src/main/java/com/villagecolony/client/PixelPanelLayout.java`
- Create: `src/test/java/com/villagecolony/client/PixelPanelLayoutTest.java`
- Modify: `src/main/java/com/villagecolony/client/ProfessionOverlayRenderer.java`
- Modify: `src/main/java/com/villagecolony/client/ConstructionOverlayRenderer.java`

**Interfaces:** Produces `PixelPanelLayout.of(Identifier, int, int, boolean)`, `width()`, `iconX()`, `textX()`, `professionTexture(String)`, and `constructionTexture(String)`. Renderers consume it and retain their existing billboard transform.

- [ ] **Step 1: Write failing mapping/layout tests.**

```java
assertEquals(BUILDER, PixelPanelLayout.professionTexture("BUILDER"));
assertEquals(UNKNOWN, PixelPanelLayout.professionTexture("NOT_A_ROLE"));
PixelPanelLayout panel = PixelPanelLayout.of(BUILDER, 16, 83, true);
assertTrue(panel.width() >= 16 + 83 + 3 * PixelPanelLayout.PADDING);
assertTrue(panel.textX() > panel.iconX());
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.client.PixelPanelLayoutTest --no-daemon`

Expected: compilation fails because `PixelPanelLayout` is absent.

- [ ] **Step 3: Implement the model and draw order.**

```java
public record PixelPanelLayout(Identifier texture, int width, int iconX, int textX) {
    static final int ICON_SIZE = 16;
    static final int PADDING = 3;
    static PixelPanelLayout of(Identifier texture, int iconWidth, int textWidth, boolean showText) {
        int content = iconWidth + (showText ? PADDING + textWidth : 0);
        return new PixelPanelLayout(texture, Math.max(32, content + 2 * PADDING), PADDING,
                PADDING + iconWidth + PADDING);
    }
}
```

Map `ProfessionType.name()` and construction state strings explicitly to existing overlay textures. Draw background, fixed left icon, then right-side translated text; never infer texture from translated text or center text independently.

- [ ] **Step 4: Run green.**

Run: `./gradlew.bat test --tests com.villagecolony.client.PixelPanelLayoutTest --no-daemon`

Expected: PASS.

- [ ] **Step 5: Commit.**

Run: `git add src/main/java/com/villagecolony/client/PixelPanelLayout.java src/main/java/com/villagecolony/client/ProfessionOverlayRenderer.java src/main/java/com/villagecolony/client/ConstructionOverlayRenderer.java src/test/java/com/villagecolony/client/PixelPanelLayoutTest; git commit -m "P0.1: desenhar paineis pixelados corretos (PixelPanelLayoutTest)"`

### Task 2: Local Mod Menu label toggle

**Files:**
- Create: `src/main/java/com/villagecolony/client/OverlayPreferences.java`
- Create: `src/test/java/com/villagecolony/client/OverlayPreferencesTest.java`
- Modify: `src/main/java/com/villagecolony/client/modmenu/ProfessionMenuScreen.java`
- Modify: `src/main/java/com/villagecolony/client/ProfessionOverlayRenderer.java`
- Modify: `src/main/resources/assets/villagecolony/lang/en_us.json`
- Modify: `src/main/resources/assets/villagecolony/lang/pt_br.json`

**Interfaces:** Produces `OverlayPreferences.showProfessionLabel()` and `setShowProfessionLabel(boolean)`, persisted only in client config. The renderer reads this value solely for layout; `OverlaySnapshotPayload` and `OverlaySync` stay unchanged.

- [ ] **Step 1: Write a failing client-only preference test.**

```java
OverlayPreferences preferences = OverlayPreferences.inMemory(true);
preferences.setShowProfessionLabel(false);
assertFalse(preferences.showProfessionLabel());
assertEquals("BUILDER", ClientOverlayState.professionOf(worker));
```

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.client.OverlayPreferencesTest --no-daemon`

Expected: compilation fails because `OverlayPreferences` is absent.

- [ ] **Step 3: Implement local persistence and menu button.**

```java
addDrawableChild(ButtonWidget.builder(
    Text.translatable("text.villagecolony.overlay.profession_label", preferences.showProfessionLabel()),
    button -> { preferences.setShowProfessionLabel(!preferences.showProfessionLabel()); rebuild(); })
    .dimensions(left, top + index++ * 24, 200, 20).build());
```

Missing/malformed config defaults to `true`. The icon and panel remain visible when the label is disabled.

- [ ] **Step 4: Run green.**

Run: `./gradlew.bat test --tests com.villagecolony.client.OverlayPreferencesTest --tests com.villagecolony.client.PixelPanelLayoutTest --no-daemon`

Expected: PASS.

- [ ] **Step 5: Commit.**

Run: `git add src/main/java/com/villagecolony/client/OverlayPreferences.java src/main/java/com/villagecolony/client/modmenu/ProfessionMenuScreen.java src/main/java/com/villagecolony/client/ProfessionOverlayRenderer.java src/main/resources/assets/villagecolony/lang/en_us.json src/main/resources/assets/villagecolony/lang/pt_br.json src/test/java/com/villagecolony/client/OverlayPreferencesTest.java; git commit -m "P0.1: permitir ocultar nome de profissao (OverlayPreferencesTest)"`

### Task 3: Exact builder withdrawal and grass-only preparation

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/BuilderMaterials.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/SitePreparation.java`
- Modify: `src/gametest/java/com/villagecolony/fabric/work/BuilderMaterialsGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/fabric/work/FoundationRepairGameTest.java`
- Modify: `src/test/java/com/villagecolony/core/construction/model/BlueprintStreetLayerTest.java`

**Interfaces:** `BuilderMaterials.takeMaterial(ServerWorld, ConstructionProject, Block)` removes and returns the exact `Item` from valid `ColonyChests`. `SitePreparation.clear(ServerWorld, ConstructionProject)` performs narrow grass foundation cleanup after existing plant cleanup.

- [ ] **Step 1: Add failing GameTests.**

```java
assertTrue(BuilderMaterials.takeMaterial(world, project, Blocks.OAK_PLANKS).isPresent());
assertEquals(0, ChestWithdrawer.count(world, validVillageChest, Items.OAK_PLANKS));
assertEquals(1, ChestWithdrawer.count(world, outsideChest, Items.OAK_PLANKS));
helper.assertBlockPresent(Blocks.DIRT, replacedGrassFoundation);
helper.assertBlockPresent(Blocks.FARMLAND, farmFoundation);
```

Create a valid remote village chest, an outside chest, and an empty valid chest. The empty case remains `WAITING_RESOURCES`; no item may appear. Cover non-farm grass, dirt/sand/stone, and a farm blueprint.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: the named scenarios fail because direct withdrawal/grass-only preparation are missing.

- [ ] **Step 3: Implement without relaxing chest or terrain protections.**

```java
for (Item candidate : MaterialChoice.forBlock(wanted)) {
    if (ColonySupply.take(world, project.colonyId(), project.origin(), candidate)) return Optional.of(candidate);
}
```

Keep fallback routing after all valid colony chests lack a candidate. In `SitePreparation`, require a non-farm blueprint, `blueprint.isBase(block)`, and `world.getBlockState(target).isOf(Blocks.GRASS_BLOCK)` before clearing; do not clear dirt, sand, stone, crops, farmland, fluids, or a farm blueprint.

- [ ] **Step 4: Run green.**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.model.BlueprintStreetLayerTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: PASS.

- [ ] **Step 5: Commit.**

Run: `git add src/main/java/com/villagecolony/fabric/work/BuilderMaterials.java src/main/java/com/villagecolony/fabric/integration/SitePreparation.java src/gametest/java/com/villagecolony/fabric/work/BuilderMaterialsGameTest.java src/gametest/java/com/villagecolony/fabric/work/FoundationRepairGameTest.java src/test/java/com/villagecolony/core/construction/model/BlueprintStreetLayerTest.java; git commit -m "P0.2: retirar material real e limpar so grama (BuilderMaterialsGameTest)"`

### Task 4: Village boundary particle marker

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/VillageAreaMarker.java`
- Create: `src/test/java/com/villagecolony/fabric/integration/VillageAreaMarkerTest.java`
- Modify: `src/main/java/com/villagecolony/fabric/event/VillageDetectionHandler.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java`

**Interfaces:** Produces `VillageAreaMarker.tick(ServerWorld)` and package-visible `perimeter(VillageBounds)`. `VillageDetectionHandler` invokes it beside `SiteMarker.tick(overworld)`; it writes no saved state.

- [ ] **Step 1: Write a failing perimeter/cadence test.**

```java
assertEquals(Set.of(new ColonyPos(0, 64, 0), new ColonyPos(2, 64, 0),
    new ColonyPos(0, 64, 1), new ColonyPos(2, 64, 1)),
    new HashSet<>(VillageAreaMarker.perimeter(new VillageBounds(0, 64, 0, 2, 64, 1))));
```

Add GameTest coverage at twenty ticks and after widening bounds, using a particle-observing fixture.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageAreaMarkerTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: missing-class failure, then new marker scenario failure.

- [ ] **Step 3: Implement server-only perimeter rendering.**

```java
if (++tickCounter < 20 || world.getPlayers().isEmpty()) return;
for (Colony colony : VillageColonyMod.COLONIES.all()) {
    colony.bounds().filter(bounds -> !noPlayerNear(world, bounds)).ifPresent(bounds ->
        perimeter(bounds).forEach(at -> spawn(world, at)));
}
```

Use the same nearby-player policy as `SiteMarker`, and read bounds every draw so expansion appears on the next cadence.

- [ ] **Step 4: Run green and commit.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageAreaMarkerTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: PASS.

Run: `git add src/main/java/com/villagecolony/fabric/integration/VillageAreaMarker.java src/main/java/com/villagecolony/fabric/event/VillageDetectionHandler.java src/test/java/com/villagecolony/fabric/integration/VillageAreaMarkerTest.java src/gametest/java/com/villagecolony/gametest/BuildSiteGameTest.java; git commit -m "P0.3: marcar perimetro atual da vila (VillageAreaMarkerTest)"`

### Task 5: Shared 5x4 mouth and 3x3 mine geometry

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/MineGeometry.java`
- Modify: `src/main/java/com/villagecolony/fabric/integration/MineMouth.java`
- Modify: `src/main/java/com/villagecolony/core/construction/model/MineShaft.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineSite.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineCuts.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/MineFrontier.java`
- Modify: `src/main/java/com/villagecolony/data/save/MineSave.java`
- Modify: `src/test/java/com/villagecolony/core/construction/model/MineShaftTest.java`
- Modify: `src/test/java/com/villagecolony/data/save/MineSaveTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/MineFluidGameTest.java`

**Interfaces:** `MineGeometry` owns `MOUTH_WIDTH = 5`, `MOUTH_HEIGHT = 4`, `PASSAGE_WIDTH = 3`, `PASSAGE_HEIGHT = 3`, `passage`, `archFrame`, and `lanternPositions`. `MineSave.SHAPE_VERSION` becomes `7`.

- [ ] **Step 1: Add failing geometry/migration tests.**

```java
assertEquals(15, MineGeometry.passage(mouth, descent).size());
assertEquals(2, MineGeometry.lanternPositions(mouth, descent).size());
assertEquals(0, migrated.arm(0).cut());
assertTrue(migrated.archRaised());
```

Update shape-six migration to reset every cursor. Add GameTest assertions for five-wide/four-high frame, two lanterns, clear 3x3 passage, and dry/water-sealed 3x3 descent.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.model.MineShaftTest --tests com.villagecolony.data.save.MineSaveTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: current 3-wide mouth/five-wide excavation fails new assertions.

- [ ] **Step 3: Implement one geometry source.**

```java
public static List<BlockPos> passage(BlockPos mouth, Side descent) {
    return IntStream.rangeClosed(-1, 1).boxed().flatMap(lane ->
        IntStream.rangeClosed(0, 2).mapToObj(height -> offset(mouth, descent, lane, -height))).toList();
}
```

Replace divergent widths/headrooms in mouth, shaft, cuts, frontier, furnishing, and tests. The save reader resets all arms when `shape != 7`, retaining mouth, descent, gallery, and arch.

- [ ] **Step 4: Run green and commit.**

Run: `./gradlew.bat test --tests com.villagecolony.core.construction.model.MineShaftTest --tests com.villagecolony.data.save.MineSaveTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: PASS.

Run: `git add src/main/java/com/villagecolony/fabric/integration/MineGeometry.java src/main/java/com/villagecolony/fabric/integration/MineMouth.java src/main/java/com/villagecolony/core/construction/model/MineShaft.java src/main/java/com/villagecolony/fabric/work/MineSite.java src/main/java/com/villagecolony/fabric/work/MineCuts.java src/main/java/com/villagecolony/fabric/work/MineFrontier.java src/main/java/com/villagecolony/data/save/MineSave.java src/test/java/com/villagecolony/core/construction/model/MineShaftTest.java src/test/java/com/villagecolony/data/save/MineSaveTest.java src/gametest/java/com/villagecolony/gametest/MineFluidGameTest.java; git commit -m "P0.4: padronizar mina 5x4 e passagem 3x3 (MineSaveTest)"`

### Task 6: Inward village sweep and outward continuation

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/VillageSpiralSweep.java`
- Create: `src/test/java/com/villagecolony/fabric/integration/VillageSpiralSweepTest.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/SurfaceGatheringWork.java`
- Modify: `src/test/java/com/villagecolony/fabric/integration/RingSweepResumeTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/SurfaceGatheringGameTest.java`

**Interfaces:** Produces `VillageSpiralSweep.next(UUID, VillageBounds, int, Predicate<BlockPos>, Function<BlockPos, Optional<T>>)` and `pausedAt(UUID)`. `SurfaceGatheringWork.findTarget` uses it while farming/general scans retain `RingSweep`.

- [ ] **Step 1: Write failing order/resume tests.**

```java
assertEquals(new BlockPos(bounds.minX(), 0, bounds.minZ()), looked.getFirst());
assertEquals(looked.size(), new HashSet<>(looked).size());
assertTrue(looked.stream().anyMatch(pos -> !bounds.containsColumn(pos.getX(), pos.getZ())));
```

Use a rectangular bound and smaller-than-area budget. Repeated calls must visit each in-bounds column once, then find a resource in the first exterior ring.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageSpiralSweepTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: missing-class failure, then new surface scenario failure.

- [ ] **Step 3: Implement the two-phase cursor and integrate it.**

```java
private record Cursor(VillageBounds bounds, Phase phase, int layer, int offset) {}
private enum Phase { INSIDE, OUTSIDE }
```

Enumerate each rectangular layer clockwise from top-left while shrinking to center. After the innermost layer, enumerate shells immediately outside bounds. Store the next candidate before a budget return; only clear a cursor after target selection or all permitted outer rings. Replace only traversal invocation in `findTarget`; retain resource-specific patches and protected-area/sector filters.

- [ ] **Step 4: Run green and commit.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageSpiralSweepTest --tests com.villagecolony.fabric.integration.RingSweepResumeTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: PASS.

Run: `git add src/main/java/com/villagecolony/fabric/integration/VillageSpiralSweep.java src/main/java/com/villagecolony/fabric/work/SurfaceGatheringWork.java src/test/java/com/villagecolony/fabric/integration/VillageSpiralSweepTest.java src/test/java/com/villagecolony/fabric/integration/RingSweepResumeTest.java src/gametest/java/com/villagecolony/gametest/SurfaceGatheringGameTest.java; git commit -m "P0.5: varrer vila em espiral e continuar fora (VillageSpiralSweepTest)"`

### Task 7: Rebuildable water/lava index

**Files:**
- Create: `src/main/java/com/villagecolony/fabric/integration/VillageFluidIndex.java`
- Create: `src/test/java/com/villagecolony/fabric/integration/VillageFluidIndexTest.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/SurfaceGatheringWork.java`
- Modify: `src/main/java/com/villagecolony/VillageColonyMod.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/SurfaceGatheringGameTest.java`

**Interfaces:** Produces `skip(UUID, VillageBounds, BlockPos)`, `invalidate(UUID)`, and `clearAll()`. `VillageColonyMod.reportGrowth` calls `invalidate` only when bounds changed.

- [ ] **Step 1: Write failing index tests.**

```java
assertTrue(index.skip(colonyId, bounds, waterColumn));
assertTrue(index.skip(colonyId, bounds, lavaColumn));
assertFalse(index.skip(colonyId, grownBounds, waterColumn));
```

GameTest scans water/lava before a valid target, expands bounds, and changes an indexed water candidate before action to prove the final world read remains authoritative.

- [ ] **Step 2: Run red.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageFluidIndexTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: missing-class failure and new GameTest failure.

- [ ] **Step 3: Implement incremental server-memory index.**

```java
private record Key(UUID colonyId, VillageBounds bounds) {}
private static final Map<Key, ScanState> STATES = new HashMap<>();
public static boolean skip(UUID id, VillageBounds bounds, BlockPos column) {
    ScanState state = STATES.computeIfAbsent(new Key(id, bounds), ignored -> ScanState.empty(bounds));
    state.advanceLoadedColumns(MAX_COLUMNS_PER_TICK);
    return state.isKnownFluid(column);
}
```

Mark only water/lava in loaded columns. Cache miss/unloaded column is not skipped. Register `clearAll` with `ServerMemory`; call `invalidate` after actual bounds growth. Filter before expensive patch predicates but preserve `world.getBlockState(job.target)` before breaking.

- [ ] **Step 4: Run green and commit.**

Run: `./gradlew.bat test --tests com.villagecolony.fabric.integration.VillageFluidIndexTest --tests com.villagecolony.fabric.integration.VillageSpiralSweepTest --no-daemon; ./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: PASS.

Run: `git add src/main/java/com/villagecolony/fabric/integration/VillageFluidIndex.java src/main/java/com/villagecolony/fabric/work/SurfaceGatheringWork.java src/main/java/com/villagecolony/VillageColonyMod.java src/test/java/com/villagecolony/fabric/integration/VillageFluidIndexTest.java src/gametest/java/com/villagecolony/gametest/SurfaceGatheringGameTest.java; git commit -m "P0.5: pular fluidos conhecidos na coleta (VillageFluidIndexTest)"`

### Task 8: ADRs, project state, verification, and save evidence

**Files:**
- Create: `docs/decisions/ADR-0XX-village-fluid-index.md`
- Modify: `docs/decisions/ADR-025-client-side-pixel-art-overlays.md`
- Modify: `docs/decisions/ADR-013-Mine-Route-Rerouting.md`
- Modify: `docs/decisions/ADR-017-lot-ground-and-road-area.md`
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `docs/technical/Development-Log.md`

- [ ] **Step 1: Record the final contracts.**

Document explicit overlay mapping/client toggle, mine shape seven reset, non-farm grass-only preparation, and fluid-index bounds key/invalidation/world recheck.

- [ ] **Step 2: Run complete automated verification.**

Run: `./gradlew.bat build --no-daemon`

Expected: PASS.

Run: `./gradlew.bat runGametest --rerun-tasks --no-daemon`

Expected: all discovered GameTests complete; record exact count from `build/gametest/logs/latest.log`.

- [ ] **Step 3: Run post-fix diagnostics.**

Run: `python scripts\time_ledger.py`

Run: `python scripts\analyze_village_log.py (Join-Path $env:APPDATA '.minecraft\logs\latest.log')`

Expected: report builder work/idle and stall counters as log evidence, separately from save confirmation.

- [ ] **Step 4: Execute the save checklist.**

Verify role icons/names toggle, responsive construction text, expansion marker, remote valid-chest consumption, 5x4/3x3 mine with two lamps, outside scan continuation, and water/lava skip.

- [ ] **Step 5: Update live status and commit.**

Record dates/counts only after commands succeed. Close only verified backlog items and leave save playtest pending until observed. Run: `git add docs/decisions STATE.md TODO.md docs/technical/Development-Log.md; git commit -m "P0.6: registrar verificacao das melhorias da vila (build e GameTests)"`

## Self-Review

- Spec coverage: Tasks 1-2 cover panels, mappings, responsive labels, and the local toggle; Task 3 covers exact chest withdrawal and terrain policy; Task 4 covers village bounds marker; Task 5 mine geometry/migration; Tasks 6-7 traversal and fluid index; Task 8 ADRs, tests, logs, and save evidence.
- Placeholder scan: every task has exact files, a red command, concrete API/code shape, a green command, and commit command.
- Type consistency: each new type is introduced in its task before a later consumer; `VillageFluidIndex.invalidate(UUID)` is the sole growth hook.
- Review focus: unknown identifiers (1), local-only preference (2), empty chest (3), resumable uniqueness (6), and changed-world recheck (7) are directly tested.
