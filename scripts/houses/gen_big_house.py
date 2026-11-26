"""Gera a casa grande do mod, ``houses/big_house_mod.nbt`` — Regras 109, 35 e 369.

Pedido do autor (2026-11-26, H5 do jogo da 0.4.143): "2 blocos maior de profundidade, reorganizar as camas e baús, para
todos baú estarem encostados em uma parede e ter acesso de trânsito dentro da casa, adicionar tochas dentro da casa,
chão de pedra no primeiro andar, tapar os buracos do segundo andar"; e "não colocar corrimão".

Planta 9 × 12 × 11 (x, y, z): paredes em x=1, x=7, z=1 e z=9; miolo x=2..6, z=2..8. Camada 0 é o piso, de tijolo
de pedra; 1 a 3 o térreo, 4 o piso de cima, 5 e 6 o andar de cima, 7 a 11 o telhado. A casa grande é posta como está,
sem a troca de blocos da Regra 262. Sete camas e sete baús, cada baú encostado em parede; a cama e o baú do
carpinteiro à direita da porta (Regra 35). A escada tem o piso de cima aberto sobre os três degraus.

Caminho misto (desenho de 2026-11-26, seção 3): escreve em ``scripts/houses/out/`` e só grava nos recursos com
``--overwrite big_house_mod``; o arquivo dos recursos, se o autor o salvar à mão, passa a ser a fonte.

Uso: ``python -P scripts/houses/gen_big_house.py [--overwrite big_house_mod]`` (precisa de ``nbtlib``).
"""
import importlib.util
import pathlib
import sys

from nbtlib import Compound, File, Int, List, String

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
OUT = ROOT / "scripts/houses/out"
RESOURCES = ROOT / "src/main/resources/data/villagecolony/structure/houses"
NAME = "big_house_mod"
DATA_VERSION = 3955  # 1.21.1
SIZE = (9, 12, 11)
WEST, EAST, NORTH, SOUTH = 1, 7, 1, 9
DOOR = (1, 1, 5)

