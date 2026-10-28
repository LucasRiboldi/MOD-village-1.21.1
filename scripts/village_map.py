#!/usr/bin/env python3
"""Desenha a vila vista de cima a partir dos arquivos de região — pedido do autor, 2026-10-28.

Lê ``r.X.Z.mca`` (só os chunks completos), pega o bloco do topo de cada coluna pelo mapa de alturas
``WORLD_SURFACE`` e pinta com a cor do bloco, sombreada pelo desnível com o vizinho do norte, como o
mapa do jogo. Com ``--before``, desenha também a diferença: as colunas cujo topo mudou ficam em cor
viva sobre o mapa do fim, apagado.

Uso:
    python scripts/village_map.py REGION_DIR --center X,Z --radius 96 --out mapa.png
    python scripts/village_map.py REGION_FIM --before REGION_DIA0 --center X,Z --out diff.png

Precisa de ``nbtlib`` e ``Pillow``. Só lê.
"""

from __future__ import annotations

import argparse
import io
import math
import os
import struct
import zlib

import nbtlib
from PIL import Image, ImageDraw

MIN_Y = -64

# Cor por nome exato; o resto cai nas famílias de PALAVRAS, e por fim no cinza.
COLORS = {
    "grass_block": (95, 159, 53), "dirt": (134, 96, 67), "coarse_dirt": (119, 85, 59),
    "dirt_path": (178, 150, 92), "farmland": (110, 74, 44), "gravel": (136, 126, 126),
    "sand": (219, 207, 163), "red_sand": (190, 102, 33), "sandstone": (216, 203, 155),
    "smooth_sandstone": (223, 214, 170), "cut_sandstone": (218, 206, 160), "water": (52, 90, 200),
    "lava": (207, 92, 15), "snow": (249, 254, 254), "snow_block": (249, 254, 254), "ice": (145, 183, 253),
    "stone": (125, 125, 125), "cobblestone": (110, 110, 110), "mossy_cobblestone": (100, 118, 90),
    "stone_bricks": (122, 121, 122), "andesite": (136, 136, 137), "diorite": (188, 188, 188),
    "granite": (149, 103, 85), "deepslate": (80, 80, 82), "clay": (160, 166, 179),
    "packed_mud": (142, 106, 79), "mud": (60, 57, 60), "podzol": (91, 63, 24),
    "mycelium": (111, 99, 105), "moss_block": (89, 109, 45), "hay_block": (166, 136, 38),
    "wheat": (220, 187, 70), "carrots": (230, 140, 30), "potatoes": (200, 180, 80),
    "beetroots": (160, 40, 50), "short_grass": (95, 159, 53), "tall_grass": (95, 159, 53),
    "fern": (80, 140, 50), "bell": (250, 210, 60), "chest": (163, 116, 46), "barrel": (140, 100, 55),
    "torch": (255, 200, 80), "lantern": (255, 200, 80), "glass": (200, 230, 240),
    "glass_pane": (200, 230, 240), "white_wool": (234, 236, 236), "composter": (110, 80, 40),
    "bricks": (150, 90, 75), "terracotta": (152, 94, 67), "white_terracotta": (209, 178, 161),
    "obsidian": (20, 18, 30), "bedrock": (60, 60, 60), "pumpkin": (198, 118, 24),
    "melon": (111, 145, 30), "cactus": (88, 129, 40), "sugar_cane": (148, 192, 101),
}

WORDS = [
    ("leaves", (60, 110, 40)), ("log", (102, 81, 50)), ("wood", (102, 81, 50)), ("planks", (162, 130, 78)),
    ("stairs", (150, 120, 75)), ("slab", (150, 120, 75)), ("fence", (140, 110, 70)), ("door", (130, 100, 60)),
    ("bed", (180, 40, 40)), ("wool", (220, 220, 220)), ("carpet", (200, 200, 200)), ("terracotta", (160, 100, 75)),
    ("concrete", (180, 180, 180)), ("flower", (230, 80, 120)), ("tulip", (230, 80, 120)), ("poppy", (220, 40, 40)),
    ("dandelion", (250, 220, 40)), ("sapling", (70, 140, 40)), ("bush", (70, 120, 40)), ("ore", (150, 140, 130)),
    ("_wall", (115, 115, 115)), ("rail", (150, 130, 110)), ("vine", (60, 110, 40)), ("lily", (40, 120, 40)),
    ("table", (140, 105, 70)), ("furnace", (100, 100, 100)), ("smoker", (100, 100, 100)), ("anvil", (70, 70, 70)),
    ("villagecolony", (240, 120, 30)),
]

