#!/usr/bin/env python3
"""O balanço dos itens nos baús — Regra 344, telemetria T3.

Soma as linhas ``VC_FLOW`` do log (uma por dia): por item, quanto entrou e saiu pelas portas do mod, quanto não coube
(recusado: o resto pode ter sido destruído por quem depositava) e quanto mudou **por fora** (o estoque de hoje menos o de
ontem, menos o que entrou e saiu). "Por fora" diferente de zero é o jogador mexendo no baú, ou um caminho do mod que
mexe no baú sem passar pelas portas: é ali que item some ou aparece sem registro.

Uso:
    python scripts/flow_report.py [log ...]

Sem argumento, lê ``latest.log`` da instalação de teste. Aceita ``.log.gz``.
"""

import gzip
import os
import re
import sys
from collections import defaultdict

LINE = re.compile(r"VC_FLOW version=1 day=(\d+) items=\d+(.*)")
ITEM = re.compile(r"([a-z0-9_:./-]+)=(\d+),(\d+),(\d+),(\?|-?\d+),(\d+),(\?|-?\d+)")


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def collect(text_lines):
    """Por item: dias, entrou, saiu, recusado, por fora (somado), último estoque."""
    items = defaultdict(lambda: {"days": 0, "in": 0, "out": 0, "refused": 0, "outside": 0, "stock": 0})
    for text in text_lines:
        match = LINE.search(text)
        if not match:
            continue
        for key, came, went, refused, _before, now, outside in ITEM.findall(match.group(2)):
            row = items[key]
            row["days"] += 1
            row["in"] += int(came)
            row["out"] += int(went)
            row["refused"] += int(refused)
            row["stock"] = int(now)
            if outside != "?":
                row["outside"] += int(outside)
    return items


def render(items):
    out = ["Balanço dos itens (Regra 344): recusados e mudanças por fora primeiro", ""]
    header = f"{'item':<28}{'dias':>5}{'entrou':>8}{'saiu':>8}{'recusado':>10}{'por fora':>10}{'estoque':>9}"
    out.append(header)
    out.append("-" * len(header))
    ordered = sorted(items.items(), key=lambda kv: (-(abs(kv[1]["outside"]) + kv[1]["refused"]),
                                                     -(kv[1]["in"] + kv[1]["out"]), kv[0]))
    for key, row in ordered:
        out.append(f"{key:<28}{row['days']:>5}{row['in']:>8}{row['out']:>8}{row['refused']:>10}"
                   f"{row['outside']:>10}{row['stock']:>9}")
    return "\n".join(out)


def main(paths):
    items = collect(line for path in paths for line in lines(path))
    if not items:
        print("Nenhuma linha VC_FLOW — o JAR é anterior à Regra 344, ou nenhum expediente fechou.")
        return 1
    print(render(items))
    return 0


if __name__ == "__main__":
    args = sys.argv[1:] or [os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "latest.log")]
    sys.exit(main(args))
