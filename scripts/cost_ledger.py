#!/usr/bin/env python3
"""O custo do ciclo da colônia por fase — ADR-035 §5 (2026-10-06).

Lê as linhas ``VC_COST`` do log (``CycleCost.sample``, uma a cada 10 ciclos) e
diz, por faixa de número de colônias, a média, o p95 e o máximo de cada fase
em milissegundos. É a medida que precede qualquer otimização: a fase com o
maior p95 é por onde começar, e a diferença entre as faixas diz o que cresce
com as colônias.

Uso:
    python scripts/cost_ledger.py [log ...]

Sem argumento, lê ``latest.log`` da instalação de teste
(``%APPDATA%/.minecraft/logs``). Aceita ``.log.gz``.
"""

import gzip
import os
import re
import sys
from collections import defaultdict

LINE = re.compile(r"VC_COST version=1 colonies=(\d+)((?: \w+_us=\d+)+)")
FIELD = re.compile(r"(\w+)_us=(\d+)")
PHASES = ("detect", "lifecycle", "population", "chests", "planner", "assign", "workers", "other", "total")
BANDS = ((1, 1, "1 colônia"), (2, 3, "2-3 colônias"), (4, 10**9, "4+ colônias"))


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def parse(text_lines):
    """Cada amostra vira (colônias, {fase: microssegundos})."""
    samples = []
    for text in text_lines:
        match = LINE.search(text)
        if match:
            phases = {name: int(value) for name, value in FIELD.findall(match.group(2))}
            samples.append((int(match.group(1)), phases))
    return samples


def band_of(colonies):
    for low, high, label in BANDS:
        if low <= colonies <= high:
            return label
    return None


def percentile(values, fraction):
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, round(fraction * (len(ordered) - 1))))
    return ordered[index]


def summarize(samples):
    """{faixa: {fase: (média, p95, máximo)}} em milissegundos, mais a contagem."""
    grouped = defaultdict(lambda: defaultdict(list))
    counts = defaultdict(int)
    for colonies, phases in samples:
        band = band_of(colonies)
        if band is None:
            continue
        counts[band] += 1
        for phase in PHASES:
            grouped[band][phase].append(phases.get(phase, 0) / 1000)
    result = {}
    for band, phases in grouped.items():
        result[band] = {
            phase: (sum(values) / len(values), percentile(values, 0.95), max(values))
            for phase, values in phases.items()
        }
    return result, counts


def main(paths):
    samples = []
    for path in paths:
        samples.extend(parse(lines(path)))

    if not samples:
        print("Nenhuma linha VC_COST — o JAR é anterior à ADR-035, ou nenhum ciclo rodou 10 vezes.")
        return 1

    result, counts = summarize(samples)
    for _, _, band in BANDS:
        if band not in result:
            continue
        print(f"\n{band} — {counts[band]} amostras (ms: média / p95 / máximo)")
        ranked = sorted((p for p in PHASES if p != "total"), key=lambda p: -result[band][p][1])
        for phase in ranked + ["total"]:
            mean, p95, peak = result[band][phase]
            print(f"  {phase:<11} {mean:8.2f} {p95:8.2f} {peak:8.2f}")
    return 0


if __name__ == "__main__":
    default = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "latest.log")
    sys.exit(main(sys.argv[1:] or [default]))
