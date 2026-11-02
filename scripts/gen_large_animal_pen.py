"""Gera o curral grande único da vila: 15x15, capacidade 20, água, cultivos e tochas.

Uso: python scripts/gen_large_animal_pen.py
Requer nbtlib. Atualiza somente colony/large_animal_pen.nbt.
"""

from pathlib import Path

from nbtlib import Compound, File, Int, List, String

DATA_VERSION = 3955
SIZE = (15, 3, 15)
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/villagecolony/structure/colony/large_animal_pen.nbt"


def add(blocks, x, y, z, name, properties=None):
    blocks.append((x, y, z, name, properties or {}))


def structure():
    blocks = []
    center = SIZE[0] // 2
    crops = [(center + dx, center + dz) for dx in range(-2, 3) for dz in range(-2, 3)
             if (dx, dz) != (0, 0)][:12]

    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            add(blocks, x, 0, z, "grass_block")
            if x in (0, SIZE[0] - 1) or z in (0, SIZE[2] - 1):
                if (x, z) == (center, 0):
                    add(blocks, x, 1, z, "oak_fence_gate", {"facing": "south", "in_wall": "false", "open": "false", "powered": "false"})
                else:
                    add(blocks, x, 1, z, "oak_fence")

    blocks = [entry for entry in blocks if entry[:4] not in {(center, 0, center, "grass_block")}
              and entry[:4] not in {(x, 0, z, "grass_block") for x, z in crops}]
    add(blocks, center, 0, center, "water", {"level": "0"})
    for x, z in crops:
        add(blocks, x, 0, z, "farmland", {"moisture": "7"})
        add(blocks, x, 1, z, "wheat", {"age": "7"})
    for x, z in ((0, 0), (0, 14), (14, 0), (14, 14)):
        add(blocks, x, 2, z, "torch")

    palette, index, encoded = [], {}, []
    for x, y, z, name, props in blocks:
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            state = {"Name": String("minecraft:" + name)}
            if props:
                state["Properties"] = Compound({key: String(value) for key, value in props.items()})
            palette.append(Compound(state))
        encoded.append(Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}))

    return File({"DataVersion": Int(DATA_VERSION), "size": List[Int]([Int(n) for n in SIZE]),
                 "palette": List[Compound](palette), "blocks": List[Compound](encoded),
                 "entities": List[Compound]([])}, gzipped=True)


if __name__ == "__main__":
    OUT.parent.mkdir(parents=True, exist_ok=True)
    structure().save(OUT)
    print(OUT)
