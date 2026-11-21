#!/usr/bin/env python3
"""A interferência do jogador e o conflito com o mod — Regras 345 e 351, telemetria F1 a F3b.

Lê do log:

- ``VC_PLAYER_EDIT``: cada edição do jogador na vila (ação, bloco, alvo, lugar);
- ``VC_PLAYER_UNDO``: o jogador desfez o que um sistema do mod tinha feito;
- ``VC_MOD_OVERRIDE``: um sistema do mod desfez o que o jogador tinha feito;
- ``VC_PLAYER_INSIST``: o jogador refez o que o mod desfez — a reafirmação, o ponto de atenção que vira item a corrigir.

Uso:
    python scripts/player_report.py [log ...]

Sem argumento, lê ``latest.log`` da instalação de teste. Aceita ``.log.gz``.
"""

import gzip
import os
import re
import sys
from collections import Counter, defaultdict

EDIT = re.compile(r"VC_PLAYER_EDIT version=1 action=(\w+) block=(\S+) was=(\S+) target=(\S+) at=(.*)")
UNDO = re.compile(r"VC_PLAYER_UNDO version=1 system=(\S+) block=(\S+) was=(\S+) target=(\S+) gap=(\d+) at=(.*)")
OVERRIDE = re.compile(r"VC_MOD_OVERRIDE version=1 system=(\S+) block=(\S+) was=(\S+) gap=(\d+) at=(.*)")
INSIST = re.compile(r"VC_PLAYER_INSIST version=1 system=(\S+) block=(\S+) target=(\S+) count=(\d+) gap=(\d+) at=(.*)")

#: Uma reafirmação é atenção; duas no mesmo lugar, ou três do mesmo sistema, é achado grave.
SAME_PLACE = 2
SAME_SYSTEM = 3
GRASS_TRANSITIONS = {("dirt", "grass_block"), ("grass_block", "dirt"),
                     ("dirt_path", "dirt"), ("dirt_path", "grass_block")}


def lines(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        yield from handle


def collect(text_lines):
    """As edições por alvo e ação, os desfazeres dos dois lados e as reafirmações."""
    data = {"edits": Counter(), "undo": Counter(), "override": Counter(), "insist": [], "insist_by_system": Counter()}
    natural_at = {}
    for text in text_lines:
        match = EDIT.search(text)
        if match:
            data["edits"][(match.group(4), match.group(1))] += 1
            continue
        match = UNDO.search(text)
        if match:
            system, _block, before, target, _gap, where = match.groups()
            natural = natural_at.pop(where.strip(), None)
            if natural is None or (system, before) != natural[:2]:
                data["undo"][(system, target)] += 1
            continue
        match = OVERRIDE.search(text)
        if match:
            system, after, before, _gap, where = match.groups()
            where = where.strip()
            natural_at.pop(where, None)
            # Regra 351: logs antigos atribuíam essas transições observadas ao terreno do mod.
            if system == "mod?natural" and (before, after) in GRASS_TRANSITIONS:
                natural_at[where] = (system, after, before)
            else:
                data["override"][(system, before + "→" + after)] += 1
            continue
        match = INSIST.search(text)
        if match:
            system, block, target, count, _gap, where = match.groups()
            where = where.strip()
            natural = natural_at.pop(where, None)
            if natural is not None and (system, block) == (natural[0], natural[2]):
                continue
            data["insist"].append({"system": system, "block": block, "target": target, "count": int(count),
                                   "at": where})
            data["insist_by_system"][system] += 1
    return data


def to_fix(data):
    """As reafirmações que viram item a corrigir: (grave, frase)."""
    places = defaultdict(int)
    for item in data["insist"]:
        places[(item["system"], item["at"])] = max(places[(item["system"], item["at"])], item["count"])
    fixes = []
    for (system, where), count in sorted(places.items(), key=lambda kv: -kv[1]):
        serious = count >= SAME_PLACE or data["insist_by_system"][system] >= SAME_SYSTEM
        fixes.append((serious, f"o jogador refez {count}× em {where} o que o sistema '{system}' desfez"))
    return sorted(fixes, key=lambda fix: not fix[0])


def render(data):
    out = ["Interferência do jogador (Regra 345)", ""]
    fixes = to_fix(data)
    out.append("Para corrigir — o jogador insistiu:")
    out += [f"  {'🔴' if serious else '⚠'} {text}" for serious, text in fixes] or ["  nenhuma reafirmação"]
    out.append("")
    out.append("O jogador desfez o mod (sistema, alvo):")
    out += [f"  {n:>4}× {system} em {target}" for (system, target), n in data["undo"].most_common()] or ["  nada"]
    out.append("")
    out.append("O mod desfez o jogador (sistema, de → para):")
    out += [f"  {n:>4}× {system}: {change}" for (system, change), n in data["override"].most_common()] or ["  nada"]
    out.append("")
    out.append("Edições do jogador por alvo:")
    out += [f"  {n:>4}× {action} em {target}" for (target, action), n in data["edits"].most_common()] or ["  nenhuma"]
    return "\n".join(out)


def main(paths):
    data = collect(line for path in paths for line in lines(path))
    if not any(data[key] for key in ("edits", "undo", "override", "insist")):
        print("Nenhuma linha VC_PLAYER_* — o JAR é anterior à Regra 345, ou o jogador não mexeu na vila.")
        return 1
    print(render(data))
    return 0


if __name__ == "__main__":
    args = sys.argv[1:] or [os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "latest.log")]
    sys.exit(main(args))