# O mapa de alturas guarda o primeiro ar acima do topo, contado de MIN_Y.
HEIGHT_BITS = 9


def color_of(name: str) -> tuple[int, int, int]:
    short = name.split(":", 1)[-1]

    if short in COLORS:
        return COLORS[short]

    if name.startswith("villagecolony:"):
        return (240, 120, 30)

    for word, rgb in WORDS:
        if word in short:
            return rgb

    return (128, 128, 128)


def region_chunks(path):
    with open(path, "rb") as source:
        data = source.read()

    for index in range(1024):
        entry = struct.unpack(">I", data[index * 4:index * 4 + 4])[0]
        offset, sectors = entry >> 8, entry & 0xFF

        if offset == 0 or sectors == 0:
            continue

        start = offset * 4096
        length = struct.unpack(">I", data[start:start + 4])[0]
        compression = data[start + 4]
        raw = data[start + 5:start + 4 + length]

        if compression == 2:
            yield zlib.decompress(raw)


def unpack(longs, bits: int, count: int) -> list[int]:
    per_long = 64 // bits
    mask = (1 << bits) - 1
    values = []

    for value in longs:
        value = int(value) & 0xFFFFFFFFFFFFFFFF

        for slot in range(per_long):
            values.append((value >> (slot * bits)) & mask)

            if len(values) == count:
                return values

    return values


class Chunk:
    def __init__(self, tag):
        self.x = int(tag["xPos"])
        self.z = int(tag["zPos"])
        self.heights = unpack(tag["Heightmaps"]["WORLD_SURFACE"], HEIGHT_BITS, 256)
        self.sections = {}

        for section in tag.get("sections", []):
            states = section.get("block_states")

            if states is None:
                continue

            palette = [str(entry["Name"]) for entry in states["palette"]]
            self.sections[int(section["Y"])] = (palette, states.get("data"))

    def block(self, lx: int, y: int, lz: int) -> str:
        found = self.sections.get(y >> 4)

        if found is None:
            return "minecraft:air"

        palette, data = found

        if data is None or len(palette) == 1:
            return palette[0]

        bits = max(4, math.ceil(math.log2(len(palette))))
        per_long = 64 // bits
        index = ((y & 15) * 256) + (lz * 16) + lx
        value = int(data[index // per_long]) & 0xFFFFFFFFFFFFFFFF

        return palette[(value >> ((index % per_long) * bits)) & ((1 << bits) - 1)]

    def top(self, lx: int, lz: int) -> tuple[int, str]:
        y = self.heights[lz * 16 + lx] + MIN_Y - 1

        # Pula a vegetação rasteira que esconde o chão (grama alta, flor).
        for _ in range(3):
            name = self.block(lx, y, lz)

            if name.endswith("air"):
                y -= 1
                continue

            return y, name

        return y, self.block(lx, y, lz)


def load(region_dir: str, cx: int, cz: int, radius: int) -> dict:
    chunks = {}
    wanted = range((cx - radius) >> 4, ((cx + radius) >> 4) + 1), range((cz - radius) >> 4, ((cz + radius) >> 4) + 1)

    for file in os.listdir(region_dir):
        parts = file.split(".")

        if len(parts) != 4 or parts[3] != "mca" or os.path.getsize(os.path.join(region_dir, file)) < 8192:
            continue

        # Uma região tem 32 x 32 chunks: só as que cobrem a área.
        rx, rz = int(parts[1]), int(parts[2])

        if not (rx * 32 <= wanted[0][-1] and wanted[0][0] < rx * 32 + 32
                and rz * 32 <= wanted[1][-1] and wanted[1][0] < rz * 32 + 32):
            continue

        for raw in region_chunks(os.path.join(region_dir, file)):
            tag = nbtlib.File.parse(io.BytesIO(raw))

            if not str(tag.get("Status", "")).endswith("full"):
                continue

            if int(tag["xPos"]) in wanted[0] and int(tag["zPos"]) in wanted[1]:
                chunk = Chunk(tag)
                chunks[(chunk.x, chunk.z)] = chunk

    return chunks


def surface(chunks: dict, cx: int, cz: int, radius: int):
    size = radius * 2 + 1
    grid = [[None] * size for _ in range(size)]

    for dz in range(size):
        for dx in range(size):
            x, z = cx - radius + dx, cz - radius + dz
            chunk = chunks.get((x >> 4, z >> 4))

            if chunk is not None:
                grid[dz][dx] = chunk.top(x & 15, z & 15)

    return grid


def shade(rgb, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def paint(grid, scale: int, dim: bool = False, changed=None) -> Image.Image:
    size = len(grid)
    image = Image.new("RGB", (size * scale, size * scale), (0, 0, 0))
    draw = ImageDraw.Draw(image)

    for z in range(size):
        for x in range(size):
            cell = grid[z][x]

            if cell is None:
                continue

            y, name = cell
            rgb = color_of(name)
            north = grid[z - 1][x] if z > 0 else None

            if north is not None and not name.endswith("water"):
                rgb = shade(rgb, 1.12 if y > north[0] else 0.82 if y < north[0] else 1.0)

            if dim:
                rgb = tuple(int(c * 0.45 + 60) for c in rgb)

            if changed is not None and changed[z][x]:
                rgb = color_of(name) if not name.endswith("air") else (255, 0, 255)
                draw.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1],
                               fill=rgb, outline=(255, 0, 255))
                continue

            draw.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1], fill=rgb)

    return image


