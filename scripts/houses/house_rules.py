"""As regras de casa do mod — Regra 369 (autor 2026-11-26, H5 do jogo da 0.4.143).

Toda casa do mod tem cama, baú e luz por dentro; todo baú fica encostado numa parede, com a frente livre; da porta se
chega a pé a cada cama e à frente de cada baú (degrau de até 1, 2 blocos livres de altura, cama e baú não se pisam); o
piso de cada andar é inteiro, a não ser o vão logo acima de uma escada; e nenhum posto de trabalho do Minecraft fica
dentro, que viraria profissão do jogo num aldeão comum da vila.

Uso: ``problems(load(caminho))`` devolve a lista de problemas; vazia, a casa passou.
"""
from collections import deque

VANILLA_JOB_BLOCKS = frozenset({
    "barrel", "composter", "smoker", "blast_furnace", "stonecutter", "loom", "fletching_table", "cartography_table",
    "lectern", "cauldron", "water_cauldron", "lava_cauldron", "powder_snow_cauldron", "smithing_table", "grindstone",
    "brewing_stand", "bell",
})

LIGHTS = frozenset({"torch", "wall_torch", "lantern", "colony_wall_torch", "colony_torch"})

SIDES = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}

# Até quantos blocos abaixo de um vão do piso a escada o justifica.
STAIR_BELOW = 2


class House:
    """A planta: tamanho (x, y, z) e os blocos por posição; o que falta é ``void``, que a obra deixa como está."""

    def __init__(self, size, blocks):
        self.size = tuple(size)
        self.blocks = dict(blocks)

    def block(self, x, y, z):
        return self.blocks.get((x, y, z), ("void", {}))

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]


def load(path):
    """Lê o ``.nbt`` de estrutura: os nomes sem ``minecraft:``."""
    import nbtlib

    data = nbtlib.load(path)
    palette = [(str(entry["Name"]).split(":", 1)[-1], {str(k): str(v) for k, v in entry.get("Properties", {}).items()})
               for entry in data["palette"]]
    blocks = {}
    for entry in data["blocks"]:
        x, y, z = (int(v) for v in entry["pos"])
        name, props = palette[int(entry["state"])]
        if name != "structure_void":
            blocks[(x, y, z)] = (name, props)
    return House([int(v) for v in data["size"]], blocks)


def passable(name):
    """Sem corpo para quem anda: ar, porta, tocha, tapete, placa."""
    return (name in ("air", "void", "cave_air") or name.endswith("_door") or name in LIGHTS
            or name.endswith("_carpet") or "sign" in name)


def is_wall(name):
    """Bloco cheio de parede: nem móvel, nem degrau, nem passagem."""
    return not passable(name) and not name.endswith("_bed") and name not in ("chest", "trapped_chest") \
        and not name.endswith("_stairs") and not name.endswith("_slab") and not name.endswith("_fence") \
        and not name.endswith("_fence_gate") and name not in LIGHTS


def _floor(house, x, y, z):
    """Se dá para pisar em cima do bloco (x, y, z): abaixo da planta é o chão do mundo."""
    if y < 0:
        return True
    name, _ = house.block(x, y, z)
    return not passable(name) and not name.endswith("_bed") and name not in ("chest", "trapped_chest") \
        and not name.endswith("_fence") and not name.endswith("_fence_gate") and not name.endswith("_wall")


def standable(house, x, y, z):
    return house.inside(x, y, z) and _floor(house, x, y - 1, z) and passable(house.block(x, y, z)[0]) \
        and passable(house.block(x, y + 1, z)[0])


