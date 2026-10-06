# Village visuals, logistics, mine, and sweep design

**Status:** design direction approved in chat on 2026-10-03; awaiting author
review and implementation plan

## Goal

Make the village legible in the world, remove a construction supply stall,
standardize the mine geometry, and make resource searches exhaustive without
rescanning known fluid cells.

## Constraints

- Fabric 1.21.1 and Java 21; no mixins or new external dependencies.
- The world remains the source of truth. A cache may only be a rebuildable
  performance index and cannot authorize placement, protection, or inventory
  changes.
- Existing clients without the mod keep the vanilla site signs and must never
  receive an unsupported custom payload.
- Mine cursor migration follows `MineSave.SHAPE_VERSION`: retain the mouth and
  restart only the incompatible frontier/cursors.
- Terrain preparation remains limited to a planned construction footprint and
  never applies to farm blueprints.

## 1. Pixel-art world overlays

The profession and construction overlays use a shared client-side
`PixelPanelLayout` model. The model maps the server-provided enum name to an
explicit texture id; it never derives an icon from translated display text.
Each panel has a pixel-art background, a fixed left icon cell, and a right text
cell. Its width is the maximum of its minimum width and icon/text/padding
widths, so translated construction state and missing-material messages stay
inside the background.

The profession panel always draws its icon. The translated profession name is
controlled by a local Mod Menu boolean, defaulting to visible. Turning it off
changes only the local renderer: no server state, worker state, or packet
schema is affected. Construction panels always show status icon plus text;
their status texture is selected from the serialized construction state.

The payload remains a periodic snapshot as specified by ADR-025. It transports
only stable presentation identifiers (`ProfessionType.name()` and
`ConstructionState.name()`), while the client owns texture mapping and layout.
Codec tests cover round trips and unknown identifiers; client layout tests
cover label-on, label-off, minimum width, and longest construction text.

## 2. Construction supply and terrain preparation

When the builder reaches the next placement but lacks its item, it withdraws
the exact required item from any loaded, valid village chest selected by
`ColonyChests`. The withdrawal reuses the same live chest set used for colony
stock accounting and removes a real item before placement. It may not create
an item, read a chest outside the village bounds, or consume player-owned /
invalid chests. If no physical item exists, existing production and fallback
routes remain responsible for satisfying the demand and the project waits with
the material named in diagnostics.

Site preparation removes only `minecraft:grass_block` occupying a planned
non-farm footprint position that the blueprint will replace. It does not remove
dirt, sand, stone, crops, farmland, fluids, or any block from a farm blueprint.
The placement/protection check remains authoritative after preparation; the
preparation is a narrow cleanup, not terrain leveling.

GameTests prove withdrawal from a non-builder village chest, rejection of an
outside/invalid chest, no item fabrication, grass-only removal, and farm
exclusion. Work-time telemetry is retained to compare builder idle time in a
save playtest after the fix.

## 3. Village boundary marker

`VillageAreaMarker` is a server-only particle marker parallel to `SiteMarker`.
Once per second, when a player is near an active colony, it renders the loaded
horizontal perimeter of that colony's `VillageBounds`. It owns no world
entities and no saved coordinates; it reads the current bounds each draw, so a
village expansion appears on the next draw automatically. Construction-site
markers keep their distinct state colors and remain unchanged.

GameTests cover the initial rectangle, an expanded bound, cadence, and no
particles when no player is nearby.

## 4. Mine geometry

The mouth becomes a five-block-wide, four-block-high arch. Its central
three-by-three opening is passable, the two outer columns form the frame, and
one lantern occupies the top of each side of the frame. Portal protection,
furnishing, lighting and chest reach use the same geometry helper so that the
mine never tries to excavate or overwrite its own arch.

Stairs and every newly mined common/branch volume use a three-wide by
three-high cross-section. The current geometry's five-wide room is removed;
width and headroom are named constants shared by the shaft generator and its
tests. `MineSave.SHAPE_VERSION` is incremented. A prior save retains its mouth
and mine identity but clears geometry-dependent cursors/frontiers exactly once.

Unit tests assert coordinate membership/counts and migration. GameTests assert
the 5x4 mouth, two lanterns, free 3x3 passage, and dry/water-sealed 3x3 mine
descent.

## 5. Resource spiral and fluid index

The surface resource traversal starts at the perimeter of the current
`VillageBounds` and moves in a deterministic inward spiral to the center. A
completed in-bounds sweep continues with concentric outer rings, allowing
resources outside the village area. Each worker/question has a resumable cursor
that records completed positions/rings; a retry begins at the next unvisited
candidate rather than restarting at the center.

`VillageFluidIndex` is an in-memory, rebuildable index keyed by colony id and
the exact measured `VillageBounds`. Building or expanding a village bound
invalidates it. Before the next resource sweep, it is rebuilt incrementally
from loaded cells inside those bounds, marking water and lava positions. A
surface search skips indexed fluid cells without calling its expensive resource
predicate. The search still reads the world at a selected target before any
action, so changed water/lava never becomes an authority problem. After a
restart, a missing index triggers the same rebuild before selection.

Unit tests prove deterministic border-to-center order, no duplicate position
across budget resumes, and the first outer ring. GameTests prove water/lava
cells are skipped, expansion invalidates/rebuilds the index, and an outer-ring
resource is found after an empty in-bounds spiral.

## Documentation and acceptance

ADR-025 receives the local overlay toggle and explicit icon mapping. ADR-013
receives the revised mine dimensions and shape-version migration. ADR-017
receives the grass-only, non-farm preparation rule. A new ADR records the
rebuildable fluid-index contract because it crosses colony bounds, surface
gathering, and server lifecycle.

Acceptance requires a green baseline build before edits, each new test observed
failing before its production change, `./gradlew.bat test`, a full
`./gradlew.bat runGametest --rerun-tasks --no-daemon`, and a save playtest. The
playtest must confirm icon/name toggle, responsive panels, village perimeter,
builder material withdrawal, mine dimensions, outward continuation, and a
post-fix `time_ledger` measurement.
