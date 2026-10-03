#!/usr/bin/env python3
"""O que cada profissão fez numa sessão, e quanto tempo cada atividade levou.

Lê os logs do jogo (``.log`` ou ``.log.gz``) e conta, por profissão:

- as atividades concluídas (árvore derrubada, colheita, tosquia, fundição,
  fabricação, bloco assentado, pedra desistida, encalhe);
- o tempo médio entre conclusões de cada trabalhador (o ciclo da atividade);
- a proporção dos estados nas linhas de situação de 30 s (procurando,
  andando, trabalhando, esperando material).

Complementa o ``time_ledger.py`` (Regra 50): este lê o que os logs já tinham
antes do ``VC_TIME``, e serve para comparar sessões antigas.

Uso:
    python scripts/activity_report.py log1 [log2 ...]
"""

import gzip
import re
import statistics
import sys
from collections import Counter, defaultdict

TIME = re.compile(r"^\[(\d\d):(\d\d):(\d\d)\] \[Server thread/INFO\]: (.*)$")
SHORT = r"([0-9a-f]{8})[0-9a-f-]*"

EVENTS = {
    "LUMBERJACK árvore derrubada": re.compile(r"Worker " + SHORT + r" finished the tree at .* — (\d+) logs"),
    "FARMER colheita": re.compile(r"Farmer " + SHORT + r" harvested (\d+) at"),
    "FARMER semeadura": re.compile(r"Farmer " + SHORT + r" sowed"),
    "SHEPHERD tosquia": re.compile(r"Shepherd " + SHORT + r" sheared (\d+)"),
    "SMELTER fundição": re.compile(r"Smelter " + SHORT + r" made "),
    "CARPENTER/MASON fabricação": re.compile(r"Worker " + SHORT + r" finished crafting — (\d+) pieces"),
    "MINER pedra desistida": re.compile(r"Miner " + SHORT + r" gave up the stone"),
    "ANY encalhe": re.compile(r"Worker " + SHORT + r" is stranded at"),
    "ANY saiu do encalhe": re.compile(r"Stranded worker " + SHORT + r" is out at"),
    "BUILDER casa pronta": re.compile(r"Builder " + SHORT + r" stopped — the house is up"),
    "BUILDER parado sem material": re.compile(r"Builder " + SHORT + r" stopped — no "),
    "SMELTER parado sem cru": re.compile(r"Smelter " + SHORT + r" stopped — none of"),
}

STATUS = re.compile(r"Colony [0-9a-f-]+ (\w+): (.*)$")

STATE_RULES = {
    "lumberjacks": [("procurando árvore", "looking for a tree"), ("andando até a árvore", "walking — tree"),
                    ("cortando", "chopping")],
    "miners": [("procurando pedra", "looking for stone"), ("andando até a pedra", "out of reach"),
               ("cavando", "digging")],
    "carpenters": [("andando até o baú", "walking to the chest"), ("no baú", "at the chest")],
    "masons": [("andando até o baú", "walking to the chest"), ("no baú", "at the chest")],
}

BUILDER_STATES = [("esperando material", "WAITING_RESOURCES"), ("andando sem chegar", "without reaching"),
                  ("reservada, indo", "RESERVED"), ("assentando", "EXECUTING")]

BLOCKS_LEFT = re.compile(r"builders: .* BUILDING at .*?, (\d+) blocks left")


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def seconds(match, day):
    return day * 86400 + int(match.group(1)) * 3600 + int(match.group(2)) * 60 + int(match.group(3))


def main(paths):
    events = defaultdict(lambda: defaultdict(list))
    amounts = defaultdict(list)
    states = defaultdict(Counter)
    builder_left = []
    sessions = []

    for index, path in enumerate(paths):
        first = last = None
        # Cada arquivo é uma linha do tempo própria: o horário dele não se
        # compara com o de outro arquivo (dias diferentes, sessões diferentes).
        day = 0
        previous = None
        offset = index * 10 * 86400

        for text in lines(path):
            match = TIME.match(text.rstrip("\n"))
            if not match:
                continue
            now = seconds(match, day)
            if previous is not None and now < previous - 3600:
                day += 1
                now += 86400
            previous = now
            now += offset
            first = now if first is None else first
            last = now
            message = match.group(4)

            for name, rule in EVENTS.items():
                found = rule.search(message)
                if found:
                    events[name][(index, found.group(1))].append(now)
                    if found.lastindex and found.lastindex >= 2:
                        amounts[name].append(int(found.group(2)))

            status = STATUS.search(message)
            if not status:
                continue
            kind, body = status.groups()
            if kind == "builders":
                for label, key in BUILDER_STATES:
                    if key in body:
                        states["builders"][label] += 1
                        break
                left = BLOCKS_LEFT.search(message)
                if left:
                    builder_left.append((now, int(left.group(1))))
                continue
            for part in body.split("; "):
                for label, key in STATE_RULES.get(kind, []):
                    if key in part:
                        states[kind][label] += 1
                        break

        if first is not None:
            sessions.append(last - first)

    print(f"Sessões: {len(sessions)}, {sum(sessions) / 60:.0f} min de log\n")
    print("== Atividades concluídas, e o ciclo médio por trabalhador ==")
    for name in EVENTS:
        per_worker = events.get(name, {})
        total = sum(len(times) for times in per_worker.values())
        gaps = [b - a for times in per_worker.values() for a, b in zip(times, times[1:]) if b - a < 3600]
        cycle = f", ciclo médio {statistics.mean(gaps) / 60:.1f} min (mediana {statistics.median(gaps) / 60:.1f})" \
            if gaps else ""
        amount = f", média {statistics.mean(amounts[name]):.1f} por vez" if amounts.get(name) else ""
        print(f"  {name:<32} {total:>4} vezes, {len(per_worker)} trabalhador(es){amount}{cycle}")

    stranded = events.get("ANY encalhe", {})
    out = events.get("ANY saiu do encalhe", {})
    spans = []
    for worker, starts in stranded.items():
        exits = sorted(out.get(worker, []))
        for start in starts:
            after = [t for t in exits if t >= start]
            if after:
                spans.append(after[0] - start)
    if spans:
        print(f"\n  Encalhe: {len(spans)} saídas, tempo médio preso {statistics.mean(spans) / 60:.1f} min"
              f" (máx {max(spans) / 60:.1f})")

    if len(builder_left) >= 2:
        placed = 0
        span = 0
        for (t0, left0), (t1, left1) in zip(builder_left, builder_left[1:]):
            if 0 < left0 - left1 < 200 and t1 - t0 <= 120:
                placed += left0 - left1
                span += t1 - t0
        if span:
            print(f"  Construtor: {placed} blocos assentados em {span / 60:.1f} min medidos"
                  f" — {placed / (span / 60):.1f} blocos/min enquanto assenta")

    print("\n== Estados nas linhas de situação (cada linha vale ~30 s) ==")
    for kind, counter in states.items():
        total = sum(counter.values())
        shares = ", ".join(f"{label} {100 * count / total:.0f}%" for label, count in counter.most_common())
        print(f"  {kind:<12} {total:>4} amostras: {shares}")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
