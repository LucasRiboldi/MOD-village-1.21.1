#!/usr/bin/env python3
"""Hook PostToolUse do Claude Code: avisa quando um arquivo passa de 500 linhas.

Regra do projeto (CLAUDE.md): "Keep files under 500 lines". Em 2026-09-24
havia 16 classes Java acima disso, e nenhuma cresceu de uma vez so — cada
edicao acrescentou um pouco. O aviso sai na edicao que cruza a linha, que e
quando dividir ainda e barato. Nao bloqueia: a edicao ja aconteceu.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

LIMIT = 500
WATCHED_SUFFIXES = (".java", ".py")


def main() -> int:
    try:
        event = json.load(sys.stdin)
    except json.JSONDecodeError:
        return 0

    path = Path(str(event.get("tool_input", {}).get("file_path", "")))
    if path.suffix not in WATCHED_SUFFIXES or not path.is_file():
        return 0

    lines = sum(1 for _ in path.open(encoding="utf-8", errors="replace"))
    if lines <= LIMIT:
        return 0

    message = (
        f"{path.name} tem {lines} linhas, acima do limite de {LIMIT} do projeto. "
        "Se esta edicao acrescentou uma responsabilidade nova, extraia-a para uma "
        "classe propria antes de seguir."
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PostToolUse",
            "additionalContext": message,
        }
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