def reached(house):
    """As células de pé alcançadas a partir da metade de baixo de cada porta."""
    starts = [pos for pos, (name, props) in house.blocks.items()
              if name.endswith("_door") and props.get("half", "lower") == "lower"]
    seen = set(starts)
    queue = deque(starts)
    while queue:
        x, y, z = queue.popleft()
        for dx, dz in SIDES.values():
            for dy in (0, 1, -1):
                nxt = (x + dx, y + dy, z + dz)
                if nxt in seen or not standable(house, *nxt):
                    continue
                # Subir um bloco é pulo, e o pulo pede 3 livres em cima; na escada sobe-se de meio em meio, sem pulo.
                if dy == 1 and not passable(house.block(x, y + 2, z)[0]) \
                        and not house.block(nxt[0], nxt[1] - 1, nxt[2])[0].endswith("_stairs"):
                    continue
                seen.add(nxt)
                queue.append(nxt)
    return seen


def enclosed(house, x, y, z):
    """Se há parede nas quatro direções, na mesma altura: a posição é de dentro da casa."""
    for dx, dz in SIDES.values():
        cx, cz = x + dx, z + dz
        while house.inside(cx, y, cz) and not is_wall(house.block(cx, y, cz)[0]):
            cx, cz = cx + dx, cz + dz
        if not house.inside(cx, y, cz):
            return False
    return True


def problems(house, require_sign=False):
    found = []
    names = {name for name, _ in house.blocks.values()}
    beds = [pos for pos, (name, _) in house.blocks.items() if name.endswith("_bed")]
    chests = [(pos, props) for pos, (name, props) in house.blocks.items() if name == "chest"]

    if not beds:
        found.append("sem cama")
    if not chests:
        found.append("sem baú")
    if not any(name in LIGHTS and enclosed(house, *pos) for pos, (name, _) in house.blocks.items()):
        found.append("sem luz")
    if require_sign and not any("sign" in name for name in names):
        found.append("sem placa")
    for name in sorted(names & VANILLA_JOB_BLOCKS):
        found.append(f"posto de trabalho do jogo {name}")

    walk = reached(house)

    for parts in _whole_beds(house, beds):
        if not any((x + dx, y, z + dz) in walk for (x, y, z) in parts for dx, dz in SIDES.values()):
            x, y, z = parts[0]
            found.append(f"cama fora do caminho {x},{y},{z}")

    for (x, y, z), props in sorted(chests):
        if not any(is_wall(house.block(x + dx, y, z + dz)[0]) for dx, dz in SIDES.values()):
            found.append(f"baú solto {x},{y},{z}")
        dx, dz = SIDES.get(props.get("facing", "north"), (0, -1))
        if (x + dx, y, z + dz) not in walk:
            found.append(f"baú sem frente {x},{y},{z}")

    found.extend(_holes(house))
    return found


def _whole_beds(house, beds):
    """Cada cama inteira, pelo pé: a cabeceira fica um bloco na direção do ``facing``. Metade sem par conta sozinha."""
    left = set(beds)
    whole = []
    for pos in sorted(beds):
        name, props = house.block(*pos)
        if pos not in left or props.get("part") != "foot":
            continue
        dx, dz = SIDES.get(props.get("facing", "north"), (0, -1))
        head = (pos[0] + dx, pos[1], pos[2] + dz)
        parts = [pos] + ([head] if head in left else [])
        left.difference_update(parts)
        whole.append(parts)
    whole.extend([pos] for pos in sorted(left))
    return whole


def _holes(house):
    """Vão no piso de um andar: debaixo de uma posição de dentro de um andar com cama, baú ou porta."""
    levels = {y for (x, y, z), (name, props) in house.blocks.items()
              if name.endswith("_bed") or name == "chest"
              or (name.endswith("_door") and props.get("half", "lower") == "lower")}
    holes = []
    for level in sorted(levels):
        if level == 0:
            continue
        for x in range(house.size[0]):
            for z in range(house.size[2]):
                name, _ = house.block(x, level, z)
                if is_wall(name) or not enclosed(house, x, level, z):
                    continue
                below, _ = house.block(x, level - 1, z)
                if not passable(below):
                    continue
                if any(house.block(x, level - 1 - d, z)[0].endswith("_stairs") for d in range(1, STAIR_BELOW + 1)):
                    continue
                holes.append(f"buraco no piso {x},{level - 1},{z}")
    return holes
