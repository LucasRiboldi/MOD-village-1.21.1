"""Relatório do diário de ações (logs/villagecolony-actions-*.jsonl) — pedido do autor, 2026-10-08.

Uso:
    python scripts/action_report.py                     # o diário mais novo em %APPDATA%/.minecraft/logs
    python scripts/action_report.py caminho.jsonl [...]

Por profissão: tarefas pegas, terminadas, soltas e canceladas; taxa de conclusão; duração mediana de
uma tarefa terminada; ações concretas (bloco posto, pedra, árvore, peça, colheita, tosquia) por
minuto de jogo; encalhes. Por aldeão: o mesmo, e o que mais o fez soltar a tarefa.
"""
from __future__ import annotations

import glob
import json
import os
import statistics
import sys
from collections import Counter, defaultdict

TASK_EVENTS = ("TASK_TAKEN", "TASK_STARTED", "TASK_DONE", "TASK_RELEASED", "TASK_CANCELLED")
TICKS_PER_MINUTE = 1200


def newest_journal():
    folder = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs")
    found = sorted(glob.glob(os.path.join(folder, "villagecolony-actions-*.jsonl")))
    return found[-1:] if found else []


def parse(text_lines):
    """As linhas JSON válidas; linha quebrada (servidor caiu no meio) é ignorada."""
    records = []
    for line in text_lines:
        line = line.strip()
        if not line:
            continue
        try:
            records.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return records


def summarize(records):
    """{profissão: {...}} e {aldeão: {...}} com as contagens e medianas."""
    by_prof = defaultdict(lambda: {"events": Counter(), "actions": Counter(), "amount": Counter(),
                                   "done_ticks": [], "workers": set()})
    by_worker = defaultdict(lambda: {"prof": "", "events": Counter(), "actions": Counter(),
                                     "released_tasks": Counter()})
    first = min((r.get("t", 0) for r in records), default=0)
    last = max((r.get("t", 0) for r in records), default=0)

    for r in records:
        prof, worker, action = r.get("prof", "NONE"), r.get("worker", "?"), r.get("action", "?")
        p, w = by_prof[prof], by_worker[worker]
        p["workers"].add(worker)
        w["prof"] = prof
        if action in TASK_EVENTS:
            p["events"][action] += 1
            w["events"][action] += 1
            if action == "TASK_DONE" and "ticks" in r:
                p["done_ticks"].append(r["ticks"])
            if action == "TASK_RELEASED":
                w["released_tasks"][r.get("task", "?") + " " + r.get("what", "")] += 1
        else:
            p["actions"][action] += 1
            p["amount"][action] += r.get("n", 1)
            w["actions"][action] += 1

    minutes = max(1e-9, (last - first) / TICKS_PER_MINUTE)
    return by_prof, by_worker, minutes


def render(by_prof, by_worker, minutes):
    out = [f"Diário de ações: {minutes:.1f} min de jogo", ""]
    out.append(f"{'profissão':<11} {'aldeões':>7} {'pegas':>6} {'feitas':>6} {'soltas':>6} "
               f"{'cancel.':>7} {'conclusão':>9} {'mediana':>8}  ações por minuto")
    out.append("-" * 110)
    for prof in sorted(by_prof):
        p = by_prof[prof]
        taken = p["events"]["TASK_TAKEN"]
        done = p["events"]["TASK_DONE"]
        rate = f"{100 * done / taken:.0f}%" if taken else "-"
        median = f"{statistics.median(p['done_ticks']) / 20:.0f}s" if p["done_ticks"] else "-"
        acts = ", ".join(f"{a} {p['amount'][a] / minutes:.1f}" for a, _ in p["actions"].most_common(4))
        out.append(f"{prof:<11} {len(p['workers']):>7} {taken:>6} {done:>6} "
                   f"{p['events']['TASK_RELEASED']:>6} {p['events']['TASK_CANCELLED']:>7} "
                   f"{rate:>9} {median:>8}  {acts}")
    out.append("")
    out.append("Por aldeão (mais tarefas soltas primeiro):")
    ranked = sorted(by_worker.items(), key=lambda kv: -kv[1]["events"]["TASK_RELEASED"])
    for worker, w in ranked:
        why = ", ".join(f"{k} ×{v}" for k, v in w["released_tasks"].most_common(2))
        acts = ", ".join(f"{k} {v}" for k, v in w["actions"].most_common(3))
        out.append(f"  {worker} {w['prof']:<10} feitas {w['events']['TASK_DONE']:>3}, "
                   f"soltas {w['events']['TASK_RELEASED']:>3}  {acts}{'  | soltou: ' + why if why else ''}")
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
    print(render(*summarize(records)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
