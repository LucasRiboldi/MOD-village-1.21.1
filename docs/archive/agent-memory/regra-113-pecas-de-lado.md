---
name: regra-113-pecas-de-lado
description: Obra presa só em peças de lado parava a vila inteira; Regra 113 (0.3.94) traz de volta 4× e depois faz surgir
metadata:
  node_type: memory
  type: project
  modified: 2026-10-27T14:11:51.524Z
---

Jogo da 0.3.93 (deserto, 22-10): armazém storage_majest ~2 h em "5 blocks left — no build task", nenhuma
obra nova (o vão único de obra preso), construtor 77% / lenhador 98% / pedreiro 93% ociosos. Trava de três
peças: BuildWait solta após 5 min; ensureTask não reabre sem nextBlock; UnsupportedRelease.MOST=4.

**Decisão do autor (22-10):** opção "1" + "se não tiver depois de mais 4 tentativas, o item só aparece".
Implementado 0.3.94 (PR #36, branch fix/obra-pecas-de-lado): PieceRescue (core, TRIES=4) + StuckPieces
(chamado no ConstructionPlanner antes do ensureTask) + UnreachablePieces.surge(…, SupplyException.STUCK_PIECE).
JAR 0.3.94 (7FDF6BBE…) instalado, não jogado.

**Why:** a vaga de obra é única — qualquer obra que não fecha para a vila toda ([[sem-obra-desde-0336]]).
**How to apply:** no próximo jogo, grep "brings back" e "came up in place"; se a vila ainda parar, conferir
se a obra está em WAITING_RESOURCES (StuckPieces só age em BUILDING).

Mesmo jogo, sem correção ainda: 115 teleportes noturnos "no path home", fazendeiro 15× could not reach the
crop, Regra 74 com 0 trocas (não verificado por quê), 12 UNCLASSIFIED no mineiro. KF-010 (MinerCaution) nova.
