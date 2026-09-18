#!/usr/bin/env python3
"""O veredito de uma sessao de jogo, item por item.

O gargalo do projeto nao e medir: a instrumentacao ja e rica -- SweepLog,
LotRefusals.accepted, o motivo de parada de cada trabalhador. O gargalo e
que ninguem le o log depois da sessao. Medido em 2026-09-18: a frase do
P1.1 aparecia 23 vezes no log da sessao anterior e ninguem a tinha lido, e
o E46 ficou dias escondido numa linha que ja estava escrita.

Este script fecha esse laco. Recebe o log e responde, por item pendente:

    VERIFICADO      a frase que PROVA apareceu, e a que refuta nao
    REFUTADO        a frase que REFUTA apareceu
    NAO EXERCITADO  nenhuma das duas -- a sessao nao tocou no assunto

O terceiro veredito e o que hoje nao existe, e e o mais util: ele diz o
que a proxima sessao precisa cobrir, em vez de deixar o item envelhecendo
numa fila indistinta.

Uso:
    python scripts/verdict.py                      # acha o log mais recente
    python scripts/verdict.py caminho/para.log
"""

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path


# Onde o Minecraft de teste guarda o log. A instalacao de teste e o
# TLauncher em AppData, separada do run/ do desenvolvimento -- as duas
# existem, e olhar a errada da "nao exercitado" para tudo.
LOG_CANDIDATES = [
    Path(os.environ.get("APPDATA", "")) / ".minecraft" / "logs" / "latest.log",
    Path.home() / "AppData" / "Roaming" / ".minecraft" / "logs" / "latest.log",
    Path("run") / "logs" / "latest.log",
]


@dataclass(frozen=True)
class Item:
    """Um item pendente e como o log responde por ele.

    `proves` e `refutes` sao trechos literais de frases que o codigo de
    producao escreve. Sao literais de proposito: uma assinatura que nao
    existe no codigo e um item que ninguem vai conseguir verificar, e isso
    precisa aparecer na hora de declara-lo -- nao uma semana depois.
    """

    tag: str
    what: str
    proves: tuple[str, ...] = ()
    refutes: tuple[str, ...] = ()
    note: str = ""
    # Quando a prova e "aconteceu mais vezes do que o defeito", em vez de
    # "aconteceu": compara as duas contagens em vez de so achar a frase.
    outweighs: bool = False


# As assinaturas saem das frases que o codigo REALMENTE escreve, conferidas
# contra src/main em 2026-09-18. Ver o teste em tests/ que as revalida.
ITEMS: list[Item] = [
    Item(
        "P0.1-b",
        "O caminho de terra nao sai de bau",
        proves=("extended the road", "grew the road"),
        note="a colonia calcou rua; o item e sobre o material NAO vir de bau",
        # <b>"found no road end it may pave" NAO refuta</b>, e a primeira
        # versao o usava: ponta em descanso e rotina — sai varias vezes
        # numa sessao em que a rua cresceu. No deserto de 09-18 deu
        # REFUTADO com 5 recusas ao lado de uma extensao de 2 blocos, que
        # e a prova de que o item funciona. O defeito seria a estrada
        # parar por falta de MATERIAL, nao por falta de ponta livre.
    ),
    Item(
        "P0.1-c",
        "A recusa de lote diz por que",
        proves=("lot refusals:", "lot columns:"),
        note="a linha existir ja e a prova: ela e a instrumentacao",
    ),
    Item(
        "P0.3",
        "Mineiro -> armazenamento -> fundidor",
        proves=("Smelter", "made"),
        refutes=("nothing in the colony chests to smelt",),
        note="o fundidor achou o cru que o mineiro guardou",
    ),
    Item(
        "P0.5",
        "Perda de item por inventario cheio (E3)",
        proves=("filled up", "is full —"),
        note="a perda passou a ser dita; silencio aqui e nao exercitado",
    ),
    Item(
        "P0.7",
        "Elegibilidade simplificada de lotes",
        proves=("survived every check",),
        refutes=("0 survived every check",),
        note="zero aprovadas e o defeito original voltando",
    ),
    Item(
        "P0.9 / E46",
        "A obra nasce condenada: a estrada leva o lote para fora",
        proves=("planned",),
        refutes=("lets go of",),
        note="obra planejada que nao e largada em seguida",
        outweighs=True,
    ),
    Item(
        "P1.0",
        "Nenhum lote aprovado: a vila nao planeja obra",
        proves=("planned", "opened a build task"),
        refutes=("0 survived every check",),
        note="alguma obra nasceu nesta sessao",
    ),
    Item(
        "P1.1",
        "A obra espera peca que ninguem fabrica",
        proves=("has what it was waiting for",),
        refutes=(" needs CRAFT_STONE", " needs CRAFT_WOOD"),
        note="a obra destravou, em vez de esperar quem nao existe",
        outweighs=True,
        # <b>A frase generica NAO serve de refutacao</b>, e a primeira
        # versao deste item usava ela: "no worker in the village can do
        # it" sai 2x por profissao numa vila que simplesmente nao tem
        # aquele trabalhador -- e "nao ha pedreiro aqui" nao e o mesmo
        # que "a peca nao tem quem faca". Deu REFUTADO com 23
        # ocorrencias que nao eram o defeito. O P1.1 e sobre a PECA da
        # planta esperando fabricacao, entao a assinatura tem de citar a
        # capacidade que fabrica.
    ),
    Item(
        "P0.8 / E45",
        "A mina fica presa na boca",
        proves=("went one level deeper",),
        note="a mina desceu de nivel; o defeito era esse numero ser ZERO",
        # <b>"is out of reach" NAO refuta</b>, e a primeira versao o usava:
        # pedra inalcancavel e rotina de mineracao, sai as dezenas numa
        # sessao saudavel. O defeito do E45 era 17.518 recusas com ZERO
        # pedra quebrada e ZERO descidas -- a assinatura e a AUSENCIA de
        # "went one level deeper", nao a presenca de recusas. Com 29
        # descidas medidas em 09-18, o item esta verificado.
    ),
    Item(
        "09-18",
        "A vila levanta casas diferentes",
        proves=("drawn from",),
        note="a linha do sorteio diz de quantas plantas a casa saiu",
    ),
]


