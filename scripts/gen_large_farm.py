"""Gera a plantação grande do mod: 16x16, borda de tora como a roça da planície, terra arada toda molhada.

Regra 241 (autor, 2026-11-11). A base é a roça da vila da planície (``village/plains/houses/plains_large_farm_1``):
borda de tora de carvalho e terra arada por dentro. Aqui o miolo é 14x14 e a água fica em quatro buracos, em
(4, 4), (4, 11), (11, 4) e (11, 11): a água molha a terra arada a até 4 blocos, e cada coluna do miolo fica a no
máximo 4 de um buraco, então toda terra arada fica molhada.

Uso: python scripts/gen_large_farm.py
Requer nbtlib. Atualiza somente colony/large_farm.nbt.
"""

from pathlib import Path

from nbtlib import Compound, File, Int, List, String

DATA_VERSION = 3955
SIZE = (16, 2, 16)
WATER = {(4, 4), (4, 11), (11, 4), (11, 11)}
HYDRATION = 4
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/villagecolony/structure/colony/large_farm.nbt"


def blocks():
    placed = []
    last = SIZE[0] - 1

    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            if x in (0, last) and z in (0, last):
                placed.append((x, 0, z, "oak_log", {"axis": "y"}))
            elif z in (0, last):
                placed.append((x, 0, z, "oak_log", {"axis": "x"}))
            elif x in (0, last):
                placed.append((x, 0, z, "oak_log", {"axis": "z"}))
            elif (x, z) in WATER:
                placed.append((x, 0, z, "water", {"level": "0"}))
            else:
                placed.append((x, 0, z, "farmland", {"moisture": "7"}))

    return placed


def check(placed):
    """Toda terra arada a até 4 blocos de água, na conta do jogo (quadrado em volta)."""
    for x, _, z, name, _ in placed:
        if name == "farmland":
            assert any(abs(x - wx) <= HYDRATION and abs(z - wz) <= HYDRATION for wx, wz in WATER), (x, z)


def structure(placed):
    palette, index, encoded = [], {}, []

    for x, y, z, name, props in placed:
        key = (name, tuple(sorted(props.items())))

        if key not in index:
            index[key] = len(palette)
            state = {"Name": String("minecraft:" + name)}

            if props:
                state["Properties"] = Compound({k: String(v) for k, v in props.items()})

            palette.append(Compound(state))

        encoded.append(Compound({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}))

    return File({"DataVersion": Int(DATA_VERSION), "size": List[Int]([Int(n) for n in SIZE]),
                 "palette": List[Compound](palette), "blocks": List[Compound](encoded),
                 "entities": List[Compound]([])}, gzipped=True)


if __name__ == "__main__":
    placed = blocks()
    check(placed)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    structure(placed).save(OUT)
    print(OUT, sum(1 for entry in placed if entry[3] == "farmland"), "terras aradas")
