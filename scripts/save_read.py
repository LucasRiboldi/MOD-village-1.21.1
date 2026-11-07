"""Lê o grafo de ruas e os blocos do save, sem abrir o Minecraft (feche o jogo antes: o save só grava ao sair).

Uso:
  python scripts/save_read.py graph [--save "Novo mundo"]
      papéis dos nós, entradas com o chão e as obras em volta, grupos de nós colados, quadrados e ramais
  python scripts/save_read.py map x0,x1,z0,z1 [--save "Novo mundo"]
      mapa de cima: H centro, E entrada, J encontro, P ponta, D porta, g portão, f cerca,
      + caminho na linha, - caminho fora da linha, o linha sem caminho, = pedra, ~ água, w plantação, # construção

O save fica em %APPDATA%/.minecraft/saves. Só lê: não altera nada.
"""
import argparse
import collections
import gzip
import os
import struct
import zlib

SOFT = {"air", "cave_air", "short_grass", "tall_grass", "fern", "dandelion", "poppy", "torch", "wall_torch", "?"}
GRAY = {"stone", "cobblestone", "gravel", "andesite", "diorite", "granite", "mossy_cobblestone"}


def parse_nbt(b, i=0):
    """NBT binário para dict e list do Python."""

    def text(i):
        size = struct.unpack(">H", b[i:i + 2])[0]
        return b[i + 2:i + 2 + size].decode("utf8", "replace"), i + 2 + size

    def value(tag, i):
        if tag == 1:
            return b[i], i + 1
        if tag == 2:
            return struct.unpack(">h", b[i:i + 2])[0], i + 2
        if tag == 3:
            return struct.unpack(">i", b[i:i + 4])[0], i + 4
        if tag == 4:
            return struct.unpack(">q", b[i:i + 8])[0], i + 8
        if tag == 5:
            return struct.unpack(">f", b[i:i + 4])[0], i + 4
        if tag == 6:
            return struct.unpack(">d", b[i:i + 8])[0], i + 8
        if tag == 7:
            size = struct.unpack(">i", b[i:i + 4])[0]
            return b[i + 4:i + 4 + size], i + 4 + size
        if tag == 8:
            return text(i)
        if tag == 9:
            inner = b[i]
            size = struct.unpack(">i", b[i + 1:i + 5])[0]
            i += 5
            items = []
            for _ in range(size):
                item, i = value(inner, i)
                items.append(item)
            return items, i
        if tag == 10:
            found = {}
            while True:
                inner = b[i]
                i += 1
                if inner == 0:
                    return found, i
                key, i = text(i)
                found[key], i = value(inner, i)
        if tag == 11:
            size = struct.unpack(">i", b[i:i + 4])[0]
            return struct.unpack(">%di" % size, b[i + 4:i + 4 + 4 * size]), i + 4 + 4 * size
        if tag == 12:
            size = struct.unpack(">i", b[i:i + 4])[0]
            return struct.unpack(">%dq" % size, b[i + 4:i + 4 + 8 * size]), i + 4 + 8 * size
        raise ValueError(tag)

    tag = b[i]
    i += 1
    _, i = text(i)
    return value(tag, i)[0]


