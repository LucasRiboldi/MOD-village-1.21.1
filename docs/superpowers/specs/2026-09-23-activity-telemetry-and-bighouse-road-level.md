# Activity Telemetry and BigHouseMOD Road-Level Design

**Status:** implemented; save playtest pending

## Goal

Make every Village Colony profession observable through a bounded, structured
activity stream so later log analysis can measure work, stalls, failures, and
their declared causes. At the same time, remove the artificial bottom row from
the mod-owned `BigHouseMOD` so its entrance starts at the walkable road level.
When a new vanilla village is adopted, give each accepted vanilla bed a
bed-adjacent chest only when its placement is unambiguously safe.

## Telemetry Contract

The Fabric integration emits one canonical `VC_ACTIVITY` log record for each
meaningful activity state transition. It is diagnostic output only: it does not
change worker decisions, world state, save data, task ownership, or chunk
loading.

Each record has stable, machine-readable fields:

- `version=1`
- `profession`: the worker's current profession
- `activity`: the capability or work family being attempted
- `outcome`: `WAITING`, `RECOVERED`, `ERROR`, or `ABANDONED`
- `reason`: a controlled key appropriate to the outcome, such as
  `NO_WORKER`, `NO_TASK`, `NO_TARGET`, `UNREACHABLE_TARGET`,
  `NO_PROGRESS`, `MISSING_RESOURCE`, or `WORKER_GAVE_UP`

The stream deliberately excludes worker UUIDs, colony UUIDs, coordinates,
player data, block names from live locations, and raw exception messages. The
source log remains the ephemeral event transport; the game world is not used
as a telemetry database.

## Bounded Emission

The mod records transitions, never ticks. The initial delivery observes the
existing idle transition, its recovery, and the controlled failure/abandonment
path. Repeated identical idle states continue using the existing `IdleLog`
transition and quiet-window rules.

`WorkerStrikes.gaveUp` remains the single cross-profession abandonment route.
The telemetry adapter records an `ERROR` followed by `ABANDONED` for that
controlled failure without adding a second strike counter or an unbounded
static worker map. Per-job state stays with the existing job lifecycle and
disappears with it; server shutdown clears the existing transition state.

All eight active professions are covered when their existing work enters the
common idle or abandonment routes. A profession that has no worker or no task
is still observable as `WAITING`; it is not mislabeled as a runtime error.

## Error Semantics

An `ERROR` record is emitted where Village Colony owns the controlled stall
failure path. Its reason is a stable category, not an exception message. Raw
uncaught exceptions from Minecraft, Fabric, the JVM, or other mods are not
attributed to a Village Colony profession.

An `ERROR`, `STALLED`, or repeated `WAITING` count is evidence for later
investigation, not proof of a defect. The report must retain this distinction.

## Offline History and Report

`scripts/analyze_village_log.py` accepts `VC_ACTIVITY` version 1 records and
saves only aggregate session totals in a versioned JSON history. For each
profession/activity/outcome/reason combination it stores an occurrence count.
It continues to deduplicate a reread input log by SHA-256 and keeps the
existing source name, timestamps, byte count, and line count.

The generated Markdown report includes:

- activity counts by profession and work family;
- stalls and errors grouped by declared cause;
- legacy signature counts in a separate compatibility section;
- versioned session history with source-hash deduplication; and
- an explicit note that candidates need log context and a reproducing
  GameTest before behavior changes.

The reader accepts schema 1 history and upgrades it in memory to schema 2
without inventing structured observations for historic free-text logs.

## BigHouseMOD Road-Level Contract

`big_house_mod.nbt` currently has size `7 x 11 x 11`, a 44-block ground/base
row at `y=0`, and the lower half of its entrance door at `y=1`. The asset will:

1. remove every original `y=0` entry;
2. move every remaining entry down one block; and
3. change its template height to `10`.

The result has size `7 x 10 x 11`, its lower door half is at `y=0`, and no
placed template block has a negative relative Y. Beds, chests, doors, walls,
and roof entries are retained and only move down with the house; no furniture
or entrance element is removed. `BigHouseFoundation` keeps using the first air
block above the natural, flat road-level ground as the template origin.
Consequently the door lower half occupies the same walkable level as a normal
road entrance, while the old raised ground/base row is absent.

The change preserves the full horizontal footprint, six beds, six chests,
clear central passage, protected-lot checks, vertical clearance, and
idempotent foundation registration.

## Vanilla Village Bed-Chest Contract

This rule applies once, when the mod creates a new colony from a generated
vanilla-village candidate. It applies to each physical `HOME` POI that belongs
to the accepted candidate cluster and to its original village structure piece.
It does not scan a broad colony radius, does not force-load chunks, and does
not run in recurring worker cycles.

`BigHouseMOD` is expressly excluded. Its existing six chests remain private
foundation storage under ADR-018: this rule neither validates, moves, rotates,
adds to, nor removes any BigHouse chest. Beds created as a last-resort
foundation fallback and later colony-built structures are also out of scope;
the new rule completes only the structures that Minecraft generated as part of
the newly adopted village.

The unit of the rule is an individual bed, not merely a building count. A bed
already paired with a compliant chest is left unchanged. A pre-existing chest
that is not compliant is never moved, rotated, renamed, claimed, or removed.
The mod only creates a single chest in an empty, valid position; otherwise it
does nothing for that bed.

A new or reused chest is compliant only when all of these conditions hold:

1. It is on the same Y level and directly beside one half of the complete bed,
   on a perpendicular side rather than at the bed's head or foot.
2. Its back touches a solid wall block in the same original structure piece,
   its lid has free space, its base has solid support, and it is not joined to
   another chest.
3. The structure piece has exactly one unambiguous usable lower door for that
   bed's room. The inside-front floor cell is determined from the piece bounds
   and the walkable interior, not guessed from a door facing property alone.
4. The chest faces along the same cardinal row or column toward that inside
   door-front cell. It never occupies either immediate approach cell of any
   door, whether inside or outside.
5. The target block is replaceable, is not either half of a bed, and no
   existing player or village block is overwritten.

If a bed, wall, door, interior cell, target orientation, or free target cannot
be proven, the result is `SKIPPED` with one stable diagnostic reason for that
one-time adoption pass. It does not retry every colony cycle and does not
create a chest later merely because a worker happens to need storage.

The accepted candidate keeps the exact bed POIs that formed its cluster, so
the placement pass cannot bleed into a neighboring village. The Fabric bridge
uses the existing village structure-piece bounding boxes to keep each bed,
door, wall, and prospective chest in the same generated structure. The core
model continues to know only neutral positions; Minecraft structure and block
queries remain in `fabric/`.

`ChestScanner` first chooses a compliant adjacent bed chest for a worker's
`HOME`; it may still read an already-existing eligible storage chest but no
longer creates one during a worker cycle. `VillageChests` excludes compliant
bed chests from public construction stock, preserving their intended private
bed-storage role before a worker claims them.

## Verification

Unit tests prove parsing and schema-1 migration in the analyzer. Fabric/GameTests
prove the BigHouse dimensions, removal of the old lower row, the door at
relative `y=0`, six beds/chests, valid strict bed-chest placement, and refusal
of generic worker-cycle placement. The complete suite passed with 418/418
GameTests. A live save playtest remains required to confirm road-level entry,
new-village placement, and the first real telemetry session.

`./gradlew.bat build`, focused unit tests, and
`./gradlew.bat runGametest --rerun-tasks` are required before release. A live
save playtest remains required to confirm road-level entry and to collect the
first real telemetry session.
