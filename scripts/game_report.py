#!/usr/bin/env python3
"""O relatório único de um jogo — Regra 344, telemetria T6.

Junta, num arquivo só, o que os relatórios do projeto dizem sobre o log e o diário de ações mais recentes, e põe no
topo os achados mais graves, em ordem:

0. o jogador refez o que o mod desfez (``VC_PLAYER_INSIST``, Regra 345): "Para corrigir — o jogador insistiu";
1. ofício fora da régua de tempo: Regra 51 (ocioso + bloqueado + encalhado > 15%), espera acima de 40%,
   travamento (bloqueado + encalhado > 10%) e fluxo (mais de 40% sem trabalhar, Regra 50);
2. aldeão morto (``VC_VILLAGER_DEATH``) e aldeão sem cama (``VC_VILLAGER_DAY``);
3. item que mudou por fora ou foi recusado no baú (``VC_FLOW``);
4. tipo de tarefa com mais de 30% soltas (diário, Regra 344);
5. bloco que pede reserva maior ou falta de cru, trava que não age ou segura demais (``VC_BLOCKS``, ``VC_KEEP``);
6. ofício Vanilla barrado ou tirado (``VC_VANILLA_JOB``).

Depois vêm as tabelas de cada relatório. Nada é lido além do log e do diário.

Uso:
    python scripts/game_report.py [--log latest.log] [--journal villagecolony-actions-*.jsonl] [--out arquivo.md]

Sem argumentos: o ``latest.log`` e o diário mais novo da instalação de teste; o arquivo vai para
``docs/testing/relatorios/jogo-<data>.md``.
"""
from __future__ import annotations

import argparse
import datetime
import io
import os
import re
import sys
from collections import Counter
from contextlib import redirect_stdout

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import action_report  # noqa: E402
import flow_report  # noqa: E402
import player_report  # noqa: E402
import stock_report  # noqa: E402
import task_report  # noqa: E402
import time_ledger  # noqa: E402
import villager_timeline  # noqa: E402

DAY_LINE = re.compile(r"VC_VILLAGER_DAY version=1 colony=(\w+) day=(\d+) villagers=\d+(.*)")
DAY_ENTRY = re.compile(r"(\w+)=(\w+),(\d+),(\d+),(\d+),(\d+),(\d+)")
DEATH = re.compile(r"VC_VILLAGER_DEATH version=1 colony=\w+ worker=(\w+) prof=(\w+) cause=(\S+) at=(.*)")
VANILLA = re.compile(r"VC_VANILLA_JOB version=1 action=(\w+) worker=(\w+) trade=(\S+)")
RELEASED_TOO_MUCH = 0.3


def villager_days(text_lines):
    """Por aldeão: ofício, segundos sem cama somados, menor vida."""
    days = {}
    for text in text_lines:
        match = DAY_LINE.search(text)
        if not match:
            continue
        for worker, prof, _work, _sleep, _awake, no_bed, health in DAY_ENTRY.findall(match.group(3)):
            row = days.setdefault(worker, {"prof": prof, "no_bed": 0, "health": int(health)})
            row["no_bed"] += int(no_bed)
            row["health"] = min(row["health"], int(health))
    return days


def findings(log_lines, records):
    """Os achados, do mais grave ao menos, cada um uma frase."""
    found = []

    # A reafirmação do jogador vem antes de tudo: é o que ele quer, dito com a mão (Regra 345).
    for serious, text in player_report.to_fix(player_report.collect(log_lines)):
        found.append((0, ("🔴 " if serious else "⚠ ") + "Para corrigir — " + text))

    seconds, _windows = time_ledger.collect(log_lines)
    for profession, row in sorted(seconds.items()):
        total = row["total"] or 1
        pct = {state: 100 * row[state] / total for state in time_ledger.STATES}
        for said in time_ledger.verdict(profession, pct):
            found.append((1, f"{profession}: {said}"))

    for text in log_lines:
        death = DEATH.search(text)
        if death:
            found.append((2, f"aldeão {death.group(1)} ({death.group(2)}) morreu: {death.group(3)} em {death.group(4)}"))
    for worker, row in sorted(villager_days(log_lines).items()):
        if row["no_bed"] > 0:
            found.append((2, f"aldeão {worker} ({row['prof']}) ficou {row['no_bed']} s sem cama"))

    for key, row in flow_report.collect(log_lines).items():
        if row["outside"] or row["refused"]:
            found.append((3, f"item {key}: {row['outside']:+d} por fora, {row['refused']} recusado no baú"))

    for kind, row in task_report.summarize(task_report.trace(records)).items():
        if row["count"] >= 5 and row["released"] / row["count"] > RELEASED_TOO_MUCH:
            reason = row["reasons"].most_common(1)[0][0] if row["reasons"] else "-"
            found.append((4, f"tarefa {kind}: {row['released']} de {row['count']} soltas, mais por: {reason}"))

    for key, row in stock_report.collect_blocks(log_lines).items():
        advice = stock_report.block_advice(row)
        if advice != "ok":
            found.append((5, f"bloco {key}: {advice}"))
    for lock, row in stock_report.collect_keeps(log_lines).items():
        advice = stock_report.keep_advice(row)
        if advice != "ok":
            found.append((5, f"trava {lock}: {advice}"))

    vanilla = Counter()
    for text in log_lines:
        match = VANILLA.search(text)
        if match:
            vanilla[(match.group(1), match.group(3))] += 1
    for (action, trade), count in vanilla.most_common():
        found.append((6, f"ofício Vanilla {trade} {action} {count}×"))

    return sorted(found)


