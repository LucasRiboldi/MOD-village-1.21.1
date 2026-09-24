#!/usr/bin/env python3
"""Hook PreToolUse do Claude Code: nao deixa commitar codigo sem build verde.

Regra do projeto (CLAUDE.md): "ALWAYS verify build succeeds before committing".
Escrita la, ela e pedido; aqui, e garantia. O hook so age quando o comando Bash
contem `git commit` e ha mudanca em codigo ou build (`src/`, `*.gradle`,
`gradle.properties`); commit so de documentacao passa direto.

Olha a arvore de trabalho, e nao so o que esta no indice: o `git add` costuma
vir no mesmo comando, antes do commit, e ainda nao rodou quando o hook roda.

Saida 2 bloqueia o comando e devolve o motivo ao agente; 0 deixa passar.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WATCHED = ("src", "build.gradle", "settings.gradle", "gradle.properties")
DEFAULT_JAVA_HOME = r"C:\Program Files\Java\jdk-21.0.12"


def touches_code() -> bool:
    status = subprocess.run(
        ["git", "status", "--porcelain", "--", *WATCHED],
        cwd=ROOT, capture_output=True, text=True, check=False,
    )
    return bool(status.stdout.strip())


def main() -> int:
    try:
        event = json.load(sys.stdin)
    except json.JSONDecodeError:
        return 0

    command = str(event.get("tool_input", {}).get("command", ""))
    if "git commit" not in command or not touches_code():
        return 0

    env = dict(os.environ)
    env.setdefault("JAVA_HOME", DEFAULT_JAVA_HOME)
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    build = subprocess.run(
        [str(gradlew), "build", "-q"],
        cwd=ROOT, env=env, capture_output=True, text=True, check=False,
    )
    if build.returncode == 0:
        return 0

    tail = "\n".join((build.stdout + build.stderr).strip().splitlines()[-25:])
    print(
        "Commit bloqueado pelo hook do projeto: `gradlew build` falhou com mudanca "
        "de codigo na arvore. Corrija e rode o build antes de commitar.\n" + tail,
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
