#!/usr/bin/env python3
"""Coletor de evidência e Quality Gate do Gauntlet Loop.

Este script NÃO julga se o requisito foi atendido — isso é do
`gauntlet-verifier`. O que ele faz é a metade que precisa ser objetiva:
roda os comandos reais do projeto, lê os relatórios que eles escrevem, e
devolve um JSON com exit code, contagem de testes e falhas nominais.

A regra que dá sentido à divisão: **o agente pode rebaixar o veredito
deste script, nunca promovê-lo.** Se aqui sai FAIL, o Verifier não pode
dizer PASS. Se aqui sai PASS, ele ainda pode dizer FAIL — porque
requisito atendido, teste que mascara e caso extremo não cabem num
`subprocess`.

Camadas, na ordem em que ficam caras (§ TESTES EM CAMADAS):

    1 scope        git diff, marcadores suspeitos, regras do CLAUDE.md
    2 lint         NÃO EXISTE neste projeto — reportado como tal
    3 typecheck    compileJava + compileTestJava + compileGametestJava
    4 unit         ./gradlew test          (~670 casos, segundos)
    5 python       unittest em tests/      (só se scripts/ ou tests/ mudou)
    6 integration  ./gradlew runGametest   (~270 casos, sobe servidor, ~2 min)
    7 security     por caminho tocado — sinaliza revisão, não aprova

Para no primeiro FAIL bloqueante: gastar dois minutos de servidor depois
de o compilador ter reprovado não acrescenta evidência nenhuma.

Sem dependência fora da biblioteca padrão, como `graphify_relabel.py` —
o build é Java e não pode passar a exigir pacote de Python.

As camadas em si moram em `gauntlet_checks.py`: aqui fica a decisão.
"""


from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from gauntlet_checks import (  # noqa: E402  (depende do sys.path acima)
    DEFAULT_MAX_ITERATIONS,
    LEDGER_DIR,
    ROOT,
    JAVA_SOURCE,
    changed_files,
    gradle,
    newest_source_mtime,
    plain_layer,
    report_layer,
    run,
    scope_layer,
    security_layer,
)


def ledger_path(name: str) -> Path:
    LEDGER_DIR.mkdir(parents=True, exist_ok=True)

    return LEDGER_DIR / name


def load_ledger() -> dict:
    path = ledger_path("ledger.json")

    if path.exists():
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            pass

    return {"runs": []}


def collect(base: str, deep: bool, reviewed: str = "") -> dict:
    """As camadas, na ordem, parando no primeiro FAIL bloqueante."""
    checks: dict[str, dict] = {}

    scope = scope_layer(base)
    checks["scope"] = scope

    files = scope["files"]
    source_mtime = newest_source_mtime(files)

    checks["lint"] = {
        "status": "NOT_APPLICABLE",
        "reason": "este projeto não tem checkstyle, spotless nem PMD. As regras "
                  "que ele de fato verifica são testes: DependencyRuleTest e "
                  "ConversionBoundaryTest, na camada unit",
    }

    if scope["status"] == "FAIL":
        return {"checks": checks, "stopped_at": "scope"}

    checks["typecheck"] = plain_layer(run(
        gradle(["compileJava", "compileTestJava", "compileGametestJava"]), 900))

    if checks["typecheck"]["status"] != "PASS":
        return {"checks": checks, "stopped_at": "typecheck"}

    checks["unit_tests"] = report_layer(
        run(gradle(["test"]), 900),
        sorted((ROOT / "build" / "test-results" / "test").glob("*.xml")),
        source_mtime)

    if checks["unit_tests"]["status"] != "PASS":
        return {"checks": checks, "stopped_at": "unit_tests"}

    if any(f.startswith(("scripts/", "tests/")) for f in files):
        checks["python_tests"] = plain_layer(run(
            [sys.executable, "-m", "unittest", "discover", "-s", "tests"], 300))

        if checks["python_tests"]["status"] != "PASS":
            return {"checks": checks, "stopped_at": "python_tests"}

    if not deep:
        checks["integration_tests"] = {
            "status": "NOT_RUN",
            "reason": "rodado sem --deep. A bateria de gametest sobe um servidor "
                      "e leva minutos; ela é obrigatória antes do PASS final",
        }

        checks["security"] = security_layer(files, reviewed)

        return {"checks": checks, "stopped_at": None}

    checks["integration_tests"] = report_layer(
        run(gradle(["runGametest"]), 1800),
        [ROOT / "build" / "gametest-report.xml"],
        source_mtime)

    checks["security"] = security_layer(files, reviewed)

    return {"checks": checks, "stopped_at":
            None if checks["integration_tests"]["status"] == "PASS"
            else "integration_tests"}


def gate(checks: dict, iteration: int, max_iterations: int) -> str:
    """PASS, FAIL ou BLOCKED — e BLOCKED nunca vira PASS."""
    if iteration > max_iterations:
        return "BLOCKED"

    states = [layer.get("status") for layer in checks.values()]

    if "FAIL" in states:
        return "FAIL"

    # Camada que não decidiu não é camada que aprovou — a regra 9 do
    # pedido, e a que mais tenta se desfazer sozinha.
    if any(state in ("BLOCKED", "NOT_RUN", "REVIEW_REQUIRED") for state in states):
        return "BLOCKED"

    return "PASS"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="HEAD",
                        help="referência do git para o diff (padrão: HEAD)")
    parser.add_argument("--iteration", type=int, default=1)
    parser.add_argument("--max-iterations", type=int, default=DEFAULT_MAX_ITERATIONS)
    parser.add_argument("--deep", action="store_true",
                        help="inclui a bateria de gametest (obrigatória para PASS)")
    parser.add_argument("--requirements", default="",
                        help="o requisito original, copiado para o relatório")
    parser.add_argument("--security-reviewed", default="",
                        help="o que o Verifier conferiu na persistência ou na "
                             "proteção de bloco. Promove aquela camada, e fica "
                             "escrito no relatório")

    args = parser.parse_args(argv)

    collected = collect(args.base, args.deep, args.security_reviewed)
    checks = collected["checks"]

    status = gate(checks, args.iteration, args.max_iterations)

    report = {
        "status": status,
        "iteration": args.iteration,
        "max_iterations": args.max_iterations,
        "requirements_text": args.requirements,
        "base": args.base,
        "stopped_at": collected["stopped_at"],
        "checks": {name: {k: v for k, v in layer.items() if k != "run"}
                   for name, layer in checks.items()},
        "commands": {name: layer["run"] for name, layer in checks.items()
                     if "run" in layer},
        "note": "Evidência objetiva apenas. Requisito atendido, teste que "
                "mascara e caso extremo são julgamento do gauntlet-verifier, "
                "que pode REBAIXAR este veredito e nunca promovê-lo.",
    }

    ledger = load_ledger()
    ledger["runs"].append({
        "iteration": args.iteration,
        "at": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "status": status,
        "deep": args.deep,
        "stopped_at": collected["stopped_at"],
        "files": checks["scope"]["files"],
    })

    ledger_path("ledger.json").write_text(
        json.dumps(ledger, indent=2, ensure_ascii=False), encoding="utf-8")

    ledger_path(f"iteration-{args.iteration}.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    print(json.dumps(report, indent=2, ensure_ascii=False))

    return 0 if status == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
