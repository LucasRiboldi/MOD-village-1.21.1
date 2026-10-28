"""Gera o armazém de cada bioma a partir de colony/storage_majest.nbt — Regra 115 (autor, 2026-10-27).

O modelo original mistura abeto, carvalho-escuro, ardósia funda, calcita e folhas de acácia: em qualquer
bioma metade das peças não tinha de onde vir e surgia no lugar (Regra 111). Aqui cada estilo troca os
materiais pelos do bioma e todos perdem as folhas, os alçapões e as teias. O original também é limpo,
para o caso de um estilo sem variante.

    python scripts/storage_variants.py          # escreve colony/<estilo>/storage_majest.nbt
    python scripts/storage_variants.py --check  # falha se algum arquivo não é o que o script gera
"""

import argparse
import collections
import os
import sys

import nbtlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data",
                    "villagecolony", "structure", "colony")
SOURCE = os.path.join(ROOT, "storage_majest.nbt")

# Saem de todos: folha não se fabrica nem se planta como peça, e alçapão e teia só enfeitam (autor, 10-27).
REMOVED = {"minecraft:acacia_leaves", "minecraft:spruce_trapdoor", "minecraft:cobweb"}

# O baú do fim não se fabrica sem obsidiana: vira baú comum, com a mesma frente.
COMMON = {"minecraft:ender_chest": "minecraft:chest"}

# As propriedades só passam quando a peça é da mesma família (degrau, laje, cerca, tora).
SAME_SHAPE = ("_stairs", "_slab", "_fence", "_fence_gate", "_log", "_wood", "_wall_hanging_sign", "chest")


def wood(name):
    """Toda a madeira do modelo numa madeira só."""
    return {
        "spruce_log": f"{name}_log", "stripped_spruce_log": f"stripped_{name}_log",
        "spruce_wood": f"{name}_wood", "oak_wood": f"{name}_wood", "spruce_planks": f"{name}_planks",
        "spruce_slab": f"{name}_slab", "spruce_stairs": f"{name}_stairs", "dark_oak_stairs": f"{name}_stairs",
        "spruce_fence": f"{name}_fence", "dark_oak_fence": f"{name}_fence",
        "spruce_fence_gate": f"{name}_fence_gate", "spruce_wall_hanging_sign": f"{name}_wall_hanging_sign",
    }


def stone(block, stairs, slab, rough, rough_stairs, rough_slab):
    """A ardósia funda e a calcita no material de pedra do bioma."""
    return {
        "deepslate_tiles": block, "deepslate_bricks": block, "polished_deepslate": rough,
        "deepslate_tile_stairs": stairs, "deepslate_brick_stairs": stairs, "polished_deepslate_stairs": rough_stairs,
        "deepslate_tile_slab": slab, "deepslate_brick_slab": slab, "polished_deepslate_slab": rough_slab,
    }


STYLES = {
    "plains": {**wood("oak"),
               **stone("stone_bricks", "stone_brick_stairs", "stone_brick_slab",
                       "cobblestone", "cobblestone_stairs", "cobblestone_slab"),
               "calcite": "oak_planks", "polished_diorite": "cobblestone"},
    "taiga": {**wood("spruce"),
              **stone("cobblestone", "cobblestone_stairs", "cobblestone_slab",
                      "cobblestone", "cobblestone_stairs", "cobblestone_slab"),
              "calcite": "spruce_planks", "polished_diorite": "cobblestone"},
    "snowy": {**wood("spruce"),
              **stone("stone_bricks", "stone_brick_stairs", "stone_brick_slab",
                      "smooth_stone", "stone_brick_stairs", "smooth_stone_slab"),
              "calcite": "spruce_planks", "polished_diorite": "stone_bricks"},
    "savanna": {**wood("acacia"),
                **stone("cobblestone", "cobblestone_stairs", "cobblestone_slab",
                        "cobblestone", "cobblestone_stairs", "cobblestone_slab"),
                "calcite": "acacia_planks", "polished_diorite": "cobblestone"},
    # No deserto quase não há árvore: a madeira vira arenito, e portão e placa saem.
    "desert": {"spruce_log": "sandstone", "stripped_spruce_log": "cut_sandstone", "spruce_wood": "sandstone",
               "oak_wood": "sandstone", "spruce_planks": "sandstone", "spruce_slab": "sandstone_slab",
               "spruce_stairs": "sandstone_stairs", "dark_oak_stairs": "sandstone_stairs",
               "spruce_fence": "sandstone_wall", "dark_oak_fence": "sandstone_wall",
               "spruce_fence_gate": None, "spruce_wall_hanging_sign": None,
               **stone("cut_sandstone", "sandstone_stairs", "cut_sandstone_slab",
                       "sandstone", "sandstone_stairs", "sandstone_slab"),
               "calcite": "sandstone", "polished_diorite": "cut_sandstone"},
}


def same_shape(source, target):
    return any(source.endswith(suffix) and target.endswith(suffix) for suffix in SAME_SHAPE)


def variant(mapping):
    """O modelo com os materiais trocados por {@code mapping} e as peças de REMOVED fora."""
    structure = nbtlib.load(SOURCE)
    gone = set()

    for index, entry in enumerate(structure["palette"]):
        name = str(entry["Name"])
        short = name.removeprefix("minecraft:")
        target = COMMON.get(name, name)

        if short in mapping:
            target = None if mapping[short] is None else "minecraft:" + mapping[short]

        if name in REMOVED or target is None:
            gone.add(index)
            continue

        if target != name:
            entry["Name"] = nbtlib.String(target)

            if "Properties" in entry and not same_shape(short, target):
                del entry["Properties"]

    structure["blocks"] = nbtlib.List[nbtlib.Compound](
        [block for block in structure["blocks"] if int(block["state"]) not in gone])

    return structure


def targets():
    yield SOURCE, {}

    for style, mapping in STYLES.items():
        yield os.path.join(ROOT, style, "storage_majest.nbt"), mapping


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    stale = []

    # O original é a fonte de todos: limpa-se por último, para as variantes lerem a versão de antes.
    planned = [(path, variant(mapping)) for path, mapping in targets()]

    for path, structure in planned:
        counts = collections.Counter(str(structure["palette"][int(b["state"])]["Name"]) for b in structure["blocks"])
        left = REMOVED & set(counts)

        if left:
            raise SystemExit(f"{path}: ainda tem {sorted(left)}")

        if args.check:
            stale += [] if os.path.exists(path) and nbtlib.load(path) == structure else [path]
            continue

        os.makedirs(os.path.dirname(path), exist_ok=True)
        structure.save(path, gzipped=True)
        print(f"{os.path.relpath(path, ROOT)}: {sum(counts.values())} peças, {len(counts)} tipos")

    if stale:
        print("desatualizado:", *stale, sep="\n  ")
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
