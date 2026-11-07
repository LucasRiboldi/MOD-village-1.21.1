---
name: planejador-acha-caminho-fora-do-cenario
description: "GameTest de pathfinding com cavar/pôr — o A* acha rota que o cenário não previu (chão do mundo abaixo da arena, ponte acima do rio); feche o cenário em bedrock"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-09-25T17:06:02.854Z
---

Nos testes do DetourPlanner/DetourWalker (ADR-025, 2026-09-25), duas vezes o teste falhou porque o planejador achou um caminho correto que eu não previ: desceu num poço raso e andou pelo chão do mundo logo abaixo da arena do GameTest; e atravessou um rio erguendo uma ponte um nível acima da água.

**Why:** com cavar e pôr bloco, quase todo cenário tem saída. "Falhou" quase sempre queria dizer "cenário aberto", não defeito.

**How to apply:** feche cenários de desvio em bedrock (chão, paredes, teto), torne o fundo inalcançável, e mande o teste imprimir o caminho planejado (`DetourWalker.path()`) na mensagem de falha antes de mexer na produção. Ver [[teste-vazio-por-condicao-impossivel]].
