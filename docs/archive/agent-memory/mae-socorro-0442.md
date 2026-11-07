---
name: mae-socorro-0442
description: "0.4.42, Fase 6 do MAE no recorte do autor (socorro, só avisa, 24 blocos, recusa água); ADR-049, Regra 222; candidata"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-07T02:46:44.147Z
---

0.4.42 (2026-11-06), branch `feat/mae-socorro`, sem jogo. Escolhas do autor para a Fase 6: só no socorro (busca normal continua), só avisa no chat, até 24 blocos da caixa, recusa lote que toca água. Bairro novo fora.

`MaeRescue` conta voltas sem lote em `LotSeeking`, dispara na segunda, um lado por passada, 2 plantas e 4 lotes no máximo.

**Why:** a rota de obra automática do plano mestre foi reduzida à de menor risco, porque toda obra já é do construtor (ADR-047) e todo caminho é do grafo (ADR-048).

**How to apply:** no próximo jogo, ler `VC_MAE_SITE result=rescue` (lado, achou, ms). A 0.4.41 também não foi jogada: se a vila piorar, separar o que veio das Regras 217–221 do que veio da 222. Gate da água segue aberto só para a obra pedida pelo jogador.

Relacionado: [[vila-viva-0441]], [[mae-fila-do-construtor]].
