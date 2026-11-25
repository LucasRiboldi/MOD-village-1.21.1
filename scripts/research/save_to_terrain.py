"""Converte o save em uma foto do terreno (ADR-064), para medir jogos de antes da foto existir.

Lê o topo de cada coluna (mapa de alturas MOTION_BLOCKING_NO_LEAVES) do save, sem abrir o jogo. Não sabe as caixas das
estruturas nem as portas: os telhados contam como chão, então o acesso a pé sai aproximado.

Uso: python scripts/research/save_to_terrain.py <pasta region> <minX> <minZ> <maxX> <maxZ> <centroX> <centroY> <centroZ>
     <colônia8> <saída.json.gz>
"""
import gzip
import io
import json
import math
import struct
import sys
import zlib

import nbtlib

NATURAL = {"sand", "red_sand", "sandstone", "red_sandstone", "stone", "dirt", "coarse_dirt", "grass_block", "podzol",
           "gravel", "clay", "terracotta", "andesite", "granite", "diorite", "deepslate", "tuff", "snow_block", "mud"}
PATH = {"dirt_path", "villagecolony:sand_path"}
MARGIN = 16


def chunk(region, cx, cz):
    with open(f"{region}/r.{cx >> 5}.{cz >> 5}.mca", "rb") as handle:
        handle.seek(((cx & 31) + (cz & 31) * 32) * 4)
        entry = handle.read(4)
        offset = (entry[0] << 16 | entry[1] << 8 | entry[2]) * 4096
        if offset == 0:
            return None
        handle.seek(offset)
        length, _ = struct.unpack(">IB", handle.read(5))
        return nbtlib.File.parse(io.BytesIO(zlib.decompress(handle.read(length - 1))))


def unpack(longs, bits, count):
    per, mask, out = 64 // bits, (1 << bits) - 1, []
    for value in longs:
        value = int(value) & 0xFFFFFFFFFFFFFFFF
        for k in range(per):
            out.append((value >> (k * bits)) & mask)
            if len(out) == count:
                return out
    return out


def main(region, x0, z0, x1, z1, cx, cy, cz, colony, out):
    min_x, min_z = x0 - MARGIN, z0 - MARGIN
    width, depth = x1 - x0 + 1 + 2 * MARGIN, z1 - z0 + 1 + 2 * MARGIN
    tops = {}
    for chunk_x in range(min_x >> 4, ((min_x + width - 1) >> 4) + 1):
        for chunk_z in range(min_z >> 4, ((min_z + depth - 1) >> 4) + 1):
            data = chunk(region, chunk_x, chunk_z)
            if data is None:
                continue
            heights = unpack(data["Heightmaps"]["MOTION_BLOCKING_NO_LEAVES"], 9, 256)
            sections = {int(s["Y"]): s for s in data["sections"] if "block_states" in s}
            cache = {}
            for lz in range(16):
                for lx in range(16):
                    y = heights[lz * 16 + lx] - 64 - 1
                    section = sections.get(y >> 4)
                    name = "?"
                    if section is not None:
                        palette = [str(p["Name"]).replace("minecraft:", "") for p in section["block_states"]["palette"]]
                        if "data" not in section["block_states"]:
                            name = palette[0]
                        else:
                            if y >> 4 not in cache:
                                bits = max(4, math.ceil(math.log2(len(palette))))
                                cache[y >> 4] = unpack(section["block_states"]["data"], bits, 4096)
                            name = palette[cache[y >> 4][((y & 15) * 16 + lz) * 16 + lx]]
                    tops[(chunk_x * 16 + lx, chunk_z * 16 + lz)] = (y, name)
    ys, kinds = [], []
    for dx in range(width):
        for dz in range(depth):
            y, name = tops.get((min_x + dx, min_z + dz), (-32768, "?"))
            ys.append(y)
            kinds.append(0 if name == "?" else 3 if name == "water" else 2 if name in PATH else 1 if name in NATURAL else 5)
    photo = {"version": 1, "label": "save", "day": 0, "colony": colony, "minX": min_x, "minZ": min_z, "width": width,
             "depth": depth, "center": [cx, cy, cz], "box": [x0, 0, z0, x1, 0, z1], "y": ys, "kind": kinds,
             "edit": [0] * len(ys), "target": [-32768] * len(ys), "structures": [], "doors": [], "mines": []}
    with gzip.open(out, "wt", encoding="utf-8") as handle:
        json.dump(photo, handle)
    print(out, width * depth, "colunas")


if __name__ == "__main__":
    a = sys.argv[1:]
    main(a[0], *map(int, a[1:8]), a[8], a[9])