@dataclass
class Result:
    item: Item
    verdict: str
    proof: int = 0
    against: int = 0
    detail: list[str] = field(default_factory=list)


def count(text: str, needle: str) -> int:
    return text.count(needle)


def judge(item: Item, text: str) -> Result:
    proof = sum(count(text, mark) for mark in item.proves)
    against = sum(count(text, mark) for mark in item.refutes)

    if item.outweighs:
        # "Aconteceu mais que o defeito" -- para os casos em que as duas
        # frases convivem numa sessao saudavel, e o que importa e qual
        # domina. Sem isto o E46 sairia REFUTADO por uma unica obra larga
        # numa sessao em que vinte subiram.
        if proof == 0 and against == 0:
            return Result(item, "NAO EXERCITADO")

        verdict = "VERIFICADO" if proof > against else "REFUTADO"

        return Result(item, verdict, proof, against)

    if against:
        return Result(item, "REFUTADO", proof, against)

    if proof:
        return Result(item, "VERIFICADO", proof, against)

    return Result(item, "NAO EXERCITADO")


def find_log(argv: list[str]) -> Path | None:
    if len(argv) > 1:
        given = Path(argv[1])

        return given if given.is_file() else None

    for candidate in LOG_CANDIDATES:
        if candidate.is_file():
            return candidate

    return None


def session_window(text: str) -> str:
    stamps = re.findall(r"^\[(\d{2}:\d{2}:\d{2})\]", text, re.MULTILINE)

    return f"{stamps[0]} -> {stamps[-1]}" if stamps else "sem marca de tempo"


def main() -> int:
    log = find_log(sys.argv)

    if log is None:
        print("Nao achei o log da sessao. Passe o caminho como argumento.")
        print("Procurei em:")

        for candidate in LOG_CANDIDATES:
            print(f"    {candidate}")

        return 2

    text = log.read_text(encoding="utf-8", errors="replace")

    results = [judge(item, text) for item in ITEMS]

    print(f"Log:    {log}")
    print(f"Sessao: {session_window(text)}  ({len(text.splitlines())} linhas)")
    print()

    width = max(len(r.item.tag) for r in results)

    for result in results:
        counts = ""

        if result.proof or result.against:
            counts = f"  (prova {result.proof} / contra {result.against})"

        print(f"  {result.item.tag:<{width}}  {result.verdict:<14}{counts}")
        print(f"  {'':<{width}}  {result.item.what}")

        if result.verdict != "VERIFICADO" and result.item.note:
            print(f"  {'':<{width}}  esperado: {result.item.note}")

        print()

    tally = {"VERIFICADO": 0, "REFUTADO": 0, "NAO EXERCITADO": 0}

    for result in results:
        tally[result.verdict] += 1

    print(
        f"{tally['VERIFICADO']} verificados, "
        f"{tally['REFUTADO']} refutados, "
        f"{tally['NAO EXERCITADO']} nao exercitados."
    )

    if tally["NAO EXERCITADO"]:
        print()
        print("Os nao exercitados dizem o que a proxima sessao precisa cobrir —")
        print("nao sao itens reprovados, sao itens que a sessao nem tocou.")

    # Sai 0 sempre: isto e um relatorio, nao um portao. Um script que
    # falha o build por causa do que o jogador fez ou deixou de fazer
    # seria ruido, e ruido ensina a ignorar a ferramenta.
    return 0


if __name__ == "__main__":
    sys.exit(main())
