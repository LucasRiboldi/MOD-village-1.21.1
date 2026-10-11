# Construction Material Flow Specification

## Goal

Make an open construction site drive a physical material flow: workers gather,
produce, and deliver construction material before the mod creates a missing
piece as a terminal fallback.

## Constraints

- Fabric Minecraft 1.21.1 and Java 21; `core/` remains independent of Minecraft.
- The active construction site has priority over surplus production, while a
  worker with another useful construction input may keep working.
- A fallback remains mandatory after four recorded, unsuccessful physical
  attempts. It is deposited in the builder-serving chest and emits
  `VC_SUPPLY_ERROR`.
- Natural blocks must never be considered unobtainable merely because they do
  not have a `ResourceType`; their physical collection route must be attempted.
- A smelter must not repeatedly reserve and release the same task when neither
  input nor fuel is available.
- Every placed construction piece must be attributable to existing stock,
  profession delivery, profession production, or terminal fallback.

## Design

`SupplyRoutes` becomes the single route explanation. It classifies direct
physical sources using vanilla block tags and the existing biome reach, then
recursively resolves crafting and smelting inputs. `ConstructionSupplyLedger`
creates a per-colony, ephemeral snapshot from `ConstructionDemand` and current
chest contents. It exposes the missing items, their route tree, and their
dispatch status; it does not create a second inventory or persist Minecraft
world state.

`WorkMaterials`, `CraftingWork`, mining/surface collection, and `SmelterWork`
consume that snapshot only after existing work is considered. A route is marked
delivered only when an item reaches a colony chest that serves the construction.
The four-attempt fallback counter is reset by that delivery, never by a task
reservation.

`ConstructionSupplyTelemetry` writes compact action events for source,
delivery, fallback, and placement. The analysis script aggregates those events
so a save playtest can report exact physical versus fallback counts.

## Success Criteria

- Calcite, leaves, logs, grass, and other tagged natural construction inputs
  receive a physical route instead of immediate fallback.
- A missing raw input holds or backs off the smelter task rather than producing
  release/reacquire churn.
- A stalled mining approach releases the claim within the existing lease bound;
  the current intermediate-descent behavior remains covered.
- A test world shows a construction demand routed to a worker chest before any
  fallback; the fallback still occurs after four failed, evidenced attempts.
- A save playtest can report fallback count, profession-sourced count, and each
  profession's idle/wait/block ratios from the same log interval.
