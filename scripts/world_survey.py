#!/usr/bin/env python3
"""Mede as vilas de um save: tamanho, camas, sinos e distância entre vizinhas.

Lê o mundo fechado, sem abrir o jogo — 2026-09-30, pedido do autor para
decidir sobre a identidade da vila com números e não com estimativa:

- as vilas que o jogo gerou (``structures/starts`` dos arquivos de região),
  com a caixa 3D somada de todas as peças;
- as camas (POI ``minecraft:home``) e os sinos (``minecraft:meeting``) que o
  jogo registrou — só existem em chunk que já foi carregado;
- as colônias e construções do mod (``data/villagecolony_colonies.dat``).

Uso:
    python scripts/world_survey.py
    python scripts/world_survey.py "%APPDATA%/.minecraft/saves/Novo mundo"

Precisa de ``nbtlib`` (``pip install nbtlib``). Só lê; não grava nada no save.
"""

from __future__ import annotations

import argparse
import glob
import gzip
import io
import math
import os
import struct
import zlib

import nbtlib

DEFAULT_WORLD = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "saves", "Novo mundo")


def chunks(path):
    """Os chunks de um arquivo de região, descomprimidos."""
    with open(path, "rb") as handle:
        data = handle.read()
    if len(data) < 8192:
        return
    for index in range(1024):
        sector = struct.unpack(">I", data[index * 4:index * 4 + 4])[0] >> 8
        if sector == 0:
            continue
        start = sector * 4096
        length = struct.unpack(">I", data[start:start + 4])[0]
        compression = data[start + 4]
        raw = data[start + 5:start + 4 + length]
        if compression == 2:
            yield zlib.decompress(raw)
        elif compression == 1:
            yield gzip.decompress(raw)


def generated_villages(world):
    villages = []
    for path in glob.glob(os.path.join(world, "region", "*.mca")):
        for raw in chunks(path):
            if b"village_" not in raw:
                continue
            starts = nbtlib.File.parse(io.BytesIO(raw)).get("structures", {}).get("starts", {})
            for name, start in starts.items():
                children = start.get("Children", []) if "village" in name else []
                if not children:
                    continue
                boxes = [list(map(int, child["BB"])) for child in children]
                houses = sum(1 for child in children
                             if "/houses/" in str(child.get("pool_element", {}).get("location", "")))
                villages.append({
                    "type": str(name).split(":")[-1].replace("village_", ""),
                    "min": [min(box[i] for box in boxes) for i in range(3)],
                    "max": [max(box[i + 3] for box in boxes) for i in range(3)],
                    "pieces": len(children),
                    "houses": houses,
                })
    return villages


def points_of_interest(world):
    homes, bells = [], []
    for path in glob.glob(os.path.join(world, "poi", "*.mca")):
        for raw in chunks(path):
            if b"minecraft:home" not in raw and b"minecraft:meeting" not in raw:
                continue
            for section in nbtlib.File.parse(io.BytesIO(raw)).get("Sections", {}).values():
                for record in section.get("Records", []):
                    kind, pos = str(record["type"]), list(map(int, record["pos"]))
                    if kind == "minecraft:home":
                        homes.append(pos)
                    elif kind == "minecraft:meeting":
                        bells.append(pos)
    return homes, bells


def uuid_of(ints):
    text = "".join("%08x" % (int(part) & 0xFFFFFFFF) for part in ints)
    return "-".join((text[:8], text[8:12], text[12:16], text[16:20], text[20:]))


def colonies(world):
    path = os.path.join(world, "data", "villagecolony_colonies.dat")
    if not os.path.exists(path):
        return [], []
    data = nbtlib.load(path)["data"]
    found = [{"id": uuid_of(c["id"]),
              "center": [int(c["centerX"]), int(c["centerY"]), int(c["centerZ"])],
              "beds": int(c["observedBeds"])} for c in data["colonies"]]
    buildings = [{"colony": uuid_of(b["colonyId"]), "blueprint": str(b["blueprint"]),
                  "min": [int(b["minX"]), int(b["minY"]), int(b["minZ"])],
                  "max": [int(b["maxX"]), int(b["maxY"]), int(b["maxZ"])]} for b in data["buildings"]]
    return found, buildings


def inside(point, box):
    return all(box["min"][i] <= point[i] <= box["max"][i] for i in range(3))


def centre(box):
    return [(box["min"][i] + box["max"][i]) // 2 for i in range(3)]


def horizontal(a, b):
    return math.hypot(a[0] - b[0], a[2] - b[2])


def gap(a, b):
    """Vão horizontal entre duas caixas; zero quando se alcançam."""
    dx = max(0, b["min"][0] - a["max"][0], a["min"][0] - b["max"][0])
    dz = max(0, b["min"][2] - a["max"][2], a["min"][2] - b["max"][2])
    return math.hypot(dx, dz)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("world", nargs="?", default=DEFAULT_WORLD)
    world = parser.parse_args().world

    villages = generated_villages(world)
    homes, bells = points_of_interest(world)
    known, buildings = colonies(world)
    villages.sort(key=lambda v: (centre(v)[2], centre(v)[0]))

    print("%d vilas geradas, %d camas, %d sinos, %d colônias, %d construções do mod\n"
          % (len(villages), len(homes), len(bells), len(known), len(buildings)))
    print("#   centro (x, y, z)        X x Y x Z      casas camas sinos  vizinha (centro / vão)  colônias")
    for number, village in enumerate(villages, 1):
        village["n"] = number
    for village in villages:
        size = [village["max"][i] - village["min"][i] + 1 for i in range(3)]
        beds = sum(1 for home in homes if inside(home, village))
        rings = sum(1 for bell in bells if inside(bell, village))
        others = [v for v in villages if v is not village]
        near = min(others, key=lambda v: horizontal(centre(village), centre(v))) if others else None
        owners = [c["id"][:8] for c in known if inside(c["center"], village)]
        neighbour = ("#%d %.0f / %.0f" % (near["n"], horizontal(centre(village), centre(near)),
                                         gap(village, near))) if near else "-"
        print("%-3d %-22s %4dx%3dx%4d   %4d %5d %5d   %-22s  %s" % (
            village["n"], centre(village), *size, village["houses"], beds, rings,
            neighbour, ",".join(owners)))

    if buildings:
        print("\nConstruções do mod por vila gerada:")
        for building in buildings:
            hits = [v["n"] for v in villages if inside(centre(building), v)]
            print("  %s %-44s %s -> %s" % (building["colony"][:8], building["blueprint"],
                                          centre(building), hits or "fora de vila gerada"))


if __name__ == "__main__":
    main()
