"""Relatório de uma sessão de jogo num bioma — pedido do autor, 2026-10-20.

Lê o log do jogo (linhas VC_BIOME, VC_BIOME_NOTE, VC_TIME, VC_SUPPLY_ERROR e as frases da colônia), o
diário de ações (logs/villagecolony-actions-*.jsonl), o registro de faltas
(villagecolony/supply-gaps.jsonl) e as peças inalcançáveis (VC_UNREACHABLE), e escreve um relatório em
Markdown: a nota de 0 a 10 (a mesma régua de scripts/biome_score.py), o que funcionou, os problemas que o
log aponta e as notas do jogador.

    python scripts/biome_report.py                                # a instalação de teste (%APPDATA%/.minecraft)
    python scripts/biome_report.py --game run/biome-sim           # a bancada de simulação
    python scripts/biome_report.py --game <pasta> --out docs/testing/relatorios/deserto.md

Roteiro: docs/testing/roteiro-por-bioma.md.
"""

import argparse
import collections
import glob
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import time_ledger  # noqa: E402  (mesma pasta)

BIOME = re.compile(r"VC_BIOME version=1 mod=(\S+) colony=(\w+) biome=(\S+) style=(\S+) beds=(\d+) villagers=(\d+)"
                   r" employed=(\d+) buildings=(\d+) project=(\S+) lots_ok=(\d+) refusals=(\S+) gaps=(\d+)"
                   r" stranded=(\d+) over51=(\S+)")
NOTE = re.compile(r"VC_BIOME_NOTE version=1 mod=(\S+) colony=(\S+) biome=(\S+) x=(-?\d+) y=(-?\d+) z=(-?\d+)"
                  r" text=\"(.*)\"")
SUPPLY = re.compile(r"VC_SUPPLY_ERROR version=\d+ piece=(\S+) profession=(\w+) attempts=\d+(?: gap=(\w+))?")
UNREACHABLE = re.compile(r"VC_UNREACHABLE version=\d+ project=\S+ piece=(\S+) at=(-?\d+), (-?\d+), (-?\d+)")
REFUSALS = re.compile(r"lot refusals: (\d+) candidates turned down — (.*)")
SIGNALS = {
    "colônia criada": r"Colony created at",
    "adoção adiada (casa fundadora sem lote)": r"Deferred colony adoption",
    "obra planejada": r"Colony \S+ planned ",
    "obra concluída": r"Colony \S+ finished \S+ at",
    "abandonada": r"is now ABANDONED",
    "encalhe": r"Stranded worker",
    "teleporte pela cama": r"was teleported from .* to its bed",
    "lote da casa fundadora aplainado": r"levelled the BigHouseMOD lot",
    "lote da casa fundadora limpo": r"cleared the BigHouseMOD lot",
    "árvores tiradas de lote de obra": r"cleared \d+ blocks off the site",
    "obra travada por peça (G2)": r"stalled the construction for",
    "peça inalcançável que surgiu": r"VC_UNREACHABLE",
}

# O que cada motivo de recusa de lote costuma querer dizer — para o problema já vir com a pista.
REFUSAL_HINT = {
    "no ground in the village's vertical window": "relevo: o chão sai da janela vertical da vila (terraplanagem)",
    "the ground is not at street level": "relevo: Regra 19, lote fora do nível da rua (terraplanagem)",
    "something stands inside the house's volume": "algo no volume da casa (árvore fora de bioma de mata, bloco)",
    "the ground there is not natural soil": "chão que não é solo do bioma",
    "village-original or player-placed, and Rule 3 protects it": "peça da vila gerada (Regra 3)",
    "the ground is inside a reserved road area": "reserva de rua",
}


def read(path):
    if not path or not os.path.exists(path):
        return ""
    opener = __import__("gzip").open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as source:
        return source.read()


def professions(log_path):
    seconds = collections.defaultdict(lambda: collections.defaultdict(float))
    for text in time_ledger.lines(log_path):
        match = time_ledger.LINE.search(text)
        if not match:
            continue
        for profession, total, shares in time_ledger.ENTRY.findall(match.group(3)):
            seconds[profession]["total"] += int(total)
            for pair in shares.split():
                state, share = pair.split("=")
                seconds[profession][state] += int(total) * int(share) / 100
    table = {}
    for profession, row in seconds.items():
        total = row["total"] or 1
        table[profession] = {s: 100 * row[s] / total for s in time_ledger.STATES} | {"total": row["total"]}
    return table


