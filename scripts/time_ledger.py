#!/usr/bin/env python3
"""O tempo dos aldeões numa sessão de jogo — Regra 50 (2026-10-02).

Soma as linhas ``VC_TIME`` do log (``WorkTime``) e diz, por profissão, quanto
do expediente foi trabalho, caminhada, espera, bloqueio, ócio e encalhe.

Uso:
    python scripts/time_ledger.py [log ...]

Sem argumento, lê ``latest.log`` da instalação de teste
(``%APPDATA%/.minecraft/logs``). Aceita ``.log.gz``.

Critério (Regra 50): profissão com mais de 40% do expediente sem trabalhar
(esperando + bloqueado + ocioso + encalhado) pede melhoria de fluxo; com mais
de 10% bloqueado + encalhado, pede correção de travamento.

Regra 51 (decisão do autor, 2026-10-08): ocioso + bloqueado + encalhado acima de
15% reprova a profissão — quem fica sem tarefa tem de entrar na cadeia de
alternativas (ver docs/research/2026-10-08-tempo-ocioso.md). A espera fica fora da
conta, e acima de 40% vira alerta. Quem não tem ofício (NONE) fica fora da regra.
"""

import gzip
import os
import re
import sys
from collections import defaultdict

LINE = re.compile(r"VC_TIME version=1 colony=(\w+) window=(\d+)s(.*)")
ENTRY = re.compile(r"(\w+)\[s=(\d+) ([^\]]*)\]")
STATES = ("work", "walk", "wait", "blocked", "idle", "stranded")

FLOW_LIMIT = 40
STALL_LIMIT = 10
RULE_51_LIMIT = 15
WAIT_ALERT = 40


def verdict(profession, pct):
    """As frases do veredito desta profissão, a partir das porcentagens de cada estado."""
    idle_share = pct["wait"] + pct["blocked"] + pct["idle"] + pct["stranded"]
    stall_share = pct["blocked"] + pct["stranded"]
    rule_51 = pct["idle"] + pct["blocked"] + pct["stranded"]
    said = []
    if profession != "NONE" and rule_51 > RULE_51_LIMIT:
        said.append(f"REGRA 51 ({rule_51:.0f}% ocioso+bloqueado)")
    if profession != "NONE" and pct["wait"] > WAIT_ALERT:
        said.append(f"ESPERA ({pct['wait']:.0f}%)")
    if stall_share > STALL_LIMIT:
        said.append(f"TRAVAMENTO ({stall_share:.0f}% bloqueado+encalhado)")
    if idle_share > FLOW_LIMIT:
        said.append(f"FLUXO ({idle_share:.0f}% sem trabalhar)")
    return said


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def main(paths):
    seconds = defaultdict(lambda: defaultdict(float))
    windows = 0

    for path in paths:
        for text in lines(path):
            match = LINE.search(text)
            if not match:
                continue
            windows += 1
            for profession, total, shares in ENTRY.findall(match.group(3)):
                total = int(total)
                seconds[profession]["total"] += total
                for pair in shares.split():
                    state, share = pair.split("=")
                    seconds[profession][state] += total * int(share) / 100

    if not seconds:
        print("Nenhuma linha VC_TIME — o JAR é anterior à Regra 50, ou a vila não foi atendida.")
        return 1

    print(f"{windows} janelas de 5 min\n")
    header = f"{'profissão':<12}{'segundos':>9}" + "".join(f"{s:>10}" for s in STATES) + "   veredito"
    print(header)
    print("-" * len(header))

    for profession in sorted(seconds):
        row = seconds[profession]
        total = row["total"] or 1
        pct = {s: 100 * row[s] / total for s in STATES}
        said = verdict(profession, pct)
        print(f"{profession:<12}{row['total']:>9.0f}" + "".join(f"{pct[s]:>9.0f}%" for s in STATES)
              + "   " + ("; ".join(said) or "ok"))

    return 0


if __name__ == "__main__":
    args = sys.argv[1:] or [os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "latest.log")]
    sys.exit(main(args))
