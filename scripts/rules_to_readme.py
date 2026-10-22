"""Copia as tabelas de regras do docs/RULES.md para a seção "Regras do mod" do README.

O RULES.md é a fonte única das regras; o README mostra a tabela completa (escolha do autor, 2026-10-22).
Para as duas não divergirem, a seção do README é gerada: tudo entre os marcadores REGRAS:INICIO e
REGRAS:FIM é substituído pelas seções do RULES.md de "Regras ativas" até antes de "Como ler esta tabela",
com os títulos descidos um nível.

Uso:
    python scripts/rules_to_readme.py          # atualiza o README
    python scripts/rules_to_readme.py --check  # só confere; sai com 1 se o README estiver desatualizado
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RULES = ROOT / "docs" / "RULES.md"
README = ROOT / "README.md"

START = "<!-- REGRAS:INICIO (gerado por scripts/rules_to_readme.py a partir de docs/RULES.md; não editar à mão) -->"
END = "<!-- REGRAS:FIM -->"


def rules_block(text: str) -> str:
    first = text.index("## Regras ativas")
    last = text.index("## Como ler esta tabela")
    block = text[first:last].rstrip()

    # O README já está dentro de "## Regras do mod": as seções do RULES.md descem um nível.
    lines = ["#" + line if line.startswith("## ") else line for line in block.splitlines()]

    while lines and lines[-1].strip() in ("", "---"):
        lines.pop()

    return "\n".join(lines)


def render(readme: str, block: str) -> str:
    begin = readme.index(START) + len(START)
    end = readme.index(END)

    return readme[:begin] + "\n\n" + block + "\n\n" + readme[end:]


def main() -> int:
    readme = README.read_text(encoding="utf-8")
    updated = render(readme, rules_block(RULES.read_text(encoding="utf-8")))

    if "--check" in sys.argv:
        if updated != readme:
            print("README desatualizado: rode python scripts/rules_to_readme.py")
            return 1

        print("README em dia com docs/RULES.md")
        return 0

    README.write_text(updated, encoding="utf-8", newline="\n")
    print("README atualizado a partir de docs/RULES.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
