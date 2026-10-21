"""Nota de 0 a 10 por bioma, a partir dos relatórios da bancada (scripts/biome_sim.py).

    python scripts/biome_score.py                 # todos os alvos em run/biome-sim/reports
    python scripts/biome_score.py desert snowy

A régua (10 pontos), escrita para a nota sair dos números e não de impressão:

    nasce        2   a colônia nasceu (sem ela, o resto não tem o que medir)
    constrói     2   obras concluídas por dia de jogo: >= 1 dá 2, > 0 dá 1
    trabalha     3   média, entre as profissões, de ocioso + bloqueado + encalhado no
                     expediente (time_ledger): 15% (Regra 51) ou menos dá 3, 70% ou mais dá 0
    nada parado  2   profissões com 80% ou mais sem trabalhar: nenhuma dá 2, uma dá 1
    estável      1   a população não caiu e a colônia não foi abandonada
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPORTS = os.path.join(ROOT, "run", "biome-sim", "reports")

ROW = re.compile(r"^(\w+)\s+(\d+)\s+(\d+)%\s+(\d+)%\s+(\d+)%\s+(\d+)%\s+(\d+)%\s+(\d+)%")


def read(path):
    if not os.path.exists(path):
        return ""

    with open(path, encoding="utf-8", errors="replace") as source:
        return source.read()


def professions(ledger):
    """profissão -> (trabalho, espera, ocioso+bloqueado+encalhado), em %."""
    found = {}

    for line in ledger.splitlines():
        match = ROW.match(line.strip())

        if match and match.group(1) != "NONE":
            work, walk, wait, blocked, idle, stranded = (int(match.group(i)) for i in range(3, 9))
            found[match.group(1)] = (work, wait, blocked + idle + stranded)

    return found


def score(name):
    folder = os.path.join(REPORTS, name)
    signals = read(os.path.join(folder, "signals.txt"))

    def count(label):
        found = re.search(re.escape(label) + r": (\d+)", signals)
        return int(found.group(1)) if found else 0

    days = int((re.search(r"dias=(\d+)", signals) or re.search("()", "")).group(1) or 1)
    people = re.search(r"aldeões: (\d+) no início, (\d+) no fim", signals)
    jobs = professions(read(os.path.join(folder, "time_ledger.txt")))

    born = count("colônia criada") > 0
    parts = {"nasce": 2 if born else 0}

    if not born or not jobs:
        parts.update({"constrói": 0, "trabalha": 0, "nada parado": 0, "estável": 0})
        return parts, jobs, days, people

    finished = count("obra concluída") / days
    parts["constrói"] = 2 if finished >= 1 else 1 if finished > 0 else 0

    stopped = sum(v[2] for v in jobs.values()) / len(jobs)
    parts["trabalha"] = round(3 * min(1.0, max(0.0, (70 - stopped) / 55)), 1)

    frozen = sum(1 for v in jobs.values() if v[2] >= 80)
    parts["nada parado"] = 2 if frozen == 0 else 1 if frozen == 1 else 0

    grew = people is None or int(people.group(2)) >= int(people.group(1))
    parts["estável"] = 1 if grew and count("abandonada") == 0 else 0

    return parts, jobs, days, people


def main(argv):
    names = argv or sorted(d for d in os.listdir(REPORTS) if os.path.isdir(os.path.join(REPORTS, d)))

    for name in names:
        parts, jobs, days, people = score(name)
        total = sum(parts.values())
        frozen = [job for job, v in sorted(jobs.items()) if v[2] >= 80]
        stopped = sum(v[2] for v in jobs.values()) / len(jobs) if jobs else 0
        crowd = f"{people.group(1)}->{people.group(2)}" if people else "?"

        print(f"{name:24} {total:4.1f}  " + "  ".join(f"{k} {v}" for k, v in parts.items())
              + f"  | dias {days}, aldeões {crowd}, sem trabalhar {stopped:.0f}%,"
              + f" paradas: {', '.join(frozen) or '-'}")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
