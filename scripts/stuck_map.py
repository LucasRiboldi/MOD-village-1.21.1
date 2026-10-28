#!/usr/bin/env python3
"""O mapa dos lugares que prendem aldeões — decisão do autor, 2026-10-28 (T3 do diagnóstico da 0.3.103).

Junta os teleportes para a cama (``VC_EVENT`` com ``type=teleport``, posição ``from``) e os encalhes do diário
de ações (``action=STRANDED``, com ``x, y, z``), agrupa em células de ``--cell`` blocos e:

- imprime os lugares que mais prendem, com quantos casos, as profissões e os motivos;
- com ``--region``, desenha os pontos sobre o mapa da vila apagado (``village_map.py``): vermelho = teleporte,
  laranja = encalhe, o tamanho cresce com a contagem.

Uso:
    python scripts/stuck_map.py LOG [DIARIO.jsonl ...] [--region DIR --center X,Z --radius 96 --out mapa.png]
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import pathlib
import re
from collections import Counter, defaultdict

from PIL import ImageDraw

TELEPORT = "teleporte"
STRANDED = "encalhe"
POSITION = re.compile(r"(-?\d+), (-?\d+), (-?\d+)")


def teleports_from(lines):
    """(x, z, quem, motivo) de cada teleporte do diário da vila."""
    found = []

    for line in lines:
        if "VC_EVENT " not in line:
            continue

        try:
            event = json.loads(line.split("VC_EVENT ", 1)[1])
        except ValueError:
            continue

        position = POSITION.search(str(event.get("from", "")))

        if event.get("type") == "teleport" and position:
            found.append((int(position.group(1)), int(position.group(3)), TELEPORT, str(event.get("why", ""))))

    return found


def strandings_from(lines):
    """(x, z, profissão, motivo) de cada encalhe do diário de ações."""
    found = []

    for line in lines:
        try:
            action = json.loads(line)
        except ValueError:
            continue

        if action.get("action") == "STRANDED" and "x" in action and "z" in action:
            found.append((int(action["x"]), int(action["z"]), action.get("prof", "?"), action.get("detail", "")))

    return found


def spots(cases, cell=4):
    """As células com casos, da que mais prende para a que menos: ((cx, cz), casos)."""
    grouped = defaultdict(list)

    for case in cases:
        grouped[(case[0] // cell * cell, case[1] // cell * cell)].append(case)

    return sorted(grouped.items(), key=lambda item: -len(item[1]))


def describe(spot, cell):
    (x, z), cases = spot
    who = Counter(case[2] for case in cases)
    why = Counter(case[3] for case in cases if case[3])

    return (f"{len(cases):4}  ({x}..{x + cell - 1}, {z}..{z + cell - 1})  "
            + ", ".join(f"{name} {count}" for name, count in who.most_common(3))
            + ("  |  " + "; ".join(f"{reason} {count}" for reason, count in why.most_common(2)) if why else ""))


def draw(region, center, radius, scale, teleports, strandings, out):
    spec = importlib.util.spec_from_file_location(
        "village_map", pathlib.Path(__file__).resolve().parent / "village_map.py")
    village_map = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(village_map)

    cx, cz = center
    image = village_map.paint(village_map.surface(village_map.load(region, cx, cz, radius), cx, cz, radius),
                              scale, dim=True)
    paint = ImageDraw.Draw(image)

    for cases, color in ((strandings, (240, 140, 30)), (teleports, (220, 30, 30))):
        for (x, z), group in spots(cases, 2):
            px, pz = (x - cx + radius) * scale, (z - cz + radius) * scale
            size = 2 + min(10, 2 * len(group))
            paint.ellipse([px - size, pz - size, px + size, pz + size], fill=color, outline=(0, 0, 0))

    image.save(out)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("log", help="latest.log com as linhas VC_EVENT")
    parser.add_argument("journals", nargs="*", help="diários de ações (villagecolony-actions-*.jsonl)")
    parser.add_argument("--cell", type=int, default=4)
    parser.add_argument("--top", type=int, default=15)
    parser.add_argument("--region")
    parser.add_argument("--center")
    parser.add_argument("--radius", type=int, default=96)
    parser.add_argument("--scale", type=int, default=4)
    parser.add_argument("--out", default="stuck_map.png")
    args = parser.parse_args(argv)

    with open(args.log, encoding="utf-8", errors="replace") as source:
        teleports = teleports_from(source)

    strandings = []

    for journal in args.journals:
        with open(journal, encoding="utf-8", errors="replace") as source:
            strandings.extend(strandings_from(source))

    print(f"{len(teleports)} teleportes, {len(strandings)} encalhes")
    print("Lugares que mais prendem (casos, célula, quem, motivo):")

    for spot in spots(teleports + strandings, args.cell)[:args.top]:
        print(describe(spot, args.cell))

    if args.region and args.center:
        center = tuple(int(v) for v in args.center.split(","))
        draw(args.region, center, args.radius, args.scale, teleports, strandings, args.out)
        print(f"mapa: {args.out}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
