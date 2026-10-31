"""Gera o armazém pequeno da colônia, ``storage_small.nbt`` — Regra 165.

Galpão de 7×7 por 5 de altura, um por estilo, com os materiais do ``storage_majest`` do mesmo
estilo: chão de pedra, cantos de tronco, paredes de tábua, porta ao sul, teto de laje, uma tocha
na parede norte e oito baús (quatro em cada parede lateral, de frente para o corredor).

Uso: ``python scripts/gen_storage_small.py`` (precisa de ``nbtlib``). Escreve em
``src/main/resources/data/villagecolony/structure/colony/`` a versão sem estilo e uma por estilo.
"""

from pathlib import Path

from nbtlib import Compound, File, Int, List, String

DATA_VERSION = 3955  # 1.21.1
SIZE = (7, 5, 7)
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/villagecolony/structure/colony"

# estilo → (chão, canto, parede, laje do teto, porta)
STYLES = {
    "plains": ("cobblestone", "oak_log", "oak_planks", "oak_slab", "oak_door"),
    "desert": ("sandstone", "cut_sandstone", "sandstone", "sandstone_slab", "oak_door"),
    "savanna": ("cobblestone", "acacia_log", "acacia_planks", "acacia_slab", "acacia_door"),
    "snowy": ("stone_bricks", "spruce_log", "spruce_planks", "spruce_slab", "spruce_door"),
    "taiga": ("cobblestone", "spruce_log", "spruce_planks", "spruce_slab", "spruce_door"),
}


def blocks_for(style):
    """[(x, y, z, nome, propriedades, é baú)] do galpão neste estilo."""
    floor, corner, wall, slab, door = STYLES[style]
    width, height, depth = SIZE
    door_x = width // 2
    placed = []

    for x in range(width):
        for z in range(depth):
            placed.append((x, 0, z, floor, {}, False))
            placed.append((x, height - 1, z, slab, {"type": "bottom", "waterlogged": "false"}, False))

            edge_x = x in (0, width - 1)
            edge_z = z in (0, depth - 1)

            for y in range(1, height - 1):
                if edge_x and edge_z:
                    props = {"axis": "y"} if corner.endswith("_log") else {}
                    placed.append((x, y, z, corner, props, False))
                elif x == door_x and z == depth - 1 and y <= 2:
                    half = "lower" if y == 1 else "upper"
                    placed.append((x, y, z, door, {"facing": "north", "half": half, "hinge": "left",
                                                    "open": "false", "powered": "false"}, False))
                elif edge_x or edge_z:
                    placed.append((x, y, z, wall, {}, False))
                elif x == 1 and 1 <= z <= 4 and y == 1:
                    placed.append((x, y, z, "chest", {"facing": "east", "type": "single",
                                                      "waterlogged": "false"}, True))
                elif x == width - 2 and 1 <= z <= 4 and y == 1:
                    placed.append((x, y, z, "chest", {"facing": "west", "type": "single",
                                                      "waterlogged": "false"}, True))
                elif x == door_x and z == 1 and y == 2:
                    placed.append((x, y, z, "wall_torch", {"facing": "south"}, False))
                else:
                    placed.append((x, y, z, "air", {}, False))

    return placed


def structure(style):
    palette, index, blocks = [], {}, []

    for x, y, z, name, props, chest in blocks_for(style):
        key = (name, tuple(sorted(props.items())))

        if key not in index:
            index[key] = len(palette)
            entry = {"Name": String("minecraft:" + name)}
            if props:
                entry["Properties"] = Compound({k: String(v) for k, v in props.items()})
            palette.append(Compound(entry))

        block = {"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}
        if chest:
            block["nbt"] = Compound({"Items": List[Compound]([]), "id": String("minecraft:chest")})
        blocks.append(Compound(block))

    return File({
        "DataVersion": Int(DATA_VERSION),
        "size": List[Int]([Int(n) for n in SIZE]),
        "palette": List[Compound](palette),
        "blocks": List[Compound](blocks),
        "entities": List[Compound]([]),
    }, gzipped=True)


def main():
    structure("plains").save(OUT / "storage_small.nbt")
    for style in STYLES:
        structure(style).save(OUT / style / "storage_small.nbt")
    print("storage_small.nbt: 1 + %d estilos em %s" % (len(STYLES), OUT))


if __name__ == "__main__":
    main()
