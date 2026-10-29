#!/usr/bin/env python3
"""O solo em volta da vila — pedido do autor, 2026-10-29 ("testes em arenas de 100 por 100 em todos os biomas").

Lê as regiões do mundo (como ``road_map.py``, pela altura sem folhas) numa janela de ``--half`` × 2 blocos em volta
de ``--center`` e mede, só entre colunas de chão natural (grama, terra, areia, pedra, cascalho, caminho, neve...):

- a fração de pares de colunas vizinhas com degrau maior que 1 (a Regra 152 quer zero dentro da vila);
- o maior degrau;
- as colunas em buraco: 3 ou mais abaixo das quatro vizinhas.

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
    pairs = steep = worst = holes = 0

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

                if other[0] - column[0] >= 3:
                    deeper += 1

            holes += deeper == 4

    return {"pares": pairs, "degraus": steep, "maior": worst, "buracos": holes}


def describe(result):
    share = 100 * result["degraus"] / max(result["pares"], 1)

    return (f"degrau > 1 em {share:.2f}% dos pares ({result['degraus']} de {result['pares']}); "
            f"maior degrau {result['maior']}; colunas em buraco {result['buracos']}")


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

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
