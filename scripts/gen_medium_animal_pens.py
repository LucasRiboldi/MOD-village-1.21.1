"""Gera os seis currais médios originais da colônia (Minecraft 1.21.1).

Uso: python scripts/gen_medium_animal_pens.py [--output DIRETORIO]
Dependência: nbtlib.
"""
import argparse
import gzip
from io import BytesIO
from itertools import product
from pathlib import Path

from nbtlib import Compound, File, Int, List, String

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/data/villagecolony/structure/colony"
SIZE = (12, 5, 17)
GATE = (6, 1, 1)
THEMES = {
    "cattle": ("oak", "dandelion"),
    "sheep": ("birch", "azure_bluet"),
    "pig": ("mangrove", "poppy"),
    "poultry": ("spruce", "oxeye_daisy"),
    "horse": ("dark_oak", "cornflower"),
    "camelid": ("acacia", "allium"),
}


def rear_yard(put, theme):
    """Faixa de serviço separada da cerca por uma fileira livre (z=13)."""
    if theme in ("pig", "poultry"):
        # Fonte enterrada e cercada de solo; a horta fica fora do pisoteio do rebanho.
        put(4, 0, 15, "water", level="0")
        crops = ("carrots", "potatoes", "beetroots") if theme == "pig" else ("wheat", "carrots")
        for row, crop in enumerate(crops):
            for x in (1, 2, 3):
                z = 14 + row
                put(x, 0, z, "farmland", moisture="7")
                age = "3" if crop == "beetroots" else "7"
                # Trigo recém-semeado junto ao comedouro de galinhas.
                put(x, 1, z, crop, age="0" if crop == "wheat" and x == 1 else age)
        if theme == "pig":
            for x in (7, 8, 9):
                put(x, 0, 15, "mud")
                put(x, 0, 16, "packed_mud")
        else:
            # Dois ninhos baixos de palha, abertos na frente, com divisória comum.
            for x in (8, 10):
                put(x, 0, 15, "hay_block", axis="y")
                put(x, 1, 16, "spruce_slab", type="bottom")
            for x in (7, 9, 11):
                put(x, 1, 15, "spruce_slab", type="bottom")
        return

    trough, bales = {
        "cattle": ((3, 4, 5), (8, 9)),
        "sheep": ((8, 9), (2,)),
        "horse": ((2, 3, 4, 5), (8, 9)),
        "camelid": ((6, 7), (3, 4)),
    }[theme]
    for x in trough:
        put(x, 1, 15, "water_cauldron", level="3")
    for x in bales:
        put(x, 1, 15, "hay_block", axis="x")
    if theme == "cattle":
        for x in (2, 6):
            put(x, 1, 15, "polished_andesite")
        put(9, 2, 15, "hay_block", axis="z")
    elif theme == "sheep":
        put(3, 1, 15, "loom", facing="north")
        put(4, 1, 15, "white_wool")
        put(4, 1, 16, "light_gray_wool")
    elif theme == "horse":
        for x in (1, 6):
            put(x, 1, 15, "smooth_stone_slab", type="bottom")
        put(8, 1, 16, "dark_oak_fence", east="true")
        put(10, 1, 16, "dark_oak_fence", west="true")
        put(9, 1, 16, "dark_oak_fence", east="true", west="true")
    else:
        for x in (9, 10):
            put(x, 1, 15, "terracotta")
            put(x, 2, 15, "blue_carpet" if x == 9 else "orange_carpet")


def structure(theme):
    wood, flower = THEMES[theme]
    cells = {}

    def put(x, y, z, name, **properties):
        cells[x, y, z] = (name, tuple(sorted(properties.items())))

    for x, z in product(range(12), range(17)):
        put(x, 0, z, "grass_block" if z <= 13 else "coarse_dirt")
    for x in (5, 6, 7):
        put(x, 0, 0, "dirt_path")
    # Conexões horizontais explícitas também funcionam ao colocar o template inteiro.
    ring = {(x, z) for x, z in product(range(12), range(1, 13))
            if x in (0, 11) or z in (1, 12)}
    for x, z in sorted(ring):
        connections = {side: "true" for side, neighbor in (
            ("west", (x-1, z)), ("east", (x+1, z)),
            ("north", (x, z-1)), ("south", (x, z+1))) if neighbor in ring}
        put(x, 1, z, wood + "_fence", **connections)
    put(*GATE, wood + "_fence_gate", facing="north", open="false", powered="false", in_wall="false")

    for x, z in ((0, 4), (11, 4), (0, 10), (11, 10), (4, 1), (7, 12), (5, 16)):
        if z == 16:
            put(x, 1, z, wood + "_fence")
        put(x, 2, z, wood + "_fence")
        put(x, 3, z, "lantern", hanging="false")

    for name, patch in (
        ("short_grass", ((2, 4), (3, 4), (4, 7), (7, 8), (8, 8), (9, 5))),
        ("tall_grass", ((3, 5), (8, 9))),
        ("fern", ((2, 8), (3, 9), (7, 4))),
        (flower, ((4, 4), (5, 8), (8, 5), (9, 9))),
    ):
        for x, z in patch:
            if name == "tall_grass":
                put(x, 1, z, name, half="lower")
                put(x, 2, z, name, half="upper")
            else:
                put(x, 1, z, name)
    rear_yard(put, theme)

    # O leitor de blueprint descarta AIR. No NBT, a colocação direta também
    # precisa limpar o portão, o pasto e a faixa de separação até a altura do modelo.
    palette, indices, blocks = [], {}, []
    for y, x, z in product(range(SIZE[1]), range(SIZE[0]), range(SIZE[2])):
        state = cells.get((x, y, z), ("air", ()))
        if state not in indices:
            name, props = state
            entry = Compound({"Name": String("minecraft:" + name)})
            if props:
                entry["Properties"] = Compound({k: String(v) for k, v in props})
            indices[state] = len(palette)
            palette.append(entry)
        blocks.append(Compound({"pos": List[Int]([x, y, z]), "state": Int(indices[state])}))
    return File({"DataVersion": Int(3955), "size": List[Int](SIZE),
                 "palette": List[Compound](palette), "blocks": List[Compound](blocks),
                 "entities": List[Compound]([])})


def generate(folder):
    folder.mkdir(parents=True, exist_ok=True)
    for theme in THEMES:
        raw = BytesIO()
        structure(theme).write(raw)
        path = folder / f"medium_animal_pen_{theme}.nbt"
        # Cabeçalho sem nome de arquivo, sistema operacional ou horário local.
        with path.open("wb") as output:
            with gzip.GzipFile(filename="", fileobj=output, mode="wb", mtime=0) as stream:
                stream.write(raw.getvalue())


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=OUT)
    generate(parser.parse_args().output)
