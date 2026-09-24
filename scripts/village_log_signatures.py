"""As frases de producao que o analisador de log conta.

Separado de analyze_village_log.py em 2026-09-24, quando a lista cresceu com
as assinaturas do E47, do N1 e do N10 e o analisador passou de 500 linhas.
Cada assinatura e uma frase que o mod ja escreve; acrescentar uma aqui nao
muda o jogo.
"""

from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass(frozen=True)
class Signature:
    """Uma frase de producao e a area que deve ser investigada."""

    key: str
    where: str
    meaning: str
    pattern: re.Pattern[str]
    # Sinal de progresso (casa pronta, encalhado que saiu): conta, mas nunca
    # vira candidato a loop — repetir e o esperado.
    progress: bool = False


SIGNATURES = (
    Signature(
        "miner_no_branch_work",
        "MineClaims / MinerWork",
        "mineiro sem frente de escavacao disponivel",
        re.compile(r"\bno miner branch work\b", re.IGNORECASE),
    ),
    Signature(
        "miner_no_standing_room",
        "MineDigging",
        "mina encontrou pedra sem patamar seguro para continuar",
        re.compile(r"\bhit stone with nowhere to stand\b", re.IGNORECASE),
    ),
    Signature(
        "construction_waiting_resources",
        "BuilderWork / WaitingWork",
        "obra aguarda recurso",
        re.compile(r"\bWAITING_RESOURCES\b", re.IGNORECASE),
    ),
    Signature(
        "builder_pathing_stalled",
        "BuilderApproach / BuilderWork",
        "construtor caminha sem alcancar o proximo bloco",
        re.compile(r"\bwalking for \d+ ticks without reaching the block\b", re.IGNORECASE),
    ),
    Signature(
        "surface_worker_unreachable",
        "SurfaceGatheringWork",
        "coletor de superficie nao alcancou o alvo",
        re.compile(r"\bunable to reach\b", re.IGNORECASE),
    ),
    Signature(
        "missing_profession",
        "ColonyCycle / ProductionHands",
        "pedido sem aldeao capaz na vila",
        re.compile(r"\bno worker in the village can do it\b", re.IGNORECASE),
    ),
    Signature(
        "site_sweep_budget_exhausted",
        "RingSweep / BuildSiteScanner",
        "varredura de lote ou alvo terminou a passagem sem resposta",
        re.compile(r"\bstill sweeping\b.*\bbudget ran out before an answer\b", re.IGNORECASE),
    ),
    Signature(
        "site_sweep_restarted",
        "SweepLog / BuildSiteScanner",
        "cursor de varredura reiniciou antes de concluir uma volta",
        re.compile(r"\b(?:restarted its sweep|the sweep starts over)\b", re.IGNORECASE),
    ),
    # Assinaturas de 2026-09-24: E47, N1, N10, bau cheio e saude do servidor.
    Signature(
        "worker_stranded",
        "WorkStall / StrandedWorkers",
        "trabalhador congelou duas vezes no mesmo ponto (E47)",
        re.compile(r"\bis stranded at\b", re.IGNORECASE),
    ),
    Signature(
        "stranded_cannot_dig_out",
        "StrandedEscape",
        "encalhado sem rumo seguro para cavar a saida",
        re.compile(r"\bcannot dig out of\b", re.IGNORECASE),
    ),
    Signature(
        "construction_let_go",
        "ConstructionPlanner / WaitingWork",
        "colonia largou uma obra aberta",
        re.compile(r"\blets go of\b", re.IGNORECASE),
    ),
    Signature(
        "miner_chest_full",
        "MinerHaul / ChestDepositor",
        "mineiro cavou e nada entrou no bau (bau cheio)",
        re.compile(r"\bMiner \S+ took 0 from\b", re.IGNORECASE),
    ),
    Signature(
        "cycle_over_tick",
        "VillageDetectionHandler",
        "ciclo da colonia levou mais que um tique do servidor",
        re.compile(r"\bColony cycle took \d+ ms\b", re.IGNORECASE),
    ),
    Signature(
        "server_overloaded",
        "Minecraft (servidor)",
        "servidor atrasado: Can't keep up",
        re.compile(r"Can't keep up!", re.IGNORECASE),
    ),
    Signature(
        "log_error_line",
        "qualquer (nivel ERROR)",
        "linha de log em nivel ERROR",
        re.compile(r"/ERROR\]"),
    ),
    Signature(
        "house_finished",
        "BuilderWork",
        "casa concluida",
        re.compile(r"\bthe house is up\b", re.IGNORECASE),
        progress=True,
    ),
    Signature(
        "stranded_freed",
        "StrandedEscape",
        "encalhado saiu cavando (E47)",
        re.compile(r"\bStranded worker \S+ is out at\b", re.IGNORECASE),
        progress=True,
    ),
    Signature(
        "stairs_backfilled",
        "EscapeBackfill",
        "escada da fuga tampada (N10)",
        re.compile(r"\bfinished backfilling\b", re.IGNORECASE),
        progress=True,
    ),
    Signature(
        "supper_shared",
        "VillageMeals",
        "comida dividida para a procriacao (N1)",
        re.compile(r"\bshared supper with\b", re.IGNORECASE),
        progress=True,
    ),
)

WAITED_ITEM = re.compile(r"\bwaiting for (minecraft:[a-z0-9_]+)")