PANE_Z = {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"}
PANE_X = {"north": "false", "south": "false", "east": "true", "west": "true", "waterlogged": "false"}

# (pé, cabeceira, facing da cama, baú, facing do baú)
BEDS = [
    ((2, 1, 3), (2, 1, 2), "north", (2, 1, 4), "east"),   # A
    ((2, 1, 7), (2, 1, 8), "south", (2, 1, 6), "east"),   # carpinteiro, à direita da porta (Regra 35)
    ((6, 1, 7), (6, 1, 8), "south", (5, 1, 8), "west"),   # B
    ((6, 1, 3), (6, 1, 2), "north", (5, 1, 2), "west"),   # C
    ((2, 5, 3), (2, 5, 2), "north", (2, 5, 4), "east"),   # D
    ((2, 5, 7), (2, 5, 8), "south", (2, 5, 6), "east"),   # E
    ((4, 5, 8), (5, 5, 8), "east", (6, 5, 8), "north"),   # F: a frente oeste é a cama, e z cresce para o sul
]

STAIRS = [(6, 1, 6), (6, 2, 5), (6, 3, 4)]
# O vão do piso de cima sobre os três degraus: quem sobe precisa de 3 livres acima de cada um.
STAIR_OPENING = {(6, 4, 4), (6, 4, 5), (6, 4, 6)}
TORCHES = [((3, 3, 2), "south"), ((3, 3, 8), "north"), ((3, 6, 2), "south"), ((3, 6, 8), "north"),
           ((0, 3, 5), "west")]


def _rules():
    spec = importlib.util.spec_from_file_location("house_rules", ROOT / "scripts/houses/house_rules.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _perimeter(x, z):
    return WEST <= x <= EAST and NORTH <= z <= SOUTH and (x in (WEST, EAST) or z in (NORTH, SOUTH))


def _interior(x, z):
    return WEST < x < EAST and NORTH < z < SOUTH


def _corner(x, z):
    return x in (WEST, EAST) and z in (NORTH, SOUTH)


def _window(x, y, z):
    """Vidraça na camada do meio de cada andar: nas paredes compridas em z=3 e z=7, nas curtas no meio."""
    return y in (2, 5) and ((x in (WEST, EAST) and z in (3, 7)) or (z in (NORTH, SOUTH) and x == 4))


def blocks():
    """{(x, y, z): (nome, propriedades)} da casa."""
    placed = {}
    log = ("oak_log", {"axis": "y"})

    for x in range(SIZE[0]):
        for z in range(SIZE[2]):
            # Piso de pedra no térreo; as paredes assentam em pedregulho.
            if _interior(x, z):
                placed[(x, 0, z)] = ("stone_bricks", {})
                placed[(x, 4, z)] = ("oak_planks", {})
            elif _perimeter(x, z):
                placed[(x, 0, z)] = ("cobblestone", {})
                placed[(x, 4, z)] = log

            if _perimeter(x, z):
                for y in (1, 2, 3, 5, 6):
                    upper = y >= 5
                    if _corner(x, z) or (x == EAST and z == 5):
                        placed[(x, y, z)] = log
                    elif _window(x, y, z):
                        placed[(x, y, z)] = ("glass_pane", PANE_Z if x in (WEST, EAST) else PANE_X)
                    else:
                        placed[(x, y, z)] = ("oak_planks" if upper else "cobblestone", {})

            # Telhado em degraus, da beirada (x=0 e x=8) até a cumeeira (x=4), com as empenas em z=1 e z=9.
            for k in range(5):
                y = 7 + k
                if x in (k, SIZE[0] - 1 - k) or (k == 0 and x in (1, SIZE[0] - 2)):
                    placed[(x, y, z)] = ("oak_planks", {})
                elif z in (NORTH, SOUTH) and k < x < SIZE[0] - 1 - k:
                    placed[(x, y, z)] = ("oak_planks", {})

    for opening in STAIR_OPENING:
        placed.pop(opening)

    door = {"facing": "east", "hinge": "left", "open": "false", "powered": "false"}
    placed[DOOR] = ("oak_door", dict(door, half="lower"))
    placed[(DOOR[0], DOOR[1] + 1, DOOR[2])] = ("oak_door", dict(door, half="upper"))

    for step in STAIRS:
        placed[step] = ("cobblestone_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                               "waterlogged": "false"})

    for foot, head, facing, chest, chest_facing in BEDS:
        placed[foot] = ("white_bed", {"facing": facing, "part": "foot", "occupied": "false"})
        placed[head] = ("white_bed", {"facing": facing, "part": "head", "occupied": "false"})
        placed[chest] = ("chest", {"facing": chest_facing, "type": "single", "waterlogged": "false"})

    for at, facing in TORCHES:
        placed[at] = ("wall_torch", {"facing": facing})

    return placed


def structure(placed):
    palette, index, entries = [], {}, []

    for (x, y, z), (name, props) in sorted(placed.items()):
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {"Name": String("minecraft:" + name)}
            if props:
                entry["Properties"] = Compound({k: String(v) for k, v in props.items()})
            palette.append(Compound(entry))
        block = {"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}
        if name == "chest":
            block["nbt"] = Compound({"Items": List[Compound]([]), "id": String("minecraft:chest")})
        entries.append(Compound(block))

    return File({
        "DataVersion": Int(DATA_VERSION),
        "size": List[Int]([Int(n) for n in SIZE]),
        "palette": List[Compound](palette),
        "blocks": List[Compound](entries),
        "entities": List[Compound]([]),
    }, gzipped=True)


def main(argv):
    placed = blocks()
    rules = _rules()
    found = rules.problems(rules.House(SIZE, placed))

    if found:
        print("a casa não passa nas regras de casa:", *found, sep="\n  ")
        return 1

    OUT.mkdir(parents=True, exist_ok=True)
    structure(placed).save(OUT / f"{NAME}.nbt")
    target = RESOURCES / f"{NAME}.nbt"

    if "--overwrite" in argv and NAME in argv:
        structure(placed).save(target)
        print(f"gravado em {target}")
    elif not target.exists():
        structure(placed).save(target)
        print(f"gravado em {target} (não existia)")
    else:
        print(f"{target} já existe e fica como está; use --overwrite {NAME}")

    print(f"{NAME}: {len(placed)} blocos")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
