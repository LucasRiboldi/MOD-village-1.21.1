"""Relatório do retrato da vila (VC_STATE) — a evolução de cada colônia entre sessões e versões.

Lê os arquivos que o mod grava na pasta do mundo (`<mundo>/villagecolony/analytics/*.jsonl`) e as linhas
`VC_STATE {...}` de um log do jogo. Por colônia, mostra:

- o período e a versão do mod;
- a população, os ofícios e o emprego;
- as obras prontas por hora;
- o enchimento dos baús;
- quantos ficaram travados;
- a média da Regra 51 por ofício;
- o servidor (MSPT);
- as notas do jogador;
- o diário da vila: o resumo de cada dia de jogo (VC_DAY), os eventos por tipo (VC_EVENT) e as
  auditorias contra o mundo com problema (VC_AUDIT).

Uso:
    python scripts/state_report.py                       # mundos da instalação de teste
    python scripts/state_report.py caminho.jsonl|pasta|latest.log ...
"""
import json
import os
import pathlib
import sys
from collections import defaultdict

TEST_INSTALL = pathlib.Path(os.environ.get("APPDATA", "")) / ".minecraft"
TICKS_PER_HOUR = 72_000


def records_from(path):
    """Os registros de um arquivo JSONL do mundo ou das linhas VC_* de um log."""
    with open(path, encoding="utf-8", errors="replace") as source:
        return records_from_lines(source)


def records_from_lines(lines):
    """Os registros JSON destas linhas: a linha inteira, ou o que vem depois de VC_STATE/EVENT/DAY/AUDIT."""
    found = []
    for line in lines:
        line = line.strip()
        if not line:
            continue
        for prefix in ("VC_STATE ", "VC_EVENT ", "VC_DAY ", "VC_AUDIT "):
            if prefix in line:
                line = line.split(prefix, 1)[1]
                break
        if not line.startswith("{"):
            continue
        try:
            found.append(json.loads(line))
        except json.JSONDecodeError:
            continue
    return found


def files_from(arguments):
    """Os arquivos pedidos; sem argumento, os registros de todos os mundos da instalação de teste."""
    if arguments:
        paths = []
        for argument in arguments:
            path = pathlib.Path(argument)
            paths.extend(sorted(path.glob("*.jsonl")) if path.is_dir() else [path])
        return paths
    return sorted((TEST_INSTALL / "saves").glob("*/villagecolony/analytics/*.jsonl"))


def summarize(records):
    """Por colônia: o resumo do período, na ordem dos tiques."""
    by_colony = defaultdict(list)
    notes = defaultdict(list)
    days = defaultdict(list)
    events = defaultdict(lambda: defaultdict(int))
    audits = defaultdict(list)
    for record in records:
        kind = record.get("kind")
        colony = record.get("colony", "?")
        if kind == "note":
            notes[record.get("colony", "-")].append(record)
        elif kind == "day":
            days[colony].append(record)
        elif kind == "event":
            events[colony][record.get("type", "?")] += 1
        elif kind == "audit":
            audits[colony].append(record)
        elif "people" in record:
            by_colony[colony].append(record)

    summaries = {}
    for colony, states in by_colony.items():
        states.sort(key=lambda state: state.get("tick", 0))
        first, last = states[0], states[-1]
        hours = max(1e-9, (last.get("tick", 0) - first.get("tick", 0)) / TICKS_PER_HOUR)
        rule51 = defaultdict(list)
        stuck = 0
        for state in states:
            stuck += state.get("people", {}).get("badges", {}).get("STUCK", 0)
            for trade, shares in state.get("time", {}).items():
                if trade != "NONE" and "rule51" in shares:
                    rule51[trade].append(shares["rule51"])
        mspt = [state["mspt"] for state in states if "mspt" in state]
        summaries[colony] = {
            "samples": len(states),
            "mods": sorted({state.get("mod", "?") for state in states}),
            "biome": last.get("biome", "?"),
            "hours": round(hours, 2) if len(states) > 1 else 0,
            "villagers": (first["people"].get("villagers", 0), last["people"].get("villagers", 0)),
            "employed": (first["people"].get("employed", 0), last["people"].get("employed", 0)),
            "buildings": (first.get("build", {}).get("buildings", 0), last.get("build", {}).get("buildings", 0)),
            "builds_per_hour": round((last.get("build", {}).get("buildings", 0)
                                      - first.get("build", {}).get("buildings", 0)) / hours, 2) if len(states) > 1 else 0,
            "fill": (first.get("stock", {}).get("fill"), last.get("stock", {}).get("fill")),
            "stuck_samples": stuck,
            "rule51": {trade: round(sum(values) / len(values)) for trade, values in sorted(rule51.items())},
            "mspt": (round(sum(mspt) / len(mspt), 1), max(mspt)) if mspt else None,
            "notes": [note.get("text", "") for note in notes.get(colony, [])],
            "days": [day_line(day) for day in sorted(days.get(colony, []), key=lambda d: d.get("ofDay", 0))],
            "events": dict(sorted(events.get(colony, {}).items())),
            "audits": [audit for audit in audits.get(colony, []) if audit.get("problems", 0) > 0],
        }
    return summaries


