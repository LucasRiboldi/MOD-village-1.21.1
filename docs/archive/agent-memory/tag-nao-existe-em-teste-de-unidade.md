---
name: tag-nao-existe-em-teste-de-unidade
description: Bootstrap.initialize() dá registro mas não tags — MaterialChoice.forBlock(GREEN_CARPET) devolve só o verde em unitário; família por tag se testa em GameTest
metadata:
  node_type: memory
  type: feedback
  modified: 2026-09-26T04:24:22.255Z
---

Em teste de unidade com `SharedConstants.createGameVersion(); Bootstrap.initialize();` os itens existem, mas as **tags** (`wool_carpets`, `beds`, `wooden_stairs`…) não estão ligadas: elas vêm com o datapack de um mundo. `MaterialChoice.forBlock(Blocks.GREEN_CARPET)` devolveu `[green_carpet]` e eu quase afirmei ao autor que a família de tapetes não funcionava (2026-09-26). Em GameTest a mesma pergunta passa.

**Why:** a falha parecia defeito de produção e era limite do ambiente de teste.

**How to apply:** tudo que depende de `isIn(tag)` vai para GameTest. Também: busca com raio grande (lenhador 64) em GameTest acha árvores de outros testes — use lote próprio e um gancho `shortenXRadiusTo` (molde `MineDigging`). Ver [[planejador-acha-caminho-fora-do-cenario]].
