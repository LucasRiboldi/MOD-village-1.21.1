---
name: playtest-2026-10-03-madrugada
description: "Spark r6nErbWNSL sem lag do mod (1,9%); travamento era construtor parado fora da casa fechada e vidraça esperando areia — corrigido em 51b7accc, não visto em jogo"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-03T04:57:31.802Z
---

Playtest 2026-10-03 01:02–01:32, JAR 923D9769 (o atual na época). Spark: TPS 20, MSPT
mediano 8–12, mod 1,9% do Server thread; janelas de TPS 13–14 = recarga de resource
pack + pausa (ESC), não o mod. Nenhuma exceção do mod.

Travamento era de fluxo: o construtor escolhia o lugar de pé mais perto dele, que caía
DENTRO da casa já fechada; ficava parado fora até WorkStall (300 tiques) ou set-aside (200).
Correção: `UnreachableSpots` lê `CANT_REACH_WALK_TARGET_SINCE` (gravado pela
MoveToTargetTask Vanilla quando o path não chega) e risca o lugar em 20 tiques.
Vidraça: `TestBarrier.nothingToWaitFor` risca na hora com EmptySweeps de SAND ativo e 0 vidro.
Commit 51b7accc (pushed), JAR D2374106…0615 instalado. 579 GameTests, 3 mutações mortas.

**Why:** "BUILDER idle" no time_ledger não é obra parada — quem executa a obra pode ser
fundidor/pedreiro; olhe `builders:` e `has not moved a block`, não só o ledger.
**How to apply:** no próximo log, conte `gives up standing at` e `has not moved a block`
de Builder. Mutação no projeto: `!Boolean.getBoolean("vc.mut")` como "true" (Error Prone
recusa `true ||`). Ver [[folga-de-chegada-contra-alcance]], [[bateria-gametest-instavel]].
