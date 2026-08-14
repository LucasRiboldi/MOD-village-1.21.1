<div align="center">

<img src="src/main/resources/assets/villagecolony/icon.png" width="180" alt="Village Colony">

# Village Colony

### Your villages stop waiting for you.

*A Fabric mod that turns vanilla villages into colonies that work, produce and grow on their own.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Environment](https://img.shields.io/badge/Side-Server%20%7C%20Singleplayer-lightgrey)
![Version](https://img.shields.io/badge/Version-0.1.0%20alpha-orange)
![License](https://img.shields.io/badge/License-MIT-informational)

</div>

---

## What it does

You find a plains village. You walk away.

When you come back, someone has been chopping wood. The logs are in a chest
marked with an axe. Someone else turned them into planks. And where there was
grass beside the road, there is a house that wasn't there before.

Nobody told them to. **You never opened a single menu.**

---

## What your villagers do

🪓 **The lumberjack** walks to a tree, fells it one block at a time — at the
speed of a player with an iron axe — carries nothing home because the wood goes
straight into his chest, and replants the sapling before he leaves.

🪚 **The manufacturer** takes logs out of the chest, turns them into planks
using the game's own recipe, and puts them back. He stops when half the
colony's storage is planks, so the lumberjack always has somewhere to put more.

🏠 **The builder** reads a real vanilla village house out of the game files and
raises it, one block per second, beside an existing road. Every block is paid
for out of the colony's chests first — **the colony never conjures materials**.

🌾 **The farmer** has a name, a hoe and a chest — and no work yet.

Each of them gets a name over their head and a picture frame nailed to their
chest, so you can tell at a glance who is who.

---

## The rules it plays by

**Vanilla first.** The villagers are ordinary villagers. The chests are ordinary
chests. The recipes are the game's recipes, asked at runtime — not copied into
the mod. The house is literally the same file the world generator uses.

**Nothing is invented.** No virtual inventory, no abstract resource counter, no
shadow economy. If the colony has 40 planks, there are 40 planks in a chest you
can walk up to and open. Take them, and the colony notices.

**Your build is safe.** The only thing a worker ever breaks is a tree, and it
has to prove the tree is a tree: a trunk with no living leaves above it counts
as a building, not a forest. Generated village pieces are asked about directly
and left alone.

**It stops on its own.** Harvesting ends when the chests are full and starts
again when you take something out. Nothing grows without limit.

---

## The loop

```text
   village found  →  villagers hired  →  wood cut  →  planks made  →  house built
        ↑                                                                  │
        └──────────────────  the new house has beds  ←─────────────────────┘
```

---

## Installation

**Requirements**

| | |
|---|---|
| Minecraft | 1.21.1 (Java Edition) |
| Loader | Fabric |
| Dependency | Fabric API |

**Steps**

1. Install [Fabric Loader](https://fabricmc.net/use/) for 1.21.1.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods` folder.
3. Drop `village-colony-0.1.0.jar` in beside it.
4. Launch, load a world, and find a plains village.

Works in singleplayer and on a dedicated server. Clients don't need the mod
installed to join a server that has it.

**Where to look**

The colony reports what it is doing in the server log. Villagers only work
during their vanilla work hours — `/time set noon` if you don't want to wait,
and note that `/time set day` is *before* the work window opens.

---

## Development status

> **This is an alpha, and honest about it.**

The mod builds, loads, and runs on a client and on a dedicated server.

| Feature | State |
|---|---|
| Village detection, stable colony identity | ✅ verified in game |
| Workers, professions, tools, chest ownership | ✅ verified in game |
| Resource counting, deficits, task assignment | ✅ verified in game |
| Wood harvesting and replanting | ✅ verified in game |
| Manufacturing — logs into planks | 🧪 covered by tests, not yet seen in game |
| Construction — houses and site selection | 🧪 covered by tests, not yet seen in game |
| Building registry and protection | 🧪 covered by tests, not yet seen in game |
| Farming, mining, blacksmith, defence | ⬜ not started |

**Known limits right now.** The colony only produces planks — a vanilla house
also wants cobblestone, glass and beds, and those have to already be in the
chests. Houses go up beside roads that already exist; the colony does not lay
new road yet. Doors and beds are placed as loose halves, so the house is a
little rough.

```text
366 unit tests  ·  76 in-game tests  ·  ./gradlew build
```

The always-current status lives in
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---

## Building from source

```bash
git clone https://github.com/LucasRiboldi/MOD-village-1.21.1.git
cd MOD-village-1.21.1
./gradlew build
```

The jar lands in `build/libs/`. Needs a JDK 21.

```bash
./gradlew runGametest    # the in-game test battery, headless
./gradlew runServer      # a dedicated server with the mod loaded
```

---

## For developers

The mod is split so that the colony's brain never touches Minecraft:

```text
core/     what a colony is and how it decides — no Minecraft types at all
fabric/   the border: adapters, world scanning, block placement, mixins
data/     persistence
```

Every architectural decision is written down and dated in
[`docs/decisions/`](docs/decisions), from why villages are detected by
clustering beds instead of asking for structures, to why the mixin surface is
one method.

The design documents come first in this project and the code follows them.
Where the two disagree, the disagreement is recorded rather than hidden — see
the "ressalvas" sections of
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

**Contributing:** read the architecture docs first, keep the core free of
Minecraft imports, and add a test at the boundary — every serious defect in
this project's history lived there.

---

## License

MIT — see [LICENSE](LICENSE).
