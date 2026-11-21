"""A linha do tempo de cada aldeão — Regra 344, telemetria T2.

Lê as linhas ``STATE`` do diário de ações (``logs/villagecolony-actions-*.jsonl``): cada uma é uma mudança de estado
firmada, com o estado de antes (``detail``: ``from X`` e o motivo), quanto ele durou (``ticks``) e onde o aldeão
estava. Por aldeão: a parte de cada estado e os trechos parados mais longos, com lugar e motivo.

Uso:
    python scripts/villager_timeline.py                 # o diário mais novo em %APPDATA%/.minecraft/logs
    python scripts/villager_timeline.py caminho.jsonl [...]
"""
from __future__ import annotations

import os
import sys
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from action_report import newest_journal, parse  # noqa: E402

TICKS_PER_SECOND = 20
STATES = ("WORK", "WALK", "WAIT", "BLOCKED", "IDLE", "STRANDED", "OFF", "SLEEP")
STOPPED = ("WAIT", "BLOCKED", "IDLE", "STRANDED")


def stretches(records):
    """{aldeão: {prof, seconds: {estado: s}, stops: [(s, estado, motivo, (x, y, z))]}} das linhas STATE."""
    villagers = defaultdict(lambda: {"prof": "", "seconds": defaultdict(float), "stops": []})
    for record in records:
        if record.get("action") != "STATE":
            continue
        detail = record.get("detail", "")
        if not detail.startswith("from "):
            continue
        parts = detail.split(" | para ", 1)[0].split(" ", 2)
        before = parts[1] if len(parts) > 1 else "?"
        reason = parts[2] if len(parts) > 2 else ""
        lasted = record.get("ticks", 0) / TICKS_PER_SECOND
        row = villagers[record.get("worker", "?")]
        row["prof"] = record.get("prof", "") or row["prof"]
        row["seconds"][before] += lasted
        if before in STOPPED:
            row["stops"].append((lasted, before, reason, (record.get("x"), record.get("y"), record.get("z"))))
    return villagers


def render(villagers, stops=3):
    out = ["Linha do tempo por aldeão (Regra 344): parte de cada estado e os trechos parados mais longos", ""]
    header = f"{'aldeão':<10}{'ofício':<12}" + "".join(f"{s.lower():>9}" for s in STATES) + "   (% do tempo medido)"
    out.append(header)
    out.append("-" * len(header))
    for worker, row in sorted(villagers.items(), key=lambda kv: (kv[1]["prof"], kv[0])):
        total = sum(row["seconds"].values()) or 1
        shares = "".join(f"{100 * row['seconds'].get(s, 0) / total:>8.0f}%" for s in STATES)
        out.append(f"{worker:<10}{row['prof']:<12}{shares}")
    out.append("")
    out.append("Trechos parados mais longos:")
    for worker, row in sorted(villagers.items(), key=lambda kv: (kv[1]["prof"], kv[0])):
        for lasted, state, reason, where in sorted(row["stops"], reverse=True)[:stops]:
            place = ",".join(str(v) for v in where) if where[0] is not None else "-"
            out.append(f"  {worker:<10}{row['prof']:<12}{lasted:>6.0f}s {state:<9} em {place:<16} {reason}")
    return "\n".join(out)


def main(argv):
    paths = argv[1:] or newest_journal()
    if not paths:
        print("Nenhum diário encontrado (logs/villagecolony-actions-*.jsonl).")
        return 1
    records = []
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            records.extend(parse(handle))
    villagers = stretches(records)
    if not villagers:
        print("Nenhuma linha STATE no diário — o JAR é anterior à Regra 344.")
        return 1
    print(render(villagers))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
