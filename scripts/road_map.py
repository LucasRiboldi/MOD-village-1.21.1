#!/usr/bin/env python3
"""Desenha só as ruas e os caminhos da vila, as ligações entre eles e o que a colônia criou — pedido do
autor, 2026-10-28.

Lê os arquivos de região como o ``village_map.py``, mas pela altura sem folhas
(``MOTION_BLOCKING_NO_LEAVES``): a copa das árvores não esconde o caminho embaixo dela. Uma coluna é rua
quando o chão é caminho de terra ou de areia; o cascalho, a terra grossa e o arenito só contam quando
encostam num caminho e não existiam no dia 0 (são a borda gasta da rua da colônia, Regra 138).

As ligações: duas colunas vizinhas (lado com lado, degrau de até 1) são a mesma rede. A maior rede é a
principal; as outras são pedaços soltos, que nenhum aldeão alcança pela rua.

Cores: cinza = rua que já existia no dia 0; laranja = rua que a colônia fez; vermelho = pedaço solto,
não ligado à rede principal (número ao lado).

Uso:
    python scripts/road_map.py REGION_FIM --before REGION_DIA0 --center X,Z --radius 96 --out ruas.png
"""

from __future__ import annotations

import argparse
import importlib.util
import os
import pathlib

from PIL import Image, ImageDraw, ImageFont

SPEC = importlib.util.spec_from_file_location(
    "village_map", pathlib.Path(__file__).resolve().parent / "village_map.py")
village_map = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(village_map)

PATHS = {"dirt_path", "sand_path"}
EDGES = {"gravel", "coarse_dirt", "sandstone", "smooth_sandstone", "packed_mud"}

OLD = (150, 150, 150)
NEW = (240, 130, 30)
LOOSE = (215, 40, 40)
BACKGROUND = (250, 248, 242)
LEGEND = 96

# A rua do jogo é pontilhada (caminho, grama e terra misturados): uma falha de até um bloco ainda liga.
REACH = 2
STEP = 2


def font(size):
    """Uma fonte com acento: a do Windows, ou a padrão do Pillow."""
    for name in ("arial.ttf", "segoeui.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def ground(region: str, cx: int, cz: int, radius: int):
    """A coluna → (y, bloco do chão), pela altura sem folhas, pulando planta rasteira."""
    chunks = village_map.load(region, cx, cz, radius)
    size = radius * 2 + 1
    grid = [[None] * size for _ in range(size)]

    for dz in range(size):
        for dx in range(size):
            x, z = cx - radius + dx, cz - radius + dz
            chunk = chunks.get((x >> 4, z >> 4))

            if chunk is None or chunk.ground_heights is None:
                continue

            y = chunk.ground_heights[(z & 15) * 16 + (x & 15)] + village_map.MIN_Y - 1

            for _ in range(3):
                name = chunk.block(x & 15, y, z & 15).split(":")[-1]

                if name.endswith("air") or "grass" in name and name != "grass_block" or "flower" in name \
                        or name in ("fern", "large_fern", "snow", "torch", "colony_torch"):
                    y -= 1
                    continue

                grid[dz][dx] = (y, name)
                break

    return grid


def streets(after, before):
    """As colunas de rua no fim, cada uma marcada se é nova (não era rua no dia 0)."""
    size = len(after)
    marked = {}

    for z in range(size):
        for x in range(size):
            cell = after[z][x]

            if cell is not None and cell[1] in PATHS:
                old = before is not None and before[z][x] is not None and before[z][x][1] in PATHS
                marked[(x, z)] = (cell[0], not old)

    # A borda gasta: material de borda, novo, encostado numa rua.
    for z in range(size):
        for x in range(size):
            cell = after[z][x]

            if cell is None or cell[1] not in EDGES or (x, z) in marked:
                continue

            was = before[z][x] if before is not None else None

            if was is not None and was[1] == cell[1]:
                continue

            if any((x + dx, z + dz) in marked for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                marked[(x, z)] = (cell[0], True)

    return marked


def networks(marked):
    """As redes ligadas: a até REACH blocos (a falha da rua pontilhada) e degrau de até STEP; maior primeiro."""
    seen = set()
    found = []

    for start in marked:
        if start in seen:
            continue

        stack = [start]
        seen.add(start)
        group = []

        while stack:
            cell = stack.pop()
            group.append(cell)
            y = marked[cell][0]

            for dx in range(-REACH, REACH + 1):
                for dz in range(-REACH, REACH + 1):
                    near = (cell[0] + dx, cell[1] + dz)

                    if near in marked and near not in seen and abs(marked[near][0] - y) <= STEP:
                        seen.add(near)
                        stack.append(near)

        found.append(group)

    return sorted(found, key=len, reverse=True)


def draw(marked, groups, size, scale, label):
    image = Image.new("RGB", (size * scale, size * scale + LEGEND), BACKGROUND)
    paint = ImageDraw.Draw(image)
    small = font(13)

    main = set(groups[0]) if groups else set()

    for index, group in enumerate(groups):
        for x, z in group:
            new = marked[(x, z)][1]
            color = (NEW if new else OLD) if (x, z) in main else LOOSE
            paint.rectangle([x * scale, z * scale, x * scale + scale - 1, z * scale + scale - 1], fill=color)

        if index > 0 and len(group) >= 3:
            x, z = min(group)
            paint.text((x * scale + scale + 2, z * scale - 4), str(index), fill=LOOSE, font=small)

    middle = size // 2 * scale + scale // 2
    paint.ellipse([middle - 6, middle - 6, middle + 6, middle + 6], outline=(30, 30, 30), width=2)

    old = sum(1 for cell in marked.values() if not cell[1])
    new = len(marked) - old
    loose = sum(len(group) for group in groups[1:])
    top = size * scale + 6

    paint.text((8, top), label, fill=(20, 20, 20), font=small)
    for row, (color, text) in enumerate([
            (OLD, f"rua que já existia no dia 0: {old} colunas"),
            (NEW, f"rua que a colônia fez: {new} colunas"),
            (LOOSE, f"pedaço solto, fora da rede principal: {len(groups) - 1 if groups else 0} pedaços,"
                    f" {loose} colunas")]):
        y = top + 20 + row * 20
        paint.rectangle([8, y, 22, y + 12], fill=color)
        paint.text((30, y), text, fill=(20, 20, 20), font=small)
    paint.text((size * scale - 200, top + 20), f"rede principal: {len(groups[0]) if groups else 0} colunas",
               fill=(20, 20, 20), font=small)
    paint.text((size * scale - 200, top + 40), "círculo: centro da vila", fill=(20, 20, 20), font=small)

    return image


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("region")
    parser.add_argument("--before", help="regiões do dia 0: separa a rua velha da nova")
    parser.add_argument("--center", required=True)
    parser.add_argument("--radius", type=int, default=96)
    parser.add_argument("--scale", type=int, default=4)
    parser.add_argument("--label", default="Ruas e caminhos")
    parser.add_argument("--out", required=True)
    args = parser.parse_args(argv)

    cx, cz = (int(v) for v in args.center.split(","))
    after = ground(args.region, cx, cz, args.radius)
    before = ground(args.before, cx, cz, args.radius) if args.before else None
    marked = streets(after, before)
    groups = networks(marked)

    draw(marked, groups, args.radius * 2 + 1, args.scale, args.label).save(args.out)
    print(f"{args.out}: {len(marked)} colunas de rua, {len(groups)} redes"
          f" (principal {len(groups[0]) if groups else 0}; soltas: {[len(g) for g in groups[1:12]]})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
