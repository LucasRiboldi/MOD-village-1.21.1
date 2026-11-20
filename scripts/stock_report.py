#!/usr/bin/env python3
"""O estoque dos baús e as travas de cru numa sessão de jogo — Regras 342 e 343 (ADR-062).

Lê as linhas ``VC_BLOCKS`` (registro de blocos, uma por colônia e por dia) e ``VC_KEEP`` (travas pequenas de cru, uma
por dia) do log e sugere, por bloco e por trava, o que fazer com a reserva. A decisão é do autor.

Uso:
    python scripts/stock_report.py [log ...]

Sem argumento, lê ``latest.log`` da instalação de teste (``%APPDATA%/.minecraft/logs``). Aceita ``.log.gz``.

Bloco: cru que a obra esperou ou que surgiu -> FALTA CRU (coleta; 256 é teto, não alvo); fabricado que a obra esperou
ou que surgiu, com estoque abaixo da reserva em algum dia -> SUBIR; nunca posto nem
esperado, fabricado e com estoque acima da reserva todos os dias -> pode DESCER; o resto -> ok.
Trava: nunca barrou -> NÃO AGE (pode sair); barrou em mais da metade das consultas -> SEGURA DEMAIS (descer); o resto
-> ok.
"""

import gzip
import os
import re
import sys

BLOCKS = re.compile(r"VC_BLOCKS version=1 colony=(\w+) day=(\d+) blocks=\d+(.*)")
BLOCK = re.compile(r"([a-z0-9_:./-]+)=([RCVSN]),(\d+),(\d+),(\d+),(\d+),(\d+)")
KEEP = re.compile(r"VC_KEEP version=1 day=(\d+) locks=\d+(.*)")
LOCK = re.compile(r"([a-z_]+)=(\d+),(\d+),(\d+),(-?\d+)")

CLASSES = {"R": "CRU", "C": "FABRICADO", "V": "VILA", "S": "ESPECIAL", "N": "NUNCA"}
HOLDS_TOO_MUCH = 0.5


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def collect_blocks(text_lines):
    """Por bloco: classe, reserva, dias, dias abaixo da reserva, menor e maior estoque, posto, esperou, surgiu."""
    blocks = {}
    for text in text_lines:
        match = BLOCKS.search(text)
        if not match:
            continue
        for key, cls, reserve, stock, used, waited, surged in BLOCK.findall(match.group(3)):
            reserve, stock = int(reserve), int(stock)
            row = blocks.setdefault(key, {"class": cls, "reserve": reserve, "days": 0, "short_days": 0,
                                          "min": stock, "max": stock, "used": 0, "waited": 0, "surged": 0})
            row["reserve"] = reserve
            row["days"] += 1
            row["short_days"] += 1 if stock < reserve else 0
            row["min"] = min(row["min"], stock)
            row["max"] = max(row["max"], stock)
            row["used"] += int(used)
            row["waited"] += int(waited)
            row["surged"] += int(surged)
    return blocks


def block_advice(row):
    """SUBIR, DESCER ou ok para a reserva deste bloco; para o cru, 256 é teto, e a falta é de coleta."""
    if row["class"] == "R":
        return "FALTA CRU (coleta)" if row["waited"] > 0 or row["surged"] > 0 else "ok"
    if (row["waited"] > 0 or row["surged"] > 0) and row["short_days"] > 0:
        return "SUBIR"
    if row["used"] == 0 and row["waited"] == 0 and row["min"] > row["reserve"] and row["class"] != "R":
        return "DESCER"
    return "ok"


def collect_keeps(text_lines):
    """Por trava: limite, consultas, barradas e o menor estoque visto."""
    keeps = {}
    for text in text_lines:
        match = KEEP.search(text)
        if not match:
            continue
        for lock, limit, checks, barred, lowest in LOCK.findall(match.group(2)):
            row = keeps.setdefault(lock, {"limit": int(limit), "checks": 0, "barred": 0, "lowest": int(lowest)})
            row["limit"] = int(limit)
            row["checks"] += int(checks)
            row["barred"] += int(barred)
            row["lowest"] = min(row["lowest"], int(lowest))
    return keeps


def keep_advice(row):
    """O que fazer com esta trava."""
    if row["barred"] == 0:
        return "NÃO AGE (pode sair)"
    if row["checks"] and row["barred"] / row["checks"] > HOLDS_TOO_MUCH:
        return "SEGURA DEMAIS (descer)"
    return "ok"


def print_blocks(blocks):
    print("Blocos (Regra 342): os que pedem mudança primeiro\n")
    header = (f"{'bloco':<28}{'classe':<11}{'reserva':>8}{'estoque':>12}{'posto':>7}{'esperou':>8}{'surgiu':>7}"
              "   sugestão")
    print(header)
    print("-" * len(header))
    ordered = sorted(blocks.items(), key=lambda kv: (block_advice(kv[1]) == "ok",
                                                      -kv[1]["waited"] - kv[1]["surged"], -kv[1]["used"], kv[0]))
    for key, row in ordered:
        stock = f"{row['min']}-{row['max']}"
        print(f"{key:<28}{CLASSES.get(row['class'], row['class']):<11}{row['reserve']:>8}{stock:>12}"
              f"{row['used']:>7}{row['waited']:>8}{row['surged']:>7}   {block_advice(row)}")


def print_keeps(keeps):
    print("\nTravas de cru (Regra 343)\n")
    header = f"{'trava':<14}{'limite':>7}{'consultas':>11}{'barradas':>10}{'menor':>7}   sugestão"
    print(header)
    print("-" * len(header))
    for lock, row in sorted(keeps.items()):
        print(f"{lock:<14}{row['limit']:>7}{row['checks']:>11}{row['barred']:>10}{row['lowest']:>7}"
              f"   {keep_advice(row)}")


def main(paths):
    text = [line for path in paths for line in lines(path)]
    blocks = collect_blocks(text)
    keeps = collect_keeps(text)

    if not blocks and not keeps:
        print("Nenhuma linha VC_BLOCKS nem VC_KEEP — o JAR é anterior às Regras 342 e 343, ou nenhum expediente fechou.")
        return 1

    if blocks:
        print_blocks(blocks)
    if keeps:
        print_keeps(keeps)

    return 0


if __name__ == "__main__":
    args = sys.argv[1:] or [os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "latest.log")]
    sys.exit(main(args))
