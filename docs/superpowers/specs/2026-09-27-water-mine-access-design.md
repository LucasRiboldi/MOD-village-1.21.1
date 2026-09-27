# Water Mine Access Design

## Status

Proposed for author review, 2026-09-27.

## Intent

A colony founded over water must still be able to reach a real underground
mining area without forcing chunks, flooding the route, or letting miners
destroy their own access. This is an exception for water-founded colonies; it
must not become a fallback for uneven land, hills, or ordinary failed mine
sites.

## Existing Constraints

- The normal dry mine remains the first and preferred option.
- Every new normal mine stair and branch stair has three walkable lanes and
  three blocks of headroom. The aquatic access uses the same clear `3 x 3`
  passage, so a miner never changes to a narrower route at the lower exit.
- World blocks remain the source of truth. The new access has no duplicate
  persistent geometry; the existing saved Mine starts at its lower exit.
- Only loaded chunks may be inspected or changed.
- Village-original blocks, colony buildings, open construction sites and
  player-protected blocks remain untouched.
- The access is infrastructure, not a resource source. It does not permit
  creating arbitrary materials or mining through protected terrain.

## Trigger

`WaterMineAccess` is evaluated only after an existing mine exhausts every
level. It succeeds only when all of the following are true:

1. a fixed sample around the colony foundation proves that the village is
   water-founded, rather than merely near a pond;
2. all candidate chunks for the access are already loaded;
3. the candidate begins outside village pieces, colony infrastructure and
   construction projects;
4. the lower exit reaches a natural, mineable `8 x 8` stone area (64 blocks)
   without crossing protected blocks.

If any condition fails, no block is changed. The existing dry re-opening on
the opposite side remains available.

## Geometry

The access begins at a safe water-side point outside the built village and
descends in a straight line to the qualified lower exit.

- Clear internal corridor: 3 blocks wide and 3 blocks high.
- Floor: three stair blocks per descending step, with their facing matching
  the descent direction.
- Shell: glass floor, side walls and ceiling surround the clear corridor,
  isolating it from water on every side.
- Lower exit: joins the existing normal Mine geometry; its saved entry is the
  exit, so restart and mine advancement require no second state model.
- Construction is bounded and performed only when the active colony's loaded
  world is being simulated. It must not force a chunk or scan the entire
  world.

The initial access is equivalent to the existing mine mouth and carved mine
infrastructure: it is placed once to make physical extraction possible. This
exception is limited to this water-only rescue route.

## Normal Mine Geometry and Save Migration

`MineShaft.STAIR_LANES` changes from two to three for the common spiral and
each branch. This changes `positionAt` ordering and every cut cursor. The save
shape version must increase. A mine saved with an older shape keeps its entry,
descent, gallery and arch state, but restores with empty cut cursors. It then
rechecks the wider geometry against the physical world incrementally; it never
tries to translate an old cursor into a new coordinate.

## Protection Contract

Every block of the completed access corridor is mining infrastructure.

- `MineDigging`, its frontier recovery, rerouting, detour actions and any
  future miner target selection must reject coordinates inside the access.
- A miner may walk the stairs to reach the lower Mine entry, but its first
  target is always part of the regular mine below the access.
- Glass, stairs and any shell block are never valid mining targets, even if a
  player later changes a neighboring block or the Mine advances after a
  restart.
- Protection is derived from the geometry and Mine entry, not from an
  unbounded global position cache. A restarted server reaches the same answer.

## Integration

1. Introduce a focused `WaterMineAccess` integration component for loaded-world
   qualification, geometry and placement.
2. Route the exhausted-mine recovery in `MineTrouble` to this component before
   its normal opposite-mouth recovery.
3. Open the existing `Mine` at the lower exit and keep `MineFurnishing` for
   the normal mine entry there.
4. Add the water-access exclusion to the common mining eligibility boundary,
   rather than duplicating exclusions in each miner branch.
5. Add a dedicated ADR recording the exception, the world-truth model and the
   no-mining contract.

## Test Plan

Unit tests:

- geometry exposes a 3-wide, 3-high walkable tunnel and identifies every shell
  and stair block as protected;
- the normal spiral and every branch expose three lanes with three blocks of
  headroom, and an older mine save resets its cursor under the new geometry;
- the lower exit only qualifies with 64 natural mineable stone blocks;
- a normal land failure cannot qualify as water-founded;
- protection still derives correctly from a reconstructed saved Mine entry.

Fabric GameTests:

- a water-founded fixture opens the glass-sealed route and an ordinary Mine at
  the lower exit;
- the internal volume contains no water after placement;
- a miner target search never returns a stair or glass-shell coordinate;
- a dry village continues to use the ordinary mine mouth;
- water without a qualified lower stone area changes no blocks and opens no
  mine.

## Non-Goals

- No global ocean-floor search, chunk loading, underwater pathfinder, or new
  mineral generation.
- No use for mountains, broken terrain, ordinary lakeside villages or mines
  that simply need to reroute.
- No modification of the currently separate smelter chest-binding warning.