def section(title, render):
    """O que um relatório imprime, num bloco de texto."""
    buffer = io.StringIO()
    with redirect_stdout(buffer):
        render()
    return f"## {title}\n\n```text\n{buffer.getvalue().rstrip()}\n```\n"


def build(log_lines, records, when):
    found = findings(log_lines, records)
    out = [f"# Relatório do jogo — {when}", "", "## Achados, do mais grave ao menos", ""]
    out += [f"{n}. {text}" for n, (_rank, text) in enumerate(found[:30], start=1)] or ["Nenhum achado."]
    if len(found) > 30:
        out.append(f"\n… e mais {len(found) - 30}.")
    out.append("")
    out.append(section("Tempo por ofício", lambda: time_ledger_print(log_lines)))
    out.append(section("Tarefas", lambda: print(task_report.render(task_report.summarize(task_report.trace(records))))))
    out.append(section("Linha do tempo dos aldeões",
                       lambda: print(villager_timeline.render(villager_timeline.stretches(records)))))
    out.append(section("Diário de ações", lambda: print(action_report.render(*action_report.summarize(records))
                                                        + action_report.render_items(action_report.by_item(records)))))
    out.append(section("Balanço dos itens", lambda: print(flow_report.render(flow_report.collect(log_lines)))))
    out.append(section("Blocos e travas", lambda: stock_print(log_lines)))
    out.append(section("Interferência do jogador",
                       lambda: print(player_report.render(player_report.collect(log_lines)))))
    out.append(section("Plano de alturas (ADR-063)", lambda: grade_print(log_lines)))
    return "\n".join(out)


def time_ledger_print(log_lines):
    seconds, windows = time_ledger.collect(log_lines)
    print(f"{windows} janelas de 5 min")
    for profession, row in sorted(seconds.items()):
        total = row["total"] or 1
        pct = {state: 100 * row[state] / total for state in time_ledger.STATES}
        print(f"{profession:<12}" + "".join(f"{state} {pct[state]:.0f}%  " for state in time_ledger.STATES)
              + "; ".join(time_ledger.verdict(profession, pct)))


GRADE_LINE = re.compile(r"VC_GRADE version=1 colony=(\w+) day=(\d+) columns=(\d+) anchors=(\d+) doors=(\d+) "
                        r"out=(\d+) path=(\d+) steps=(\d+) pending=(\d+) conflicts=(\d+) ms=(\d+)")


def grade_print(log_lines):
    """O plano de alturas por dia: o que ele mexeria e quantos degraus maiores que 1 o chão tem (ADR-063)."""
    rows = [match.groups() for match in map(GRADE_LINE.search, log_lines) if match]
    if not rows:
        print("Sem linha VC_GRADE.")
        return
    print(f"{'colônia':<9} {'dia':>4} {'colunas':>8} {'âncoras':>8} {'portas':>6} {'fora':>6} {'caminho':>8} "
          f"{'degraus':>8} {'a mexer':>8} {'conflitos':>9} {'ms':>5}")
    for colony, *numbers in rows:
        day, columns, anchors, doors, out, path, steps, pending, conflicts, millis = numbers
        print(f"{colony:<9} {day:>4} {columns:>8} {anchors:>8} {doors:>6} {out:>6} {path:>8} {steps:>8} {pending:>8} "
              f"{conflicts:>9} {millis:>5}")


def stock_print(log_lines):
    blocks = stock_report.collect_blocks(log_lines)
    keeps = stock_report.collect_keeps(log_lines)
    if blocks:
        stock_report.print_blocks(blocks)
    if keeps:
        stock_report.print_keeps(keeps)


def main(argv):
    parser = argparse.ArgumentParser(description="Relatório único de um jogo (Regra 344).")
    logs = os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs")
    parser.add_argument("--log", default=os.path.join(logs, "latest.log"))
    parser.add_argument("--journal", nargs="*", default=None)
    parser.add_argument("--out", default=None)
    args = parser.parse_args(argv[1:])

    log_lines = list(time_ledger.lines(args.log)) if os.path.exists(args.log) else []
    records = []
    for path in args.journal if args.journal is not None else action_report.newest_journal():
        with open(path, encoding="utf-8") as handle:
            records.extend(action_report.parse(handle))
    if not log_lines and not records:
        print("Nem log nem diário para ler.")
        return 1

    when = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
    out = args.out or os.path.join(HERE, "..", "docs", "testing", "relatorios",
                                   "jogo-" + datetime.datetime.now().strftime("%Y%m%d-%H%M") + ".md")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with open(out, "w", encoding="utf-8") as handle:
        handle.write(build(log_lines, records, when))
    print(f"Relatório: {os.path.abspath(out)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
