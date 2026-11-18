"""Medidas da simulação longa e comparação com a linha de base — perfil ``longo`` do Sentinela (autor, 2026-11-18).

Lê o relatório de cada alvo da bancada (``scripts/biome_sim.py``, em ``run/biome-sim/reports/<alvo>``) e tira números que
decidem se uma versão melhorou a vila, por profissão e no todo:

    trabalho     média do trabalho no expediente (VC_TIME, Regra 50)            maior é melhor
    travado      média de bloqueado + encalhado no expediente                    menor é melhor
    sem_tarefa   média de ócio sem tarefa: falta de demanda, não defeito        só informa
    espera       média de espera por insumo                                      só informa
    obras_dia    obras concluídas por dia de jogo                                maior é melhor
    grafo_dia    mudanças do grafo por dia (nós, arestas ou partes soltas)       menor é melhor
    soltas       partes e portas fora do grafo na última releitura               menor é melhor
    surgidos_dia blocos que surgiram (VC_SUPPLY, todas as exceções) por dia      menor é melhor
    encalhes_dia aldeões dados como encalhados por dia                           menor é melhor

Uso:

    python scripts/sim_gate.py plains desert taiga                 # compara com a base
    python scripts/sim_gate.py plains desert taiga --write-baseline

Indicativo por decisão do autor (2026-11-18): a mesma semente varia muito entre rodadas (o comportamento dos aldeões tem
acaso), então o padrão só informa. Com ``--strict``, sai com 1 quando algum número piorou além da folga.
"""

import argparse
import importlib.util
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPORTS = os.path.join(ROOT, "run", "biome-sim", "reports")
BASELINE = os.path.join(ROOT, "docs", "testing", "sim-baseline.json")

# Nome, direção (+1 maior é melhor, -1 menor é melhor, 0 só informa) e folga absoluta antes de contar como piora.
METRICS = (
    ("trabalho", 1, 3.0),
    ("travado", -1, 3.0),
    ("sem_tarefa", 0, 0.0),
    ("espera", 0, 0.0),
    ("obras_dia", 1, 0.25),
    ("grafo_dia", -1, 2.0),
    ("soltas", -1, 1.0),
    ("surgidos_dia", -1, 16.0),
    ("encalhes_dia", -1, 1.0),
)

GRAPH = re.compile(r"VC_ROAD_GRAPH colony=\w+ nodes=(\d+) edges=(\d+) .*?isolated=(\d+)(?:.*?loose=(\S*))?")
SUPPLY = re.compile(r"VC_SUPPLY version=1 exception=(\w+) .*? count=(\d+)")
STRANDED = re.compile(r"Worker \w+ is stranded at")
FINISHED = re.compile(r"Colony \S+ finished \S+ at")
DAYS = re.compile(r"dias=(\d+)")
SEED = re.compile(r"semente=(\S+)")