class Save:
    def __init__(self, name):
        self.root = os.path.join(os.environ["APPDATA"], ".minecraft", "saves", name)
        self.chunks = {}
        raw = open(os.path.join(self.root, "data", "villagecolony_colonies.dat"), "rb").read()
        try:
            raw = gzip.decompress(raw)
        except OSError:
            pass
        data = parse_nbt(raw)
        self.data = data.get("data", data)

    def chunk(self, cx, cz):
        key = (cx, cz)
        if key not in self.chunks:
            self.chunks[key] = self._read_chunk(cx, cz)
        return self.chunks[key]

    def _read_chunk(self, cx, cz):
        path = os.path.join(self.root, "region", "r.%d.%d.mca" % (cx >> 5, cz >> 5))
        if not os.path.exists(path):
            return None
        d = open(path, "rb").read()
        n = (cx & 31) + (cz & 31) * 32
        offset = int.from_bytes(d[n * 4:n * 4 + 3], "big") * 4096
        if offset == 0:
            return None
        size = int.from_bytes(d[offset:offset + 4], "big")
        return parse_nbt(zlib.decompress(d[offset + 5:offset + 4 + size]))

    def block(self, x, y, z):
        c = self.chunk(x >> 4, z >> 4)
        if c is None:
            return "?"
        for section in c["sections"]:
            if section["Y"] == y >> 4 and "block_states" in section:
                states = section["block_states"]
                palette = states["palette"]
                if "data" not in states:
                    return palette[0]["Name"].replace("minecraft:", "")
                bits = max(4, len(bin(len(palette) - 1)) - 2)
                per_long = 64 // bits
                index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15)
                word = states["data"][index // per_long]
                if word < 0:
                    word += 1 << 64
                entry = (word >> ((index % per_long) * bits)) & ((1 << bits) - 1)
                return palette[entry]["Name"].replace("minecraft:", "")
        return "?"

    def graphs(self):
        return self.data["roadGraphs"]

    def buildings(self):
        return [b for b in self.data.get("buildings", []) if b.get("finished")]


def road_of(graph):
    nodes = {tuple(n["position"]): sorted(n["roles"]) for n in graph["nodes"]}
    line = {}
    for edge in graph["edges"]:
        c = edge["columns"]
        for i in range(0, len(c), 3):
            line[(c[i], c[i + 2])] = c[i + 1]
    return nodes, line


def report_graph(save):
    for colony, graph in save.graphs().items():
        nodes, line = road_of(graph)
        print("colônia", colony[:8], "nós", len(nodes), "arestas", len(graph["edges"]), "colunas da linha", len(line))
        print("papéis", collections.Counter(",".join(r) for r in nodes.values()).most_common())
        print("quadrados na linha:", sorted((x, z) for x, z in line if (x + 1, z) in line and (x, z + 1) in line and (x + 1, z + 1) in line))

        linked = {tuple(e["from"]) for e in graph["edges"]} | {tuple(e["to"]) for e in graph["edges"]}
        print("=== entradas (posição, papéis, chão, obra em volta)")
        for at in sorted(p for p, r in nodes.items() if "ENTRANCE" in r):
            around = []
            for b in save.buildings():
                dx = max(b["minX"] - at[0], 0, at[0] - b["maxX"])
                dz = max(b["minZ"] - at[2], 0, at[2] - b["maxZ"])
                if dx + dz <= 1:
                    around.append(str(b["blueprint"]).split("/")[-1])
            print(at, ",".join(r[0] for r in nodes[at]), save.block(*at), around, "" if at in linked else "SEM ARESTA")

        print("=== grupos de nós a até 3 blocos")
        parent = {p: p for p in nodes}

        def root(p):
            while parent[p] != p:
                parent[p] = parent[parent[p]]
                p = parent[p]
            return p

        for a in nodes:
            for b in nodes:
                if a < b and abs(a[0] - b[0]) + abs(a[2] - b[2]) <= 3:
                    parent[root(a)] = root(b)
        groups = collections.defaultdict(list)
        for p in nodes:
            groups[root(p)].append(p)
        for group in sorted(groups.values(), key=len, reverse=True):
            if len(group) > 1:
                print(len(group), sorted(group))


def report_map(save, x0, x1, z0, z1):
    node = {}
    roads = set()
    for graph in save.graphs().values():
        nodes, line = road_of(graph)
        node.update({(p[0], p[2]): r for p, r in nodes.items()})
        roads.update(line)
    print("x %d a %d, z %d a %d" % (x0, x1, z0, z1))
    for z in range(z0, z1 + 1):
        row = ""
        for x in range(x0, x1 + 1):
            row += glyph(save, x, z, node, roads)
        print("%6d %s" % (z, row))


def glyph(save, x, z, node, roads):
    if (x, z) in node:
        roles = node[(x, z)]
        return "H" if "HUB" in roles else "E" if "ENTRANCE" in roles else "P" if "ENDPOINT" in roles else "J"
    for y in range(120, 40, -1):
        b = save.block(x, y, z)
        if b in SOFT:
            continue
        if b.endswith("_door") and "trap" not in b:
            return "D"
        if "fence_gate" in b:
            return "g"
        if "fence" in b:
            return "f"
        if b == "dirt_path":
            return "+" if (x, z) in roads else "-"
        if b in GRAY:
            return "="
        if "water" in b:
            return "~"
        if b in ("grass_block", "dirt", "sand", "snow_block"):
            return "o" if (x, z) in roads else "."
        if any(w in b for w in ("wheat", "farmland", "carrot", "potato", "beet")):
            return "w"
        if "leaves" in b:
            return "%"
        return "#"
    return " "


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("what", choices=["graph", "map"])
    parser.add_argument("area", nargs="?", help="x0,x1,z0,z1 para o mapa")
    parser.add_argument("--save", default="Novo mundo")
    args = parser.parse_args()
    save = Save(args.save)
    if args.what == "graph":
        report_graph(save)
    else:
        report_map(save, *map(int, args.area.split(",")))


if __name__ == "__main__":
    main()