def actions(paths):
    counts = collections.Counter()
    paved = collections.Counter()
    reasons = collections.Counter()
    mined = collections.Counter()
    for path in paths:
        for line in read(path).splitlines():
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            counts[(event.get("prof"), event.get("action"))] += 1
            if event.get("action") == "PAVED":
                paved[event.get("what")] += 1
            if event.get("action") == "MINED":
                mined[(event.get("x"), event.get("y"), event.get("z"))] += 1
            if event.get("action") in ("TARGET_REJECTED", "TASK_RELEASED", "GAVE_UP", "SET_ASIDE"):
                reasons[(event.get("prof"), event.get("action"), event.get("detail"))] += 1
    return counts, paved, reasons, mined


def score(signals, table, snapshots, days):
    """A régua de scripts/biome_score.py: nasce 2, constrói 2, trabalha 3, nada parado 2, estável 1."""
    born = signals["colônia criada"] > 0 or bool(snapshots)
    parts = {"nasce": 2 if born else 0}
    jobs = {p: v for p, v in table.items() if p != "NONE"}
    if not born or not jobs:
        return parts | {"constrói": 0, "trabalha": 0, "nada parado": 0, "estável": 0}
    finished = signals["obra concluída"] / max(days, 0.01)
    parts["constrói"] = 2 if finished >= 1 else 1 if finished > 0 else 0
    stopped = sum(v["idle"] + v["blocked"] + v["stranded"] for v in jobs.values()) / len(jobs)
    parts["trabalha"] = round(3 * min(1.0, max(0.0, (70 - stopped) / 55)), 1)
    frozen = sum(1 for v in jobs.values() if v["idle"] + v["blocked"] + v["stranded"] >= 80)
    parts["nada parado"] = 2 if frozen == 0 else 1 if frozen == 1 else 0
    parts["estável"] = 0 if signals["abandonada"] else 1
    return parts


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--game", default=os.path.join(os.environ.get("APPDATA", ""), ".minecraft"),
                        help="pasta do jogo (com logs/ e villagecolony/)")
    parser.add_argument("--log", help="log a ler (padrão: <game>/logs/latest.log)")
    parser.add_argument("--out", help="grava o relatório neste arquivo, além de mostrar")
    args = parser.parse_args(argv)

    log_path = args.log or os.path.join(args.game, "logs", "latest.log")
    text = read(log_path)
    if not text:
        print(f"Log vazio ou ausente: {log_path}")
        return 1

    version = (re.search(r"villagecolony (\d+\.\d+\.\d+)", text) or re.search("()", "")).group(1) or "?"
    snapshots = [m.groups() for m in BIOME.finditer(text)]
    notes = [m.groups() for m in NOTE.finditer(text)]
    signals = {name: len(re.findall(pattern, text)) for name, pattern in SIGNALS.items()}
    supply = collections.Counter((m.group(1).replace("minecraft:", ""), m.group(3) or "?") for m in SUPPLY.finditer(text))
    unreachable = collections.Counter(m.group(1).replace("minecraft:", "") for m in UNREACHABLE.finditer(text))
    refusal = REFUSALS.findall(text)
    table = professions(log_path)
    journals = sorted(glob.glob(os.path.join(os.path.dirname(log_path), "villagecolony-actions-*.jsonl")))[-1:]
    counts, paved, reasons, mined = actions(journals)
    days = len(snapshots) * 5 / 20 if snapshots else 0  # uma linha VC_BIOME a cada 5 min; dia = 20 min

    biomes = collections.Counter(s[2] for s in snapshots)
    styles = collections.Counter(s[3] for s in snapshots)
    parts = score(signals, table, snapshots, days)
    out = []
    say = out.append

    title = ", ".join(biomes) or "bioma não registrado (sem VC_BIOME)"
    say(f"# Relatório do bioma — {title}\n")
    say(f"Mod {version}. Estilo da vila: {', '.join(styles) or '?'}. Tempo atendido: {days:.1f} dia(s) de jogo "
        f"({len(snapshots)} retratos VC_BIOME).\n")
    say(f"**Nota: {sum(parts.values()):.1f} / 10** — " + ", ".join(f"{k} {v}" for k, v in parts.items()) + "\n")

    if snapshots:
        last = snapshots[-1]
        say("## A vila no fim da sessão\n")
        say(f"- camas {last[4]}, aldeões {last[5]} ({last[6]} com ofício), construções {last[7]}, encalhados {last[12]}")
        say(f"- obra aberta: {last[8]}; lotes aprovados: {last[9]}; recusas principais: {last[10]}")
        say(f"- peças que surgiram sem rota: {last[11]}; acima da Regra 51: {last[13]}\n")

    say("## Sinais do log\n")
    for name, count in signals.items():
        say(f"- {name}: {count}")
    if paved:
        say("- caminhos assentados pelo construtor: " + ", ".join(f"{k} {v}" for k, v in paved.most_common()))
    shored = counts[("MINER", "SHORED")]
    if shored or counts[("MINER", "MINED")]:
        repeated = sum(1 for v in mined.values() if v > 1)
        say(f"- mina: {counts[('MINER', 'MINED')]} blocos, {shored} tetos escorados, {repeated} posições repetidas")
    say("")

    if table:
        say("## Tempo das profissões (expediente)\n")
        say("| profissão | trabalho | caminhada | espera | bloqueio | ócio | encalhe | Regra 51 |")
        say("|---|---|---|---|---|---|---|---|")
        for profession, pct in sorted(table.items()):
            rule = pct["idle"] + pct["blocked"] + pct["stranded"]
            say(f"| {profession} | {pct['work']:.0f}% | {pct['walk']:.0f}% | {pct['wait']:.0f}% | {pct['blocked']:.0f}% "
                f"| {pct['idle']:.0f}% | {pct['stranded']:.0f}% | {'ok' if rule <= 15 else f'{rule:.0f}%'} |")
        say("")

    problems = []
    if signals["adoção adiada (casa fundadora sem lote)"] and not signals["colônia criada"]:
        problems.append("a colônia não nasceu: a casa fundadora não achou lote (ver relevo e vegetação)")
    if signals["colônia criada"] and not signals["obra concluída"]:
        problems.append("nenhuma obra concluída na sessão")
    for profession, pct in sorted(table.items()):
        rule = pct["idle"] + pct["blocked"] + pct["stranded"]
        if profession != "NONE" and rule > 15:
            problems.append(f"{profession}: {rule:.0f}% ocioso+bloqueado+encalhado (Regra 51: ≤ 15%)")
    if refusal:
        total, detail = refusal[-1]
        top = max(((int(n), why.strip()) for n, why in re.findall(r"(\d+) ([^;]+)", detail)), default=None)
        if top:
            problems.append(f"lote: {top[0]} de {total} recusas por \"{top[1]}\" — {REFUSAL_HINT.get(top[1], 'ver LotRefusals')}")
    for (item, gap), count in supply.most_common(8):
        problems.append(f"falta de rota: {item} surgiu {count}x ({gap})")
    for item, count in unreachable.most_common(5):
        problems.append(f"acesso: {item} surgiu {count}x sem o construtor alcançar (unreachable-pieces.jsonl)")
    for (prof, action, why), count in reasons.most_common(6):
        if count >= 5:
            problems.append(f"{prof} {action}: {why} ({count}x)")

    say("## Problemas que o log aponta\n")
    say("\n".join(f"- {p}" for p in problems) or "- nenhum")
    say("")

    say("## Notas do jogador (/vc bioma nota)\n")
    say("\n".join(f"- [{n[2]} {n[3]}, {n[4]}, {n[5]}] {n[6]}" for n in notes) or "- nenhuma")

    report = "\n".join(out) + "\n"
    if args.out:
        os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
        with open(args.out, "w", encoding="utf-8") as target:
            target.write(report)
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # o console do Windows não é UTF-8
    print(report)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
