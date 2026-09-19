#!/usr/bin/env python3
"""As assinaturas do verdict.py existem mesmo no codigo de producao.

Sem este teste o leitor de log mente de um jeito especialmente ruim: uma
assinatura escrita errado -- ou certa hoje e renomeada amanha -- faz o item
sair como NAO EXERCITADO para sempre. E o pior veredito possivel, porque
parece inofensivo: o item volta para a fila, ninguem desconfia da
ferramenta, e a fila cresce de novo pela mesma razao que ela ja tinha
crescido.

Roda sem servidor e sem jogo:
    python scripts/test_verdict.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from verdict import ITEMS, count, judge  # noqa: E402


ROOT = Path(__file__).resolve().parent.parent

SOURCE = ROOT / "src" / "main"


def production_text() -> str:
    """Todo o codigo de producao, concatenado."""
    parts = []

    for java in SOURCE.rglob("*.java"):
        parts.append(java.read_text(encoding="utf-8", errors="replace"))

    return "\n".join(parts)


def test_every_signature_exists(code: str) -> list[str]:
    """Cada trecho de assinatura aparece em algum LOGGER de producao."""
    missing = []

    for item in ITEMS:
        for mark in item.proves + item.refutes:
            if mark in code:
                continue

            # Assinatura composta: o log junta um literal com o nome de um
            # enum -- ' needs ' + type.required(). Nesse caso as duas
            # metades existem no codigo, e e isso que se confere.
            halves = [half for half in mark.split(" ") if half]

            if halves and all(half in code for half in halves):
                continue

            missing.append(f"{item.tag}: '{mark}' nao existe em src/main")

    return missing


def test_every_item_can_be_proven() -> list[str]:
    """Item sem frase de prova e item que nunca sai de NAO EXERCITADO."""
    broken = []

    for item in ITEMS:
        if not item.proves:
            broken.append(f"{item.tag}: sem frase de prova")

    return broken


def test_the_three_verdicts_are_reachable() -> list[str]:
    """Os tres vereditos saem de fato, e nao so em teoria.

    E o teste que pega o erro que eu ja cometi duas vezes em 09-18: uma
    condicao que nunca pode ocorrer passa para sempre sem medir nada. Aqui
    cada veredito e produzido com um log sintetico.
    """
    broken = []

    provable = next((i for i in ITEMS if i.proves and i.refutes), None)

    if provable is None:
        return ["nenhum item tem prova E refutacao -- nada a conferir"]

    empty = judge(provable, "log sem nada disto")

    if empty.verdict != "NAO EXERCITADO":
        broken.append(f"log vazio deu '{empty.verdict}', esperado NAO EXERCITADO")

    proven = judge(provable, provable.proves[0])

    if proven.verdict != "VERIFICADO":
        broken.append(f"so a prova deu '{proven.verdict}', esperado VERIFICADO")

    denied = judge(provable, provable.refutes[0])

    if denied.verdict != "REFUTADO":
        broken.append(f"so a refutacao deu '{denied.verdict}', esperado REFUTADO")

    return broken


def test_a_number_does_not_match_inside_another() -> list[str]:
    """`0 survived` nao casa dentro de `30 survived` -- 2026-09-19.

    O falso positivo REAL: a sessao das 02:19 escreveu
    `lot columns: 30 survived every check`, que e a linha de SUCESSO, e
    tres itens sairam REFUTADO porque a refutacao `"0 survived every
    check"` casa dentro de `"30 ..."`. Nao era um veredito errado, eram
    tres -- P0.7, P1.0 e P1.3 dividem essa assinatura.

    O teste fixa as duas metades, porque um conserto que so silencie o
    falso positivo pode ter matado a refutacao de verdade junto -- e af
    o item nunca mais sai REFUTADO, que e a falha silenciosa que este
    arquivo inteiro existe para impedir.
    """
    broken = []

    mark = "0 survived every check"

    real_success = "lot columns: 30 survived every check, 616 were turned down"

    if count(real_success, mark) != 0:
        broken.append(
            "'30 survived every check' contou como refutacao -- o falso"
            " positivo que derrubou tres itens em 09-19"
        )

    # A outra metade: o zero de verdade AINDA refuta. Sem isto o conserto
    # poderia ser "nunca casar nada", que passa neste teste e cega a
    # ferramenta.
    real_failure = "lot columns: 0 survived every check, 900 were turned down"

    if count(real_failure, mark) != 1:
        broken.append(
            "'0 survived every check' de verdade deixou de refutar -- o"
            " conserto cegou a ferramenta em vez de afina-la"
        )

    return broken


def main() -> int:
    code = production_text()

    if len(code) < 10000:
        print(f"FALHA: src/main parece vazio ({len(code)} chars) — caminho errado?")

        return 1

    failures = (
        test_every_signature_exists(code)
        + test_every_item_can_be_proven()
        + test_the_three_verdicts_are_reachable()
        + test_a_number_does_not_match_inside_another()
    )

    if failures:
        print(f"FALHA — {len(failures)} problema(s):")

        for failure in failures:
            print(f"    {failure}")

        return 1

    marks = sum(len(i.proves) + len(i.refutes) for i in ITEMS)

    print(f"OK — {len(ITEMS)} itens, {marks} assinaturas, todas existem em src/main.")
    print("OK — os tres vereditos sao alcancaveis.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