def mark_center(image: Image.Image, radius: int, scale: int, label: str) -> Image.Image:
    draw = ImageDraw.Draw(image)
    c = radius * scale + scale // 2
    draw.ellipse([c - 6, c - 6, c + 6, c + 6], outline=(255, 255, 255), width=2)
    draw.rectangle([0, 0, image.width, 18], fill=(0, 0, 0))
    draw.text((4, 3), label, fill=(255, 255, 255))

    return image


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("region")
    parser.add_argument("--center", required=True, help="X,Z do centro")
    parser.add_argument("--radius", type=int, default=96)
    parser.add_argument("--scale", type=int, default=4)
    parser.add_argument("--before", help="pasta de região do início: desenha só o que mudou")
    parser.add_argument("--label", default="")
    parser.add_argument("--out", required=True)
    args = parser.parse_args(argv)

    cx, cz = (int(v) for v in args.center.split(","))
    after = surface(load(args.region, cx, cz, args.radius), cx, cz, args.radius)

    if args.before:
        before = surface(load(args.before, cx, cz, args.radius), cx, cz, args.radius)
        changed = [[a is not None and b is not None and a != b for a, b in zip(row_a, row_b)]
                   for row_a, row_b in zip(after, before)]
        count = sum(sum(row) for row in changed)
        swaps: dict[str, int] = {}

        for row_a, row_b, row_c in zip(after, before, changed):
            for a, b, moved in zip(row_a, row_b, row_c):
                if moved:
                    height = " (subiu)" if a[0] > b[0] else " (desceu)" if a[0] < b[0] else ""
                    key = f"{b[1].split(':')[-1]} -> {a[1].split(':')[-1]}{height}"
                    swaps[key] = swaps.get(key, 0) + 1

        for key, many in sorted(swaps.items(), key=lambda item: -item[1])[:40]:
            print(f"{many:6}  {key}")

        image = paint(after, args.scale, dim=True, changed=changed)
        label = f"{args.label} - {count} colunas mudaram (realce)"
    else:
        image = paint(after, args.scale)
        label = args.label

    mark_center(image, args.radius, args.scale, label).save(args.out)
    print(f"{args.out}: {image.width}x{image.height}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
