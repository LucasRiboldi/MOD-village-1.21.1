#!/usr/bin/env python3
"""O solo em volta da vila — pedido do autor, 2026-10-29 ("testes em arenas de 100 por 100 em todos os biomas").

Lê as regiões do mundo (como ``road_map.py``, pela altura sem folhas) numa janela de ``--half`` × 2 blocos em volta
de ``--center`` e mede, só entre colunas de chão natural (grama, terra, areia, pedra, cascalho, caminho, neve...):

- a fração de pares de colunas vizinhas com degrau maior que 1 (a Regra 152 quer zero dentro da vila);
- o maior degrau;
- as colunas em buraco: 3 ou mais abaixo das quatro vizinhas;
- os paredões: pares de colunas vizinhas com degrau de 4 ou mais (o "quebrou a montanha no meio" do jogo da 0.3.108);
- o que ficou no ar: colunas com chão natural sobre um vão perto do topo, e troncos sem nada embaixo.

Uso:
    python scripts/ground_report.py REGION_DIR --center X,Z [--half 50]
"""

from __future__ import annotations

import argparse
import importlib.util
import pathlib

NATURAL = {"grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium", "sand", "red_sand", "gravel",
           "stone", "andesite", "diorite", "granite", "tuff", "deepslate", "dirt_path", "farmland", "snow_block",
           "powder_snow", "mud", "clay", "terracotta", "packed_ice", "ice", "calcite", "moss_block", "sand_path"}


def is_ground(column):
    return column is not None and (column[1] in NATURAL or column[1].endswith("_terracotta"))


def measure(grid):
    """{pares, degraus, maior, buracos} de uma grade de colunas (altura, bloco) ou None."""
    pairs = steep = worst = holes = walls = 0

    for z, row in enumerate(grid):
        for x, column in enumerate(row):
            if not is_ground(column):
                continue

            deeper = 0

            for dx, dz in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                if not (0 <= z + dz < len(grid) and 0 <= x + dx < len(row)):
                    continue

                other = grid[z + dz][x + dx]

                if not is_ground(other):
                    continue

                if dx >= 0 and dz >= 0:
                    step = abs(other[0] - column[0])
                    pairs += 1
                    steep += step > 1
                    worst = max(worst, step)
                    walls += step >= WALL

                if other[0] - column[0] >= 3:
                    deeper += 1

            holes += deeper == 4

    return {"pares": pairs, "degraus": steep, "maior": worst, "buracos": holes, "paredoes": walls}


WALL = 4

#: Até quantos blocos abaixo do topo um vão sob chão natural conta como "no ar" (mais fundo é caverna).
HANG_DEPTH = 12


def hangs(column):
    """(chão suspenso, tronco no ar) de uma coluna dada de cima para baixo como nomes de bloco, a partir do topo."""
    ground_over_gap = log_in_air = False
    seen_ground = False

    for depth, (name, below) in enumerate(zip(column, column[1:])):
        if depth > HANG_DEPTH:
            break

        empty_below = below.endswith("air")

        if name.endswith("_log") and empty_below:
            log_in_air = True

        if name in NATURAL or name.endswith("_terracotta"):
            seen_ground = True

            if empty_below:
                ground_over_gap = True
        elif seen_ground and not name.endswith("air"):
            break

    return ground_over_gap, log_in_air


def describe_floating(result):
    return (f"no ar perto do topo: {result['suspensas']} colunas com chão sobre vão, {result['troncos']} troncos sem "
            f"nada embaixo (de {result['colunas']} colunas)")


def floating(region, cx, cz, half):
    """{colunas, suspensas, troncos} na janela: o que ficou no ar perto do topo."""
    spec = importlib.util.spec_from_file_location(
        "village_map", pathlib.Path(__file__).resolve().parent / "village_map.py")
    village_map = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(village_map)
    chunks = village_map.load(region, cx, cz, half)
    columns = hanging = logs = 0

    for x in range(cx - half, cx + half + 1):
        for z in range(cz - half, cz + half + 1):
            chunk = chunks.get((x >> 4, z >> 4))

            if chunk is None or chunk.heights is None:
                continue

            top = chunk.heights[(z & 15) * 16 + (x & 15)] + village_map.MIN_Y - 1
            names = [chunk.block(x & 15, y, z & 15).split(":")[-1] for y in range(top, top - HANG_DEPTH - 2, -1)]
            ground_over_gap, log_in_air = hangs(names)
            columns += 1
            hanging += ground_over_gap
            logs += log_in_air

    return {"colunas": columns, "suspensas": hanging, "troncos": logs}


def describe(result):
    share = 100 * result["degraus"] / max(result["pares"], 1)

    return (f"degrau > 1 em {share:.2f}% dos pares ({result['degraus']} de {result['pares']}); "
            f"maior degrau {result['maior']}; colunas em buraco {result['buracos']}; "
            f"paredões (degrau de {WALL} ou mais) {result.get('paredoes', 0)}")


def around(region, cx, cz, half):
    spec = importlib.util.spec_from_file_location(
        "road_map", pathlib.Path(__file__).resolve().parent / "road_map.py")
    road_map = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(road_map)

    return road_map.ground(region, cx, cz, half)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("region")
    parser.add_argument("--center", required=True)
    parser.add_argument("--half", type=int, default=50)
    args = parser.parse_args(argv)
    cx, cz = (int(v) for v in args.center.split(","))

    print(describe(measure(around(args.region, cx, cz, args.half))))
    print(describe_floating(floating(args.region, cx, cz, args.half)))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