def _time_ledger():
    spec = importlib.util.spec_from_file_location("time_ledger", os.path.join(ROOT, "scripts", "time_ledger.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    return module


def _read(path):
    if not os.path.exists(path):
        return ""

    with open(path, encoding="utf-8", errors="replace") as source:
        return source.read()


def time_shares(log_lines):
    """Por profissão, a porcentagem de cada estado no expediente; o sem ofício fica fora (Regra 270)."""
    seconds, _ = _time_ledger().collect(log_lines)
    shares = {}

    for profession, row in seconds.items():
        total = row.get("total", 0)

        if total > 0:
            shares[profession] = {state: 100 * row.get(state, 0) / total
                                  for state in ("work", "walk", "wait", "blocked", "idle", "stranded")}

    return shares


def graph_changes(log_lines):
    """Quantas releituras do grafo mudaram nós, arestas ou partes soltas, e quantas partes soltas ficaram na última."""
    last = None
    changes = 0
    loose = 0

    for text in log_lines:
        found = GRAPH.search(text)

        if not found:
            continue

        loose_parts = tuple(sorted(part for part in (found.group(4) or "").split(";") if part))
        state = (found.group(1), found.group(2), loose_parts)

        if last is not None and state != last:
            changes += 1

        last = state
        loose = len(loose_parts)

    return changes, loose


def measure(report_dir):
    """Os números de um alvo, ou None se o relatório não tem log."""
    log = os.path.join(report_dir, "latest.log")

    if not os.path.exists(log):
        return None

    text = _read(log)
    log_lines = text.splitlines()
    signals = _read(os.path.join(report_dir, "signals.txt"))
    days_found = DAYS.search(signals)
    seed_found = SEED.search(signals)
    days = max(1, int(days_found.group(1))) if days_found else 1
    shares = time_shares(log_lines)
    changes, loose = graph_changes(log_lines)
    surged = {}

    for exception, count in SUPPLY.findall(text):
        surged[exception] = surged.get(exception, 0) + int(count)

    def average(states):
        if not shares:
            return 0.0

        return sum(sum(row[state] for state in states) for row in shares.values()) / len(shares)

    return {
        "dias": days,
        "semente": seed_found.group(1) if seed_found else "",
        "trabalho": round(average(("work",)), 1),
        "travado": round(average(("blocked", "stranded")), 1),
        "sem_tarefa": round(average(("idle",)), 1),
        "espera": round(average(("wait",)), 1),
        "obras_dia": round(len(FINISHED.findall(text)) / days, 2),
        "grafo_dia": round(changes / days, 1),
        "soltas": loose,
        "surgidos_dia": round(sum(surged.values()) / days, 1),
        "encalhes_dia": round(len(STRANDED.findall(text)) / days, 1),
        "por_profissao": {profession: {state: round(value, 1) for state, value in row.items()}
                          for profession, row in sorted(shares.items())},
        "surgidos": dict(sorted(surged.items())),
    }


def compare(current, base):
    """Para cada número: (nome, base, atual, veredito). Veredito: melhor, pior, igual ou info."""
    rows = []

    for name, direction, slack in METRICS:
        now = current.get(name)
        before = base.get(name) if base else None

        if before is None or direction == 0:
            rows.append((name, before, now, "info"))
            continue

        delta = (now - before) * direction

        if delta < -slack:
            rows.append((name, before, now, "pior"))
        elif delta > slack:
            rows.append((name, before, now, "melhor"))
        else:
            rows.append((name, before, now, "igual"))

    return rows


def comparable(current, base):
    """A base só vale com a mesma semente e os mesmos dias."""
    return bool(base) and base.get("semente") == current.get("semente") and base.get("dias") == current.get("dias")


def load_baseline(path=BASELINE):
    if not os.path.exists(path):
        return {}

    with open(path, encoding="utf-8") as source:
        return json.load(source)


def save_baseline(baseline, path=BASELINE):
    with open(path, "w", encoding="utf-8", newline="\n") as out:
        json.dump(baseline, out, ensure_ascii=False, indent=2, sort_keys=True)
        out.write("\n")


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("targets", nargs="+")
    parser.add_argument("--reports", default=REPORTS)
    parser.add_argument("--baseline", default=BASELINE)
    parser.add_argument("--write-baseline", action="store_true", help="grava os números de agora como a base")
    parser.add_argument("--strict", action="store_true", help="reprova quando um número piora além da folga")
    args = parser.parse_args(argv)

    baseline = load_baseline(args.baseline)
    worse = False

    for target in args.targets:
        current = measure(os.path.join(args.reports, target))

        if current is None:
            print(f"## {target}\n\nsem relatório da bancada em {args.reports}\n")
            worse = True
            continue

        with open(os.path.join(args.reports, target, "metrics.json"), "w", encoding="utf-8", newline="\n") as out:
            json.dump(current, out, ensure_ascii=False, indent=2)

        base = baseline.get(target)
        usable = comparable(current, base)
        print(f"## {target} (semente {current['semente']}, {current['dias']} dias)\n")

        if not usable:
            print("sem base comparável (falta, ou outra semente ou outros dias): só informa\n")

        print("| número | base | agora | veredito |\n| --- | --- | --- | --- |")

        for name, before, now, verdict in compare(current, base if usable else None):
            print(f"| {name} | {'' if before is None else before} | {now} | {verdict} |")
            worse = worse or (usable and verdict == "pior")

        print()

        if args.write_baseline:
            baseline[target] = {key: value for key, value in current.items()}

    if args.write_baseline:
        save_baseline(baseline, args.baseline)
        print(f"base gravada em {os.path.relpath(args.baseline, ROOT)}")

        return 0

    return 1 if worse and args.strict else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
