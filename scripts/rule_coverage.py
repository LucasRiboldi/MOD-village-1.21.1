#!/usr/bin/env python3
"""Cruza cada regra de docs/RULES.md com os testes das suites do Sentinela e escreve docs/testing/regras-e-testes.md.

Três graus de prova, do mais forte ao mais fraco: o teste cita "Regra N"; um teste só usa a classe que a regra indica; nenhum.
Citar não prova que o teste mede a regra, e usar a classe prova menos ainda: é um mapa de onde olhar, não um atestado.
"""
import argparse
import glob
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RULES = ROOT / "docs" / "RULES.md"
OUTPUT = ROOT / "docs" / "testing" / "regras-e-testes.md"
TEST_GLOBS = ("src/test/**/*.java", "src/gametest/**/*.java", "tests/**/*.py")
ROW = re.compile(r"\|\s*(\d+)\s*\|(.*)\|\s*$")
SINGLE = re.compile(r"Regra\s+(\d{1,3})\b")
RANGE = re.compile(r"Regras\s+(\d{1,3})\s+a\s+(\d{1,3})\b")
PAIR = re.compile(r"Regras\s+(\d{1,3})\s+e\s+(\d{1,3})\b")
CLASS = re.compile(r"`([A-Z][A-Za-z0-9]+)(?:[.#][A-Za-z_]+)?")
RETIRED = re.compile(r"❌|↪|substitu|retirada|revogada", re.IGNORECASE)
NAME_LENGTH = 90


def read_rules(path):
    rules = {}

    for line in path.read_text(encoding="utf-8").splitlines():
        match = ROW.match(line)

        if match and int(match.group(1)) not in rules:
            cells = [cell.strip() for cell in match.group(2).split("|")]

            if len(cells) >= 2 and not cells[0].startswith("Emenda"):
                rules[int(match.group(1))] = cells

    return rules


def read_tests(root):
    tests = {}

    for pattern in TEST_GLOBS:
        for name in glob.glob(str(root / pattern), recursive=True):
            tests[Path(name).stem] = tests.get(Path(name).stem, "") + Path(name).read_text(encoding="utf-8", errors="replace")

    return tests


def citations(tests):
    cited = defaultdict(set)

    for name, text in tests.items():
        for match in SINGLE.finditer(text):
            cited[int(match.group(1))].add(name)

        for match in RANGE.finditer(text):
            for number in range(int(match.group(1)), int(match.group(2)) + 1):
                cited[number].add(name)

        for match in PAIR.finditer(text):
            cited[int(match.group(1))].add(name)
            cited[int(match.group(2))].add(name)

    return cited


def classify(rules, tests):
    cited = citations(tests)
    rows = []

    for number, cells in sorted(rules.items()):
        state = cells[-2] if len(cells) >= 3 else ""
        classes = sorted(set(CLASS.findall(cells[-1])))
        users = sorted(name for name, text in tests.items()
                       if any(re.search(r"\b" + re.escape(item) + r"\b", text) for item in classes))

        if RETIRED.search(state):
            grade = "fora de vigor"
            proof = ""
        elif cited.get(number):
            grade = "cita"
            proof = ", ".join(sorted(cited[number])[:3])
        elif users:
            grade = "classe"
            proof = ", ".join(users[:3])
        else:
            grade = "nenhum"
            proof = ""

        rows.append((number, cells[0], state, grade, proof))

    return rows


def clean(text):
    text = re.sub(r"\*\*|`", "", text).replace("|", "/")

    return text if len(text) <= NAME_LENGTH else text[:NAME_LENGTH - 1].rstrip() + "…"


def render(rows):
    count = defaultdict(int)

    for row in rows:
        count[row[3]] += 1

    lines = [
        "# Regras e testes",
        "",
        "Gerado por `python scripts/rule_coverage.py`; não editar à mão. Toda suite do Sentinela Local roda todos os testes das classes",
        "abaixo: a pergunta aqui é se existe teste para a regra, não se ele rodou. Citar não prova que o teste mede a regra, e usar a",
        "classe prova menos ainda.",
        "",
        f"- **Cita a regra:** {count['cita']}",
        f"- **Só usa a classe indicada:** {count['classe']}",
        f"- **Sem nenhum teste:** {count['nenhum']}",
        f"- **Fora de vigor:** {count['fora de vigor']}",
        "",
        "| # | regra | grau | testes |",
        "| --- | --- | --- | --- |",
    ]

    for number, name, _state, grade, proof in rows:
        lines.append(f"| {number} | {clean(name)} | {grade} | {proof} |")

    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="só diz quantas regras estão sem teste; não escreve")
    args = parser.parse_args()
    rows = classify(read_rules(RULES), read_tests(ROOT))
    missing = [row[0] for row in rows if row[3] == "nenhum"]

    if not args.check:
        OUTPUT.write_text(render(rows), encoding="utf-8", newline="\n")

    print(f"{len(rows)} regras; sem teste: {len(missing)} {missing}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
