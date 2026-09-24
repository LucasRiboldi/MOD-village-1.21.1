# BigHouseMOD Design

**Status:** approved in chat on 2026-09-20

## Goal

Every adopted village receives one mod-owned `BigHouseMOD` built from the
Vanilla plains big-house layout. The Vanilla structure remains untouched.

## Contract

- `BigHouseMOD` is a mod resource under `villagecolony:houses/big_house_mod`.
- Its shell/layout comes from `minecraft:village/plains/houses/plains_big_house_1`.
- Mod furniture and decorations are removed from the blueprint.
- The interior contains exactly six mod beds and six mod chests, with a
  clear central passage and a usable entrance.
- The six foundation villagers are spawned or moved into this house and
  receive unique `HOME` beds and registered storage chests.
- Farmer and carpenter remain valid growth professions, but their furniture
  sets are intentionally outside this foundation house.
- A village gets one house only; reloads and repeated detection are idempotent.
- Existing Vanilla structures and player blocks are never overwritten.

## Integration

`BigHouseFoundation` owns placement and registration. It chooses a safe lot
with the existing build-site rules, places the structure only after the full
 footprint is clear, registers a finished `Building`, then places the six
 beds/chests and runs the existing villager scanner. The foundation fallback
remains available only while the house cannot yet be placed.

## Verification

Unit tests cover the blueprint contract and idempotence. A Fabric GameTest
covers one house per colony, 6 beds, 6 chests, 6 foundation roles, clear door
and corridor blocks, and preservation of a protected block outside the lot.
The live save still requires a playtest after the JAR is installed.
