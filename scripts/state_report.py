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
- as notas do jogador.

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
    """Os registros de um arquivo JSONL do mundo ou das linhas VC_STATE de um log."""
    found = []
    with open(path, encoding="utf-8", errors="replace") as source:
        for line in source:
            line = line.strip()
            if not line:
                continue
            if "VC_STATE " in line:
                line = line.split("VC_STATE ", 1)[1]
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
    for record in records:
        if record.get("kind") == "note":
            notes[record.get("colony", "-")].append(record)
        elif "people" in record:
            by_colony[record.get("colony", "?")].append(record)

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
        }
    return summaries


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
    return lines


def main(arguments):
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
