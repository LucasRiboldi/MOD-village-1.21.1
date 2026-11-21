"""O ciclo de vida das tarefas — Regra 344, telemetria T1.

Lê o diário de ações (``logs/villagecolony-actions-*.jsonl``) e segue cada tarefa pelo número dela (``id``): quando foi
pega, começou, deu a primeira peça e terminou, ou foi solta e por quê. Por tipo de tarefa: quantas, quantas terminaram,
quantas foram soltas, e a mediana e o p95 (em segundos) de cada trecho.

Uso:
    python scripts/task_report.py                 # o diário mais novo em %APPDATA%/.minecraft/logs
    python scripts/task_report.py caminho.jsonl [...]
"""
from __future__ import annotations

import os
import statistics
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from action_report import newest_journal, parse, reason_of  # noqa: E402

TICKS_PER_SECOND = 20
ENDS = ("TASK_DONE", "TASK_RELEASED", "TASK_CANCELLED")


def trace(records):
    """{id: {type, what, taken, started, first, end, outcome, reason}} das tarefas com número."""
    tasks = {}
    for record in records:
        task_id = record.get("id")
        if not task_id:
            continue
        action = record.get("action", "")
        t = record.get("t", 0)
        task = tasks.setdefault(task_id, {"type": record.get("task", ""), "what": record.get("what", ""),
                                          "taken": None, "started": None, "first": None, "end": None,
                                          "outcome": None, "reason": ""})
        if record.get("task"):
            task["type"] = record["task"]
        if action == "TASK_TAKEN" and task["taken"] is None:
            task["taken"] = t
        elif action == "TASK_STARTED" and task["started"] is None:
            task["started"] = t
        elif action in ENDS:
            task["end"] = t
            task["outcome"] = action
            task["reason"] = reason_of(record.get("detail", ""))
        elif not action.startswith("TASK_") and task["first"] is None:
            task["first"] = t
    return tasks


def seconds(values):
    """Mediana e p95, em segundos, ou nada."""
    if not values:
        return "-", "-"
    ordered = sorted(values)
    p95 = ordered[min(len(ordered) - 1, int(round(0.95 * (len(ordered) - 1))))]
    return f"{statistics.median(ordered) / TICKS_PER_SECOND:.0f}", f"{p95 / TICKS_PER_SECOND:.0f}"


def summarize(tasks):
    """{tipo: {count, done, released, cancelled, to_start, to_first, to_end, reasons}}."""
    by_type = defaultdict(lambda: {"count": 0, "done": 0, "released": 0, "cancelled": 0,
                                   "to_start": [], "to_first": [], "to_end": [], "reasons": Counter()})
    for task in tasks.values():
        row = by_type[task["type"] or "?"]
        row["count"] += 1
        if task["outcome"] == "TASK_DONE":
            row["done"] += 1
        elif task["outcome"] == "TASK_RELEASED":
            row["released"] += 1
            row["reasons"][task["reason"]] += 1
        elif task["outcome"] == "TASK_CANCELLED":
            row["cancelled"] += 1
        if task["taken"] is not None:
            for key, mark in (("to_start", "started"), ("to_first", "first"), ("to_end", "end")):
                if task[mark] is not None and task[mark] >= task["taken"]:
                    row[key].append(task[mark] - task["taken"])
    return by_type


def render(by_type):
    out = ["Tarefas pelo número (Regra 344): segundos desde que foi pega, mediana / p95", ""]
    header = (f"{'tipo':<26}{'qtd':>5}{'feitas':>8}{'soltas':>8}{'começo':>12}{'1ª peça':>12}{'fim':>12}")
    out.append(header)
    out.append("-" * len(header))
    for kind, row in sorted(by_type.items(), key=lambda kv: -kv[1]["count"]):
        cells = ["/".join(seconds(row[key])) for key in ("to_start", "to_first", "to_end")]
        out.append(f"{kind:<26}{row['count']:>5}{row['done']:>8}{row['released']:>8}"
                   f"{cells[0]:>12}{cells[1]:>12}{cells[2]:>12}")
    released = [(kind, reason, n) for kind, row in by_type.items() for reason, n in row["reasons"].most_common(3)]
    if released:
        out.append("")
        out.append("Por que foram soltas (os 3 motivos mais comuns de cada tipo):")
        for kind, reason, n in released:
            out.append(f"  {kind:<26}{n:>4}× {reason}")
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
    tasks = trace(records)
    if not tasks:
        print("Nenhuma tarefa com número no diário — o JAR é anterior à Regra 344.")
        return 1
    print(render(summarize(tasks)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
