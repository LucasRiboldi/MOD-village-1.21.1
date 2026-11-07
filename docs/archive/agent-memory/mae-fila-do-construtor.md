---
name: mae-fila-do-construtor
description: "0.4.38, Regra 213: decisão do autor de 2026-11-06, a obra MAE é a próxima do construtor e sempre termina; a causa da parada era grama coberta virando terra"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-06T15:53:59.495Z
---

Decisão do autor em 2026-11-06, depois de eu mostrar que a prova por seção exigia um mundo parado: "1 não impedir que a obra pare; 2 não deixar blocos soltos, sempre terminar; 3 se torna a próxima construção na fila do construtor; reduzir as dificuldades de construir". Li o item 1 como "o mundo vivo não para a obra". ADR-047.

- `MaeSites` só confere o lote e põe em `MaeBuildQueue` (no save, `maeWork.requests`); `ConstructionPlanner.plan` abre via `MaeRequestedWork` antes de reparo e busca.
- `MaeStructureSite`, `MaeStructureExecution`, `MaeTerrainExecution` e `road_v2` ficaram sem chamador na obra, com os testes. Remover é decisão do autor.
- `TestServer` roda com `RANDOM_TICK_SPEED` 0 e sem jogador: grama nunca morre em GameTest. Para testar mundo vivo, chamar `state.randomTick` na mão.
- Mutação de MAE: cuidado com Error Prone (-Werror) em expressão idêntica ou `continue` redundante; ela não compila e não prova nada.
- `integration` não pode chamar `event` (ArchUnit de ciclos): o cancelamento da obra aberta ficou em `MaeCommand`.

**Why:** a obra MAE nunca terminou em jogo, e cada correção da prova trocava de recusa.
**How to apply:** tratar como candidata até o jogo. Sem teste próprio: desistência por espera longa e `MaeSites.move`. Falta ver o construtor caminhando até lote `fora`.

**Confirmado em jogo (Spark uoFhAtB15p, 2026-11-06):** primeira obra MAE concluída, 151 de 151 peças pelo construtor, 16 min depois de abrir, com `result=done`. PR 56 integrada. Ao fechar o jogo, a porta ainda estava sem ligação completa (`toPave=21`, `isolated=10`).