def day_line(day):
    """Uma linha por dia de jogo: população, obras, camas, baús e a Regra 51 de cada ofício no dia."""
    people = day.get("people", {})
    rule51 = ", ".join(f"{trade} {shares.get('rule51', '?')}%" for trade, shares in sorted(day.get("dayTime", {}).items())
                       if trade != "NONE")
    return (f"dia {day.get('ofDay', '?')}: aldeões {people.get('villagers', '?')}, camas {people.get('beds', '?')},"
            f" obras {day.get('build', {}).get('buildings', '?')}, baús {day.get('stock', {}).get('fill', '?')}%"
            f" — Regra 51: {rule51 or '-'}")


def render(summaries):
    lines = []
    for colony, s in summaries.items():
        lines.append(f"Colônia {colony} — {s['biome']}, {s['samples']} retratos em {s['hours']} h, mod {', '.join(s['mods'])}")
        lines.append(f"  aldeões {s['villagers'][0]} → {s['villagers'][1]}; com ofício {s['employed'][0]} → {s['employed'][1]}")
        lines.append(f"  obras prontas {s['buildings'][0]} → {s['buildings'][1]} ({s['builds_per_hour']} por hora)")
        lines.append(f"  baús cheios {s['fill'][0]}% → {s['fill'][1]}%; travados somados nos retratos: {s['stuck_samples']}")
        if s["mspt"]:
            lines.append(f"  servidor: {s['mspt'][0]} ms por tique em média, pior retrato {s['mspt'][1]} ms")
        over = {trade: share for trade, share in s["rule51"].items() if share > 15}
        lines.append("  Regra 51 (média): " + (", ".join(f"{t} {v}%" for t, v in s["rule51"].items()) or "-")
                     + (f" — acima de 15%: {', '.join(over)}" if over else ""))
        for note in s["notes"]:
            lines.append(f"  nota: {note}")
        for day in s["days"]:
            lines.append(f"  {day}")
        if s["events"]:
            lines.append("  eventos: " + ", ".join(f"{kind} {count}" for kind, count in s["events"].items()))
        for audit in s["audits"]:
            details = {key: value for key, value in audit.items()
                       if key not in ("schema", "mod", "tick", "kind", "colony", "at")}
            lines.append(f"  auditoria com problema: {details}")
    return lines


def main(arguments):
    # O console do Windows (cp1252) não tem o "→" do relatório: a saída vai em UTF-8.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    records = []
    for path in files_from(arguments):
        records.extend(records_from(path))
    if not records:
        print("Nenhum retrato VC_STATE encontrado.")
        return 1
    print("\n".join(render(summarize(records))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
