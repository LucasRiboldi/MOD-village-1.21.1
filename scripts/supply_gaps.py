"""Resumo do registro permanente de faltas de suprimento — G2, pedido do autor, 2026-10-20.

Cada linha de ``villagecolony/supply-gaps.jsonl`` é uma peça que apareceu num baú sem uma
profissão a ter obtido. O resumo é a lista de itens que ainda pedem rota própria, do mais
frequente ao menos, com o motivo e os estilos de vila em que faltou.

    python scripts/supply_gaps.py                          # a instalação de teste (%APPDATA%/.minecraft)
    python scripts/supply_gaps.py run/biome-sim            # a bancada de simulação
    python scripts/supply_gaps.py caminho/supply-gaps.jsonl [...]

Motivos: NO_ROUTE (nenhuma profissão obtém a peça no bioma), NO_TASK (a rota existe, ninguém a
executa), STALLED (a rota tem tarefa e não avançou), NOT_FOUND (as buscas no mundo não acharam).
"""

import collections
import json
import os
import sys


def ledger_files(args):
    if not args:
        args = [os.path.join(os.environ.get("APPDATA", ""), ".minecraft")]

    for arg in args:
        if os.path.isdir(arg):
            arg = os.path.join(arg, "villagecolony", "supply-gaps.jsonl")

        if os.path.exists(arg):
            yield arg


def main(args):
    by_item = collections.defaultdict(lambda: {"cases": 0, "count": 0, "reasons": collections.Counter(),
                                               "styles": collections.Counter(), "professions": collections.Counter()})
    cases = 0

    for path in ledger_files(args):
        with open(path, encoding="utf-8", errors="replace") as source:
            for line in source:
                try:
                    gap = json.loads(line)
                except json.JSONDecodeError:
                    continue

                cases += 1
                entry = by_item[gap.get("item", "?")]
                entry["cases"] += 1
                entry["count"] += int(gap.get("count", 0))
                entry["reasons"][gap.get("reason", "?")] += 1
                entry["styles"][gap.get("style", "?").replace("minecraft:", "")] += 1
                entry["professions"][gap.get("profession", "?")] += 1

    if not cases:
        print("Nenhuma falta registrada (villagecolony/supply-gaps.jsonl vazio ou ausente).")
        return 0

    print(f"{cases} faltas, {len(by_item)} itens sem rota própria que entregue\n")
    print(f"{'item':38} {'casos':>5} {'peças':>6}  motivos / estilos / quem usa")
    print("-" * 110)

    for item, entry in sorted(by_item.items(), key=lambda pair: -pair[1]["cases"]):
        reasons = ", ".join(f"{k} {v}" for k, v in entry["reasons"].most_common())
        styles = ", ".join(f"{k} {v}" for k, v in entry["styles"].most_common())
        users = ", ".join(entry["professions"])
        print(f"{item.replace('minecraft:', ''):38} {entry['cases']:5} {entry['count']:6}  {reasons} / {styles} / {users}")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
