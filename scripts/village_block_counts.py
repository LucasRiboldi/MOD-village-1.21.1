"""Conta os blocos das estruturas das vilas geradas — ADR-062, Regra 340.

Lê as estruturas Vanilla ``data/minecraft/structure/village/<estilo>/...`` do JAR do Minecraft (sem as vilas zumbi) e
grava, por bloco, quantas vezes ele aparece somando todas elas. É a régua da classe do fabricado: 10 ou mais é VILA
(guarda 12), menos de 10 é ESPECIAL (guarda 1), fora das vilas é FABRICADO (guarda 4).

Uso:
  python scripts/village_block_counts.py <minecraft.jar> [saida.json]

Sem saída, grava ``src/main/resources/data/villagecolony/stock/village_blocks.json``.
"""

from __future__ import annotations

import gzip
import json
import sys
import zipfile
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from export_vanilla_village_block_routes import IGNORED_TEMPLATE_BLOCKS, NbtReader  # noqa: E402

DEFAULT_OUT = Path("src/main/resources/data/villagecolony/stock/village_blocks.json")
PREFIX = "data/minecraft/structure/village/"


def template_counts(data: bytes) -> Counter:
    """Quantos de cada bloco a estrutura tem, pela primeira paleta."""
    if data.startswith(b"\x1f\x8b"):
        data = gzip.decompress(data)
    root = NbtReader(data).root()
    palette = (root.get("palettes") or [root.get("palette", [])])[0]
    counts = Counter()
    for block in root.get("blocks", []):
        name = palette[block["state"]].get("Name")
        if name and name not in IGNORED_TEMPLATE_BLOCKS:
            counts[name] += 1
    return counts


def village_counts(jar_path: Path) -> dict[str, int]:
    total = Counter()
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            if name.startswith(PREFIX) and name.endswith(".nbt") and "/zombie/" not in name:
                total.update(template_counts(jar.read(name)))
    return dict(sorted(total.items()))


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    out = Path(argv[2]) if len(argv) > 2 else DEFAULT_OUT
    counts = village_counts(Path(argv[1]))
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(counts, indent=1) + "\n", encoding="utf-8")
    print(f"{len(counts)} blocos em {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
